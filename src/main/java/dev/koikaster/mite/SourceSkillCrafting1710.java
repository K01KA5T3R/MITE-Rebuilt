package dev.koikaster.mite;
import net.minecraft.world.entity.player.Player;import net.minecraft.world.inventory.CraftingContainer;import net.minecraft.world.item.ItemStack;import net.minecraft.world.item.crafting.*;
/**
 * 0.4.17: Quality-eligible crafting recipes remain craftable without skills (capped at POOR),
 * while non-quality skill recipes and repair combinations enforce skillsets.
 */
public final class SourceSkillCrafting1710 {
 public static boolean hasSkill(Player player,Recipe<?> recipe,CraftingContainer grid,ItemStack output){
  if(!SourceSkills1685.enabled(player.level()))return true;
  var decision=SourceSkillFacts1710.crafting(recipe,grid,output);
  return !decision.known()||SourceSkillRules1685.alternatives(SourceSkills1685.bits(player),decision.skillsets());
 }
 public static boolean allowed(Player player,Recipe<?> recipe,CraftingContainer grid,ItemStack output){
  if(!SourceSkills1685.enabled(player.level()))return true;
  if(!(recipe instanceof ClassicCraftRepair)&&ClassicQuality.eligible(output))return true;
  return hasSkill(player,recipe,grid,output);
 }
 public static boolean allowed(Player player,CraftingContainer grid){
  if(!SourceSkills1685.enabled(player.level()))return true;
  var recipe=player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level());
  if(recipe.isEmpty())return true;
  return allowed(player,recipe.get(),grid,recipe.get().assemble(grid,player.level().registryAccess()));
 }
 private SourceSkillCrafting1710(){}
}
