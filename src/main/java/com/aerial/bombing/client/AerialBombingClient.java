package com.aerial.bombing.client;

import com.aerial.bombing.screen.ModScreenHandlers;
import com.aerial.bombing.screen.MissileTableScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class AerialBombingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 初始化客户端按键绑定
        AerialBombingKeybinds.initialize();

        // 注册GUI屏幕
        HandledScreens.register(ModScreenHandlers.MISSILE_TABLE_SCREEN_HANDLER, MissileTableScreen::new);
    }
}
