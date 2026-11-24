package dev.koikaster.mite;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Persistence and Minecraft adapter for verified R195 nutrients, with explicit modern food entries. */
public final class Diet {
 public record Profile(int satiation,int nutrition,int sugar,int mask,boolean counterpart){}
 public static final Profile CAKE_SLICE=new Profile(2,2,1000/6,1,true),MILK_BUCKET=new Profile(0,4,0,1,true);
 private static final Map<String,Profile> PROFILES=profiles();
 private static final Map<String,net.minecraft.world.food.FoodProperties> EDIBLE_OVERRIDES=new java.util.concurrent.ConcurrentHashMap<>();
 /** Restores source golden apple effects and food properties for original edible items. */
 public static Object foodProperties(Object original,Object rawItem){
  if(original==Foods.GOLDEN_APPLE&&rawItem==Items.GOLDEN_APPLE){
   return EDIBLE_OVERRIDES.computeIfAbsent("minecraft:golden_apple",ignored->goldenApple(Foods.GOLDEN_APPLE,false));
  }
  if(original==Foods.ENCHANTED_GOLDEN_APPLE&&rawItem==Items.ENCHANTED_GOLDEN_APPLE){
   return EDIBLE_OVERRIDES.computeIfAbsent("minecraft:enchanted_golden_apple",ignored->goldenApple(Foods.ENCHANTED_GOLDEN_APPLE,true));
  }
  if(original!=null||net.minecraftforge.registries.ForgeRegistries.ITEMS==null)return original;
  String key=String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey((Item)rawItem));var profile=PROFILES.get(key);
  if(profile==null||!profile.counterpart())return original;
  return EDIBLE_OVERRIDES.computeIfAbsent(key,ignored->new net.minecraft.world.food.FoodProperties.Builder().nutrition(profile.nutrition()).saturationMod(profile.nutrition()==0?0:profile.satiation()/(2f*profile.nutrition())).build());
 }
 private static FoodProperties goldenApple(FoodProperties original,boolean enchanted){
  var food=new FoodProperties.Builder().nutrition(original.getNutrition()).saturationMod(original.getSaturationModifier())
   .alwaysEat().effect(()->new MobEffectInstance(MobEffects.REGENERATION,1200,enchanted?1:0),1f);
  if(enchanted){
   food.effect(()->new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,1200,0),1f);
   food.effect(()->new MobEffectInstance(MobEffects.FIRE_RESISTANCE,1200,0),1f);
  }
  return food.build();
 }
 /** R195 xx.onEaten supplies the original risk on player consumption. */
 public static FoodProperties foodEffectProperties(FoodProperties original,Object rawStack,Object eater){
  if(!(eater instanceof Player))return original;
  var stack=(ItemStack)rawStack;
  if(original==Foods.CHICKEN&&stack.is(Items.CHICKEN)){
   return EDIBLE_OVERRIDES.computeIfAbsent("minecraft:chicken",ignored->poisonousMeat(original,.3f));
  }
  if(original==Foods.ROTTEN_FLESH&&stack.is(Items.ROTTEN_FLESH)){
   return EDIBLE_OVERRIDES.computeIfAbsent("minecraft:rotten_flesh",ignored->poisonousMeat(original,.8f));
  }
  return original;
 }
 private static FoodProperties poisonousMeat(FoodProperties original,float chance){
  return new FoodProperties.Builder().nutrition(original.getNutrition()).saturationMod(original.getSaturationModifier())
   .meat().effect(()->new MobEffectInstance(MobEffects.POISON,400,0),chance).build();
 }
 private static Map<String,Profile> profiles(){
  var result=new HashMap<String,Profile>();
  try(var stream=Diet.class.getResourceAsStream("/data/mite_reforged/diet_profiles.json")){
   if(stream==null)throw new IllegalStateException("Missing diet profile resource");
   var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
   for(String group:new String[]{"counterparts","modern_adaptations"})for(var entry:data.getAsJsonObject(group).entrySet()){
    var food=entry.getValue().getAsJsonObject();boolean counterpart=group.equals("counterparts");
    int satiation=counterpart?food.get("satiation").getAsInt():0,nutrition=counterpart?food.get("nutrition").getAsInt():0;
    int sugar=food.get("sugar").getAsInt(),mask=food.get("nutrient_mask").getAsInt();
    if(satiation<0||satiation>40||nutrition<0||nutrition>40||sugar<0||sugar>40000||mask<0||mask>7)throw new IllegalStateException("Invalid diet profile: "+entry.getKey());
    if(result.put(entry.getKey(),new Profile(satiation,nutrition,sugar,mask,counterpart))!=null)throw new IllegalStateException("Duplicate diet profile: "+entry.getKey());
   }
  }catch(Exception error){throw new IllegalStateException("Unable to load diet profiles",error);}
  return Map.copyOf(result);
 }
 public static Profile profile(ItemStack stack){if(stack.getItem() instanceof ScorchedFood2072)return ScorchedFood2072.diet(stack);var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());String key=String.valueOf(id);var profile=PROFILES.get(key);return profile!=null?profile:FruitForaging.diet(key);}
 public static Profile profile(ItemStack stack,Player player){
  return IteGoldenApples.profile(profile(stack),stack,player);
 }
 public static void initialize(CompoundTag data){
  if(data.getInt("DietSchema")==1)return;
  var state=new LegacyDiet();
  if(data.contains("Protein")||data.contains("Phyto")){
   if(data.contains("Protein"))state.protein=fromPercent(data.getDouble("Protein"));
   if(data.contains("Phyto"))state.phytonutrients=fromPercent(data.getDouble("Phyto"));
   var previous=new CompoundTag();for(String key:new String[]{"Protein","Vegetable","Phyto","Sugar"})if(data.contains(key)){previous.put(key,data.get(key).copy());data.remove(key);}data.put("PreClassicDiet",previous);
  }
  // No old essential-fat or insulin history exists; initialize those new stores to original defaults.
  write(data,state);data.putInt("DietSchema",1);
 }
 private static int fromPercent(double value){return Double.isFinite(value)?(int)Math.max(0,Math.min(LegacyDiet.NUTRIENT_LIMIT,value*1600)):0;}
 public static LegacyDiet read(Player player){return read(PlayerState.data(player));}
 public static LegacyDiet read(CompoundTag data){
  initialize(data);var state=new LegacyDiet();state.protein=nutrient(data,"DietProtein");state.fats=nutrient(data,"DietFats");state.phytonutrients=nutrient(data,"DietPhyto");
  state.resistance=Math.max(0,Math.min(3,data.getInt("DietResistance")));state.setInsulin(data.getInt("DietInsulin"));return state;
 }
 private static int nutrient(CompoundTag data,String key){return Math.max(0,Math.min(LegacyDiet.NUTRIENT_LIMIT,data.getInt(key)));}
 public static void write(CompoundTag data,LegacyDiet state){
  data.putInt("DietProtein",state.protein);data.putInt("DietFats",state.fats);data.putInt("DietPhyto",state.phytonutrients);data.putInt("DietInsulin",state.insulin);data.putInt("DietResistance",state.resistance);
 }
 public static LegacyDiet tick(Player player){var state=read(player);state.tick();write(PlayerState.data(player),state);return state;}
 public static void eat(Player player,ItemStack stack,int nutrition){
  if(player.level().isClientSide||player.isCreative()||player.isSpectator())return;
  var profile=profile(stack,player);if(profile==null)return;eat(player,profile,nutrition);
  if(stack.is(net.minecraft.world.item.Items.WHEAT_SEEDS)){var state=read(player);state.fats=Math.min(160000,state.fats+2000);write(PlayerState.data(player),state);}
 }
 public static void eat(Player player,Profile profile,int nutrition){
  if(player.level().isClientSide||player.isCreative()||player.isSpectator())return;
  var state=read(player);int response=state.addFood(nutrition,profile.sugar(),profile.mask());write(PlayerState.data(player),state);
  if(response>0&&state.resistance>0){
   player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,400,state.resistance-1));
   if(state.resistance==3)player.addEffect(new MobEffectInstance(MobEffects.POISON,Math.max((int)(response/48f),100),0));
  }
 }
 public static void reset(CompoundTag data){data.putInt("DietSchema",1);write(data,new LegacyDiet());data.remove("PreClassicDiet");}
 public static float hungerMultiplier(Player player,LegacyDiet diet){
  var feet=player.blockPosition();var level=player.level();var biome=level.getBiome(feet).value();
  int water=level.getFluidState(feet.above()).is(net.minecraft.tags.FluidTags.WATER)?2:level.getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER)?1:0;
  boolean rain=level.isRainingAt(feet)||level.isRainingAt(net.minecraft.core.BlockPos.containing(player.getX(),player.getBoundingBox().maxY,player.getZ()));
  return LegacyDiet.hungerMultiplier(rain,level.isThundering(),water,biome.getBaseTemperature()<=.15f,biome.getBaseTemperature(),diet.malnourished());
 }
 private Diet(){}
}
