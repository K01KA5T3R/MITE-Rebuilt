package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
/** Only source-checked leaf attributes. The tm/Arachnid parent AI is not replaced by this helper. */
public final class SourceArachnidProfiles {
 public static AttributeSupplier.Builder attributes(SourceArachnidState.Species species){
  var values=net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.FOLLOW_RANGE,28).add(Attributes.MOVEMENT_SPEED,1).add(Attributes.ATTACK_DAMAGE,4);
  return switch(species){case WOOD,BLACK_WIDOW->values.add(Attributes.MAX_HEALTH,6).add(Attributes.MOVEMENT_SPEED,.8f).add(Attributes.ATTACK_DAMAGE,1);case PHASE->values.add(Attributes.MAX_HEALTH,6).add(Attributes.MOVEMENT_SPEED,.8f).add(Attributes.ATTACK_DAMAGE,3);case DEMON->values.add(Attributes.MAX_HEALTH,18).add(Attributes.ATTACK_DAMAGE,5);case NORMAL->values;case CAVE->values.add(Attributes.MAX_HEALTH,16);};
 }
 public static float width(SourceArachnidState.Species species){return 1.4f*(species==SourceArachnidState.Species.DEMON||species==SourceArachnidState.Species.NORMAL?1:species==SourceArachnidState.Species.CAVE?.7f:.6f);}
 public static float height(SourceArachnidState.Species species){return .9f*(species==SourceArachnidState.Species.DEMON||species==SourceArachnidState.Species.NORMAL?1:species==SourceArachnidState.Species.CAVE?.7f:.6f);}
 public static int experience(SourceArachnidState.Species species){return switch(species){case WOOD,NORMAL->5;case PHASE,CAVE->10;case DEMON->15;case BLACK_WIDOW->8;};}
 public static boolean woodFamily(SourceArachnidState.Species species){return species==SourceArachnidState.Species.WOOD||species==SourceArachnidState.Species.PHASE||species==SourceArachnidState.Species.BLACK_WIDOW;}
 /** New wood-only branch. Demon poison480/slowness50 is already wired and frozen in main; do not apply twice. */
 public static void woodMelee(Mob actor,Entity target,boolean affected,float healthLost){if(affected&&healthLost>0&&target instanceof LivingEntity living&&living.isAlive())living.addEffect(new MobEffectInstance(MobEffects.POISON,240,0),actor);}
 public static float woodRange(Mob actor,float inheritedSourceRange){return OriginalWeather.brightness(actor)<.65f?inheritedSourceRange:inheritedSourceRange*.5f;}
 private SourceArachnidProfiles(){}
}
