package com.aerial.bombing.block.entity;

import com.aerial.bombing.AerialBombing;
import com.aerial.bombing.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<MissileTableBlockEntity> MISSILE_TABLE_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    new Identifier(AerialBombing.MOD_ID, "missile_table_entity"),
                    FabricBlockEntityTypeBuilder.create(MissileTableBlockEntity::new,
                            ModBlocks.MISSILE_TABLE).build());


    public static void registerBlockEntities() {
        AerialBombing.LOGGER.info("Registering Block Entities for " + AerialBombing.MOD_ID);
    }
}
