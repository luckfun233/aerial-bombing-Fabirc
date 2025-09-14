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

// 实现 Inventory 接口来处理物品
public class MissileTableBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, Inventory {
    // 定义物品栏，0: TNT, 1: 烟花, 2: 输出
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);

    // 将 propertyDelegate 设为 public 以便从外部访问
    public final PropertyDelegate propertyDelegate;
    private boolean instantExplosion = false; // 默认不瞬爆

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

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.aerial-bombing.missile_table");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MissileTableScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putBoolean("InstantExplosion", this.instantExplosion);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        this.instantExplosion = nbt.getBoolean("InstantExplosion");
    }

    // Tick 方法，在服务器端执行合成逻辑
    public static void tick(World world, BlockPos pos, BlockState state, MissileTableBlockEntity entity) {
        if (!world.isClient()) {
            entity.craftMissile();
        }
    }


    private void craftMissile() {
        ItemStack tntSlot = this.getStack(0);
        ItemStack rocketSlot = this.getStack(1);
        ItemStack outputSlot = this.getStack(2);

        // 检查配方是否有效
        if (!tntSlot.isEmpty() && TntValidator.isValidTnt(tntSlot) && !rocketSlot.isEmpty() && rocketSlot.getItem() == Items.FIREWORK_ROCKET) {
            ItemStack potentialOutput = createMissileOutput(tntSlot, rocketSlot, this.instantExplosion);

            // 如果输出槽为空，或者可以合并，则设置输出
            if (outputSlot.isEmpty()){
                this.setStack(2, potentialOutput);
                this.markDirty();
            }
        } else {
            // 如果输入不满足条件，清空输出槽
            if (!outputSlot.isEmpty()){
                this.setStack(2, ItemStack.EMPTY);
                this.markDirty();
            }
        }
    }

    // 创建导弹物品的方法，以便重用
    private ItemStack createMissileOutput(ItemStack tnt, ItemStack rocket, boolean instant) {
        ItemStack newMissile = tnt.copy();
        newMissile.setCount(1);
        NbtCompound nbt = newMissile.getOrCreateNbt();

        // 计算飞行时间
        int flightDuration = getFlightDuration(rocket);
        nbt.putBoolean("is_missile", true);
        // 总飞行时间 = 单个火箭时间 * 火箭数量
        nbt.putInt("flight_duration_sec", flightDuration * rocket.getCount());
        nbt.putBoolean("instant_explosion", instant);

        // 设置自定义名称
        newMissile.setCustomName(Text.translatable("item.aerial-bombing.missile_tnt"));

        return newMissile;
    }

    public static int getFlightDuration(ItemStack rocketStack) {
        NbtCompound nbt = rocketStack.getNbt();
        if (nbt != null && nbt.contains("Fireworks", 10)) {
            NbtCompound fireworksNbt = nbt.getCompound("Fireworks");
            if (fireworksNbt.contains("Flight", 99)) {
                return fireworksNbt.getByte("Flight");
            }
        }
        return 1; // 默认为1，避免无法飞行
    }

    // --- 实现 Inventory 接口的方法 ---

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            if (!getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(this.inventory, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
            // 移除了材料，需要重新检查合成
            if(slot == 0 || slot == 1){
                setStack(2, ItemStack.EMPTY);
            }
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
        // 放置了材料，需要重新检查合成
        if(slot == 0 || slot == 1){
            setStack(2, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }
}
