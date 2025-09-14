package com.aerial.bombing.entity;

/**
 * An interface to attach missile properties to a TntEntity via Mixin.
 * This allows us to modify any TntEntity's behavior without creating a new entity,
 * ensuring compatibility with other mods.
 */
public interface MissileData {
    boolean isMissile();

    void setMissile(boolean isMissile);

    int getFlightDurationTicks();

    void setFlightDurationTicks(int ticks);

    boolean isInstantExplosion();

    void setInstantExplosion(boolean instant);

    // A custom method to trigger an explosion quickly and reliably.
    void detonate();
}
