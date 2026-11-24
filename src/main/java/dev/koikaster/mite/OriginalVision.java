package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;

/** of/nn visibility and RaycastPolicies.for_vision, with native voxel shapes/pose coordinates. */
public final class OriginalVision {
 private static final class Trace {final Entity observer;final Vec3 origin,end;final boolean ignoreLeaves;int impedance,steps;Trace(Entity observer,Vec3 origin,Vec3 end,boolean ignoreLeaves){this.observer=observer;this.origin=origin;this.end=end;this.ignoreLeaves=ignoreLeaves;}}
 private static boolean glassOrIce(BlockState state){var block=state.getBlock();return block instanceof AbstractGlassBlock||block instanceof StainedGlassPaneBlock||state.is(Blocks.GLASS_PANE)||state.is(Blocks.GLOWSTONE)||state.is(Blocks.BEACON)||state.is(Blocks.SEA_LANTERN)||block instanceof IceBlock||state.is(Blocks.PACKED_ICE)||state.is(Blocks.BLUE_ICE);}
 private static boolean sparsePlants(BlockState state){var block=state.getBlock();return block instanceof TallGrassBlock||block instanceof FlowerBlock||block instanceof DoublePlantBlock||block instanceof CropBlock||block instanceof WebBlock||block instanceof SugarCaneBlock||block instanceof VineBlock;}
 private static boolean ignore(Trace trace,BlockState state){var block=state.getBlock();if(state.isAir()||block instanceof LiquidBlock||glassOrIce(state))return true;if(block instanceof DoorBlock||block instanceof TrapDoorBlock||block instanceof FenceGateBlock||block instanceof FenceBlock||block instanceof IronBarsBlock)return true;if(block instanceof LeavesBlock&&trace.ignoreLeaves)return true;if(block instanceof LeavesBlock||sparsePlants(state)){trace.impedance+=50;return trace.impedance<100;}return false;}
 private static BlockHitResult exhausted(Trace trace){return BlockHitResult.miss(trace.end,Direction.UP,BlockPos.containing(trace.end));}
 private static boolean single(Entity observer,Vec3 origin,Vec3 end,boolean ignoreLeaves){if(Double.isNaN(origin.x)||Double.isNaN(origin.y)||Double.isNaN(origin.z)||Double.isNaN(end.x)||Double.isNaN(end.y)||Double.isNaN(end.z))return true;var trace=new Trace(observer,origin,end,ignoreLeaves);var level=observer.level();var context=CollisionContext.of(observer);BlockHitResult hit=BlockGetter.traverseBlocks(origin,end,trace,(current,pos)->{if(++current.steps>202)return exhausted(current);var state=level.getBlockState(pos);if(ignore(current,state))return null;var shape=state.getBlock() instanceof HopperBlock||state.getBlock() instanceof AbstractCauldronBlock?state.getCollisionShape(level,pos,context):state.getShape(level,pos,context);return shape.clip(origin,end,pos);},OriginalVision::exhausted);return hit.getType()==HitResult.Type.MISS;}
 /** The four world traces reset impedance independently; any collision blocks the source line. */
 public static boolean line(Entity observer,Vec3 origin,Vec3 end,boolean ignoreLeaves){boolean clear=true;for(var start:new Vec3[]{origin.add(-.0001,0,0),origin.add(.0001,0,0),origin.add(0,0,-.0001),origin.add(0,0,.0001)})if(!single(observer,start,end,ignoreLeaves))clear=false;return clear;}
 public static boolean from(Entity observer,Entity target,Vec3 origin,double maxRangeSquared,boolean ignoreLeaves){if(observer.level()!=target.level())return false;var center=target.position().add(0,target.getBbHeight()*.5f,0);if(origin.distanceToSqr(center)>maxRangeSquared)return false;if(target instanceof LivingEntity)return line(observer,origin,target.position().add(0,target.getBbHeight()*.25f,0),ignoreLeaves)||line(observer,origin,target.position().add(0,target.getBbHeight()*.75f,0),ignoreLeaves);return line(observer,origin,center,ignoreLeaves);}
 public static boolean see(Object rawObserver,Object rawTarget){var observer=(LivingEntity)rawObserver;var target=(Entity)rawTarget;boolean seen=from(observer,target,observer.getEyePosition(),Double.MAX_VALUE,false);if(seen){if(observer instanceof Player)OriginalDespawn.refresh(target,-1200);else if(target instanceof Player)OriginalDespawn.refresh(observer,-1200);}return seen;}
 private OriginalVision(){}
}
