package dev.koikaster.mite;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** R195 ItemBowl + ItemVessel. Human choice48D keeps modern unlimited adult-cow
 * milking and native mooshroom bowl use; finite milk and livestock thirst stay out. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class OriginalBowls {
 private static final Set<String> SOUPS=Set.of("chicken_soup","vegetable_stew","cream_of_mushroom_soup","cream_of_vegetable_soup","pumpkin_soup");
 private static final Set<String> FULL=Set.of("water_bowl","milk_bowl","meat_stew","chicken_soup","vegetable_stew","ice_cream","salad","cream_of_mushroom_soup","cream_of_vegetable_soup","pumpkin_soup","mashed_potato","sorbet","porridge","cereal");
 public static void register(){
  Content.item("water_bowl",()->new WaterBowl());
  Content.item("milk_bowl",()->meal("milk_bowl",0,1,true));
  Content.item("chicken_soup",()->meal("chicken_soup",10,10,false));
  Content.item("ice_cream",()->meal("ice_cream",5,4,false));
  Content.item("cream_of_mushroom_soup",()->meal("cream_of_mushroom_soup",3,5,false));
  Content.item("cream_of_vegetable_soup",()->meal("cream_of_vegetable_soup",7,7,false));
  Content.item("mashed_potato",()->meal("mashed_potato",12,8,false));
  Content.item("sorbet",()->meal("sorbet",4,2,false));
  Content.item("cereal",()->meal("cereal",4,2,false));
  Content.item("blueberries",()->new Item(new Item.Properties().stacksTo(16).food(food(1,1,false))));
  SourceBlueberryBush.register();
 }
 private static FoodProperties food(int satiation,int nutrition,boolean always){var builder=new FoodProperties.Builder().nutrition(nutrition).saturationMod(nutrition==0?0:satiation/(2f*nutrition));if(always)builder.alwaysEat();return builder.build();}
 public static Item meal(String name,int satiation,int nutrition,boolean always){return new Meal(name,new Item.Properties().stacksTo(4).food(food(satiation,nutrition,always)));}
 private static String name(ItemStack stack){var id=ForgeRegistries.ITEMS.getKey(stack.getItem());return id!=null&&id.getNamespace().equals(MiteReforged.ID)?id.getPath():"";}
 public static boolean full(ItemStack stack){return !stack.isEmpty()&&(stack.is(Items.MUSHROOM_STEW)||FULL.contains(name(stack)));}
 public static boolean water(ItemStack stack){return name(stack).equals("water_bowl");}
 public static boolean douses(ItemStack stack){String name=name(stack);return water(stack)||name.equals("milk_bowl")||name.equals("ice_cream")||SOUPS.contains(name);}
 private static boolean prefersIngestion(Player player,ItemStack held){return !water(held)&&full(held)&&SurvivalFood.canIngest(player,held);}
 static void quench(Level level,BlockPos pos){level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,.7f,1.6f+(level.random.nextFloat()-level.random.nextFloat())*.4f);if(level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(ParticleTypes.LARGE_SMOKE,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,8,.3,.3,.3,0);}
 /** Shared held-item transaction. Empty bowls scoop flowing water without removing it. */
 public static InteractionResult blockUse(Level level,Player player,InteractionHand hand,BlockHitResult hit){
  var held=player.getItemInHand(hand);if(hit.getType()!=HitResult.Type.BLOCK||(!held.is(Items.BOWL)&&!full(held)))return InteractionResult.PASS;
  var pos=hit.getBlockPos();var next=pos.relative(hit.getDirection());if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),held))return InteractionResult.FAIL;
  if(held.is(Items.BOWL)&&(level.getFluidState(pos).is(FluidTags.WATER)||level.getFluidState(next).is(FluidTags.WATER))){
   var target=level.getFluidState(pos).is(FluidTags.WATER)?pos:next;if(!level.mayInteract(player,target)||!player.mayUseItemAt(target,hit.getDirection(),held))return InteractionResult.FAIL;
   if(!level.isClientSide)ClassicVessels.replaceOne(player,hand,new ItemStack(Content.get("water_bowl")));
   return InteractionResult.sidedSuccess(level.isClientSide);
  }
  if(douses(held)&&level.getBlockState(next).getBlock() instanceof BaseFireBlock&&!prefersIngestion(player,held)){
   if(!level.mayInteract(player,next)||!player.mayUseItemAt(next,hit.getDirection(),held))return InteractionResult.FAIL;
   if(!level.isClientSide){level.removeBlock(next,false);quench(level,next);ClassicVessels.replaceOne(player,hand,new ItemStack(Items.BOWL));}
   return InteractionResult.sidedSuccess(level.isClientSide);
  }
  return InteractionResult.PASS;
 }
 @SubscribeEvent(priority=EventPriority.HIGH) public static void block(PlayerInteractEvent.RightClickBlock event){var result=blockUse(event.getLevel(),event.getEntity(),event.getHand(),new BlockHitResult(event.getHitVec().getLocation(),event.getFace(),event.getPos(),false));if(result!=InteractionResult.PASS){event.setCanceled(true);event.setCancellationResult(result);}}
 @SubscribeEvent(priority=EventPriority.HIGH) public static void emptyUse(PlayerInteractEvent.RightClickItem event){if(!event.getItemStack().is(Items.BOWL))return;var result=blockUse(event.getLevel(),event.getEntity(),event.getHand(),OriginalRaycast.itemPov(event.getLevel(),event.getEntity(),ClipContext.Fluid.ANY));if(result!=InteractionResult.PASS){event.setCanceled(true);event.setCancellationResult(result);}}
 @SubscribeEvent(priority=EventPriority.HIGH) public static void entity(PlayerInteractEvent.EntityInteract event){
  var player=event.getEntity();var held=event.getItemStack();var target=event.getTarget();
  if(held.is(Items.BOWL)&&target instanceof Cow cow&&!(cow instanceof MushroomCow)&&!cow.isBaby()){
   if(!event.getLevel().isClientSide){if(!player.isCreative())ClassicVessels.replaceOne(player,event.getHand(),new ItemStack(Content.get("milk_bowl")));player.playSound(SoundEvents.COW_MILK,1,1);}
   event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;
  }
  if(!douses(held)||!(target instanceof Mobs.Undead mob))return;
  boolean fire=Mobs.name(mob).equals("fire_elemental"),magma=Mobs.name(mob).equals("earth_elemental")&&mob.originalEarth().magma();if(!fire&&!magma)return;
  if(!event.getLevel().isClientSide){if(fire){mob.hurt(mob.damageSources().drown(),5);Network.environmentSteam(mob);}else mob.originalEarth().normal(true);if(!player.isCreative())ClassicVessels.replaceOne(player,event.getHand(),new ItemStack(Items.BOWL));}
  event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
 }
 /** Source ss.spentTickInWater applies to every bowl, including food-filled ones. */
 public static void dropped(ItemEntity entity){if(entity.level().isClientSide||(!entity.getItem().is(Items.BOWL)&&!full(entity.getItem()))||water(entity.getItem())||!entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER))return;
  var held=entity.getItem();int count=held.getCount();entity.setItem(new ItemStack(Content.get("water_bowl"),Math.min(4,count)));
  for(int left=count-4;left>0;left-=4){var extra=new ItemEntity(entity.level(),entity.getX(),entity.getY(),entity.getZ(),new ItemStack(Content.get("water_bowl"),Math.min(4,left)));var tag=entity.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.remove("UUID");tag.put("Item",extra.getItem().save(new net.minecraft.nbt.CompoundTag()));extra.load(tag);entity.level().addFreshEntity(extra);}
 }
 public static final class Meal extends SpecialItems.BowlMeal {
  private final String name;Meal(String name,Properties properties){super(properties);this.name=name;}
  public UseAnim getUseAnimation(ItemStack stack){return SOUPS.contains(name)||name.equals("milk_bowl")?UseAnim.DRINK:UseAnim.EAT;}
  public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity actor){if(name.equals("milk_bowl")&&!level.isClientSide)actor.removeAllEffects();return super.finishUsingItem(stack,level,actor);}
 }
 public static final class WaterBowl extends Item {
  WaterBowl(){super(new Properties().stacksTo(4));}
  public boolean hasCraftingRemainingItem(ItemStack stack){return true;}
  public ItemStack getCraftingRemainingItem(ItemStack stack){return new ItemStack(Items.BOWL);}
  public int getUseDuration(ItemStack stack){return 32;}
  public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
  public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var result=blockUse(level,player,hand,OriginalRaycast.itemPov(level,player,ClipContext.Fluid.ANY));if(result!=InteractionResult.PASS)return new InteractionResultHolder<>(result,player.getItemInHand(hand));var held=player.getItemInHand(hand);if(!SurvivalFood.canIngest(player,held))return InteractionResultHolder.fail(held);player.startUsingItem(hand);return InteractionResultHolder.consume(held);}
  public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity actor){if(actor instanceof Player player){if(!level.isClientSide)SourceUseFinish1042.finishStat(player,Stats.ITEM_USED.get(this),stack);if(player.isCreative())return stack;}var converted=SourceUseFinish1042.ownFinish(stack,actor);if(converted!=null)return converted;stack.shrink(1);var bowl=new ItemStack(Items.BOWL);if(stack.isEmpty())return bowl;if(!level.isClientSide){if(actor instanceof Player player){if(!player.getInventory().add(bowl))player.drop(bowl,false);}else actor.spawnAtLocation(bowl);}return stack;}
 }
 private OriginalBowls(){}
}
