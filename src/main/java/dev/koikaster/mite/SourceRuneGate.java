package dev.koikaster.mite;

import java.util.Optional;
import java.util.Random;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.phys.Vec3;

/** aps corner seed and same-dimension runegates. Destination coordinates are server-owned. */
public final class SourceRuneGate {
 public record Code(boolean adamantium,int seed){}
 public static Optional<Code> code(SourcePortalFrames.Frame frame){
  // PortalShape.rightDir may be WEST/NORTH. Source significance is numeric X/Z order.
  var rectangle=frame.rectangle();var low=rectangle.minCorner;
  var high=frame.axis()==net.minecraft.core.Direction.Axis.X?low.east():low.south();
  var lowCorner=frame.axis()==net.minecraft.core.Direction.Axis.X?low.west().below():low.north().below();
  var highCorner=frame.axis()==net.minecraft.core.Direction.Axis.X?high.east().below():high.south().below();
  var points=new BlockPos[]{lowCorner,highCorner,lowCorner.above(4),highCorner.above(4)};
  SourceRunestoneBlock metal=null;int seed=0;
  for(int i=0;i<4;i++){
   var state=frame.level().getBlockState(points[i]);if(!(state.getBlock() instanceof SourceRunestoneBlock rune)||metal!=null&&metal!=rune)return Optional.empty();
   metal=rune;seed|=state.getValue(SourceRunestoneBlock.RUNE)<<(i*4);
  }
  return Optional.of(new Code(metal.adamantium,seed));
 }
 public static void write(SourcePortalFrames.Frame frame){
  var state=Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS,frame.axis())
   .setValue(SourcePortalState.DESTINATION,SourcePortalState.of(((Level)frame.level()).dimension())).setValue(SourcePortalState.RUNEGATE,true);
  for(int x=0;x<2;x++)for(int y=0;y<3;y++)frame.level().setBlock(frame.at(x,y),state,18);
 }
 public static void scheduledTick(Object rawLevel,Object rawPos){
  var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;var state=level.getBlockState(pos);if(!state.is(Blocks.NETHER_PORTAL))return;
  var frame=SourcePortalFrames.complete(level,pos);if(frame.isEmpty())return;
  boolean rune=code(frame.get()).isPresent();
  if(rune==state.getValue(SourcePortalState.RUNEGATE))return; // Preserve ordinary return role.
  if(rune)write(frame.get());else SourcePortalFrames.write(frame.get(),SourcePortalFrames.initial(level,frame.get()));
 }
 public static void contact(ServerPlayer player,BlockPos pos){
  if(player.isPassenger()||player.isVehicle()||SourcePortalTransition.pending(player))return;
  var level=player.serverLevel();var frame=SourcePortalFrames.complete(level,pos);if(frame.isEmpty())return;
  var code=code(frame.get());if(code.isEmpty()){level.scheduleTick(pos,Blocks.NETHER_PORTAL,1);return;}
  destination(level,code.get()).ifPresent(at->SourcePortalTransition.beginRune(player,new Vec3(at.getX()+.5,at.getY()+.1,at.getZ()+.5),pos));
 }
 public static int radius(ServerLevel level,boolean adamantium){
  var border=level.getWorldBorder();double extent=Math.min(Math.min(-border.getMinX(),border.getMaxX()),Math.min(-border.getMinZ(),border.getMaxZ()));
  int radius=(int)Math.min(Math.floor(extent/2),40000);return adamantium?radius:radius/8;
 }
 public static BlockPos horizontal(Code code,int radius,BiPredicate<Integer,Integer> ocean){
  if(code.seed()==0)return BlockPos.ZERO;
  var random=new Random(code.seed());int x=0,z=0;
  for(int attempts=0;attempts<4;attempts++){
   x=random.nextInt(radius*2)-radius;z=random.nextInt(radius*2)-radius;
   // abw -> ls.a(double) rounds sqrt to float before the source radius test.
   while(code.adamantium()&&(float)Math.sqrt((double)x*x+(double)z*z)<radius/2){x=random.nextInt(radius*2)-radius;z=random.nextInt(radius*2)-radius;}
   if(!ocean.test(x,z))break;
  }
  return new BlockPos(x,0,z);
 }
 public static Optional<BlockPos> destination(ServerLevel level,Code code){
  int radius=radius(level,code.adamantium());if(radius<1)return Optional.empty();
  // Modern ocean variants share the source Ocean exclusion. No user setting is exposed.
  var point=horizontal(code,radius,(x,z)->level.getBiome(new BlockPos(x,level.getSeaLevel(),z)).is(BiomeTags.IS_OCEAN));
  if(!level.getWorldBorder().isWithinBounds(point))return Optional.empty();
  int cx=point.getX()>>4,cz=point.getZ()>>4;
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   level.getChunk(cx+dx,cz+dz);var chunk=new ChunkPos(cx+dx,cz+dz);level.getChunkSource().addRegionTicket(TicketType.PORTAL,chunk,3,point);
  }
  return Optional.of(height(level,point.getX(),point.getZ()));
 }
 private static boolean pass(ServerLevel level,int x,int y,int z,boolean liquids){
  if(level.isOutsideBuildHeight(y))return true;
  if(!level.hasChunk(x>>4,z>>4))return false;
  var state=level.getBlockState(new BlockPos(x,y,z));return state.isAir()||!(!liquids&&SourcePhaseNativeWorld.liquid(state))&&!SourcePhaseNativeWorld.solid(state);
 }
 private static boolean lava(ServerLevel level,int x,int y,int z){return level.getFluidState(new BlockPos(x,y,z)).is(FluidTags.LAVA);}
 public static BlockPos height(ServerLevel level,int x,int z){
  int min=level.getMinBuildHeight(),max=level.getMaxBuildHeight(),y;
  if(level.dimension()==Level.NETHER){
   y=min;while(++y<max-2){
    if(!pass(level,x,y,z,false)||!pass(level,x,++y,z,false)||pass(level,x,y-2,z,false)||lava(level,x,y-2,z)||lava(level,x,y-1,z))continue;
    return new BlockPos(x,y-1,z);
   }
  }else if(level.dimension()==WorldEvents.UNDERWORLD){
   y=max-2;while(--y>min){
    if(pass(level,x,y,z,false)&&pass(level,x,y+1,z,false)&&!pass(level,x,y-1,z,false)&&!lava(level,x,y-1,z)&&!(y-1>31&&level.getBlockState(new BlockPos(x,y-1,z)).is(Blocks.BEDROCK)))return new BlockPos(x,y,z);
   }
  }else{
   y=max;while(--y>min){
    if(!pass(level,x,y,z,false)||!pass(level,x,--y,z,false))continue;
    while(y>min&&pass(level,x,y-1,z,false))--y;
    if(y==min)y=64;return new BlockPos(x,y,z);
   }
  }
  for(y=64;y<=65;y++)if(!pass(level,x,y,z,true))level.removeBlock(new BlockPos(x,y,z),false);
  return new BlockPos(x,64,z);
 }
 public static void award(ServerPlayer player){SourceAchievementService570.grant(player,"runegate");
  var advancement=player.server.getAdvancements().getAdvancement(Content.id("runegate"));if(advancement!=null)for(String criterion:player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria())player.getAdvancements().award(advancement,criterion);
 }
 private SourceRuneGate(){}
}
