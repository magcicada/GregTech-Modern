package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.longs.LongIntPair;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2023/2/26
 * @implNote RecipeHelper
 */
public class RecipeHelper {
    public static long getInputEUt(GTRecipe recipe) {
        return recipe.getTickInputContents(EURecipeCapability.CAP).stream()
                .map(Content::getContent)
                .mapToLong(EURecipeCapability.CAP::of)
                .sum();
    }

    public static long getOutputEUt(GTRecipe recipe) {
        return recipe.getTickOutputContents(EURecipeCapability.CAP).stream()
                .map(Content::getContent)
                .mapToLong(EURecipeCapability.CAP::of)
                .sum();
    }

    public static void setInputEUt(GTRecipe recipe, long eut) {
        recipe.getTickInputContents(EURecipeCapability.CAP).forEach(c -> c.content = eut);
    }

    public static void setOutputEUt(GTRecipe recipe, long eut) {
        recipe.getTickOutputContents(EURecipeCapability.CAP).forEach(c -> c.content = eut);
    }

    public static int getRecipeEUtTier(GTRecipe recipe) {
        long EUt = getInputEUt(recipe);
        if (EUt == 0) {
            EUt = getOutputEUt(recipe);
        }
        return GTUtil.getTierByVoltage(EUt);
    }

    /**
     * Calculates the overclocked Recipe's final duration and EU/t
     *
     * @param recipe the recipe to run
     * @return a new recipe
     */
    public static GTRecipe applyOverclock(OverclockingLogic logic, @NotNull GTRecipe recipe, long maxOverclockVoltage) {
        long EUt = getInputEUt(recipe);
        if (EUt > 0) {
            var ocResult = performOverclocking(logic, recipe, EUt, maxOverclockVoltage);
            if (ocResult.getEut() != EUt || recipe.duration != ocResult.getDuration()) {
                recipe = recipe.copy();
                recipe.duration = ocResult.getDuration();
                for (Content content : recipe.getTickInputContents(EURecipeCapability.CAP)) {
                    content.content = ocResult.getEut();
                }
            }
        }
        EUt = getOutputEUt(recipe);
        if (EUt > 0) {
            var ocResult = performOverclocking(logic, recipe, EUt, maxOverclockVoltage);
            if (ocResult.getEut() != EUt || recipe.duration != ocResult.getDuration()) {
                recipe = recipe.copy();
                recipe.duration = ocResult.getDuration();
                for (Content content : recipe.getTickOutputContents(EURecipeCapability.CAP)) {
                    content.content = ocResult.getEut();
                }
            }
        }
        return recipe;
    }

    /**
     * Determines the maximum number of overclocks that can be performed for a recipe.
     * Then performs overclocking on the Recipe.
     *
     * @param recipe the recipe to overclock
     * @return an int array of {OverclockedEUt, OverclockedDuration}
     */
    public static OCResult performOverclocking(OverclockingLogic logic, @NotNull GTRecipe recipe, long EUt,
                                                  long maxOverclockVoltage) {
        int recipeTier = GTUtil.getTierByVoltage(EUt);
        int maximumTier = maxOverclockVoltage < Integer.MAX_VALUE ? logic.getOverclockForTier(maxOverclockVoltage) :
                GTUtil.getFakeVoltageTier(maxOverclockVoltage);
        // The maximum number of overclocks is determined by the difference between the tier the recipe is running at,
        // and the maximum tier that the machine can overclock to.
        int numberOfOCs = maximumTier - recipeTier;
        if (recipeTier == GTValues.ULV) numberOfOCs--; // no ULV overclocking

        // Always overclock even if numberOfOCs is <=0 as without it, some logic for coil bonuses ETC won't apply.
        OCParams ocParams = new OCParams();
        OCResult ocResult = new OCResult();
        ocParams.initialize(EUt, recipe.duration, numberOfOCs);
        if(ocParams.getOcAmount() <= 0) {
            // number of OCs is <=0, so do not overclock
            ocResult.init(ocParams.getEut(), ocParams.getDuration());
        } else {
            logic.getLogic().runOverclockingLogic(ocParams, ocResult, maxOverclockVoltage);
        }
        ocParams.reset();
        return ocResult;
    }

    public static <T> List<T> getInputContents(GTRecipeBuilder builder, RecipeCapability<T> capability) {
        return builder.input.getOrDefault(capability, Collections.emptyList()).stream()
                .map(content -> capability.of(content.getContent()))
                .collect(Collectors.toList());
    }

    public static <T> List<T> getInputContents(GTRecipe recipe, RecipeCapability<T> capability) {
        return recipe.getInputContents(capability).stream()
                .map(content -> capability.of(content.getContent()))
                .collect(Collectors.toList());
    }

    public static <T> List<T> getOutputContents(GTRecipeBuilder builder, RecipeCapability<T> capability) {
        return builder.output.getOrDefault(capability, Collections.emptyList()).stream()
                .map(content -> capability.of(content.getContent()))
                .collect(Collectors.toList());
    }

    public static <T> List<T> getOutputContents(GTRecipe recipe, RecipeCapability<T> capability) {
        return recipe.getOutputContents(capability).stream()
                .map(content -> capability.of(content.getContent()))
                .collect(Collectors.toList());
    }

    /*
     * Those who use these methods should note that these methods do not guarantee that the returned values are valid,
     * because the relevant data, such as tag information, may not be loaded at the time these methods are called.
     * Methods for getting Recipe Builder input items or fluids are not provided, as these data are not yet loaded when
     * they are needed.
     */

    /**
     * get all input items from GTRecipes
     *
     * @param recipe GTRecipe
     * @return all input items
     */
    public static List<ItemStack> getInputItems(GTRecipe recipe) {
        return recipe.getInputContents(ItemRecipeCapability.CAP).stream()
                .map(content -> ItemRecipeCapability.CAP.of(content.getContent()))
                .map(ingredient -> ingredient.getItems()[0])
                .collect(Collectors.toList());
    }

    /**
     * get all input fluids from GTRecipes
     *
     * @param recipe GTRecipe
     * @return all input fluids
     */
    public static List<FluidStack> getInputFluids(GTRecipe recipe) {
        return recipe.getInputContents(FluidRecipeCapability.CAP).stream()
                .map(content -> FluidRecipeCapability.CAP.of(content.getContent()))
                .map(ingredient -> ingredient.getStacks()[0])
                .collect(Collectors.toList());
    }

    /**
     * get all output items from GTRecipes
     *
     * @param recipe GTRecipe
     * @return all output items
     */
    public static List<ItemStack> getOutputItems(GTRecipe recipe) {
        return recipe.getOutputContents(ItemRecipeCapability.CAP).stream()
                .map(content -> ItemRecipeCapability.CAP.of(content.getContent()))
                .map(ingredient -> ingredient.getItems()[0])
                .collect(Collectors.toList());
    }

    /**
     * get all output items from GTRecipeBuilder
     *
     * @param builder GTRecipeBuilder
     * @return all output items
     */
    public static List<ItemStack> getOutputItems(GTRecipeBuilder builder) {
        return builder.output.getOrDefault(ItemRecipeCapability.CAP, Collections.emptyList()).stream()
                .map(content -> ItemRecipeCapability.CAP.of(content.getContent()))
                .map(ingredient -> ingredient.getItems()[0])
                .collect(Collectors.toList());
    }

    /**
     * get all output fluids from GTRecipes
     *
     * @param recipe GTRecipe
     * @return all output fluids
     */
    public static List<FluidStack> getOutputFluids(GTRecipe recipe) {
        return recipe.getOutputContents(FluidRecipeCapability.CAP).stream()
                .map(content -> FluidRecipeCapability.CAP.of(content.getContent()))
                .map(ingredient -> ingredient.getStacks()[0])
                .collect(Collectors.toList());
    }

    /**
     * get all output fluids from GTRecipeBuilder
     *
     * @param builder GTRecipeBuilder
     * @return all output fluids
     */
    public static List<FluidStack> getOutputFluids(GTRecipeBuilder builder) {
        return builder.output.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList()).stream()
                .map(content -> FluidRecipeCapability.CAP.of(content.getContent()))
                .map(ingredient -> ingredient.getStacks()[0])
                .collect(Collectors.toList());
    }
}
