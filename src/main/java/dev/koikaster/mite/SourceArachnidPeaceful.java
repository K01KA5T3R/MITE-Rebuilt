package dev.koikaster.mite;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
/** EntityArachnid.checkSwitchingToPeaceful, source031 memory-owner adapter.
 * sourceLastHarming comes from OriginalParentHarmState.harming(), never the
 * shorter-lived native revenge field. Outdoors is evaluated AFTER the draw. */
public final class SourceArachnidPeaceful {
 public static void afterTick(Mob actor,Entity sourceLastHarming,BooleanSupplier sourceOutdoors){
  if(actor.level().isClientSide)return;
  if(actor.getTarget() instanceof Player&&SourceArachnidAI.peacefulDuringDay(actor)&&OriginalWeather.brightness(actor)>.5f&&!(sourceLastHarming instanceof Player)&&actor.getRandom().nextInt(100)==0&&sourceOutdoors.getAsBoolean())actor.setTarget(null);
 }
 private SourceArachnidPeaceful(){}
}
