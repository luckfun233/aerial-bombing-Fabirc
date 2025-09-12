package com.aerial.bombing;

import com.aerial.bombing.config.ModConfig;
import com.aerial.bombing.entity.ModTntEntity;
import com.aerial.bombing.network.BombDropPacket;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AerialBombing implements ModInitializer {
	public static final String MOD_ID = "aerial-bombing";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 注册自定义TNT实体类型
	public static final EntityType<ModTntEntity> MOD_TNT_ENTITY = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(MOD_ID, "mod_tnt"),
			FabricEntityTypeBuilder.<ModTntEntity>create(SpawnGroup.MISC, ModTntEntity::new)
					.dimensions(EntityDimensions.fixed(0.98f, 0.98f))
					.build()
	);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing Aerial Bombing mod");

		// 初始化配置
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

		// 初始化投弹管理器
		AerialBombingManager.getInstance().initialize();

		// 注册网络包
		BombDropPacket.registerReceiver();

		LOGGER.info("Aerial Bombing mod initialized");
	}
}
