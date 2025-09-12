package com.aerial.bombing;

import com.aerial.bombing.config.ModConfig;
import com.aerial.bombing.entity.ModTntEntity;
import com.aerial.bombing.physics.AdvancedMomentumCalculator;
import com.aerial.bombing.physics.BombMotionState;
import com.aerial.bombing.util.TntValidator;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AerialBombingManager {
    private static AerialBombingManager INSTANCE;

    private final Map<UUID, Long> lastBombTime = new HashMap<>();

    public static AerialBombingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AerialBombingManager();
        }
        return INSTANCE;
    }

    public void initialize() {
        // 注册服务器刻事件以清理过期数据
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        // 定期清理过期的冷却时间记录
        long currentTime = System.currentTimeMillis();
        lastBombTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > 10000);
    }

    /**
     * 尝试执行空中投弹
     * @param player 玩家
     * @param world 世界
     * @return 是否成功投弹
     */
    public boolean tryAerialBombing(PlayerEntity player, World world) {
        // 检查配置是否启用
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.enableAerialBombing) {
            return false;
        }

        // 检查玩家是否在鞘翅飞行
        if (!player.isFallFlying()) {
            if (world.isClient) {
                player.sendMessage(Text.translatable("text.aerial_bombing.requires_elytra"), true);
            }
            return false;
        }

        // 检查主手是否持有有效的TNT
        ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!TntValidator.isValidTnt(mainHandStack)) {
            // 提示玩家需要在主手持有TNT
            if (world.isClient) {
                player.sendMessage(Text.translatable("text.aerial_bombing.requires_tnt"), true);
            }
            return false;
        }

        // 检查是否需要打火石
        if (config.requireFlintAndSteel) {
            ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
            if (offHandStack.getItem() != Items.FLINT_AND_STEEL) {
                if (world.isClient) {
                    player.sendMessage(Text.translatable("text.aerial_bombing.requires_flint"), true);
                }
                return false;
            }
        }

        // 检查冷却时间
        UUID playerId = player.getUuid();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastBombTime.getOrDefault(playerId, 0L);

        if (currentTime - lastTime < config.bombCooldownMs) {
            // 冷却中，不执行投弹
            return false;
        }

        // 执行投弹
        return executeAdvancedBombing(player, world, mainHandStack, config);
    }

    /**
     * 执行高级物理投弹
     * @param player 玩家
     * @param world 世界
     * @param tntStack TNT物品
     * @param config 配置
     * @return 是否成功
     */
    private boolean executeAdvancedBombing(PlayerEntity player, World world, ItemStack tntStack, ModConfig config) {
        // 消耗一个TNT
        tntStack.decrement(1);

        // 记录投弹时间
        lastBombTime.put(player.getUuid(), System.currentTimeMillis());

        // 使用高级物理计算投弹动量
        BombMotionState motionState = AdvancedMomentumCalculator.calculateAdvancedMomentum(
                player, config.advancedPhysics);

        // 创建真实的TNT实体
        if (!world.isClient) {
            // 使用正确的构造函数创建TNT实体
            ModTntEntity tntEntity = new ModTntEntity(
                    world,
                    motionState.position.x,
                    motionState.position.y,
                    motionState.position.z,
                    player
            );

            // 设置炸弹的初始速度
            tntEntity.setVelocity(
                    motionState.velocity.x,
                    motionState.velocity.y,
                    motionState.velocity.z
            );

            // 设置TNT的Fuse时长（与原版一致）
            tntEntity.setFuse(80);

            world.spawnEntity(tntEntity);
        }

        // 播放声音
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1.0F, 1.0F);

        return true;
    }

    /**
     * 检查玩家是否可以投弹（用于按键绑定）
     * @param player 玩家
     * @return 是否可以投弹
     */
    public boolean canPlayerBomb(PlayerEntity player) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.enableAerialBombing) {
            return false;
        }

        if (!player.isFallFlying()) {
            return false;
        }

        ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!TntValidator.isValidTnt(mainHandStack)) {
            return false;
        }

        if (config.requireFlintAndSteel) {
            ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
            return offHandStack.getItem() == Items.FLINT_AND_STEEL;
        }

        return true;
    }
}
