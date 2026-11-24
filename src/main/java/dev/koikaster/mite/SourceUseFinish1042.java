package dev.koikaster.mite;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Scoped finish owner: retain native effects and Forge's original use-stack copy while
 * the actual one-item conversion and final selected-slot result follow R195.
 */
public final class SourceUseFinish1042 {
 private static final ThreadLocal<Frame> active=new ThreadLocal<>();
 private static final int NONE=0,BOWL=1,BUCKET=2,GLASS=3,UNHANDLED=-1;
 private static final class Frame {
  final ItemStack input;final LivingEntity actor;final Player player;final int kind;final ItemStack identity;boolean converted;
  Frame(ItemStack input,LivingEntity actor){this.input=input;this.actor=actor;player=actor instanceof Player p&&p.getMainHandItem()==input?p:null;kind=player==null?UNHANDLED:kind(input);identity=kind<0?ItemStack.EMPTY:input.copyWithCount(1);}
  boolean matches(ItemStack input,LivingEntity actor){return kind>=0&&this.input==input&&this.actor==actor;}
  ItemStack output(){return switch(kind){case BOWL->new ItemStack(Items.BOWL);case BUCKET->ClassicVessels.peer(identity,"");case GLASS->new ItemStack(Items.GLASS_BOTTLE);default->ItemStack.EMPTY;};}
  ItemStack result(){return player.getMainHandItem();}
 }
 private static int kind(ItemStack stack){
  if(stack.isEmpty())return UNHANDLED;var item=stack.getItem();var id=ForgeRegistries.ITEMS.getKey(item);if(id==null||!id.getNamespace().equals("minecraft")&&!id.getNamespace().equals(MiteReforged.ID))return UNHANDLED;
  if(item instanceof BowlFoodItem||item instanceof SuspiciousStewItem||item instanceof SpecialItems.BowlMeal||item instanceof OriginalBowls.WaterBowl)return BOWL;
  if(ClassicVessels.handled(stack)&&ClassicVessels.fluid(stack).equals("milk"))return BUCKET;
  if(item instanceof PotionItem||item instanceof HoneyBottleItem||item instanceof DisenchantingBottle)return GLASS;
  return stack.isEdible()?NONE:UNHANDLED;
 }
 public static ItemStack finish(Object rawStack,Object rawLevel,Object rawActor){
  var input=(ItemStack)rawStack;var level=(Level)rawLevel;var actor=(LivingEntity)rawActor;var item=input.getItem();var previous=active.get();var frame=new Frame(input,actor);active.set(frame);
  try{var result=item.finishUsingItem(input,level,actor);return frame.converted?frame.result():result;}
  finally{if(previous==null)active.remove();else active.set(previous);}
 }
 private static boolean convert(ItemStack stack,LivingEntity actor){
  var frame=active.get();if(frame==null||!frame.matches(stack,actor))return false;
  if(!frame.converted){
   if(!actor.level().isClientSide&&!frame.player.getAbilities().instabuild)SourceHeldConversion1037.convert(frame.player,net.minecraft.world.InteractionHand.MAIN_HAND,frame.output());
   frame.converted=true;
  }
  return true;
 }
 /** Replaces only the audited shrink1 sites inside eat, milk and potion finish owners. */
 public static void consume(Object rawStack,int amount,Object rawActor){var stack=(ItemStack)rawStack;if(amount!=1||!convert(stack,(LivingEntity)rawActor))stack.shrink(amount);}
 /** Own non-eating finish methods call this after their existing effects and statistics. */
 public static ItemStack ownFinish(ItemStack stack,LivingEntity actor){return convert(stack,actor)?active.get().result():null;}
 /** Own BowlMeal must not generate its second bowl after native eat already converted once. */
 public static ItemStack convertedResult(ItemStack stack,LivingEntity actor){var frame=active.get();return frame!=null&&frame.matches(stack,actor)&&frame.converted?frame.result():null;}
 /** Native potion/honey owners contain a second container insertion after the actual consume. */
 public static boolean addContainer(Object rawInventory,Object rawStack){
  var inventory=(net.minecraft.world.entity.player.Inventory)rawInventory;var stack=(ItemStack)rawStack;var frame=active.get();
  if(frame!=null&&frame.converted&&frame.kind==GLASS&&frame.player.getInventory()==inventory&&stack.is(Items.GLASS_BOTTLE))return true;
  return inventory.add(stack);
 }

 // Only LivingEntity.completeUsingItem's actual item callback establishes this owner.
 private static final ThreadLocal<Owner> completedOwner=new ThreadLocal<>();
 private record Owner(ItemStack input,LivingEntity actor,Player player,Item item){}
 public static ItemStack ownerFinish(Object rawStack,Object rawLevel,Object rawActor){
  var input=(ItemStack)rawStack;var level=(Level)rawLevel;var actor=(LivingEntity)rawActor;
  if(!(actor instanceof Player player)||player.getMainHandItem()!=input||kind(input)<0)return input.finishUsingItem(level,actor);
  var previous=completedOwner.get();var owner=new Owner(input,actor,player,input.getItem());completedOwner.set(owner);
  try{var result=input.finishUsingItem(level,actor);if(!level.isClientSide)player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(owner.item));return result;}
  finally{if(previous==null)completedOwner.remove();else completedOwner.set(previous);}
 }
 // Explicit input identity prevents a nested direct finish of another same-item stack
 // from being mistaken for this actor's completed use. No global statistic interception.
 public static void finishStat(Object rawPlayer,Object rawStat,Object rawInput){
  var player=(Player)rawPlayer;var stat=(net.minecraft.stats.Stat<?>)rawStat;var owner=completedOwner.get();var frame=active.get();
  if(owner!=null&&owner.player==player&&owner.input==rawInput&&frame!=null&&frame.matches(owner.input,owner.actor)&&stat.getType()==net.minecraft.stats.Stats.ITEM_USED&&stat.getValue()==owner.item)return;
  player.awardStat(stat);
 }
 private SourceUseFinish1042(){}
}
