package com.aerial.bombing.entity;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Objects;

// 这个类定义了我们想要附加到实体上的数据
public final class MissileDataComponent {
    public int flightDurationTicks;
    public boolean isInstantExplosion;
    public boolean propulsionFinished;

    // 默认构造函数
    public MissileDataComponent() {
        this(0, false, false);
    }

    public MissileDataComponent(int flightDurationTicks, boolean isInstantExplosion, boolean propulsionFinished) {
        this.flightDurationTicks = flightDurationTicks;
        this.isInstantExplosion = isInstantExplosion;
        this.propulsionFinished = propulsionFinished;
    }

    public void readFromNbt(NbtCompound nbt) {
        this.flightDurationTicks = nbt.getInt("FlightDuration");
        this.isInstantExplosion = nbt.getBoolean("InstantExplosion");
        this.propulsionFinished = nbt.getBoolean("PropulsionFinished");
    }

    public void writeToNbt(NbtCompound nbt) {
        nbt.putInt("FlightDuration", this.flightDurationTicks);
        nbt.putBoolean("InstantExplosion", this.isInstantExplosion);
        nbt.putBoolean("PropulsionFinished", this.propulsionFinished);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MissileDataComponent that = (MissileDataComponent) o;
        return flightDurationTicks == that.flightDurationTicks && isInstantExplosion == that.isInstantExplosion && propulsionFinished == that.propulsionFinished;
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightDurationTicks, isInstantExplosion, propulsionFinished);
    }
}
