package dev.koikaster.mite;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Direct aaf workbench repair branches; 0.4.17 preserves quality and XP exemption. */
public final class ClassicCraftRepair extends CustomRecipe {
 public record Plan(ItemStack output,int threadSlot,int consumed,boolean leather){}
 public ClassicCraftRepair(ResourceLocation id,CraftingBookCategory category){super(id,category);}
 public static Plan plan(CraftingContainer grid){
  ItemStack first=ItemStack.EMPTY,second=ItemStack.EMPTY;int firstSlot=-1,secondSlot=-1;
  for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;if(first.isEmpty()){first=stack;firstSlot=i;}else if(second.isEmpty()){second=stack;secondSlot=i;}else return null;}
  if(first.isEmpty()||second.isEmpty()||first.is(Content.BANDAGE.get())||second.is(Content.BANDAGE.get())||LargeBonePickaxe.matches(first)||LargeBonePickaxe.matches(second))return null;
  if(first.is(second.getItem())&&first.getCount()==1&&second.getCount()==1&&first.isDamageableItem()&&ClassicToolCatalogue.sourceItem(first)){
   if(!first.isDamaged()||!second.isDamaged()||first.isEnchanted()||second.isEnchanted())return null;
   if(first.getItem() instanceof DyeableLeatherItem dye){if(dye.hasCustomColor(first)!=dye.hasCustomColor(second))return null;if(dye.hasCustomColor(first)&&dye.getColor(first)!=dye.getColor(second))return null;}
   var output=new ItemStack(first.getItem());output.setDamageValue(LegacyProduction.combinedDamage(first.getMaxDamage(),first.getDamageValue(),second.getDamageValue()));
   if(ClassicQuality.eligible(first))ClassicQuality.set(output,ClassicQuality.get(first));
   if(first.getItem() instanceof DyeableLeatherItem dye&&dye.hasCustomColor(first))dye.setColor(output,dye.getColor(first));
   return new Plan(output,-1,0,false);
  }
  ItemStack armor,thread;int threadSlot;
  if(first.is(Items.STRING)||first.is(Content.get("sinew"))){thread=first;threadSlot=firstSlot;armor=second;}else if(second.is(Items.STRING)||second.is(Content.get("sinew"))){thread=second;threadSlot=secondSlot;armor=first;}else return null;
  if(!(armor.getItem() instanceof ArmorItem item)||item.getMaterial()!=ArmorMaterials.LEATHER||armor.getCount()!=1||!armor.isDamaged())return null;
  var profile=ClassicRepair.profile(armor);var repair=LegacyProduction.repair(armor.getDamageValue(),armor.getMaxDamage(),profile.cost(),thread.getCount(),false);if(repair.consumed()<=0)return null;
  var output=armor.copy();output.setDamageValue(repair.remainingDamage());return new Plan(output,threadSlot,repair.consumed(),true);
 }
 public boolean matches(CraftingContainer grid,Level level){var plan=plan(grid);return plan!=null&&(!plan.leather()||ClassicCoins.owner(grid)!=null);}
 public ItemStack assemble(CraftingContainer grid,RegistryAccess access){var plan=plan(grid);return plan==null?ItemStack.EMPTY:plan.output();}
 public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
 public RecipeSerializer<?> getSerializer(){return Content.CRAFT_REPAIR_SERIALIZER.get();}
 public static float difficulty(Player player,CraftingContainer grid){var plan=plan(grid);if(plan==null)return 0;if(plan.leather())return plan.consumed()*50f;return CraftingDifficulty.productCost(player,plan.output().getItem())/2f;}
 public static void settle(Player player,CraftingContainer grid){if(player.level().isClientSide)return;var recipe=player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level());if(recipe.isEmpty()||!(recipe.get() instanceof ClassicCraftRepair))return;var plan=plan(grid);if(plan!=null&&plan.leather()&&plan.consumed()>1)grid.removeItem(plan.threadSlot(),plan.consumed()-1);}
}
