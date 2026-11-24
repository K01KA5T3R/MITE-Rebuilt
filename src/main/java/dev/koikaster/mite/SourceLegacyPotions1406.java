package dev.koikaster.mite;
import java.util.*;import net.minecraft.nbt.Tag;import net.minecraft.world.item.*;import net.minecraft.world.item.alchemy.*;import net.minecraft.world.effect.*;
/** Legacy bottle identities, exact brewed subtypes and source effect contents. */
public final class SourceLegacyPotions1406{
 public static final String TAG="MiteR195Potion";
 private static final Map<Potion,Integer> DRINKS=Map.ofEntries(
  Map.entry(Potions.WATER,0),Map.entry(Potions.AWKWARD,16),Map.entry(Potions.THICK,32),Map.entry(Potions.MUNDANE,64),
  Map.entry(Potions.REGENERATION,8193),Map.entry(Potions.SWIFTNESS,8194),Map.entry(Potions.POISON,8196),Map.entry(Potions.STRENGTH,8201),
  Map.entry(Potions.STRONG_REGENERATION,8225),Map.entry(Potions.STRONG_SWIFTNESS,8226),Map.entry(Potions.FIRE_RESISTANCE,8227),Map.entry(Potions.STRONG_POISON,8228),Map.entry(Potions.STRONG_HEALING,8229),Map.entry(Potions.NIGHT_VISION,8230),Map.entry(Potions.WEAKNESS,8232),Map.entry(Potions.STRONG_STRENGTH,8233),Map.entry(Potions.SLOWNESS,8234),Map.entry(Potions.STRONG_HARMING,8236),Map.entry(Potions.INVISIBILITY,8238),
  Map.entry(Potions.LONG_REGENERATION,8257),Map.entry(Potions.LONG_SWIFTNESS,8258),Map.entry(Potions.LONG_FIRE_RESISTANCE,8259),Map.entry(Potions.LONG_POISON,8260),Map.entry(Potions.HEALING,8261),Map.entry(Potions.LONG_NIGHT_VISION,8262),Map.entry(Potions.LONG_WEAKNESS,8264),Map.entry(Potions.LONG_STRENGTH,8265),Map.entry(Potions.LONG_SLOWNESS,8266),Map.entry(Potions.HARMING,8268),Map.entry(Potions.LONG_INVISIBILITY,8270));
 public static Integer subtype(ItemStack stack){
  if(!stack.is(Items.POTION)&&!stack.is(Items.SPLASH_POTION))return null;var data=stack.getTag();
  if(data!=null&&data.contains(TAG,Tag.TAG_ANY_NUMERIC))return data.getInt(TAG)&32767;
  if(data!=null&&data.contains("MiteR195WitchPotion",Tag.TAG_ANY_NUMERIC))return data.getInt("MiteR195WitchPotion")&32767;
  var value=DRINKS.get(PotionUtils.getPotion(stack));if(value==null)return null;
  return stack.is(Items.SPLASH_POTION)?(value&~8192)|16384:value;
 }
 private static MobEffect effect(int id){return switch(id){case 1->MobEffects.MOVEMENT_SPEED;case 2->MobEffects.MOVEMENT_SLOWDOWN;case 5->MobEffects.DAMAGE_BOOST;case 6->MobEffects.HEAL;case 7->MobEffects.HARM;case 10->MobEffects.REGENERATION;case 12->MobEffects.FIRE_RESISTANCE;case 14->MobEffects.INVISIBILITY;case 16->MobEffects.NIGHT_VISION;case 18->MobEffects.WEAKNESS;case 19->MobEffects.POISON;default->throw new IllegalArgumentException("Original potion effect "+id);};}
 /** Null delegates retained modern/third-party bottle contents to their native owner. */
 public static List<MobEffectInstance> contents(Object raw){var stack=(ItemStack)raw;var subtype=subtype(stack);if(subtype==null)return null;
  if(stack.hasTag()&&stack.getTag().contains(PotionUtils.TAG_CUSTOM_POTION_EFFECTS,Tag.TAG_LIST))return PotionUtils.getCustomEffects(stack);
  return OriginalPotionRules1403.effects(subtype).stream().map(e->new MobEffectInstance(effect(e.id()),e.duration(),e.amplifier())).toList();
 }
 private static String ingredient(ItemStack stack){var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());return key==null?"":key.toString();}
 /** Native hasMix order is bottle, ingredient. */
 public static Boolean hasMix(Object rawBottle,Object rawIngredient){var bottle=(ItemStack)rawBottle;var ingredient=(ItemStack)rawIngredient;var subtype=subtype(bottle);String key=ingredient(ingredient);if(subtype==null||!OriginalPotionRules1403.ingredients().containsKey(key))return null;
  return OriginalPotionRules1403.canBrew(subtype,OriginalPotionRules1403.mixedSubtype(subtype,key));
 }
 /** Native mix order is ingredient, bottle. Exact metadata survives save and network NBT. */
 public static ItemStack mix(Object rawIngredient,Object rawBottle){var bottle=(ItemStack)rawBottle;var ingredient=(ItemStack)rawIngredient;Boolean allowed=hasMix(bottle,ingredient);if(allowed==null)return null;if(!allowed)return bottle;
  int next=OriginalPotionRules1403.mixedSubtype(subtype(bottle),ingredient(ingredient));var output=stack(next);if(bottle.hasTag()){var tag=bottle.getTag().copy();tag.putString(PotionUtils.TAG_POTION,net.minecraftforge.registries.ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(output)).toString());tag.remove("MiteR195WitchPotion");tag.putInt(TAG,next);output.setTag(tag);}output.setCount(bottle.getCount());return output;
 }
 public static ItemStack stack(int subtype){boolean splash=(subtype&16384)!=0;var target=OriginalPotionRules1403.effects(subtype);Potion potion=null;
  for(var entry:DRINKS.entrySet()){int value=splash?(entry.getValue()&~8192)|16384:entry.getValue();if(!OriginalPotionRules1403.effects(value).equals(target))continue;if(!target.isEmpty()){potion=entry.getKey();break;}}
  if(potion==null)potion=subtype==0?Potions.WATER:subtype==16?Potions.AWKWARD:subtype==32?Potions.THICK:Potions.MUNDANE;
  var output=PotionUtils.setPotion(new ItemStack(splash?Items.SPLASH_POTION:Items.POTION),potion);output.getOrCreateTag().putInt(TAG,subtype&32767);return output;
 }
 private SourceLegacyPotions1406(){}
}
