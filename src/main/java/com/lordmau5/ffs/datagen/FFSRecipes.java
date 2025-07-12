package com.lordmau5.ffs.datagen;

import com.lordmau5.ffs.FancyFluidStorage;
import com.lordmau5.ffs.holder.FFSBlocks;
import com.lordmau5.ffs.holder.FFSItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

import java.util.concurrent.CompletableFuture;

public class FFSRecipes extends RecipeProvider {

    public FFSRecipes(DataGenerator generatorIn, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(generatorIn.getPackOutput(), lookupProvider);
    }

    private void whenModLoaded(RecipeOutput consumer, String modid, RecipeBuilder recipeBuilder) {
        recipeBuilder.save(consumer.withConditions(new ModLoadedCondition(modid)));
    }

    @Override
    protected void buildRecipes(RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FFSBlocks.fluidValve.get())
                .pattern("igi")
                .pattern("gbg")
                .pattern("igi")
                .define('i', Tags.Items.INGOTS_IRON)
                .define('g', Blocks.IRON_BARS)
                .define('b', Items.BUCKET)
                .group(FancyFluidStorage.MOD_ID)
                .unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BUCKET))
                .save(consumer);

        var cc_computer = BuiltInRegistries.ITEM.getHolder(ResourceLocation.fromNamespaceAndPath("computercraft", "computer_normal"));
        cc_computer.ifPresent(itemHolder -> whenModLoaded(consumer, "computercraft", ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, FFSBlocks.tankComputer.get())
                .requires(FFSBlocks.fluidValve.get())
                .requires(itemHolder.value())
                .group(FancyFluidStorage.MOD_ID)
                .unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(FFSBlocks.fluidValve.get(), itemHolder.value()))
        ));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, FFSItems.titEgg.get())
                .requires(FFSBlocks.fluidValve.get())
                .requires(Items.EGG)
                .group(FancyFluidStorage.MOD_ID)
                .unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(FFSBlocks.fluidValve.get()))
                .save(consumer);

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(FFSItems.titEgg.get()),
                        RecipeCategory.MISC, FFSItems.tit.get(), 1.0f, 100)
                .unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(FFSItems.titEgg.get()))
                .save(consumer);
    }
}
