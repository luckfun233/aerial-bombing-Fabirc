package com.aerial.bombing.block.entity;

import com.aerial.bombing.screen.MissileTableScreenHandler;
import com.aerial.bombing.util.TntValidator;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MissileTableBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, Inventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    public final PropertyDelegate propertyDelegate;
    private boolean instantExplosion = false;

    // ... 其他方法保持不变 ...

    public MissileTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_TABLE_ENTITY, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return instantExplosion ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {
                instantExplosion = (value != 0);
            }

            @Override
            public int size() {
                return 1;
            }
        };
    }

    private ItemStack createOrUpgradeMissile(ItemStack base, ItemStack rocket, boolean instant) {
        NbtCompound baseNbt = base.getNbt();
        boolean isUpgrading = baseNbt != null && baseNbt.getBoolean("is_missile");

        ItemStack newMissile;
        int initialFlightDuration = 0;

        if (isUpgrading) {
            newMissile = base.copy();
            initialFlightDuration = baseNbt.getInt("flight_duration_sec");
        } else {
            newMissile = new ItemStack(base.getItem(), 1);
        }
        newMissile.setCount(1);

        NbtCompound nbt = newMissile.getOrCreateNbt();
        int newRocketDuration = getFlightDuration(rocket);

        nbt.putBoolean("is_missile", true);
        nbt.putInt("flight_duration_sec", initialFlightDuration + newRocketDuration);
        nbt.putBoolean("instant_explosion", instant);

        // --- [修复] 动态命名逻辑 ---
        // 始终基于物品的基础翻译键来构建名称，避免重复叠加
        Text missilePrefix = Text.translatable("item.aerial-bombing.missile_prefix");
        // 使用 base.getItem().getName() 而不是 base.getName() 来获取原始物品名
        Text finalName = missilePrefix.copy().append(base.getItem().getName());
        newMissile.setCustomName(finalName);

        return newMissile;
    }

    private void craftMissile() {
        ItemStack inputStack = this.getStack(0);
        ItemStack rocketSlot = this.getStack(1);
        ItemStack outputSlot = this.getStack(2);

        if (!inputStack.isEmpty() && TntValidator.isValidTnt(inputStack) && !rocketSlot.isEmpty() && rocketSlot.getItem() == Items.FIREWORK_ROCKET) {
            ItemStack potentialOutput = createOrUpgradeMissile(inputStack, rocketSlot, this.instantExplosion);
            if (outputSlot.isEmpty()){
                this.setStack(2, potentialOutput);
                this.markDirty();
            }
        } else {
            if (!outputSlot.isEmpty()){
                this.setStack(2, ItemStack.EMPTY);
                this.markDirty();
            }
        }
    }

    public static int getFlightDuration(ItemStack rocketStack) {
        NbtCompound nbt = rocketStack.getNbt();
        if (nbt != null && nbt.contains("Fireworks", 10)) {
            NbtCompound fireworksNbt = nbt.getCompound("Fireworks");
            if (fireworksNbt.contains("Flight", 99)) {
                return fireworksNbt.getByte("Flight");
            }
        }
        return 1;
    }

    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(this.pos); }
    @Override public Text getDisplayName() { return Text.translatable("block.aerial-bombing.missile_table"); }
    @Nullable @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new MissileTableScreenHandler(syncId, playerInventory, this, this.propertyDelegate); }
    @Override protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); Inventories.writeNbt(nbt, inventory); nbt.putBoolean("InstantExplosion", this.instantExplosion); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); Inventories.readNbt(nbt, inventory); this.instantExplosion = nbt.getBoolean("InstantExplosion"); }
    public static void tick(World world, BlockPos pos, BlockState state, MissileTableBlockEntity entity) { if (!world.isClient()) { entity.craftMissile(); } }
    @Override public int size() { return inventory.size(); }
    @Override public boolean isEmpty() { for (ItemStack stack : inventory) { if (!stack.isEmpty()) return false; } return true; }
    @Override public ItemStack getStack(int slot) { return this.inventory.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack result = Inventories.splitStack(this.inventory, slot, amount); if (!result.isEmpty()) { markDirty(); if (slot == 0 || slot == 1) { setStack(2, ItemStack.EMPTY); } } return result; }
    @Override public ItemStack removeStack(int slot) { return Inventories.removeStack(this.inventory, slot); }
    @Override public void setStack(int slot, ItemStack stack) { this.inventory.set(slot, stack); if (stack.getCount() > getMaxCountPerStack()) { stack.setCount(getMaxCountPerStack()); } markDirty(); if (slot == 0 || slot == 1) { setStack(2, ItemStack.EMPTY); } }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public void clear() { inventory.clear(); }
}
