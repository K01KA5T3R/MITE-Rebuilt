package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;

/** Exact on.bl flee/stop branch, before ordinary target decisions. It owns no
 * second target/path. A true return suppresses hunting but must NOT suppress
 * the Driver's following/movement block or Phase's post-parent active blink.
 */
public final class OriginalParentFleeTransition {
 public interface PathOwner {Path path();void path(Path value);default void onFleeing(){}}
 private final PathfinderMob actor;private final OriginalParentHarmState harm;private final PathOwner paths;private final OriginalParentAwayPath.World world;
 public OriginalParentFleeTransition(PathfinderMob actor,OriginalParentHarmState harm,PathOwner paths){this(actor,harm,paths,new SourceParentNativeWorld(actor));}
 public OriginalParentFleeTransition(PathfinderMob actor,OriginalParentHarmState harm,PathOwner paths,OriginalParentAwayPath.World world){this.actor=Objects.requireNonNull(actor);this.harm=Objects.requireNonNull(harm);this.paths=Objects.requireNonNull(paths);this.world=Objects.requireNonNull(world);}
 public boolean beforeDecisions(){
  var flags=harm.flags();
  if(flags.fleeing()){
   // Source checks null, not Path.isDone(). The later follow-path block owns
   // completion/discard behavior, and the if/else-if ordering is intentional.
   if(paths.path()==null||harm.considerStopFleeing()){flags.fleeing(false);paths.path(null);}
  }else if(flags.hasDecidedToFlee()&&!harm.considerStopFleeing()){
   var attacker=Objects.requireNonNull(harm.harming(),"Stop decision must retain a live remembered attacker");
   Path path=OriginalParentAwayPath.find(actor,BlockPos.containing(attacker.position()),16,48,false,world);
   if(path!=null){flags.fleeing(true);actor.setTarget(null);paths.path(path);paths.onFleeing();}
  }
  return flags.fleeing();
 }
 /** Actual single native path carrier. The native moveTo trimming/control is
  * an explicit movement adapter; it is not a second stored source bp field.
  */
 public static PathOwner nativePath(PathfinderMob actor,double speed){return new PathOwner(){public Path path(){return actor.getNavigation().getPath();}public void path(Path value){SourceArachnidPathBudget.commit(actor,value);if(value!=null)actor.getNavigation().setSpeedModifier(speed);}};}
}
