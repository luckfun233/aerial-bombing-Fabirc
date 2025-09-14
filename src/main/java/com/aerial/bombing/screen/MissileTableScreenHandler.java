package com.aerial.bombing.screen;

import com.aerial.bombing.util.TntValidator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class MissileTableScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public MissileTableScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, (Inventory) playerInventory.player.getWorld().getBlockEntity(buf.readBlockPos()),
                new ArrayPropertyDelegate(1));
    }

    public MissileTableScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.MISSILE_TABLE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        inventory.onOpen(playerInventory.player);

        // 更新槽位坐标以匹配新的 GUI 布局 (类似锻造台)
        this.addSlot(new Slot(inventory, 0, 27, 47) { // TNT Slot
            @Override
            public boolean canInsert(ItemStack stack) {
                return TntValidator.isValidTnt(stack);
            }
        });
        this.addSlot(new Slot(inventory, 1, 76, 47) { // Rocket Slot
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() == Items.FIREWORK_ROCKET;
            }
        });

        this.addSlot(new Slot(inventory, 2, 134, 47) { // Output Slot
            @Override
            public boolean canInsert(ItemStack stack) {
                return false; // 禁止手动放入
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                // 当玩家取出物品时，减少原料
                inventory.getStack(0).decrement(1);
                inventory.getStack(1).decrement(1);
                super.onTakeItem(player, stack);
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        addProperties(propertyDelegate);
    }

    public boolean isInstantExplosion() {
        return this.propertyDelegate.get(0) == 1;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (slotIndex < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else {
                if (TntValidator.isValidTnt(originalStack)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (originalStack.isOf(Items.FIREWORK_ROCKET)) {
                    if (!this.insertItem(originalStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex >= this.inventory.size() && slotIndex < this.slots.size() - 9) {
                    if(!this.insertItem(originalStack, this.slots.size() - 9, this.slots.size(), false)){
                        return ItemStack.EMPTY;
                    }
                } else {
                    if(!this.insertItem(originalStack, this.inventory.size(), this.slots.size() - 9, false)){
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
