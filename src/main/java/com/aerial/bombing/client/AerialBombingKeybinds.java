package com.aerial.bombing.client;

import com.aerial.bombing.AerialBombing;
import com.aerial.bombing.AerialBombingManager;
import com.aerial.bombing.network.BombDropPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AerialBombingKeybinds {
    public static final String KEY_CATEGORY_AERIAL_BOMBING = "key.category.aerial_bombing";
    public static final String KEY_DROP_BOMB = "key.aerial_bombing.drop_bomb";

    public static KeyBinding dropBombKey;

    public static void initialize() {
        // 注册按键绑定
        dropBombKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_DROP_BOMB,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KEY_CATEGORY_AERIAL_BOMBING
        ));

        // 注册按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (dropBombKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    // 检查是否可以投弹
                    if (AerialBombingManager.getInstance().canPlayerBomb(client.player)) {
                        // 发送投弹请求到服务器
                        BombDropPacket.send(); // 修复：移除参数
                    }
                }
            }
        });
    }
}
