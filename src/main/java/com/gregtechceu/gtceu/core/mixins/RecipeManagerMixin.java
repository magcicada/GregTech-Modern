package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// only fires if KJS is NOT loaded.
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Shadow private Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> recipes;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "TAIL"))
    private void gtceu$cloneVanillaRecipes(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
                                           ProfilerFiller profiler, CallbackInfo ci) {
        for (RecipeType<?> recipeType : BuiltInRegistries.RECIPE_TYPE) {
            if (recipeType instanceof GTRecipeType gtRecipeType) {
                gtRecipeType.getLookup().removeAllRecipes();

                var proxyRecipes = gtRecipeType.getProxyRecipes();
                for (Map.Entry<RecipeType<?>, List<RecipeHolder<GTRecipe>>> entry : proxyRecipes.entrySet()) {
                    var type = entry.getKey();
                    var recipes = entry.getValue();
                    recipes.clear();
                    if (this.recipes.containsKey(type)) {
                        for (var recipe : this.recipes.get(type).entrySet()) {
                            recipes.add(gtRecipeType.toGTrecipe(recipe.getValue()));
                        }
                    }
                }

                if (this.recipes.containsKey(gtRecipeType)) {
                    //noinspection unchecked
                    Stream.concat(
                            this.recipes.get(gtRecipeType).values().stream(),
                            proxyRecipes.entrySet().stream().flatMap(entry -> entry.getValue().stream())
                        ).filter(holder -> holder != null && holder.value() instanceof GTRecipe)
                        .forEach(gtRecipeHolder -> gtRecipeType.getLookup().addRecipe((RecipeHolder<GTRecipe>) gtRecipeHolder));
                } else if (!proxyRecipes.isEmpty()) {
                    proxyRecipes.values().stream()
                            .flatMap(List::stream)
                            .forEach(gtRecipe -> gtRecipeType.getLookup().addRecipe(gtRecipe));
                }
            }
        }
    }
}
