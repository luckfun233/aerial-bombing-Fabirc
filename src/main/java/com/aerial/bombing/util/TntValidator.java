package com.aerial.bombing.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

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

        // 排除矿车类型的物品
        if (path.contains("minecart")) {
            return false;
        }

        // 检查是否包含tnt关键字
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
