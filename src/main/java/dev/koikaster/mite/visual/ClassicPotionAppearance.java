package dev.koikaster.mite.visual;

import dev.koikaster.mite.MiteReforged;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Collection;
import java.util.Map;

/** Client item appearance only. Effect identities, attributes, packets and NBT stay native. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClassicPotionAppearance {
 private static final Map<MobEffect,Integer> COLORS=Map.ofEntries(
  Map.entry(MobEffects.MOVEMENT_SPEED,8171462),Map.entry(MobEffects.MOVEMENT_SLOWDOWN,5926017),
  Map.entry(MobEffects.DAMAGE_BOOST,9643043),Map.entry(MobEffects.HARM,4393481),
  Map.entry(MobEffects.JUMP,7889559),Map.entry(MobEffects.DAMAGE_RESISTANCE,10044730),
  Map.entry(MobEffects.FIRE_RESISTANCE,14981690),Map.entry(MobEffects.WATER_BREATHING,3035801),
  Map.entry(MobEffects.INVISIBILITY,8356754),Map.entry(MobEffects.NIGHT_VISION,2039713),
  Map.entry(MobEffects.POISON,5149489),Map.entry(MobEffects.WITHER,3484199));
 @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
  event.register(ClassicPotionAppearance::color,Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION,Items.TIPPED_ARROW);
 }
 public static int color(ItemStack stack,int tint){
  if(tint>0)return -1;
  if(!ClassicVisualState.ready||!ClassicVisuals.textures())return PotionUtils.getColor(stack);
  var tag=stack.getTag();
  if(tag!=null&&tag.contains(PotionUtils.TAG_CUSTOM_POTION_COLOR,99))return tag.getInt(PotionUtils.TAG_CUSTOM_POTION_COLOR);
  return PotionUtils.getPotion(stack)==Potions.EMPTY?0xf800f8:mix(PotionUtils.getMobEffects(stack));
 }
 public static int effectColor(MobEffect effect){return COLORS.getOrDefault(effect,effect.getColor());}
 /** Preserve native weighting, hidden effects, empty water and explicit custom colors. */
 public static int mix(Collection<MobEffectInstance> effects){
  if(effects.isEmpty())return 3694022;
  float red=0,green=0,blue=0;int weight=0;
  for(var effect:effects){
   if(!effect.isVisible())continue;
   int rgb=effectColor(effect.getEffect()),amount=effect.getAmplifier()+1;
   red+=(float)(amount*(rgb>>16&255))/255;green+=(float)(amount*(rgb>>8&255))/255;blue+=(float)(amount*(rgb&255))/255;weight+=amount;
  }
  if(weight==0)return 0;
  return (int)(red/weight*255)<<16|(int)(green/weight*255)<<8|(int)(blue/weight*255);
 }
 /** Called only by client item renderers, never by the shared ItemStack/PotionItem. */
 public static boolean hasFoil(Object raw){
  var stack=(ItemStack)raw;if(stack.hasFoil())return true;
  return ClassicVisualState.ready&&ClassicVisuals.particles()
   &&(stack.is(Items.POTION)||stack.is(Items.SPLASH_POTION)||stack.is(Items.LINGERING_POTION))
   &&!PotionUtils.getMobEffects(stack).isEmpty();
 }
 private ClassicPotionAppearance(){}
}
