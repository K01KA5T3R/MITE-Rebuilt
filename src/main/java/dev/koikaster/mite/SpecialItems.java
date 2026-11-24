package dev.koikaster.mite;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
public final class SpecialItems {
 /** Stackable bowl meals return exactly one bowl for each serving. */
 public static class BowlMeal extends Item {
  public BowlMeal(Properties properties){super(properties);}
  public boolean hasCraftingRemainingItem(ItemStack stack){return true;}
  public ItemStack getCraftingRemainingItem(ItemStack stack){return new ItemStack(Items.BOWL);}
  public ItemStack finishUsingItem(ItemStack stack,Level level,net.minecraft.world.entity.LivingEntity user){
   ItemStack remaining=super.finishUsingItem(stack,level,user);
   var converted=SourceUseFinish1042.convertedResult(stack,user);if(converted!=null)return converted;
   if(user instanceof Player player&&player.isCreative())return remaining;
   ItemStack bowl=new ItemStack(Items.BOWL);
   if(remaining.isEmpty())return bowl;
   if(!level.isClientSide){
    if(user instanceof Player player){if(!player.getInventory().add(bowl))player.drop(bowl,false);}
    else user.spawnAtLocation(bowl);
   }
   return remaining;
  }
 }
 public static class Coin extends Item {
  public final String material;public final int value;
  public Coin(String material,Properties p){super(p);this.material=material;value=ClassicCoins.value(material);}
  public void appendHoverText(ItemStack s,Level l,List<Component> t,TooltipFlag f){t.add(Component.translatable("tooltip.mite_reforged.coin",value));}
 }
 public static class Bandage extends Item {
  public Bandage(Properties properties){super(properties);}

  @Override public int getUseDuration(ItemStack stack){return 32;}
  @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.EAT;}
  @Override public int getMaxStackSize(ItemStack stack){return stack.isDamaged()?1:16;}
  @Override public boolean isEnchantable(ItemStack stack){return false;}

  private boolean canTreat(Player player,ItemStack stack){
   return player.getHealth()<player.getMaxHealth()&&!player.getCooldowns().isOnCooldown(this)
       &&stack.getDamageValue()<stack.getMaxDamage();
  }

  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
   var stack=player.getItemInHand(hand);
   if(!canTreat(player,stack))return InteractionResultHolder.fail(stack);
   player.startUsingItem(hand);
   return InteractionResultHolder.consume(stack);
  }

  @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
   if(level.isClientSide||!(user instanceof Player player)||!canTreat(player,stack))return stack;
   float previousHealth=player.getHealth();
   player.heal(6);
   if(player.getHealth()<=previousHealth)return stack;
   player.getCooldowns().addCooldown(this,400);
   player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
   if(player.getAbilities().instabuild)return stack;
   var fresh=stack.getCount()>1?stack.split(stack.getCount()-1):ItemStack.EMPTY;
   stack.setDamageValue(stack.getDamageValue()+1);
   if(!fresh.isEmpty()&&!player.getInventory().add(fresh))player.drop(fresh,false);
   return stack;
  }
 }
 public static class RunicKey extends Item {
  public RunicKey(Properties p){super(p);}
  // Retained registry identity and gate crafting component for existing saves;
  // a key is no longer a replacement for a real obsidian portal.
 }
 /** Obsolete registry identity retained for old saves; use the two real runestone blocks. */
 public static class RuneStone extends Item {public RuneStone(Properties p){super(p);}}

 public static class MetalBucket extends Item {
  public final String material;
  public MetalBucket(String material,Properties p){super(material.equals("adamantium")?p.fireResistant():p);this.material=material;}
  public String fluid(ItemStack s){return s.hasTag()?s.getTag().getString("Fluid"):"";}
  public boolean hasCraftingRemainingItem(ItemStack s){return !fluid(s).isEmpty();}
  public ItemStack getCraftingRemainingItem(ItemStack s){return ClassicVessels.peer(s,"");}
  public int getUseDuration(ItemStack s){return fluid(s).equals("milk")?32:0;}
  public UseAnim getUseAnimation(ItemStack s){return fluid(s).equals("milk")?UseAnim.DRINK:UseAnim.NONE;}
  public ItemStack finishUsingItem(ItemStack s,Level l,net.minecraft.world.entity.LivingEntity user){if(!l.isClientSide&&fluid(s).equals("milk")){SourceMilkFinish1066.ownEffects(user);if(!(user instanceof Player p)||!p.isCreative()){var converted=SourceUseFinish1042.ownFinish(s,user);return converted!=null?converted:ClassicVessels.peer(s,"");}}return s;}
  public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag f){String value=fluid(s);text.add(Component.translatable("tooltip.mite_reforged.bucket_"+(value.isEmpty()?"empty":value)));if(f.isAdvanced()&&value.equals("lava"))text.add(Component.translatable("tooltip.mite_reforged.bucket_melt_chance",Math.round(ClassicVessels.melting(s)*100)));}
  public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
   var stack=p.getItemInHand(h);if(fluid(stack).equals("milk")){p.startUsingItem(h);return InteractionResultHolder.consume(stack);}return ClassicVessels.use(l,p,h);
  }
 }
 private SpecialItems(){}
}
