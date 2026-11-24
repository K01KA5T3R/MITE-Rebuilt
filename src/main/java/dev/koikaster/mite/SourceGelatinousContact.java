package dev.koikaster.mite;
import net.minecraft.world.entity.player.Player;
/** Contact is established on both native sides by the new cube's playerTouch.
 * Modern player movement transmits positions; its local input adapter applies
 * the source 0.4 multiplier before travel, leaving flying movement separate. */
public final class SourceGelatinousContact {
 private static final String CONTACT="R195GelatinousContactTick";
 public static void touch(Player player){player.getPersistentData().putInt(CONTACT,player.tickCount);}
 public static boolean slowed(Player player){var data=player.getPersistentData();if(!data.contains(CONTACT))return false;int elapsed=player.tickCount-data.getInt(CONTACT);return elapsed>=0&&elapsed<=1&&!(player.getAbilities().flying&&!player.isPassenger());}
 private SourceGelatinousContact(){}
}
