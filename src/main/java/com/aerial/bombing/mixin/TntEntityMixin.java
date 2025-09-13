package com.aerial.bombing.mixin;

import com.aerial.bombing.entity.TntEntityOwner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(TntEntity.class)
public abstract class TntEntityMixin extends Entity implements TntEntityOwner {
    @Unique
    @Nullable
    private UUID ownerUuid;

    public TntEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void writeOwnerDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.ownerUuid != null) {
            nbt.putUuid("AerialBombingOwner", this.ownerUuid);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void readOwnerDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.containsUuid("AerialBombingOwner")) {
            this.ownerUuid = nbt.getUuid("AerialBombingOwner");
        }
    }

    @Override
    public @Nullable UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @Override
    public @Nullable PlayerEntity getOwner() {
        if (this.ownerUuid != null && !this.getWorld().isClient()) {
            return this.getWorld().getPlayerByUuid(this.ownerUuid);
        }
        return null;
    }

    @Override
    public void setOwner(@Nullable PlayerEntity owner) {
        if (owner != null) {
            this.ownerUuid = owner.getUuid();
        } else {
            this.ownerUuid = null;
        }
    }
}
