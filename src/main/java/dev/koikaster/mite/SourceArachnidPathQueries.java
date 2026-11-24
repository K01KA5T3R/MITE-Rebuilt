package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
/** on.bl calls the graph afresh. Native createPath otherwise returns its cached
 * path for the same target, silently ignoring a newly supplied source budget. */
public final class SourceArachnidPathQueries {
 public static Path fresh(PathfinderMob actor,BlockPos target,float sourceBudget,boolean entityRoute){
  var owner=(SourceArachnidPathBudget)actor.getNavigation();Object previous=owner.mite$sourceBeginQuery();
  try{return owner.mite$sourceCreatePath(target,entityRoute?Math.min(sourceBudget,32):sourceBudget,entityRoute);}
  finally{owner.mite$sourceRestoreQuery(previous);}
 }
 private SourceArachnidPathQueries(){}
}
