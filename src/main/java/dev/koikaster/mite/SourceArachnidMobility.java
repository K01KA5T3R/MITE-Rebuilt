package dev.koikaster.mite;
import net.minecraft.world.entity.Mob;
/** R195 on.bl sets forward input from tp.d; of.bg independently supplies .1
 * ground acceleration. Modern Mob.setSpeed instead sets BOTH acceleration and
 * forward input. Keep the source attribute as input, not as both factors.
 * Native air acceleration already is the source .02 and stays untouched. */
public final class SourceArachnidMobility {
 public static float motor(Object raw,float requested){return SourceArachnidOwners.species((Mob)raw)==null?requested:.1f;}
 public static void forward(Object raw,float requested){var mob=(Mob)raw;if(SourceArachnidOwners.species(mob)!=null)mob.setZza(requested);}
 private SourceArachnidMobility(){}
}
