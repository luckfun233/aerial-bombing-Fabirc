package com.aerial.bombing.client;

import com.aerial.bombing.client.AerialBombingKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

public class AerialBombingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 初始化客户端按键绑定
        AerialBombingKeybinds.initialize();
    }
}
