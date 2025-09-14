package com.aerial.bombing.mixin;

import com.aerial.bombing.entity.MissileData;
import com.aerial.bombing.entity.TntEntityOwner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(TntEntity.class)
public abstract class TntEntityMixin extends Entity implements TntEntityOwner, MissileData {

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("TntEntityMixin");
    @Unique private static final double MISSILE_SPEED_PER_TICK = 2.0; // 2.0 blocks/tick = 40 m/s

    @Shadow public abstract int getFuse();
    @Shadow public abstract void setFuse(int fuse);

    @Unique @Nullable private UUID ownerUuid;
    @Unique private boolean isMissile = false;
    @Unique private int flightDurationTicks = 0;
    @Unique private boolean isInstantExplosion = false;
    @Unique private boolean propulsionFinished = false;

    public TntEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (!this.isMissile || this.propulsionFinished) {
            return;
        }

        this.flightDurationTicks--;
        setNoGravity(true);

        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d trailOffset = this.getVelocity().normalize().multiply(-0.3);
            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 0.25, this.getZ(),
                    5,
                    trailOffset.x, trailOffset.y, trailOffset.z,
                    0.15);
        }

        // --- 终极平滑飞行逻辑 V2 (基于恒定速度向量) ---
        // 这种方法不依赖于上一刻的速度进行累加，而是每一刻都重新设定标准速度。
        // 这使得客户端和服务器之间的位置同步更加稳定，能有效消除瞬移感。
        Vec3d direction = this.getVelocity().normalize();
        // 如果向量无效 (例如速度为0)，则使用实体的朝向
        if (direction.lengthSquared() < 0.1) {
            direction = this.getRotationVec(1.0f);
        }
        this.setVelocity(direction.multiply(MISSILE_SPEED_PER_TICK));

        this.move(MovementType.SELF, this.getVelocity());
        // 确保实体朝向与飞行方向一致
        this.setRotation(this.getYaw(), this.getPitch());

        boolean collided = this.horizontalCollision || this.verticalCollision;
        boolean timeUp = this.flightDurationTicks <= 0;

        if (collided || timeUp) {
            this.propulsionFinished = true;
            setNoGravity(false);

            if (collided) {
                LOGGER.info("导弹碰撞，触发引爆！剩余飞行时间: {} ticks", this.flightDurationTicks);
                this.setFuse(1);
            } else { // timeUp
                if (this.isInstantExplosion) {
                    LOGGER.info("导弹飞行时间结束，瞬爆模式触发引爆！");
                    this.setFuse(1);
                } else {
                    LOGGER.info("导弹飞行时间结束，转入常规下落模式。");
                    this.setFuse(20);
                }
            }
            return;
        }

        ci.cancel();
    }

    // (NBT 和其他接口方法保持不变)
    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.getBoolean("IsMissile")) {
            this.isMissile = true;
            this.flightDurationTicks = nbt.getInt("FlightDuration");
            this.isInstantExplosion = nbt.getBoolean("InstantExplosion");
            this.propulsionFinished = nbt.getBoolean("PropulsionFinished");

            LOGGER.info("TntEntity 被配置为导弹！飞行时间: {}, 瞬爆: {}", this.flightDurationTicks, this.isInstantExplosion);
        }
        if (nbt.containsUuid("AerialBombingOwner")) {
            this.ownerUuid = nbt.getUuid("AerialBombingOwner");
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void writeMissileDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.ownerUuid != null) {
            nbt.putUuid("AerialBombingOwner", this.ownerUuid);
        }
        if (this.isMissile) {
            nbt.putBoolean("IsMissile", true);
            nbt.putInt("FlightDuration", this.flightDurationTicks);
            nbt.putBoolean("InstantExplosion", this.isInstantExplosion);
            nbt.putBoolean("PropulsionFinished", this.propulsionFinished);
        }
    }

    @Override @Unique public @Nullable UUID getOwnerUuid() { return this.ownerUuid; }
    @Override @Unique public void setOwnerUuid(@Nullable UUID ownerUuid) { this.ownerUuid = ownerUuid; }
    @Override @Unique public @Nullable PlayerEntity getPlayerOwner() { if (this.ownerUuid != null && !this.getWorld().isClient()) { return this.getWorld().getPlayerByUuid(this.ownerUuid); } return null; }
    @Override @Unique public void setOwner(@Nullable PlayerEntity owner) { if (owner != null) { this.ownerUuid = owner.getUuid(); } else { this.ownerUuid = null; } }
    @Override @Unique public boolean isMissile() { return this.isMissile; }
    @Override @Unique public void setMissile(boolean isMissile) { this.isMissile = isMissile; }
    @Override @Unique public int getFlightDurationTicks() { return this.flightDurationTicks; }
    @Override @Unique public void setFlightDurationTicks(int ticks) { this.flightDurationTicks = ticks; }
    @Override @Unique public boolean isInstantExplosion() { return this.isInstantExplosion; }
    @Override @Unique public void setInstantExplosion(boolean instant) { this.isInstantExplosion = instant; }
    @Override @Unique public void detonate() { this.propulsionFinished = true; this.setFuse(1); }
}
