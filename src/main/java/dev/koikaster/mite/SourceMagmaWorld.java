package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/** Per-block tl.onCollidedWithBlock, immediately following the actual native
 * block callback. R195 contact has no mobGriefing/doFireTick condition. */
public final class SourceMagmaWorld {
 public static final ResourceKey<DamageType> ABSOLUTE=ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("source_absolute"));
 static final ResourceKey<DamageType> CONTACT=ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("source_magma_contact"));
 static DamageSource attack(SourceMagmaCube actor){return new DamageSource(actor.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CONTACT),actor);}
 public static void entityInside(Object rawState,Object rawWorld,Object rawPos,Object rawEntity){var state=(BlockState)rawState;var level=(Level)rawWorld;var pos=(BlockPos)rawPos;var entity=(Entity)rawEntity;state.entityInside(level,pos,entity);if(entity instanceof SourceMagmaCube magma&&!level.isClientSide)contact(magma,state,pos);}
 private static boolean intersects(SourceMagmaCube actor,BlockState state,BlockPos pos){AABB box=actor.getBoundingBox();for(var part:state.getShape(actor.level(),pos).toAabbs())if(part.move(pos).intersects(box))return true;return false;}
 static void contact(SourceMagmaCube actor,BlockState state,BlockPos pos){
  if(!intersects(actor,state,pos))return;var world=actor.level();
  if(state.getBlock() instanceof TorchBlock){world.destroyBlock(pos,true,actor);return;}
  if(state.getBlock() instanceof BushBlock||state.getBlock() instanceof SugarCaneBlock||state.is(Blocks.COBWEB)){
   if(state.getBlock() instanceof DoublePlantBlock){var other=state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.LOWER?pos.above():pos.below();if(world.getBlockState(other).is(state.getBlock()))world.setBlock(other,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);}
   if(SourceChemicalBlocks.removeNoDrop(world,pos))fx(world,pos,true);return;
  }
  if(state.is(Blocks.ICE)||state.is(Blocks.PACKED_ICE)||state.is(Blocks.BLUE_ICE)||state.is(Blocks.SNOW)||state.is(Blocks.SNOW_BLOCK)){
   if(OriginalFireSpread.melt(world,pos))fx(world,pos,false);
   actor.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ABSOLUTE)),1);
  }
 }
 static void acidSupport(SourceMagmaCube actor){var world=actor.level();if(world.isClientSide)return;var box=actor.getBoundingBox();int y=Mth.floor(actor.getY()+(double)1.0E-4f)-1;for(int x=Mth.floor(box.minX);x<=Mth.floor(box.maxX-.001);x++)for(int z=Mth.floor(box.minZ);z<=Mth.floor(box.maxZ-.001);z++){var pos=new BlockPos(x,y,z);var state=world.getBlockState(pos);var shape=state.getShape(world,pos);if(!shape.isEmpty()&&box.minY-y-shape.max(Direction.Axis.Y)<=.01&&!(world.getBlockState(pos.above()).getBlock() instanceof BaseFireBlock))SourceChemicalBlocks.contact(world,pos,Direction.UP,true,false);}}
 private static void fx(Level world,BlockPos pos,boolean smoke){if(world instanceof ServerLevel server)SourceChemicalFx.block(server,pos,smoke?SourceChemicalFx.BLOCK_SMOKE_STEAM:SourceChemicalFx.BLOCK_STEAM);}
 private SourceMagmaWorld(){}
}
