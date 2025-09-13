package com.aerial.bombing;

import com.aerial.bombing.config.ModConfig;
import com.aerial.bombing.entity.TntEntityOwner;
import com.aerial.bombing.physics.AdvancedMomentumCalculator;
import com.aerial.bombing.physics.BombMotionState;
import com.aerial.bombing.physics.MomentumCalculator;
import com.aerial.bombing.util.TntValidator;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AerialBombingManager {
    private static AerialBombingManager INSTANCE;
    public static final Logger LOGGER = LoggerFactory.getLogger("AerialBombingManager");

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
     * 执行高级物理投弹 - 已重构以兼容其他模组
     * @param player 玩家
     * @param world 世界
     * @param tntStack TNT物品
     * @param config 配置
     * @return 是否成功
     */
    private boolean executeAdvancedBombing(PlayerEntity player, World world, ItemStack tntStack, ModConfig config) {
        if (world.isClient) {
            return true; // 客户端只负责发送请求，不执行逻辑
        }

        // 从物品ID推断实体ID
        Identifier itemIdentifier = Registries.ITEM.getId(tntStack.getItem());
        // 大多数模组遵循 item id 和 entity id 相同的惯例
        Identifier entityIdentifier = new Identifier(itemIdentifier.getNamespace(), itemIdentifier.getPath());

        Optional<EntityType<?>> entityTypeOptional = EntityType.get(entityIdentifier.toString());

        if (entityTypeOptional.isEmpty()) {
            LOGGER.warn("未能为物品 {} 找到对应的实体类型 {}，将使用原版TNT作为备用。", itemIdentifier, entityIdentifier);
            // 如果找不到，就退回到生成原版TNT
            entityTypeOptional = Optional.of(EntityType.TNT);
        }

        EntityType<?> entityType = entityTypeOptional.get();
        Entity spawnedEntity = entityType.create(world);

        if (spawnedEntity == null) {
            LOGGER.error("无法创建实体 {}！", entityIdentifier);
            return false;
        }

        // 消耗一个TNT
        if (!player.isCreative()) {
            tntStack.decrement(1);
        }

        // 记录投弹时间
        lastBombTime.put(player.getUuid(), System.currentTimeMillis());

        // 根据配置选择物理模拟器
        BombMotionState motionState;
        if (config.useAdvancedPhysics) {
            // 使用高级物理计算
            motionState = AdvancedMomentumCalculator.calculateAdvancedMomentum(player, config.advancedPhysics);
        } else {
            // 使用你的原始物理计算
            Vec3d position = MomentumCalculator.calculateDropPosition(player);
            Vec3d velocity = MomentumCalculator.calculateRealisticMomentum(player);
            motionState = new BombMotionState(position, velocity, Vec3d.ZERO); // 原始模拟不含角速度
        }

        // 设置实体位置
        spawnedEntity.setPosition(motionState.position);

        // 设置实体初始速度 (关键步骤：应用物理模拟结果)
        spawnedEntity.setVelocity(motionState.velocity);

        // 如果是TntEntity或其子类，进行点燃并设置所有者
        if (spawnedEntity instanceof TntEntity tnt) {
            tnt.setFuse(80); // 原版TNT的点燃时间
            ((TntEntityOwner) tnt).setOwner(player); // 使用Mixin接口设置所有者
        } else if (spawnedEntity instanceof TntEntityOwner ownerTnt) {
            // 兼容实现了我们接口但不是TntEntity的实体（不太可能但保险）
            ownerTnt.setOwner(player);
        }

        // 在世界中生成实体
        world.spawnEntity(spawnedEntity);

        // 播放声音
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("玩家 {} 投下了一个 {} 实体。", player.getName().getString(), entityIdentifier);

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
