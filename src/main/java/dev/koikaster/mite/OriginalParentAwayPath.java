package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;

/** Direct abw3913 sixteen-candidate away search. Graph construction is an
 * explicit adapter; candidate and final-point source tests remain in this owner.
 */
public final class OriginalParentAwayPath {
 public interface World {
  int nextWorldRandomInt();
  boolean airOrPassable(int x,int y,int z,boolean includeLiquid);
  Path graph(PathfinderMob actor,BlockPos destination,int maximumLength,boolean useNavigator);
 }
 private static double sourceSquared(int dx,int dy,int dz){float x=dx,y=dy,z=dz;return x*x+y*y+z*z;}
 public static Path find(PathfinderMob actor,BlockPos danger,int minimumDistance,int maximumLength,boolean useNavigator){return find(actor,danger,minimumDistance,maximumLength,useNavigator,new SourceParentNativeWorld(actor));}
 public static Path find(PathfinderMob actor,BlockPos danger,int minimumDistance,int maximumLength,boolean useNavigator,World world){
  Objects.requireNonNull(actor);Objects.requireNonNull(danger);Objects.requireNonNull(world);
  int x=Mth.floor(actor.getX()),y=Mth.floor(actor.getY()),z=Mth.floor(actor.getZ());
  int minimumSquared=minimumDistance*minimumDistance;
  double farthest=sourceSquared(x-danger.getX(),y-danger.getY(),z-danger.getZ());Path selected=null;
  int index=world.nextWorldRandomInt();
  for(int attempt=0;attempt<16;attempt++){
   int dx=OriginalRandomTables.int_max[++index&32767]%(maximumLength*2+1)-maximumLength;
   int dy=OriginalRandomTables.int_7_minus_3[++index&32767];
   int dz=OriginalRandomTables.int_max[++index&32767]%(maximumLength*2+1)-maximumLength;
   int trialX=x+dx,trialY=y+dy,trialZ=z+dz;
   for(int i=0;i<8&&world.airOrPassable(trialX,trialY-1,trialZ,false);i++)--trialY;
   for(int i=0;i<8&&!world.airOrPassable(trialX,trialY,trialZ,false);i++)++trialY;
   double distance=sourceSquared(trialX-danger.getX(),trialY-danger.getY(),trialZ-danger.getZ());
   if(!(distance>=(double)minimumSquared)||!(distance>farthest))continue;
   Path path=world.graph(actor,new BlockPos(trialX,trialY,trialZ),maximumLength,useNavigator);if(path==null)continue;
   var end=Objects.requireNonNull(path.getEndNode(),"Source graph returned a non-null empty path");
   distance=sourceSquared(end.x-danger.getX(),end.y-danger.getY(),end.z-danger.getZ());
   if(!(distance>=(double)minimumSquared)||!(distance>farthest))continue;
   farthest=distance;selected=path;
  }
  return selected;
 }
 private OriginalParentAwayPath(){}
}
