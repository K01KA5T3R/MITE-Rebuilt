package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

final class SourceOrbGeometry860 {
 private static boolean ignores(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
  var block=state.getBlock();if(state.is(Blocks.IRON_BARS)||block instanceof FenceGateBlock&&state.getValue(FenceGateBlock.OPEN))return true;
  if(block instanceof DoorBlock||block instanceof TrapDoorBlock||block instanceof FenceGateBlock||block instanceof FenceBlock||block instanceof IronBarsBlock)return false;
  if(block instanceof BushBlock||block instanceof VineBlock||block instanceof GrowingPlantBlock||block instanceof SugarCaneBlock)return true;
  if((block instanceof AbstractCauldronBlock||block instanceof HopperBlock)&&!level.getBlockState(pos.above()).isFaceSturdy(level,pos.above(),Direction.DOWN))return true;
  return !state.isSolid()||state.getCollisionShape(level,pos,context).isEmpty();
 }
 static boolean line(Entity orb,Vec3 from,Vec3 to){var context=CollisionContext.of(orb);
  for(var start:new Vec3[]{from.add(-.0001,0,0),from.add(.0001,0,0),from.add(0,0,-.0001),from.add(0,0,.0001)}){
   var hit=orb.level().clip(new ClipContext(start,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,orb){public VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){return ignores(state,world,pos,context)?Shapes.empty():super.getBlockShape(state,world,pos);}});
   if(hit.getType()==HitResult.Type.BLOCK)return false;
  }return true;
 }
 static boolean reachable(Entity orb,LivingEntity player){var from=orb.position().add(0,(double)(orb.getBbHeight()*.5f),0);return SourceExperienceOrb854.reachable(fraction->line(orb,from,player.position().add(0,(double)(player.getBbHeight()*fraction),0)));}
 static boolean fullLava(Entity orb){var pos=BlockPos.containing(orb.position());var fluid=orb.level().getFluidState(pos);return fluid.is(FluidTags.LAVA)&&fluid.isSource();}
 static int escape(SourceXpOrb860 orb){
  if(!(orb.level() instanceof ServerLevel level)||orb.tickCount<2)return 0;OriginalRandomTables.initialize(level.getSeed());var box=orb.getBoundingBox();
  return SourceOrbEscape860.escape(new SourceOrbEscape860.Host(){
   public boolean client(){return false;}public int ticks(){return orb.tickCount;}public double x(){return orb.getX();}public double y(){return orb.getY();}public double z(){return orb.getZ();}public double centerY(){return (box.minY+box.maxY)/2;}
   public boolean solid(int x,int y,int z){var p=new BlockPos(x,y,z);var s=level.getBlockState(p);return s.isSolidRender(level,p)&&s.isCollisionShapeFullBlock(level,p);}public boolean cauldron(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z)).getBlock() instanceof AbstractCauldronBlock;}
   public boolean clear(double x,double y,double z){return !level.getBlockCollisions(orb,box.move(x,y,z)).iterator().hasNext();}public boolean loaded(int x,int y,int z){var p=new BlockPos(x,y,z);return !level.isOutsideBuildHeight(p)&&level.hasChunkAt(p);}
   public double globalRandom(){return Math.random();}public double table(int index){return OriginalRandomTables.double_1[index];}
   public void relocate(double x,double y,double z){orb.setPos(orb.getX()+x,orb.getY()+y,orb.getZ()+z);orb.setDeltaMovement(Vec3.ZERO);orb.xo=orb.xOld=orb.getX();orb.yo=orb.yOld=orb.getY();orb.zo=orb.zOld=orb.getZ();level.getChunkSource().broadcast(orb,new ClientboundTeleportEntityPacket(orb));}
  });
 }
 private SourceOrbGeometry860(){}
}
