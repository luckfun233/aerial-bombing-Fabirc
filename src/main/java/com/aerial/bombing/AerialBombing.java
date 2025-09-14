package com.aerial.bombing;

import com.aerial.bombing.block.ModBlocks;
import com.aerial.bombing.block.entity.MissileTableBlockEntity;
import com.aerial.bombing.block.entity.ModBlockEntities;
import com.aerial.bombing.config.ModConfig;
import com.aerial.bombing.network.BombDropPacket;
import com.aerial.bombing.screen.MissileTableScreenHandler;
import com.aerial.bombing.screen.ModScreenHandlers;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemGroups;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AerialBombing implements ModInitializer {
	public static final String MOD_ID = "aerial-bombing";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Aerial Bombing mod");

		// 初始化配置
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

		// 初始化投弹管理器
		AerialBombingManager.getInstance().initialize();

		// 注册方块和物品
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();

		// 注册网络包
		BombDropPacket.registerReceiver();
		registerServerPackets();

		// 将物品添加到物品组
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
			content.add(ModBlocks.MISSILE_TABLE);
		});

		LOGGER.info("Aerial Bombing mod initialized");
	}

	public void registerServerPackets() {
		ServerPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "toggle_instant_explosion"), (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				// 获取玩家当前的ScreenHandler
				ScreenHandler screenHandler = player.currentScreenHandler;

				// 确保玩家打开的是我们的ScreenHandler
				if (screenHandler instanceof MissileTableScreenHandler) {
					// 从 ScreenHandler 获取 inventory (它就是 BlockEntity)
					if (screenHandler instanceof com.aerial.bombing.screen.MissileTableScreenHandler mtsh) {
						if(mtsh.getSlot(0).inventory instanceof MissileTableBlockEntity entity) {
							// 直接在BlockEntity上切换状态
							entity.propertyDelegate.set(0, entity.propertyDelegate.get(0) == 0 ? 1 : 0);
							// 标记为脏数据，促使客户端同步
							entity.markDirty();
							// 通知客户端内容已更改（触发GUI更新）
							screenHandler.sendContentUpdates();
						}
					}
				}
			});
		});
	}
}
