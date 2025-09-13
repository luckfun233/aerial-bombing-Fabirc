package com.aerial.bombing.util;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TntValidator {

    /**
     * 检查物品是否为有效的TNT类型（兼容大多数TNT模组）
     * @param stack 要检查的物品堆
     * @return 如果是有效的TNT返回true，否则返回false
     */
    public static boolean isValidTnt(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id.getPath().toLowerCase();
        Text name = stack.getName();
        String nameString = name.getString().toLowerCase();

        // 检查ID和名称是否包含"minecart"或中文"矿车"，如果包含则排除
        if (path.contains("minecart") || nameString.contains("minecart") || nameString.contains("矿车")) {
            return false;
        }

        // 检查ID是否包含"tnt"关键字
        return path.contains("tnt");
    }

    /**
     * 获取TNT物品的标识符路径（用于未来扩展）
     * @param stack TNT物品堆
     * @return 标识符路径
     */
    public static String getTntPath(ItemStack stack) {
        if (!isValidTnt(stack)) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()).getPath();
    }
}
