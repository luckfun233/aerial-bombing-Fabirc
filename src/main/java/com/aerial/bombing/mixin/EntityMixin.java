package com.aerial.bombing.mixin;

import com.aerial.bombing.entity.TntEntityOwner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("EntityMixinForMissile");
    @Unique
    private static final double MISSILE_SPEED_PER_TICK = 2.0; // 2.0 blocks/tick = 40 m/s

    @Shadow public World world;
    @Shadow public abstract NbtCompound writeNbt(NbtCompound nbt);
    @Shadow public abstract void readNbt(NbtCompound nbt);
    @Shadow public abstract Vec3d getVelocity();
    @Shadow public abstract void setVelocity(Vec3d velocity);
    @Shadow public abstract void move(MovementType movementType, Vec3d movement);
    @Shadow public abstract Vec3d getRotationVec(float tickDelta);
    @Shadow public abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float getYaw();
    @Shadow public abstract float getPitch();
    @Shadow public abstract void setNoGravity(boolean noGravity);
    @Shadow public abstract double getX();
    @Shadow public abstract double getY();
    @Shadow public abstract double getZ();
    @Shadow public boolean horizontalCollision;
    @Shadow public boolean verticalCollision;
    @Shadow public abstract void discard();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        // 从实体的持久化NBT数据中读取导弹标记
        NbtCompound persistentNbt = this.writeNbt(new NbtCompound());
        if (!persistentNbt.getBoolean("IsMissile")) {
            return;
        }

        // 获取导弹数据
        int flightDurationTicks = persistentNbt.getInt("FlightDuration");
        boolean propulsionFinished = persistentNbt.getBoolean("PropulsionFinished");
        boolean isInstantExplosion = persistentNbt.getBoolean("InstantExplosion");

        if (propulsionFinished) {
            return;
        }

        // 更新飞行时间
        flightDurationTicks--;
        setNoGravity(true);

        // 粒子效果
        if (!this.world.isClient && this.world instanceof ServerWorld serverWorld) {
            Vec3d trailOffset = this.getVelocity().normalize().multiply(-0.3);
            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 0.25, this.getZ(),
                    5,
                    trailOffset.x, trailOffset.y, trailOffset.z,
                    0.15);
        }

        // 平滑飞行逻辑
        Vec3d direction = this.getVelocity().normalize();
        if (direction.lengthSquared() < 0.1) {
            direction = this.getRotationVec(1.0f);
        }
        this.setVelocity(direction.multiply(MISSILE_SPEED_PER_TICK));

        this.move(MovementType.SELF, this.getVelocity());
        this.setRotation(this.getYaw(), this.getPitch());

        boolean collided = this.horizontalCollision || this.verticalCollision;
        boolean timeUp = flightDurationTicks <= 0;

        if (collided || timeUp) {
            propulsionFinished = true; // 标记推进结束
            setNoGravity(false);

            // 触发爆炸
            // 这是最通用的方法：在实体当前位置创建一个爆炸
            // 并移除原实体，模拟爆炸效果
            float explosionPower = 4.0F; // 默认为TNT的威力
            if (this instanceof TntEntityOwner ownerInterface) { // 如果实体支持，尝试获取所有者
                world.createExplosion(null, getX(), getY(), getZ(), explosionPower, World.ExplosionSourceType.TNT);
            } else {
                world.createExplosion(null, getX(), getY(), getZ(), explosionPower, World.ExplosionSourceType.TNT);
            }
            this.discard(); // 移除实体

            LOGGER.info("通用导弹逻辑触发爆炸！原因: {}", collided ? "碰撞" : "时间结束");
            // 不需要再更新NBT，因为实体马上被移除
            return; // 结束处理
        }

        // 将更新后的数据写回NBT
        persistentNbt.putInt("FlightDuration", flightDurationTicks);
        persistentNbt.putBoolean("PropulsionFinished", propulsionFinished);
        this.readNbt(persistentNbt);

        ci.cancel(); // 接管实体运动，取消原版tick逻辑
    }
}
