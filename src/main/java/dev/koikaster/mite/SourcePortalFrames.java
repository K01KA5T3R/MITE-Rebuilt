package dev.koikaster.mite;

import java.util.Optional;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import static dev.koikaster.mite.SourcePortalState.Destination;

/** aps fixed two-by-three frame and its location-based initial classification.
 * Ordinary return destinations are deliberately never recomputed on neighbor updates. */
public final class SourcePortalFrames {
 public record Frame(LevelAccessor level,BlockPos bottom,Direction right,Direction.Axis axis){
  public BlockPos at(int across,int up){return bottom.relative(right,across).above(up);}
  public BlockUtil.FoundRectangle rectangle(){BlockPos other=at(1,0);return new BlockUtil.FoundRectangle(new BlockPos(Math.min(bottom.getX(),other.getX()),bottom.getY(),Math.min(bottom.getZ(),other.getZ())),2,3);}
 }
 public static boolean valid(Object raw){
  var shape=(SourcePortalShapeAccess)raw;
  if(shape.mite$portalWidth()!=2||shape.mite$portalHeight()!=3||shape.mite$portalBottom()==null)return false;
  if(!(shape.mite$portalLevel() instanceof Level level)||!SourcePortalState.supported(level.dimension()))return false;
  Frame frame=from(shape);
  for(int x=-1;x<=2;x++)for(int y=-1;y<=3;y++){
   // Original frame corners can be absent or contain runestones.
   if((x==-1||x==2)&&(y==-1||y==3))continue;
   if(x!=-1&&x!=2&&y!=-1&&y!=3)continue;
   if(!level.getBlockState(frame.at(x,y)).is(Blocks.OBSIDIAN))return false;
  }
  return true;
 }
 private static Frame from(SourcePortalShapeAccess shape){return new Frame(shape.mite$portalLevel(),shape.mite$portalBottom(),shape.mite$portalRight(),shape.mite$portalAxis());}
 public static Optional<Frame> complete(Level level,BlockPos pos){
  var state=level.getBlockState(pos);if(!state.is(Blocks.NETHER_PORTAL))return Optional.empty();
  var shape=new PortalShape(level,pos,state.getValue(NetherPortalBlock.AXIS));
  return shape.isComplete()?Optional.of(from((SourcePortalShapeAccess)(Object)shape)):Optional.empty();
 }
 public static Destination initial(Level level,Frame frame){
  if(level.dimension()==CoreProgression.WORLD)return Destination.NETHER;
  if(level.dimension()==Level.NETHER)return WorldContentMode.rebuilt(level)&&touchesCore(level,frame)?Destination.CORE:Destination.UNDERWORLD;
  if(touchesBottom(level,frame))return level.dimension()==Level.OVERWORLD?Destination.UNDERWORLD:Destination.NETHER;
  return Destination.OVERWORLD;
 }
 static boolean coreFloor(Level level,BlockPos pos){
  int depth=pos.getY()-level.getMinBuildHeight();
  if(level.dimension()!=Level.NETHER||depth<0||depth>4)return false;
  var state=level.getBlockState(pos);
  return state.is(Blocks.BEDROCK)||depth==0&&state.is(SourceUnderworldContent.CORE.get());
 }
 static boolean touchesCore(Level level,Frame frame){
  return coreFloor(level,frame.at(0,-2))&&coreFloor(level,frame.at(1,-2));
 }
 private static boolean touchesBottom(Level level,Frame frame){
  if(frame.bottom().getY()-1>level.getMinBuildHeight()+8)return false;
  for(int x=-1;x<=2;x++)if(SourceMantleBlock.bottom(level,frame.at(x,-2)))return true;
  return false;
 }
 public static void create(Object raw){
  if(!valid(raw))return;
  Frame frame=from((SourcePortalShapeAccess)raw);var level=(Level)frame.level();
  if(SourceRuneGate.code(frame).isPresent()){SourceRuneGate.write(frame);return;}
  var destination=initial(level,frame);
  write(frame,destination);
  if(level instanceof net.minecraft.server.level.ServerLevel server && level.dimension()==Level.OVERWORLD
     && destination==Destination.OVERWORLD)SourceBiomePortal.ignite(server,frame);
 }
 public static void write(Frame frame,Destination destination){
  BlockState state=Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS,frame.axis()).setValue(SourcePortalState.DESTINATION,destination).setValue(SourcePortalState.RUNEGATE,false);
  for(int x=0;x<2;x++)for(int y=0;y<3;y++)frame.level().setBlock(frame.at(x,y),state,18);
 }
 public static Destination resolve(Level level,BlockPos pos){
  var state=level.getBlockState(pos);
  if(!state.is(Blocks.NETHER_PORTAL))return Destination.UNSET;
  var destination=state.getValue(SourcePortalState.DESTINATION);
  boolean oldCoreGate=destination==Destination.UNDERWORLD&&level.dimension()==Level.NETHER&&pos.getY()<=level.getMinBuildHeight()+4;
  boolean disabledCoreGate=destination==Destination.CORE&&!WorldContentMode.rebuilt(level);
  if(destination!=Destination.UNSET&&!oldCoreGate&&!disabledCoreGate)return destination;
  var frame=complete(level,pos);if(frame.isEmpty())return Destination.UNSET;
  var classified=initial(level,frame.get());
  if(oldCoreGate&&classified!=Destination.CORE)return destination;
  destination=classified;
  // One-time classification of legacy states only; never rewrites a return role.
  write(frame.get(),destination);return destination;
 }
 private SourcePortalFrames(){}
}
