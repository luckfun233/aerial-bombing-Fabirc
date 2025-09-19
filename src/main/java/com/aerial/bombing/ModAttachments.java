package com.aerial.bombing;

import com.aerial.bombing.entity.MissileDataComponent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class ModAttachments {

    public static final Codec<MissileDataComponent> MISSILE_DATA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("flight_duration_ticks").forGetter(MissileDataComponent::getFlightDurationTicks),
                    Codec.BOOL.fieldOf("is_instant_explosion").forGetter(MissileDataComponent::isInstantExplosion),
                    Codec.BOOL.fieldOf("propulsion_finished").forGetter(MissileDataComponent::isPropulsionFinished),
                    // 添加 Vec3d 的序列化，并提供默认值以兼容旧数据
                    Vec3d.CODEC.optionalFieldOf("initial_velocity", Vec3d.ZERO).forGetter(MissileDataComponent::getInitialVelocity),
                    Codec.BOOL.optionalFieldOf("physics_applied", false).forGetter(MissileDataComponent::isPhysicsApplied)
            ).apply(instance, MissileDataComponent::new)
    );

    public static final AttachmentType<MissileDataComponent> MISSILE_DATA = AttachmentRegistry.<MissileDataComponent>builder()
            // 新增：提供一个默认构造器，以修复 getAttachedOrCreate 崩溃
            .initializer(MissileDataComponent::new)
            .persistent(MISSILE_DATA_CODEC)
            .buildAndRegister(new Identifier(AerialBombing.MOD_ID, "missile_data"));


    public static void registerAttachments() {
        AerialBombing.LOGGER.info("Registering Attachments for " + AerialBombing.MOD_ID);
    }
}
