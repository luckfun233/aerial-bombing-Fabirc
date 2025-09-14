package com.aerial.bombing.mixin;

import com.aerial.bombing.util.TntValidator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract boolean hasNbt();

    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    private void allowMissileStacking(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;

        // 确保物品是TNT并且带有NBT
        if (this.hasNbt() && TntValidator.isValidTnt(stack)) {
            NbtCompound nbt = stack.getNbt();

            // 关键检查：只对我们自己的、明确标记为导弹的TNT生效
            // 使用 nbt.contains("is_missile", 1) 是一种更安全的检查方式，
            // 它会检查 "is_missile" 是否存在并且是一个布尔/字节类型。
            if (nbt != null && nbt.contains("is_missile") && nbt.getBoolean("is_missile")) {
                // 如果是导弹，将最大堆叠数量设为16并立即返回
                cir.setReturnValue(16);
            }
        }
    }
}
