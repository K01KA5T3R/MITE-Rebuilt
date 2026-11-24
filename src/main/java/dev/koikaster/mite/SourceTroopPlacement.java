package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** js.tryCreateNewLivingEntityCloseTo skeleton branch and aci spawn surface.
 * Source flags select support; native shapes supply only its resolved surface Y.
 * No normal natural-spawn light or distance24 gate is inserted into summons. */
final class SourceTroopPlacement {
 static boolean passable(Level level,BlockPos pos,boolean liquids){
  if(level.isOutsideBuildHeight(pos))return true;if(!level.hasChunkAt(pos))return false;
  var state=level.getBlockState(pos);return (liquids||!SourcePhaseNativeWorld.liquid(state))&&!SourcePhaseNativeWorld.solid(state);
 }
 static double surfaceY(Level level,BlockPos pos){
  if(pos.getY()<level.getMinBuildHeight()+1)return Double.NaN;
  var state=level.getBlockState(pos);if(state.isSolidRender(level,pos)&&state.isCollisionShapeFullBlock(level,pos)||state.getBlock() instanceof LiquidBlock&&state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA))return Double.NaN;
  var support=pos.below();var below=level.getBlockState(support);
  if(!SourcePhaseNativeWorld.solid(below)&&!(below.getBlock() instanceof SnowLayerBlock)&&!(below.getBlock() instanceof WaterlilyBlock))return Double.NaN;
  var block=below.getBlock();
  if(below.is(Blocks.BEDROCK)||below.is(Blocks.MYCELIUM)||below.is(Blocks.BROWN_MUSHROOM_BLOCK)||below.is(Blocks.RED_MUSHROOM_BLOCK)||below.is(Blocks.MUSHROOM_STEM)||block instanceof SourceGiantBrownMushroomBlock||below.is(SourceUnderworldContent.MANTLE.get())||below.is(SourceUnderworldContent.CORE.get()))return Double.NaN;
  boolean allowed=block instanceof SnowLayerBlock||block instanceof WaterlilyBlock||block instanceof SlabBlock||block instanceof StairBlock||below.isFaceSturdy(level,support,net.minecraft.core.Direction.UP);
  if(!allowed)return Double.NaN;var shape=below.getCollisionShape(level,support);
  return shape.isEmpty()?Double.NaN:support.getY()+shape.max(net.minecraft.core.Direction.Axis.Y)+.005;
 }
 static AbstractSkeleton createCloseTo(SourceBoneLord lord,boolean ancient){
  var level=(ServerLevel)lord.level();OriginalRandomTables.initialize(level.getSeed());
  var origin=lord.blockPosition();int index=level.random.nextInt();
  for(int attempt=0;attempt<16;attempt++){
   int dx=OriginalRandomTables.int_max[++index&32767]%25-12,dy=OriginalRandomTables.int_7_minus_3[++index&32767],dz=OriginalRandomTables.int_max[++index&32767]%25-12;
   var pos=origin.offset(dx,dy,dz);for(int i=0;i<8&&passable(level,pos.below(),true);i++)pos=pos.below();for(int i=0;i<8&&!passable(level,pos,true);i++)pos=pos.above();
   int vertical=pos.getY()-origin.getY(),squared=dx*dx+vertical*vertical+dz*dz;
   if(squared<4||squared>144||!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos.below())||level.isOutsideBuildHeight(pos.above())||passable(level,pos.below(),true)||!passable(level,pos,true)||!passable(level,pos.above(),false))continue;
   double y=surfaceY(level,pos);if(Double.isNaN(y))continue;
   AbstractSkeleton skeleton=ancient?SourceLongdeadContent.LONGDEAD.get().create(level):net.minecraft.world.entity.EntityType.SKELETON.create(level);
   if(skeleton==null)continue;skeleton.moveTo(pos.getX()+.5,y,pos.getZ()+.5,level.random.nextFloat()*360,0);return skeleton;
  }
  return null;
 }
 static boolean pathEndsNear(AbstractSkeleton skeleton,BlockPos target){var path=skeleton.getNavigation().createPath(target,0,16);if(path==null||path.getEndNode()==null)return false;var node=path.getEndNode();double dx=node.x-target.getX(),dy=node.y-target.getY(),dz=node.z-target.getZ();return dx*dx+dy*dy+dz*dz<=2;}
 /** Same qn piercing family adapted for any source skeleton owner. */
 static boolean projectileLine(AbstractSkeleton mob,Vec3 end){
  var origin=mob.getEyePosition();for(var from:new Vec3[]{origin.add(-.0001,0,0),origin.add(.0001,0,0),origin.add(0,0,-.0001),origin.add(0,0,.0001)}){
   var hit=mob.level().clip(new net.minecraft.world.level.ClipContext(from,end,net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,mob){
    public VoxelShape getBlockShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos){var block=state.getBlock();if(block instanceof LiquidBlock||block instanceof TallGrassBlock||block instanceof FlowerBlock||block instanceof DoublePlantBlock||block instanceof CropBlock||block instanceof SugarCaneBlock||block instanceof VineBlock||block instanceof IronBarsBlock||block instanceof FenceGateBlock&&state.getValue(FenceGateBlock.OPEN)||block instanceof LeavesBlock)return Shapes.empty();return super.getBlockShape(state,level,pos);}
   });if(hit.getType()==HitResult.Type.BLOCK)return false;
  }return true;
 }
 private SourceTroopPlacement(){}
}
