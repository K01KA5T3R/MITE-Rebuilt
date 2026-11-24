package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
/** Source0.31 PathNavigation graph-query and selected-path owner. */
public interface SourceArachnidPathBudget {
 /** Snapshot7 native path/cache/timeout fields and clear only the cached Path. */
 Object mite$sourceBeginQuery();
 void mite$sourceRestoreQuery(Object snapshot);
 Path mite$sourceCreatePath(BlockPos target,float maxDistance,boolean entityRoute);
 /** Restore the metadata createPath normally commits before native moveTo. */
 void mite$sourceCommitPath(Path path);
 static Path block(PathfinderMob actor,BlockPos target,float range){return SourceArachnidPathQueries.fresh(actor,target,range,false);}
 static Path entity(PathfinderMob actor,Entity target,float range){return SourceArachnidPathQueries.fresh(actor,target.blockPosition(),range,true);}
 static void commit(PathfinderMob actor,Path path){var navigation=actor.getNavigation();if(navigation instanceof net.minecraft.world.entity.ai.navigation.WallClimberNavigation&&!(navigation instanceof SourceArachnidClimberFallback))throw new IllegalStateException("Source031 climber fallback bridge is not installed");if(navigation instanceof SourceArachnidClimberFallback climber)climber.mite$sourceClearClimberFallback();if(path==null)navigation.stop();else{((SourceArachnidPathBudget)navigation).mite$sourceCommitPath(path);navigation.moveTo(path,1);}}
}
