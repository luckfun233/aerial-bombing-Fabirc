package com.aerial.bombing.entity;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Interface to add an owner to any TntEntity.
 * Implemented via Mixin.
 */
public interface TntEntityOwner {
    @Nullable
    UUID getOwnerUuid();

    void setOwnerUuid(@Nullable UUID ownerUuid);

    @Nullable
    PlayerEntity getOwner();

    void setOwner(@Nullable PlayerEntity owner);
}
