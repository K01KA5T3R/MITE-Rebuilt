package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
/** abw passability, trial settlement, occupiedByLivingEntity(og, excluding players) and exact toward-XYZ fallback. */
public final class OriginalEnvironmentPaths {
 public static void initialize(PathfinderMob mob){if(mob.level() instanceof ServerLevel level)OriginalRandomTables.initialize(level.getSeed());}
 public static boolean passable(PathfinderMob mob,BlockPos pos,boolean liquids){return OriginalDigging.passable(mob.level(),pos,liquids);}
 public static BlockPos settle(PathfinderMob mob,BlockPos trial){for(int i=0;i<8&&passable(mob,trial.below(),false);i++)trial=trial.below();for(int i=0;i<8&&!passable(mob,trial,false);i++)trial=trial.above();return trial;}
 public static boolean occupied(PathfinderMob mob,BlockPos pos){return !mob.level().getEntitiesOfClass(Mob.class,new AABB(pos)).isEmpty();}
 public static double sq(int x,int y,int z){return (double)x*x+(double)y*y+(double)z*z;}
 public static Path toward(PathfinderMob mob,BlockPos target,int maximum){initialize(mob);int x=mob.getBlockX(),y=mob.getBlockY(),z=mob.getBlockZ(),index=mob.level().random.nextInt();double best=sq(x-target.getX(),y-target.getY(),z-target.getZ());Path selected=null;for(int attempt=0;attempt<16;attempt++){int dx=OriginalRandomTables.int_max[++index&32767]%(maximum*2+1)-maximum;int dy=OriginalRandomTables.int_7_minus_3[++index&32767];int dz=OriginalRandomTables.int_max[++index&32767]%(maximum*2+1)-maximum;var trial=settle(mob,new BlockPos(x+dx,y+dy,z+dz));double distance=sq(trial.getX()-target.getX(),trial.getY()-target.getY(),trial.getZ()-target.getZ());if(!(distance<best))continue;var path=mob.getNavigation().createPath(trial,0,maximum);if(path==null)continue;var end=path.getEndNode();distance=sq(end.x-target.getX(),end.y-target.getY(),end.z-target.getZ());if(!(distance<best))continue;best=distance;selected=path;}return selected;}
 private OriginalEnvironmentPaths(){}
}
