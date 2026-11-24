package dev.koikaster.mite;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** ItemBottleOfDisenchanting and va consumable;32ticks/container/recipe remain frozen0.18 evidence. */
public final class DisenchantingBottle extends Item {
 public DisenchantingBottle(){super(new Properties().stacksTo(1));}
 public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
 public int getUseDuration(ItemStack stack){return 32;}
 public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return ItemUtils.startUsingInstantly(level,player,hand);}
 public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity actor){if(!level.isClientSide){actor.removeAllEffects();if(actor instanceof net.minecraft.server.level.ServerPlayer player)OriginalCurseEffects.clear(player);}var converted=SourceUseFinish1042.ownFinish(stack,actor);if(converted!=null)return converted;if(actor instanceof Player player)return ItemUtils.createFilledResult(stack,player,new ItemStack(Items.GLASS_BOTTLE));stack.shrink(1);return stack.isEmpty()?new ItemStack(Items.GLASS_BOTTLE):stack;}
 public static final class Recipe extends net.minecraft.world.item.crafting.CustomRecipe {
  public Recipe(net.minecraft.resources.ResourceLocation id,net.minecraft.world.item.crafting.CraftingBookCategory category){super(id,category);}
  public boolean canCraftInDimensions(int width,int height){return width*height>=3;}
  public boolean matches(net.minecraft.world.inventory.CraftingContainer grid,Level level){boolean water=false,wart=false,coal=false;for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;if(!water&&stack.is(Items.POTION)&&net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack)==net.minecraft.world.item.alchemy.Potions.WATER)water=true;else if(!wart&&stack.is(Items.NETHER_WART))wart=true;else if(!coal&&stack.is(Items.COAL))coal=true;else return false;}return water&&wart&&coal;}
  public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer grid,net.minecraft.core.RegistryAccess access){return new ItemStack(Content.BOTTLE_OF_DISENCHANTING.get());}
  public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer(){return Content.DISENCHANTING_RECIPE.get();}
 }
}
