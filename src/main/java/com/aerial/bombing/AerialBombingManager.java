package com.aerial.bombing;

import com.aerial.bombing.config.ModConfig;
import com.aerial.bombing.entity.MissileData;
import com.aerial.bombing.entity.MissileDataComponent;
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
import net.minecraft.nbt.NbtCompound;
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
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        long currentTime = System.currentTimeMillis();
        lastBombTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > 10000);
    }

    public boolean tryAerialBombing(PlayerEntity player, World world) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.enableAerialBombing) {
            return false;
        }

        if (!player.isFallFlying()) {
            if (world.isClient) {
                player.sendMessage(Text.translatable("text.aerial_bombing.requires_elytra"), true);
            }
            return false;
        }

        ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
        if (!TntValidator.isValidTnt(mainHandStack)) {
            if (world.isClient) {
                player.sendMessage(Text.translatable("text.aerial_bombing.requires_tnt"), true);
            }
            return false;
        }

        if (config.requireFlintAndSteel) {
            ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
            if (offHandStack.getItem() != Items.FLINT_AND_STEEL) {
                if (world.isClient) {
                    player.sendMessage(Text.translatable("text.aerial_bombing.requires_flint"), true);
                }
                return false;
            }
        }

        UUID playerId = player.getUuid();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastBombTime.getOrDefault(playerId, 0L);

        if (currentTime - lastTime < config.bombCooldownMs) {
            return false;
        }

        // --- 核心逻辑重构：先计算物理，再执行发射 ---

        // 1. 根据配置选择物理引擎并计算初始运动状态
        BombMotionState motionState;
        if (config.useAdvancedPhysics) {
            LOGGER.info("使用高级物理模拟引擎计算投掷/发射轨迹。");
            motionState = AdvancedMomentumCalculator.calculateAdvancedMomentum(player, config.advancedPhysics);
        } else {
            LOGGER.info("使用标准物理模拟引擎计算投掷/发射轨迹。");
            Vec3d position = MomentumCalculator.calculateDropPosition(player);
            Vec3d velocity = MomentumCalculator.calculateRealisticMomentum(player);
            motionState = new BombMotionState(position, velocity, Vec3d.ZERO);
        }

        NbtCompound nbt = mainHandStack.getNbt();
        boolean isMissile = nbt != null && nbt.getBoolean("is_missile");

        if (isMissile) {
            return executeMissileLaunch(player, world, mainHandStack, nbt, motionState);
        } else {
            return executeAdvancedBombing(player, world, mainHandStack, motionState);
        }
    }

    private boolean executeAdvancedBombing(PlayerEntity player, World world, ItemStack tntStack, BombMotionState motionState) {
        if (world.isClient) {
            return true;
        }

        Identifier itemIdentifier = Registries.ITEM.getId(tntStack.getItem());
        Identifier entityIdentifier = new Identifier(itemIdentifier.getNamespace(), itemIdentifier.getPath());

        Entity spawnedEntity = EntityType.get(entityIdentifier.toString()).map(type -> type.create(world)).orElse(null);
        if (spawnedEntity == null) {
            spawnedEntity = EntityType.TNT.create(world); // 备用方案
            LOGGER.warn("未能为物品 {} 找到对应的实体类型 {}，将使用原版TNT作为备用。", itemIdentifier, entityIdentifier);
        }

        if (!player.isCreative()) {
            tntStack.decrement(1);
        }

        lastBombTime.put(player.getUuid(), System.currentTimeMillis());

        // 应用物理模拟计算出的位置和速度
        spawnedEntity.setPosition(motionState.position);
        spawnedEntity.setVelocity(motionState.velocity);

        if (spawnedEntity instanceof TntEntity tnt) {
            tnt.setFuse(80);
        }

        // 尝试设置所有者
        if (spawnedEntity instanceof TntEntityOwner ownerTnt) {
            ownerTnt.setOwner(player);
        }

        world.spawnEntity(spawnedEntity);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("玩家 {} 投下了一个 {} 实体。", player.getName().getString(), entityIdentifier);

        return true;
    }

    private boolean executeMissileLaunch(PlayerEntity player, World world, ItemStack tntStack, NbtCompound missileNbt, BombMotionState motionState) {
        if (world.isClient) {
            return true;
        }

        Identifier itemIdentifier = Registries.ITEM.getId(tntStack.getItem());
        Identifier entityIdentifier = new Identifier(itemIdentifier.getNamespace(), itemIdentifier.getPath());
        Entity spawnedEntity = EntityType.get(entityIdentifier.toString()).map(type -> type.create(world)).orElse(null);

        if (spawnedEntity == null) {
            LOGGER.error("发射失败: 无法为导弹创建实体 {}！", entityIdentifier);
            return false;
        }

        if (!player.isCreative()) {
            tntStack.decrement(1);
        }

        lastBombTime.put(player.getUuid(), System.currentTimeMillis());

        // 应用物理模拟计算出的位置和速度
        spawnedEntity.setPosition(motionState.position);

        if (spawnedEntity.getClass() == TntEntity.class) {
            LOGGER.info("[路径 A' - Mixin] 检测到纯原版 TntEntity。将使用 TntEntityMixin 进行物理控制。");
            spawnedEntity.setVelocity(motionState.velocity);

            MissileData missile = (MissileData) spawnedEntity;
            missile.setMissile(true);
            missile.setFlightDurationTicks(missileNbt.getInt("flight_duration_sec") * 20);
            missile.setInstantExplosion(missileNbt.getBoolean("instant_explosion"));

            ((TntEntityOwner) spawnedEntity).setOwner(player);

        } else {
            LOGGER.info("[路径 B - 通用] 检测到自定义实体 {}。将强制使用附件系统和 GenericMissileManager。", entityIdentifier);

            MissileDataComponent data = spawnedEntity.getAttachedOrCreate(ModAttachments.MISSILE_DATA);
            data.flightDurationTicks = missileNbt.getInt("flight_duration_sec") * 20;
            data.isInstantExplosion = missileNbt.getBoolean("instant_explosion");
            data.propulsionFinished = false;
            data.initialVelocity = motionState.velocity;
            data.physicsApplied = false;

            GenericMissileManager.getInstance().registerMissile(spawnedEntity);
        }

        world.spawnEntity(spawnedEntity);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.5F, 1.0F);
        LOGGER.info("玩家 {} 成功发射了一枚 {} 导弹。", player.getName().getString(), entityIdentifier);

        return true;
    }

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
