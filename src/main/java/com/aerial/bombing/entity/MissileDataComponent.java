package com.aerial.bombing.entity;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public final class MissileDataComponent {
    public int flightDurationTicks;
    public boolean isInstantExplosion;
    public boolean propulsionFinished;
    // 新增字段：用于存储初始速度向量
    public Vec3d initialVelocity;
    // 新增字段：用于确保初始速度只被应用一次
    public boolean physicsApplied;

    public MissileDataComponent() {
        this(0, false, false, Vec3d.ZERO, false);
    }

    public MissileDataComponent(int flightDurationTicks, boolean isInstantExplosion, boolean propulsionFinished, Vec3d initialVelocity, boolean physicsApplied) {
        this.flightDurationTicks = flightDurationTicks;
        this.isInstantExplosion = isInstantExplosion;
        this.propulsionFinished = propulsionFinished;
        this.initialVelocity = initialVelocity;
        this.physicsApplied = physicsApplied;
    }

    // --- Getters for Codec ---
    public int getFlightDurationTicks() { return flightDurationTicks; }
    public boolean isInstantExplosion() { return isInstantExplosion; }
    public boolean isPropulsionFinished() { return propulsionFinished; }
    public Vec3d getInitialVelocity() { return initialVelocity; }
    public boolean isPhysicsApplied() { return physicsApplied; }

    // --- NBT methods (kept for potential other uses, though Codec is primary now) ---
    public void readFromNbt(NbtCompound nbt) {
        this.flightDurationTicks = nbt.getInt("FlightDuration");
        this.isInstantExplosion = nbt.getBoolean("InstantExplosion");
        this.propulsionFinished = nbt.getBoolean("PropulsionFinished");
        if(nbt.contains("velX")) {
            this.initialVelocity = new Vec3d(nbt.getDouble("velX"), nbt.getDouble("velY"), nbt.getDouble("velZ"));
        }
        this.physicsApplied = nbt.getBoolean("PhysicsApplied");
    }

    public void writeToNbt(NbtCompound nbt) {
        nbt.putInt("FlightDuration", this.flightDurationTicks);
        nbt.putBoolean("InstantExplosion", this.isInstantExplosion);
        nbt.putBoolean("PropulsionFinished", this.propulsionFinished);
        if(this.initialVelocity != null) {
            nbt.putDouble("velX", this.initialVelocity.x);
            nbt.putDouble("velY", this.initialVelocity.y);
            nbt.putDouble("velZ", this.initialVelocity.z);
        }
        nbt.putBoolean("PhysicsApplied", this.physicsApplied);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MissileDataComponent that = (MissileDataComponent) o;
        return flightDurationTicks == that.flightDurationTicks && isInstantExplosion == that.isInstantExplosion && propulsionFinished == that.propulsionFinished && physicsApplied == that.physicsApplied && Objects.equals(initialVelocity, that.initialVelocity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightDurationTicks, isInstantExplosion, propulsionFinished, initialVelocity, physicsApplied);
    }
}
