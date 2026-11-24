package dev.koikaster.mite;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/** 0.4.17: Adds quality cycling prompt, XP cost, and unskilled POOR-cap lines on crafting slot 0. */
final class ClientSlotContext1533 {
 static void append(ItemTooltipEvent event){
  var player=event.getEntity();var minecraft=Minecraft.getInstance();
  if(player==null||player!=minecraft.player||!(minecraft.screen instanceof AbstractContainerScreen<?> screen)||screen.getMenu()!=player.containerMenu)return;
  var slot=screen.getSlotUnderMouse();var stack=event.getItemStack();
  if(slot==null||stack.isEmpty()||slot.getItem()!=stack)return;
  var menu=screen.getMenu();var lines=event.getToolTip();
  if(menu instanceof SourceRepairMenu repair&&slot==menu.getSlot(2)&&repair.gradeBlocked()){
   lines.add(Component.translatable("container.repair.mite.needs_stronger_anvil").withStyle(ChatFormatting.GOLD));
  }else if(menu instanceof SourceFurnaceMenu furnace&&slot==menu.getSlot(0)){
   int heat=furnace.heatLevel();if(heat<=0)heat=ThermalFurnace.Entity.heat(menu.getSlot(1).getItem());
   if(heat>0){var plan=ModernCooking.recipe(player.level(),stack,heat,furnace.cookingMode);if(!plan.output().isEmpty()&&heat<plan.requiredHeat())lines.add(Component.translatable("container.furnace.needsMoreHeat").withStyle(ChatFormatting.GOLD));}
  }else if((menu instanceof TimedCrafting.Menu||menu instanceof InventoryMenu&&slot instanceof InventoryCrafting.OutputSlot)&&slot==menu.getSlot(0)){
   if(menu instanceof TimedCrafting.Menu&&stack.getItem() instanceof SourceRunestoneItem)lines.add(Component.translatable("tooltip.mite_reforged.select_rune").withStyle(ChatFormatting.YELLOW));
   CraftingContainer grid=menu instanceof TimedCrafting.Menu table?table.grid:(menu instanceof InventoryMenu&&menu.getSlot(1).container instanceof CraftingContainer c?c:null);
   var recipe=grid==null?null:player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level()).orElse(null);
   boolean qualitySupported=ClassicQualityCrafting.supportsQuality(recipe,stack);
   if(qualitySupported&&grid!=null){
    boolean hasSkill=ClassicQualityCrafting.hasSkill(player,recipe,grid,stack);
    float baseDiff=CraftingDifficulty.difficulty(player,grid,recipe);
    var minQ=ClassicQualityCrafting.minQuality(player,stack,hasSkill);
    var maxQ=ClassicQualityCrafting.maxQuality(player,baseDiff,stack,hasSkill,false);
    var curQ=ClassicQuality.get(stack);
    int cost=ClassicQualityCrafting.xpCost(player,baseDiff,curQ,false);
    if(maxQ!=null&&minQ!=null&&maxQ.isHigherThan(minQ)){
     lines.add(Component.translatable("container.crafting.differentQuality").withStyle(ChatFormatting.YELLOW));
    }
    if(cost>0){
     boolean ok=ClassicQualityCrafting.availableXp(player)>=cost+ClassicCoins.mintCost(stack);
     lines.add(Component.translatable("container.crafting.qualityCost",cost).withStyle(ok?ChatFormatting.GREEN:ChatFormatting.RED));
    }
    if(SourceSkills1685.enabled(player.level())&&!hasSkill){
     var missing=ClientSkillContext1839.missingSkillNames(player,menu,stack);
     if(missing!=null)lines.add(Component.translatable("gui.mite_reforged.unskilled_poor_quality",missing).withStyle(ChatFormatting.GOLD));
    }
   }
   if(SourceCraftingContext1533.upperBodyInWeb(player))lines.add(Component.translatable("container.crafting.prevented").withStyle(ChatFormatting.GOLD));
   else if(menu instanceof TimedCrafting.Menu table&&!table.allowsWorkbench()
           ||slot instanceof InventoryCrafting.OutputSlot output&&!output.allowsWorkbench())
    lines.add(Component.translatable(menu instanceof TimedCrafting.Menu?"container.crafting.mite.workbench_tier":
        "container.crafting.mite.workbench_required").withStyle(ChatFormatting.GOLD));
   else if(!ClientSkillContext1839.appendRequirement(player,menu,stack,lines)){
    if(!ClassicCoins.affordable(player,stack))lines.add(Component.translatable("container.crafting.requiresExperience").withStyle(ChatFormatting.GOLD));
    else if(menu instanceof TimedCrafting.Menu table&&!table.allowsIngredients()
            ||slot instanceof InventoryCrafting.OutputSlot output&&!output.allowsIngredients())
     lines.add(Component.translatable("container.crafting.mite.damaged_ingredient").withStyle(ChatFormatting.GOLD));
   }
  }
 }
 private ClientSlotContext1533(){}
}
