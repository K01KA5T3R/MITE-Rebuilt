package dev.koikaster.mite;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
/** R195 EntityHellhound.attackEntityAsMob, called once after the native Wolf attack result. */
public final class OriginalHellhoundStrike {
 public static void result(Mob mob,Entity target,boolean affected,SoundEvent breath){
  if(!affected||target.isRemoved()||!target.isAlive())return;
  if(mob.getRandom().nextFloat()<.4f){mob.playSound(breath,4,1);target.setSecondsOnFire(1+mob.getRandom().nextInt(8));}
 }
 private OriginalHellhoundStrike(){}
}
