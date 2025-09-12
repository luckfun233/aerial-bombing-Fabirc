package com.aerial.bombing.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * 自定义TNT实体，用于跟踪投弹玩家
 */
public class ModTntEntity extends TntEntity {
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(ModTntEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    private UUID ownerUuid;

    public ModTntEntity(EntityType<? extends TntEntity> entityType, World world) {
        super(entityType, world);
    }

    public ModTntEntity(World world, double x, double y, double z, PlayerEntity owner) {
        super(world, x, y, z, owner);
        if (owner != null) {
            this.ownerUuid = owner.getUuid();
            this.dataTracker.set(OWNER_UUID, Optional.of(owner.getUuid()));
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerUuid != null) {
            nbt.putUuid("Owner", this.ownerUuid);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("Owner")) {
            this.ownerUuid = nbt.getUuid("Owner");
        }
    }

    public PlayerEntity getOwner() {
        if (this.ownerUuid == null) {
            return null;
        }
        return this.getWorld().getPlayerByUuid(this.ownerUuid);
    }
}
