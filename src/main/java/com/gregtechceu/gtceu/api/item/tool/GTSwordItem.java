package com.gregtechceu.gtceu.api.item.tool;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.client.renderer.item.ToolItemRenderer;

import com.lowdragmc.lowdraglib.Platform;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GTSwordItem extends SwordItem implements IGTTool {

    @Getter
    private final GTToolType toolType;
    @Getter
    private final Material material;
    @Getter
    private final int electricTier;
    @Getter
    private final IGTToolDefinition toolStats;

    protected GTSwordItem(GTToolType toolType, MaterialToolTier tier, Material material, IGTToolDefinition toolStats, Properties properties) {
        super(tier, properties);
        this.toolType = toolType;
        this.material = material;
        this.electricTier = toolType.electricTier;
        this.toolStats = toolStats;
        if (Platform.isClient()) {
            ToolItemRenderer.create(this, toolType);
        }
        definition$init();
    }

    public static GTSwordItem create(GTToolType toolType, MaterialToolTier tier, Material material,
                                     IGTToolDefinition toolStats, Item.Properties properties) {
        return new GTSwordItem(toolType, tier, material, toolStats, properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return get();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        return definition$onItemUseFirst(itemStack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return definition$onItemUse(context);
    }

    @Override
    public String getDescriptionId() {
        return toolType.getUnlocalizedName();
    }

    @Override
    public Component getDescription() {
        return Component.translatable(toolType.getUnlocalizedName(), material.getLocalizedName());
    }

    @Override
    public Component getName(ItemStack stack) {
        return this.getDescription();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        return definition$mineBlock(stack, level, state, pos, miningEntity);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        return definition$use(level, player, usedHand);
    }

    @Override
    public boolean isElectric() {
        return electricTier > -1;
    }

    @Nullable
    @Override
    public SoundEntry getSound() {
        return toolType.soundEntry;
    }

    @Override
    public boolean playSoundOnBlockDestroy() {
        return toolType.playSoundOnBlockDestroy;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return definition$getDestroySpeed(stack, state);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return definition$hurtEnemy(stack, target, attacker);
    }

    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        return definition$onBlockStartBreak(stack, pos, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        definition$appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return definition$canApplyAtEnchantingTable(stack, enchantment);
    }

    public int getEnchantmentValue(ItemStack stack) {
        return getTotalEnchantability(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return definition$isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        return definition$getDefaultAttributeModifiers(stack);
    }

    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return definition$canDisableShield(shield, shield, entity, attacker);
    }

    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return definition$doesSneakBypassUse(stack, level, pos, player);
    }

    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return definition$shouldCauseBlockBreakReset(oldStack, newStack);
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return definition$hasCraftingRemainingItem(stack);
    }

    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        return definition$getCraftingRemainingItem(itemStack);
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return definition$shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }
}
