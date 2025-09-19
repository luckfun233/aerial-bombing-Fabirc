package com.aerial.bombing;

import com.aerial.bombing.entity.MissileDataComponent;
import com.aerial.bombing.util.ExplosionUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GenericMissileManager {
    private static GenericMissileManager INSTANCE;
    public static final Logger LOGGER = LoggerFactory.getLogger("GenericMissileManager");
    private static final double MISSILE_SPEED_PER_TICK = 2.0;

    private final ConcurrentHashMap<UUID, Boolean> activeMissiles = new ConcurrentHashMap<>();

    public static GenericMissileManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GenericMissileManager();
        }
        return INSTANCE;
    }

    public void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    public void registerMissile(Entity entity) {
        if (entity != null) {
            activeMissiles.put(entity.getUuid(), true);
            LOGGER.info("通用导弹管理器已接管实体: {} (UUID: {})", entity.getType().getUntranslatedName(), entity.getUuid());
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (activeMissiles.isEmpty()) {
            return;
        }

        for (ServerWorld world : server.getWorlds()) {
            Iterator<UUID> iterator = activeMissiles.keySet().iterator();
            while (iterator.hasNext()) {
                UUID entityUuid = iterator.next();
                Entity entity = world.getEntity(entityUuid);

                if (entity == null || entity.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                MissileDataComponent missileData = entity.getAttached(ModAttachments.MISSILE_DATA);

                if (missileData == null || missileData.propulsionFinished) {
                    if (entity.hasNoGravity()) {
                        entity.setNoGravity(false);
                    }
                    iterator.remove();
                    continue;
                }

                // --- 物理控制进行中 ---
                missileData.flightDurationTicks--;

                if (!entity.hasNoGravity()) {
                    entity.setNoGravity(true);
                }

                if (!missileData.physicsApplied && missileData.initialVelocity != null) {
                    entity.setVelocity(missileData.initialVelocity);
                    missileData.physicsApplied = true;
                }

                // --- 核心改动：使用 move() 并检查原生碰撞标志 ---
                Vec3d direction = entity.getVelocity().normalize();
                if (direction.lengthSquared() < 1.0E-4) {
                    direction = entity.getRotationVec(1.0f);
                }
                Vec3d newVelocity = direction.multiply(MISSILE_SPEED_PER_TICK);

                // 1. 设置我们期望的速度
                entity.setVelocity(newVelocity);

                // 2. 调用原版 move 方法，让游戏引擎处理移动和碰撞
                entity.move(net.minecraft.entity.MovementType.SELF, entity.getVelocity());

                // 3. 读取游戏引擎设置的碰撞标志
                boolean collided = entity.horizontalCollision || entity.verticalCollision;
                boolean timeUp = missileData.flightDurationTicks <= 0;

                // 为了调试，我们每一tick都打印状态
                LOGGER.debug("导弹 {} | 剩余时间: {} | 是否碰撞: {} (H: {}, V: {})", entity.getType().getUntranslatedName(), missileData.flightDurationTicks, collided, entity.horizontalCollision, entity.verticalCollision);

                // --- 发射粒子效果（不影响物理） ---
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        entity.getX(), entity.getY() + entity.getHeight() / 2.0, entity.getZ(),
                        5, 0, 0, 0, 0.05);

                if (collided || timeUp) {
                    missileData.propulsionFinished = true; // 标记推进结束

                    if (collided || missileData.isInstantExplosion) {
                        LOGGER.info("通用导弹 {} 触发引爆 (原因 - 碰撞: {}, 时间耗尽: {}, 瞬爆开启: {})", entity.getType().getUntranslatedName(), collided, timeUp, missileData.isInstantExplosion);
                        ExplosionUtils.triggerExplosion(entity);
                    } else {
                        LOGGER.info("通用导弹 {} 飞行时间结束，将转为自由落体。", entity.getType().getUntranslatedName());
                        // 标记为结束后，下一tick的循环会自动为它恢复重力并停止管理
                    }
                }
            }
        }
    }
}
