package dev.koikaster.mite;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** Adapts the R195 two-store tick to the vanilla food save fields and Forge food API. */
public final class SurvivalFood {
 private static final Map<FoodData,WeakReference<Player>> OWNERS=Collections.synchronizedMap(new WeakHashMap<>());
 public record Energy(int satiation,int nutrition){}
 public static Energy energy(ItemStack stack,Player player){
  var profile=Diet.profile(stack,player);var food=stack.getFoodProperties(player);
  int satiation=profile!=null&&profile.counterpart()?profile.satiation():food==null?0:Math.max(0,Math.round(food.getNutrition()*food.getSaturationModifier()*2));
  int nutrition=profile!=null&&profile.counterpart()?profile.nutrition():food==null?0:Math.max(0,food.getNutrition());
  if(profile!=null&&player!=null)satiation=Math.max(0,Diet.read(player).adjustedSatiation(satiation,profile.sugar()));
  return new Energy(satiation,nutrition);
 }
 public static void initialize(Player player){
  var food=player.getFoodData();OWNERS.put(food,new WeakReference<>(player));
  if(player.level().isClientSide||player.isCreative()||player.isSpectator())return;
  var data=PlayerState.data(player);Diet.initialize(data);int cap=PlayerState.foodCap(player);
  if(!data.getBoolean("ClassicFoodInitialized")){
   if(data.getBoolean("FreshFoodStores")){food.setFoodLevel(cap);food.setSaturation(cap);food.setExhaustion(0);data.remove("FreshFoodStores");}
   else {food.setFoodLevel(Math.min(cap,Math.max(0,food.getFoodLevel())));food.setSaturation(Math.min(cap,Math.max(0,food.getSaturationLevel())));}
   data.putBoolean("ClassicFoodInitialized",true);
  }
 }
 /** Called in place of Player's single FoodData.tick call. Creative/spectator retain vanilla behavior. */
 public static void tick(Object rawFood,Object rawPlayer){
  var food=(FoodData)rawFood;var player=(Player)rawPlayer;
  if(!(player instanceof ServerPlayer)||player.isCreative()||player.isSpectator()){food.tick(player);return;}
  if(!player.isAlive())return;
  initialize(player);var data=PlayerState.data(player);int cap=PlayerState.foodCap(player);var state=read(food,data,cap);
  float rate=(float)Settings.EXHAUSTION;state.addHunger(food.getExhaustionLevel(),rate);food.setExhaustion(0);
  var diet=Diet.tick(player);
  var result=state.tick(new LegacyFood.Context(player.getHealth(),player.getHealth()<player.getMaxHealth(),
   player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION),diet.malnourished(),player.isSleeping(),player.level().getDifficulty().getId(),Diet.hungerMultiplier(player,diet),rate,1+.5f*net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(Content.REGENERATION.get(),player)));
  write(food,data,state);if(result.heal())player.heal(1);if(result.starveDamage())player.hurt(player.damageSources().starve(),1);
 }
 /** Coarse vanilla eligibility; item-specific eligibility is checked by Forge's use-item event. */
 public static boolean canEat(Object rawPlayer,boolean alwaysEdible){
  var player=(Player)rawPlayer;
  if(player.getAbilities().invulnerable)return true;
  var food=player.getFoodData();int cap=PlayerState.foodCap(player);
  return alwaysEdible||food.getFoodLevel()<cap||food.getSaturationLevel()<cap||Diet.read(player).malnourished();
 }
 /** Supplied R195 bex movement gate uses hasFoodEnergy; retain modern passenger/flying exceptions. */
 public static boolean canSprint(Object rawPlayer){
  var player=(Player)rawPlayer;var food=player.getFoodData();
  return (player.isPassenger()||player.getAbilities().mayfly||food.getFoodLevel()>0||food.getSaturationLevel()>0)&&!OriginalCurseEffects.has(player,3,true);
 }
 /** R195 uf: either reserve can supply action energy, including nutrition level one. */
 public static boolean hasFoodEnergy(Player player){var food=player.getFoodData();return food.getFoodLevel()>0||food.getSaturationLevel()>0;}
 public static boolean canEatCake(Object rawPlayer,boolean ignored){
  var player=(Player)rawPlayer;var food=player.getFoodData();OWNERS.put(food,new WeakReference<>(player));
  if(player.isCreative())return true;var diet=Diet.read(player);var slice=Diet.CAKE_SLICE;
  return diet.needsNutrients(slice.mask())||LegacyFood.canIngest((int)food.getSaturationLevel(),food.getFoodLevel(),PlayerState.foodCap(player),diet.adjustedSatiation(slice.satiation(),slice.sugar()),slice.nutrition());
 }
 public static void eatCake(Object rawFood,int ignoredNutrition,float ignoredSaturation){
  var food=(FoodData)rawFood;var owner=OWNERS.get(food);var player=owner==null?null:owner.get();
  if(player==null){eatGeneric(food,2,.5f);return;}
  var slice=Diet.CAKE_SLICE;int cap=player.isCreative()?20:PlayerState.foodCap(player);
  food.setFoodLevel(Math.min(cap,food.getFoodLevel()+slice.nutrition()));food.setSaturation(Math.min(cap,food.getSaturationLevel()+Diet.read(player).adjustedSatiation(slice.satiation(),slice.sugar())));
  Diet.eat(player,slice,slice.nutrition());
 }
 public static void drinkMilk(Object entity){
  if(!(entity instanceof Player player)||player.level().isClientSide||player.isCreative()||player.isSpectator())return;
  initialize(player);var milk=Diet.MILK_BUCKET;var food=player.getFoodData();food.setFoodLevel(Math.min(PlayerState.foodCap(player),food.getFoodLevel()+milk.nutrition()));Diet.eat(player,milk,milk.nutrition());
 }
 public static boolean canIngest(Player player,ItemStack stack){
  return canIngestBeforeCurse(player,stack)&&!CurseIngestion.forbidden(player,stack);
 }
 /** Eligibility precedes discovery in uf.canIngest; a full player does not learn a food curse. */
 private static boolean canIngestBeforeCurse(Player player,ItemStack stack){
  if(!stack.isEdible()||player.isCreative())return true;
  var properties=stack.getFoodProperties(player);if(properties==null)return false;
  if(properties.canAlwaysEat())return true;
  var food=player.getFoodData();var energy=energy(stack,player);
  var profile=Diet.profile(stack,player);
  if(profile!=null&&Diet.read(player).needsNutrients(profile.mask()))return true;
  if(energy.satiation()==0&&energy.nutrition()==0&&(profile==null||profile.sugar()==0))return true;
  return LegacyFood.canIngest((int)food.getSaturationLevel(),food.getFoodLevel(),PlayerState.foodCap(player),energy.satiation(),energy.nutrition());
 }
 /** Forge's entity-aware eating entry point; FoodData's item overload delegates here as well. */
 public static void eat(Object rawFood,Object rawStack,Object rawEntity){
  var food=(FoodData)rawFood;var stack=(ItemStack)rawStack;if(stack.is(net.minecraft.world.item.Items.EGG)&&rawEntity instanceof ServerPlayer sourcePlayer)SourceAchievementService570.grant(sourcePlayer,"eggs");
  if(!(rawEntity instanceof Player player)||player.isCreative()){
   var properties=stack.getFoodProperties(rawEntity instanceof net.minecraft.world.entity.LivingEntity living?living:null);
   if(properties!=null)eatGeneric(food,properties.getNutrition(),properties.getSaturationModifier());return;
  }
  if(!stack.isEdible())return;initialize(player);var energy=energy(stack,player);int cap=PlayerState.foodCap(player);
  food.setFoodLevel(Math.min(cap,Math.max(0,food.getFoodLevel()+energy.nutrition())));
  food.setSaturation(Math.min(cap,Math.max(0,food.getSaturationLevel()+energy.satiation())));
  Diet.eat(player,stack,energy.nutrition());
 }
 public static void eatGeneric(Object rawFood,int nutrition,float saturation){
  var food=(FoodData)rawFood;var owner=OWNERS.get(food);var player=owner==null?null:owner.get();
  int cap=player==null||player.isCreative()||player.isSpectator()?20:PlayerState.foodCap(player);
  food.setFoodLevel(Math.min(cap,Math.max(0,food.getFoodLevel()+nutrition)));
  float limit=player==null||player.isCreative()||player.isSpectator()?food.getFoodLevel():cap;
  food.setSaturation(Math.min(limit,Math.max(0,food.getSaturationLevel()+nutrition*saturation*2)));
 }
 public static void reset(CompoundTag data){
  for(String key:new String[]{"ClassicFoodInitialized","FoodHunger","FoodNutritionHunger","FoodHealProgress","FoodStarveProgress"})data.remove(key);
  data.putBoolean("FreshFoodStores",true);
 }
 private static float finite(CompoundTag data,String key,float limit){float value=data.getFloat(key);return Float.isFinite(value)?Math.max(0,Math.min(limit,value)):0;}
 private static LegacyFood read(FoodData food,CompoundTag data,int cap){
  var state=new LegacyFood();state.nutrition=Math.min(cap,Math.max(0,food.getFoodLevel()));state.satiation=Math.min(cap,Math.max(0,(int)food.getSaturationLevel()));
  state.hunger=finite(data,"FoodHunger",40);state.nutritionHunger=finite(data,"FoodNutritionHunger",40);
  state.healProgress=finite(data,"FoodHealProgress",1);state.starveProgress=finite(data,"FoodStarveProgress",1);return state;
 }
 private static void write(FoodData food,CompoundTag data,LegacyFood state){
  food.setFoodLevel(state.nutrition);food.setSaturation(state.satiation);data.putFloat("FoodHunger",state.hunger);data.putFloat("FoodNutritionHunger",state.nutritionHunger);
  data.putFloat("FoodHealProgress",state.healProgress);data.putFloat("FoodStarveProgress",state.starveProgress);
 }
 private SurvivalFood(){}
}
