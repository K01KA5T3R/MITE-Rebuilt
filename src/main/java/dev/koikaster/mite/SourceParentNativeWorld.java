package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;

/** Existing source tables and explicit modern graph oracle; no table algorithm
 * is copied, changed or replayed. Source block flags reuse the0.30 owner.
 */
public final class SourceParentNativeWorld implements OriginalParentAwayPath.World {
 private final PathfinderMob actor;
 public SourceParentNativeWorld(PathfinderMob actor){this.actor=Objects.requireNonNull(actor);}
 public int nextWorldRandomInt(){if(actor.level() instanceof ServerLevel level)OriginalRandomTables.initialize(level.getSeed());return actor.level().random.nextInt();}
 public boolean airOrPassable(int x,int y,int z,boolean includeLiquid){
  // abw4564 tests the world vertical limit before chunk existence. Adapt that
  // limit to the actual dimension so retained1.20 negative/high terrain is solid.
  var level=actor.level();if(level.isOutsideBuildHeight(y))return true;if(!level.hasChunk(x>>4,z>>4))return false;
  var state=level.getBlockState(new BlockPos(x,y,z));if(state.isAir())return true;
  if(!includeLiquid&&SourcePhaseNativeWorld.liquid(state))return false;
  return !SourcePhaseNativeWorld.solid(state);
 }
 public Path graph(PathfinderMob actor,BlockPos destination,int maximumLength,boolean useNavigator){
  var navigation=actor.getNavigation();
  // The actual public native overload is (position,accuracy,maxDistance), not
  // (position,maxDistance,accuracy). Native navigation itself remains the73A
  // graph/shape adapter, including its ability to update in air/liquid.
  if(useNavigator)return navigation.createPath(destination,0,maximumLength);
  // Original world path call passes open doors=true,closed doors=false,
  // avoid_water=false,can_swim=true. Restore native actor/evaluator policy even
  // when the graph rejects or throws; no global registry/state is modified.
  var evaluator=navigation.getNodeEvaluator();boolean pass=evaluator.canPassDoors(),open=evaluator.canOpenDoors(),floating=navigation.canFloat();float water=actor.getPathfindingMalus(BlockPathTypes.WATER);
  // The new031 bridge requests a fresh graph and restores all native query
  // metadata. Merely calling createPath here can return an old cached path
  // and can leave the last rejected candidate's target/timeout installed.
  try{evaluator.setCanPassDoors(true);evaluator.setCanOpenDoors(false);navigation.setCanFloat(true);actor.setPathfindingMalus(BlockPathTypes.WATER,0);return SourceArachnidPathBudget.block(actor,destination,(float)maximumLength);}
  finally{evaluator.setCanPassDoors(pass);evaluator.setCanOpenDoors(open);navigation.setCanFloat(floating);actor.setPathfindingMalus(BlockPathTypes.WATER,water);}
 }
}
