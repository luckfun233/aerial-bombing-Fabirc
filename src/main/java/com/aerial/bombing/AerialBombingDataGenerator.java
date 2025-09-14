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

		// 这个 `addProvider` 方法现在可以正确找到匹配的单参数构造函数
		pack.addProvider(ModRecipeProvider::new);
	}

	private static class ModRecipeProvider extends FabricRecipeProvider {

		// 构造函数现在只接收一个参数，解决了“应为1 个实参”的错误
		public ModRecipeProvider(FabricDataOutput output) {
			super(output);
		}

		// generate 方法现在使用了正确的签名，解决了“必须实现抽象方法”和“方法未从其超类重写”的错误
		@Override
		public void generate(Consumer<RecipeJsonProvider> exporter) {
			ShapedRecipeJsonBuilder.create(RecipeCategory.REDSTONE, ModBlocks.MISSILE_TABLE)
					.pattern("ITI")
					.pattern("TCT")
					.pattern("ITI")
					.input('I', Items.IRON_INGOT)
					.input('T', Items.TNT)
					.input('C', Items.CRAFTING_TABLE)
					.criterion(hasItem(Items.CRAFTING_TABLE), conditionsFromItem(Items.CRAFTING_TABLE))
					// offerTo 方法现在接收一个 Consumer，并使用 getRecipeName 生成 ID，解决了“无法解析方法”的错误
					.offerTo(exporter, new Identifier(getRecipeName(ModBlocks.MISSILE_TABLE)));
		}
	}
}
