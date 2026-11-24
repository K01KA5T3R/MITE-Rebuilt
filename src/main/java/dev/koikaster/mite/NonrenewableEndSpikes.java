package dev.koikaster.mite;

import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;

/** Human N018: native initial cages are finite stock; dragon respawning may
 * rebuild stone/crystals but cannot manufacture additional mineral iron bars.
 * The native ritual uses crystalInvulnerable=true for its rebuilding feature.
 * No cached spike/configuration is mutated and no player setting is exposed. */
public final class NonrenewableEndSpikes {
 public static boolean cage(Object rawSpike,Object rawConfiguration){
  var spike=(SpikeFeature.EndSpike)rawSpike;
  var configuration=(SpikeConfiguration)rawConfiguration;
  return spike.isGuarded()&&!configuration.isCrystalInvulnerable();
 }
 private NonrenewableEndSpikes(){}
}
