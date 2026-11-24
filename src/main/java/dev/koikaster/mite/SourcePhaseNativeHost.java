package dev.koikaster.mite;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Native path carrier for the original leaf operations. The parent source
 * fleeing field and source player selector are shared with the Arachnid owner;
 * this adapter does not claim that modern path generation is the old on.bp AI.
 */
public final class SourcePhaseNativeHost implements OriginalPhaseSpiderLife.Host {
 private final PathfinderMob mob;
 public SourcePhaseNativeHost(PathfinderMob mob){this.mob=Objects.requireNonNull(mob);}
 public boolean hasPath(){return mob.getNavigation().getPath()!=null;}
 public Path path(){return mob.getNavigation().getPath();}
 public boolean fleeing(){return SourceArachnidOwners.owner(mob).fleeing;}
 public float maximumTargetingRange(){return SourceArachnidAI.maximumTargetingRange(mob);}
 public Player findPlayerToAttack(float range){return SourceArachnidAI.findPlayerToAttack(mob,range);}
 public void teleportTrail(BlockPos destination,short count,BlockPos origin){Network.phaseTrail(mob,destination,count,origin);}
 public void portalSoundAt(Vec3 previous){mob.level().playSound(null,previous.x,previous.y,previous.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.HOSTILE,1,1);}
 public void requestImmediatePositionUpdate(){SourcePhasePositionUpdates.flush(mob);}
}
