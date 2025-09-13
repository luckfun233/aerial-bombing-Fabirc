package com.aerial.bombing;

import com.aerial.bombing.config.ModConfig;
import com.aerial.bombing.network.BombDropPacket;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AerialBombing implements ModInitializer {
	public static final String MOD_ID = "aerial-bombing";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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
