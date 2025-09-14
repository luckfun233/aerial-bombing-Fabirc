package com.aerial.bombing;

import com.aerial.bombing.config.ModConfig;
import com.aerial.bombing.entity.MissileData;
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

        // 检查物品是否为导弹
        NbtCompound nbt = mainHandStack.getNbt();
        boolean isMissile = nbt != null && nbt.getBoolean("is_missile");

        if (isMissile) {
            return executeMissileLaunch(player, world, mainHandStack, config, nbt);
        } else {
            return executeAdvancedBombing(player, world, mainHandStack, config);
        }
    }

    private boolean executeAdvancedBombing(PlayerEntity player, World world, ItemStack tntStack, ModConfig config) {
        // ... (原有的普通投弹逻辑，无需修改)
        if (world.isClient) {
            return true;
        }

        Identifier itemIdentifier = Registries.ITEM.getId(tntStack.getItem());
        Identifier entityIdentifier = new Identifier(itemIdentifier.getNamespace(), itemIdentifier.getPath());

        Optional<EntityType<?>> entityTypeOptional = EntityType.get(entityIdentifier.toString());

        if (entityTypeOptional.isEmpty()) {
            LOGGER.warn("未能为物品 {} 找到对应的实体类型 {}，将使用原版TNT作为备用。", itemIdentifier, entityIdentifier);
            entityTypeOptional = Optional.of(EntityType.TNT);
        }

        EntityType<?> entityType = entityTypeOptional.get();
        Entity spawnedEntity = entityType.create(world);

        if (spawnedEntity == null) {
            LOGGER.error("无法创建实体 {}！", entityIdentifier);
            return false;
        }

        if (!player.isCreative()) {
            tntStack.decrement(1);
        }

        lastBombTime.put(player.getUuid(), System.currentTimeMillis());

        BombMotionState motionState;
        if (config.useAdvancedPhysics) {
            motionState = AdvancedMomentumCalculator.calculateAdvancedMomentum(player, config.advancedPhysics);
        } else {
            Vec3d position = MomentumCalculator.calculateDropPosition(player);
            Vec3d velocity = MomentumCalculator.calculateRealisticMomentum(player);
            motionState = new BombMotionState(position, velocity, Vec3d.ZERO);
        }

        spawnedEntity.setPosition(motionState.position);
        spawnedEntity.setVelocity(motionState.velocity);

        if (spawnedEntity instanceof TntEntity tnt) {
            tnt.setFuse(80);
            ((TntEntityOwner) tnt).setOwner(player);
        } else if (spawnedEntity instanceof TntEntityOwner ownerTnt) {
            ownerTnt.setOwner(player);
        }

        world.spawnEntity(spawnedEntity);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1.0F, 1.0F);
        LOGGER.info("玩家 {} 投下了一个 {} 实体。", player.getName().getString(), entityIdentifier);

        return true;
    }

    private boolean executeMissileLaunch(PlayerEntity player, World world, ItemStack tntStack, ModConfig config, NbtCompound missileNbt) {
        if (world.isClient) {
            return true;
        }

        Identifier itemIdentifier = Registries.ITEM.getId(tntStack.getItem());
        Identifier entityIdentifier = new Identifier(itemIdentifier.getNamespace(), itemIdentifier.getPath());
        EntityType<?> entityType = EntityType.get(entityIdentifier.toString()).orElse(EntityType.TNT);
        Entity spawnedEntity = entityType.create(world);

        if (!(spawnedEntity instanceof TntEntity)) {
            LOGGER.warn("物品 {} 对应的实体不是 TntEntity，无法作为导弹发射。将作为普通炸弹投掷。", itemIdentifier);
            return executeAdvancedBombing(player, world, tntStack, config);
        }

        if (!player.isCreative()) {
            tntStack.decrement(1);
        }

        lastBombTime.put(player.getUuid(), System.currentTimeMillis());

        // 设置导弹初始位置和速度 (发射时，应该从玩家正前方发射)
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1f);
        spawnedEntity.setPosition(eyePos.add(lookVec.multiply(1.5)));
        // 初始速度是玩家速度+一个发射初速度
        spawnedEntity.setVelocity(player.getVelocity().add(lookVec.multiply(0.5)));

        // --- 关键步骤: 将导弹数据从 ItemStack 传递给 TntEntity ---
        if (spawnedEntity instanceof MissileData missile) {
            missile.setMissile(true);
            // 将秒转换为 tick (1秒=20ticks)
            missile.setFlightDurationTicks(missileNbt.getInt("flight_duration_sec") * 20);
            missile.setInstantExplosion(missileNbt.getBoolean("instant_explosion"));
        }

        // 设置所有者
        if (spawnedEntity instanceof TntEntityOwner ownerTnt) {
            ownerTnt.setOwner(player);
        }

        world.spawnEntity(spawnedEntity);
        // 发射时使用不同的声音
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.5F, 1.0F);
        LOGGER.info("玩家 {} 发射了一枚 {} 导弹。", player.getName().getString(), entityIdentifier);

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
