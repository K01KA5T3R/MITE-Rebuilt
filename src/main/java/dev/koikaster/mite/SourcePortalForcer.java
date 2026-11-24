package dev.koikaster.mite;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.phys.Vec3;
import static dev.koikaster.mite.SourcePortalState.Destination;

/** acj: source +/-128 search and +/-16 creation. Native POIs replace the old
 * coordinate cache; placement scan, RNG order, floor/air and height preference
 * are retained. Heights and border bounds use the destination's modern world. */
public final class SourcePortalForcer {
 private static final Map<ServerLevel,SourcePortalForcer> OWNERS=new WeakHashMap<>();
 private final Random random;
 private SourcePortalForcer(long seed){random=new Random(seed);}
 public static SourcePortalForcer in(ServerLevel level){return OWNERS.computeIfAbsent(level,key->new SourcePortalForcer(key.getSeed()));}
 public Optional<BlockUtil.FoundRectangle> find(ServerLevel level,Vec3 desired,Destination returningTo){
  var origin=BlockPos.containing(desired);var poi=level.getPoiManager();
  poi.ensureLoadedAndValid(level,origin,128);
  return poi.getInSquare(holder->holder.is(PoiTypes.NETHER_PORTAL),origin,128,PoiManager.Occupancy.ANY)
   .map(PoiRecord::getPos).filter(pos->level.getWorldBorder().isWithinBounds(pos))
   .filter(pos->level.getBlockState(pos).is(Blocks.NETHER_PORTAL))
   .filter(pos->SourcePortalFrames.resolve(level,pos)==returningTo)
   .map(pos->{var bottom=pos;while(level.getBlockState(bottom.below()).is(Blocks.NETHER_PORTAL))bottom=bottom.below();return bottom.immutable();})
   .distinct().sorted(Comparator.<BlockPos>comparingDouble(pos->Vec3.atCenterOf(pos).distanceToSqr(desired)).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ).thenComparing(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()))
   .map(pos->SourcePortalFrames.complete(level,pos)).flatMap(Optional::stream).findFirst().map(frame->{
    BlockPos pos=frame.bottom();level.getChunkSource().addRegionTicket(TicketType.PORTAL,new ChunkPos(pos),3,pos);return frame.rectangle();
   });
 }
 private static int top(ServerLevel level){return Math.min(level.getMaxBuildHeight(),level.getMinBuildHeight()+level.getLogicalHeight());}
 private static boolean ceilingBedrock(ServerLevel level,BlockPos pos){return pos.getY()>level.getMinBuildHeight()+31&&level.getBlockState(pos).is(Blocks.BEDROCK);}
 private static boolean suitable(ServerLevel level,BlockPos pos,boolean floor){
  if(!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos))return false;
  return floor?!ceilingBedrock(level,pos)&&SourcePortalMaterials.solid(level.getBlockState(pos)):level.isEmptyBlock(pos);
 }
 private static boolean forcedCellAllowed(ServerLevel level,BlockPos pos,boolean coreExit,boolean coreArrival){
  return level.getWorldBorder().isWithinBounds(pos)&&!level.isOutsideBuildHeight(pos)
   &&(!(coreExit||coreArrival)||level.getBlockEntity(pos)==null)
   &&(!coreExit||!level.getBlockState(pos).is(Blocks.BEDROCK));
 }
 private static boolean coreFoundation(ServerLevel level,int x,int y,int z,int alongX,int alongZ){
  var floor=new BlockPos(x,y-2,z);
  return SourcePortalFrames.coreFloor(level,floor)&&SourcePortalFrames.coreFloor(level,floor.offset(alongX,0,alongZ));
 }
 private static boolean forcedAreaAllowed(ServerLevel level,int x,int y,int z,int alongX,int alongZ,
                                          boolean coreExit,boolean coreArrival){
  if(coreExit&&!coreFoundation(level,x,y,z,alongX,alongZ))return false;
  for(int depth=-1;depth<=1;depth++)for(int width=1;width<3;width++)for(int height=-1;height<3;height++){
   var pos=new BlockPos(x+(width-1)*alongX+depth*alongZ,y+height,z+(width-1)*alongZ-depth*alongX);
   if(!forcedCellAllowed(level,pos,coreExit,coreArrival))return false;
  }
  for(int width=0;width<4;width++)for(int height=-1;height<4;height++){
   var pos=new BlockPos(x+(width-1)*alongX,y+height,z+(width-1)*alongZ);
   if(!forcedCellAllowed(level,pos,coreExit,coreArrival))return false;
  }
  return true;
 }
 public Optional<BlockUtil.FoundRectangle> create(ServerLevel level,Vec3 desired,Destination returningTo){
  return create(level,desired,returningTo,()->{});
 }
 Optional<BlockUtil.FoundRectangle> create(ServerLevel level,Vec3 desired,Destination returningTo,Runnable onLockedExit){
  boolean coreExit=returningTo==Destination.CORE&&level.dimension()==Level.NETHER;
  boolean coreArrival=returningTo==Destination.NETHER&&level.dimension()==CoreProgression.WORLD;
  var generator=level.getChunkSource().getGenerator();
  int arrivalDepth=coreArrival?TerrainProfiles2187.coreArrivalDepth(generator):-1;
  boolean low=coreExit||returningTo==Destination.NETHER||(returningTo==Destination.UNDERWORLD&&level.dimension()==Level.OVERWORLD);
  int min=level.getMinBuildHeight(),max=top(level),x0=(int)Math.floor(desired.x),z0=(int)Math.floor(desired.z);
  double anchorY=coreExit?min+2:low?min+1:max-1,best=-1;
  int bx=x0,by=(int)anchorY,bz=z0,orientation=0,start=random.nextInt(4);
  if(arrivalDepth<0)for(int x=x0-16;x<=x0+16;x++)for(int z=z0-16;z<=z0+16;z++){
   double dx=x+.5-desired.x,dz=z+.5-desired.z;
   scan:for(int y=max-1;y>=min;y--){
    if(!level.isEmptyBlock(new BlockPos(x,y,z)))continue;
    while(y>min&&level.isEmptyBlock(new BlockPos(x,y-1,z)))--y;
    if(coreExit&&(y<min+2||y>min+6))continue;
    for(int facing=start;facing<start+4;facing++){
     int alongX=facing%2,alongZ=1-alongX;if(facing%4>=2){alongX=-alongX;alongZ=-alongZ;}
     if(coreExit&&!coreFoundation(level,x,y,z,alongX,alongZ))continue;
     // Preserve the source's continue-to-next-height label, including orientation failures.
     for(int depth=0;depth<3;depth++)for(int width=0;width<4;width++)for(int height=-1;height<4;height++){
      var pos=new BlockPos(x+(width-1)*alongX+depth*alongZ,y+height,z+(width-1)*alongZ-depth*alongX);
      if(!suitable(level,pos,height<0)||coreExit&&!forcedCellAllowed(level,pos,true,false))continue scan;
     }
     double dy=y+.5-anchorY,distance=dx*dx+dy*dy+dz*dz;
     if(best<0||distance<best){best=distance;bx=x;by=y;bz=z;orientation=facing%4;}
    }
   }
  }
  if(best<0&&arrivalDepth<0)for(int x=x0-16;x<=x0+16;x++)for(int z=z0-16;z<=z0+16;z++){
   double dx=x+.5-desired.x,dz=z+.5-desired.z;
   scan:for(int y=max-1;y>=min;y--){
    if(!level.isEmptyBlock(new BlockPos(x,y,z)))continue;
    while(y>min&&level.isEmptyBlock(new BlockPos(x,y-1,z)))--y;
    if(coreExit&&(y<min+2||y>min+6))continue;
    for(int facing=start;facing<start+2;facing++){
     int alongX=facing%2,alongZ=1-alongX;
     if(coreExit&&!coreFoundation(level,x,y,z,alongX,alongZ))continue;
     for(int width=0;width<4;width++)for(int height=-1;height<4;height++){
      var pos=new BlockPos(x+(width-1)*alongX,y+height,z+(width-1)*alongZ);
      if(!suitable(level,pos,height<0)||coreExit&&!forcedCellAllowed(level,pos,true,false))continue scan;
     }
     double dy=y+.5-anchorY,distance=dx*dx+dy*dy+dz*dz;
     if(best<0||distance<best){best=distance;bx=x;by=y;bz=z;orientation=facing%2;}
    }
   }
  }
  int alongX=orientation%2,alongZ=1-alongX;if(orientation%4>=2){alongX=-alongX;alongZ=-alongZ;}
  boolean needsPlatform=best<0;
  if(needsPlatform){
   // Source 70..height-10, translated relative to the modern dimension floor.
   int lower=min+70,upper=max-10;if(lower>upper)return Optional.empty();
   if(arrivalDepth>=0)by=Math.max(min+2,Math.min(upper,generator.getSeaLevel()-arrivalDepth));
   else by=coreExit?min+2:Math.max(lower,Math.min(upper,by));
   if(!forcedAreaAllowed(level,bx,by,bz,alongX,alongZ,coreExit,coreArrival)){
    if(!coreExit)return Optional.empty();
    // The return portal may be gone in a world with the old bedrock bands.
    // Find an adjacent footprint without replacing the saved world's bedrock.
    double closest=Double.POSITIVE_INFINITY;
    for(int x=x0-16;x<=x0+16;x++)for(int z=z0-16;z<=z0+16;z++){
     double dx=x+.5-desired.x,dz=z+.5-desired.z;
     for(int y=min+2;y<=min+6;y++){
      double dy=y+.5-anchorY,distance=dx*dx+dy*dy+dz*dz;
      if(distance>=closest)continue;
      for(int axis=0;axis<2;axis++){
       if(!forcedAreaAllowed(level,x,y,z,axis,1-axis,true,false))continue;
       closest=distance;bx=x;by=y;bz=z;alongX=axis;alongZ=1-axis;break;
      }
     }
    }
    if(!Double.isFinite(closest))return Optional.empty();
   }
  }
  var axis=alongX==0?Direction.Axis.Z:Direction.Axis.X;
  BlockPos first=new BlockPos(bx,by,bz),second=first.offset(alongX,0,alongZ);
  var bottom=new BlockPos(Math.min(first.getX(),second.getX()),by,Math.min(first.getZ(),second.getZ()));
  var exit=new BlockUtil.FoundRectangle(bottom,2,3);
  if(BossArenaPortals.blockedPortal(level,exit,axis)){
   onLockedExit.run();
   return Optional.empty();
  }
  if(needsPlatform){
   for(int depth=-1;depth<=1;depth++)for(int width=1;width<3;width++)for(int height=-1;height<3;height++){
    var pos=new BlockPos(bx+(width-1)*alongX+depth*alongZ,by+height,bz+(width-1)*alongZ-depth*alongX);
    level.setBlock(pos,height<0?Blocks.OBSIDIAN.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
   }
  }
  var portal=Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS,axis)
   .setValue(SourcePortalState.DESTINATION,returningTo).setValue(SourcePortalState.RUNEGATE,false);
  for(int pass=0;pass<4;pass++){
   for(int width=0;width<4;width++)for(int height=-1;height<4;height++){
    var pos=new BlockPos(bx+(width-1)*alongX,by+height,bz+(width-1)*alongZ);
    boolean frame=width==0||width==3||height==-1||height==3;
    level.setBlock(pos,frame?Blocks.OBSIDIAN.defaultBlockState():portal,2);
   }
   for(int width=0;width<4;width++)for(int height=-1;height<4;height++){
    var pos=new BlockPos(bx+(width-1)*alongX,by+height,bz+(width-1)*alongZ);level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());
   }
  }
  // Native findPortalAround adds this ticket for an existing portal. R195 can
  // create an exit for non-players too, so its newly created exit needs the same
  // native loading/visibility guarantee before addDuringTeleport is called.
  level.getChunkSource().addRegionTicket(TicketType.PORTAL,new ChunkPos(bottom),3,bottom);
  return Optional.of(exit);
 }
}
