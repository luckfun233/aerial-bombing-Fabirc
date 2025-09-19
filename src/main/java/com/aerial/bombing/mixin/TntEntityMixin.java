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
        // --- 逻辑前置检查 ---
        // 如果不是导弹，或者导弹推进已结束，则立刻返回，让原版的 tick 方法继续执行
        if (!this.isMissile || this.propulsionFinished) {
            // 确保在推进结束后恢复重力
            if (this.isMissile && this.propulsionFinished) {
                if (this.hasNoGravity()) {
                    setNoGravity(false);
                }
            }
            return;
        }

        // --- 从这里开始，我们完全接管了这个 tick ---
        // 1. 更新飞行时间并禁用重力
        this.flightDurationTicks--;
        if (!this.hasNoGravity()) {
            setNoGravity(true);
        }

        // 2. 刷新实体的 bounding box 和基础状态
        super.tick();

        // 3. 粒子效果 (确保在服务器端执行)
        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d trailOffset = this.getVelocity().normalize().multiply(-0.5);
            serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 0.25, this.getZ(),
                    5,
                    trailOffset.x, trailOffset.y, trailOffset.z,
                    0.05); // 减小粒子扩散，使其更像尾焰
        }

        // 4. 强制设定速度向量，以维持恒定高速
        Vec3d direction = this.getVelocity().normalize();
        // 如果速度过低（例如，刚发射时），则使用其朝向作为初始方向
        if (direction.lengthSquared() < 0.1) {
            direction = this.getRotationVec(1.0f);
        }
        this.setVelocity(direction.multiply(MISSILE_SPEED_PER_TICK));

        // 5. 执行移动
        this.move(MovementType.SELF, this.getVelocity());
        this.velocityModified = true; // 标记速度已被我们修改，防止游戏内部逻辑再次修改

        // 6. 检查结束条件
        boolean collided = this.horizontalCollision || this.verticalCollision;
        boolean timeUp = this.flightDurationTicks <= 0;

        if (collided || timeUp) {
            this.propulsionFinished = true;
            this.setNoGravity(false); // 恢复重力

            if (collided) {
                LOGGER.info("导弹 (TntEntity) 碰撞，触发引爆！");
                this.detonate(); // 使用我们自己的引爆接口
            } else { // timeUp
                if (this.isInstantExplosion) {
                    LOGGER.info("导弹 (TntEntity) 飞行时间结束，瞬爆模式触发引爆！");
                    this.detonate();
                } else {
                    LOGGER.info("导弹 (TntEntity) 飞行时间结束，转入常规下落模式。");
                    this.setFuse(20); // 转为普通TNT下落引信
                }
            }
            // 推进结束，但本 tick 依然由我们控制，因此仍然取消后续逻辑
        }

        // 7. 取消原版 tick() 方法的其余部分，防止它应用重力或阻力
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
