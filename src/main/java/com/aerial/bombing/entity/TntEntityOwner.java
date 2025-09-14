package com.aerial.bombing.entity;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Interface to add a player owner to any TntEntity.
 * Implemented via Mixin.
 */
public interface TntEntityOwner {
    @Nullable
    UUID getOwnerUuid();

    void setOwnerUuid(@Nullable UUID ownerUuid);

    // Changed to getPlayerOwner to avoid conflicts with Entity.getOwner()
    @Nullable
    PlayerEntity getPlayerOwner();

    void setOwner(@Nullable PlayerEntity owner);
}
