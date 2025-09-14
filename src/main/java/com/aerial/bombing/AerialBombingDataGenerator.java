package com.aerial.bombing;

import com.aerial.bombing.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class AerialBombingDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(ModRecipeProvider::new);
	}

	private static class ModRecipeProvider extends FabricRecipeProvider {
		public ModRecipeProvider(FabricDataOutput output) {
			super(output);
		}

		@Override
		public void generate(Consumer<RecipeJsonProvider> exporter) {
			// --- 修正配方 ---
			// 配方已更改为我们之前讨论的更合理的版本
			// 顶行: 铁锭, TNT, 铁锭
			// 中行: 红石, 工作台, 红石
			// 底行: 铁锭, 铁锭, 铁锭
			ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, ModBlocks.MISSILE_TABLE)
					.pattern("ITI")
					.pattern("RCR") // <--- 修正
					.pattern("III") // <--- 修正
					.input('I', Items.IRON_INGOT)
					.input('T', Items.TNT)
					.input('R', Items.REDSTONE) // <-- 新增定义
					.input('C', Items.CRAFTING_TABLE)
					.criterion(hasItem(Items.CRAFTING_TABLE), conditionsFromItem(Items.CRAFTING_TABLE))
					.offerTo(exporter, new Identifier(getRecipeName(ModBlocks.MISSILE_TABLE)));
		}
	}
}
