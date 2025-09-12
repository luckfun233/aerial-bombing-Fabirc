package com.aerial.bombing.network;

import com.aerial.bombing.AerialBombingManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class BombDropPacket {
    public static final Identifier ID = new Identifier("aerial-bombing", "drop_bomb");

    public static void registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            // 在服务器线程执行
            server.execute(() -> {
                if (player.getWorld() != null) { // 修复：使用getWorld()
                    AerialBombingManager.getInstance().tryAerialBombing(player, player.getWorld()); // 修复：使用getWorld()
                }
            });
        });
    }

    // 修复：客户端发送数据包的方法
    public static void send() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(ID, buf);
    }
}
