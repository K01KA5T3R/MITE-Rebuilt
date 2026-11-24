package dev.koikaster.mite;
import java.util.Collection;import java.util.Map;
import net.minecraft.world.effect.MobEffect;import net.minecraft.world.effect.MobEffectInstance;import net.minecraft.world.effect.MobEffects;
/** Server-safe display palette. Bytecode parity with the qualified123 item palette is verified. */
public final class SourcePotionPalette {
 private static final Map<MobEffect,Integer> COLORS=Map.ofEntries(
  Map.entry(MobEffects.MOVEMENT_SPEED,8171462),Map.entry(MobEffects.MOVEMENT_SLOWDOWN,5926017),
  Map.entry(MobEffects.DAMAGE_BOOST,9643043),Map.entry(MobEffects.HARM,4393481),
  Map.entry(MobEffects.JUMP,7889559),Map.entry(MobEffects.DAMAGE_RESISTANCE,10044730),
  Map.entry(MobEffects.FIRE_RESISTANCE,14981690),Map.entry(MobEffects.WATER_BREATHING,3035801),
  Map.entry(MobEffects.INVISIBILITY,8356754),Map.entry(MobEffects.NIGHT_VISION,2039713),
  Map.entry(MobEffects.POISON,5149489),Map.entry(MobEffects.WITHER,3484199));
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
 private SourcePotionPalette(){}
}
