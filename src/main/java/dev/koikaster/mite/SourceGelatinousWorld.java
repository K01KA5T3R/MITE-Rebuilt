package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.registries.ForgeRegistries;

/** Native world adapter for the original fixed block progress. The per-entity
 * saved palettes retain stable names; runtime registry numbers are never saved.
 * Original progress identity is block identity, not changing block metadata. */
final class SourceGelatinousWorld implements OriginalGelatinousProgress.World {
 private final SourceGelatinousCube mob;
 private final List<String> blocks=new ArrayList<>(),dimensions=new ArrayList<>();
 SourceGelatinousWorld(SourceGelatinousCube mob){this.mob=mob;blocks.add("minecraft:air");}
 private int token(List<String> palette,String key){int index=palette.indexOf(key);if(index<0){index=palette.size();palette.add(key);}return index;}
 public int dimensionToken(){return token(dimensions,mob.level().dimension().location().toString());}
 public int blockTokenAt(int x,int y,int z){var state=mob.level().getBlockState(new BlockPos(x,y,z));return state.isAir()?0:token(blocks,ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());}
 public double distanceSquaredToBlock(int x,int y,int z){return mob.distanceToSqr(x+.5,y+.5,z+.5);}
 public int dissolvePeriodInSourceSeconds(OriginalGelatinousProgress.Occupied block){int ticks=SourceChemicalMaterials.blockTicks(mob.level().getBlockState(new BlockPos(block.x(),block.y(),block.z())),mob.profile().acidic());return ticks>=0?ticks/20:-1;}
 public void destroyWithoutDrop(OriginalGelatinousProgress.Occupied block){destroy(new BlockPos(block.x(),block.y(),block.z()));}
 public void steam(OriginalGelatinousProgress.Occupied block){fx(new BlockPos(block.x(),block.y(),block.z()),false,false);}
 void fx(BlockPos pos,boolean smoke,boolean hiss){if(mob.level() instanceof ServerLevel level)SourceChemicalFx.block(level,pos,smoke?SourceChemicalFx.BLOCK_SMOKE_STEAM:hiss?SourceChemicalFx.BLOCK_STEAM:SourceChemicalFx.BLOCK_STEAM_PARTICLES);}
 void destroy(BlockPos pos){var partners=SourceChemicalBlocks.partners(mob.level(),pos);if(SourceChemicalBlocks.removeNoDrop(mob.level(),pos)){for(var other:partners)fx(other,mob.profile().acidic(),true);fx(pos,mob.profile().acidic(),true);}}
 boolean renderIntersects(BlockState state,BlockPos pos,AABB box){var shape=state.getShape(mob.level(),pos);if(state.getBlock() instanceof LiquidBlock)shape=Shapes.block();for(var part:shape.toAabbs())if(part.move(pos).intersects(box))return true;return false;}
 OriginalGelatinousProgress.Occupied[] occupied(){var box=mob.getBoundingBox().inflate((double).01f);var result=new ArrayList<OriginalGelatinousProgress.Occupied>();for(int x=net.minecraft.util.Mth.floor(box.minX);x<=net.minecraft.util.Mth.floor(box.maxX);x++)for(int z=net.minecraft.util.Mth.floor(box.minZ);z<=net.minecraft.util.Mth.floor(box.maxZ);z++)for(int y=net.minecraft.util.Mth.floor(box.minY);y<=net.minecraft.util.Mth.floor(box.maxY);y++){var pos=new BlockPos(x,y,z);var state=mob.level().getBlockState(pos);result.add(state.isAir()||!renderIntersects(state,pos,box)?null:new OriginalGelatinousProgress.Occupied(blockTokenAt(x,y,z),x,y,z));}return result.toArray(OriginalGelatinousProgress.Occupied[]::new);}
 void beforeMotion(){for(var block:occupied()){if(block==null)continue;var pos=new BlockPos(block.x(),block.y(),block.z());var state=mob.level().getBlockState(pos);int seconds=dissolvePeriodInSourceSeconds(block);if(seconds==0||state.getBlock() instanceof TorchBlock||state.getBlock() instanceof CropBlock){if(SourcePhaseNativeWorld.solid(state)){destroy(pos);continue;}collided(pos,state);continue;}if(seconds<=0)continue;mob.progress().blockFeeding(20);break;}}
 void collided(BlockPos pos,BlockState state){if(!renderIntersects(state,pos,mob.getBoundingBox()))return;if(state.getBlock() instanceof TorchBlock){if(SourceChemicalBlocks.removeNoDrop(mob.level(),pos))fx(pos,true,true);mob.hurt(mob.damageSources().inFire(),1);return;}if(SourceChemicalMaterials.blockTicks(state,mob.profile().acidic())==0){destroy(pos);return;}if(!WorldContentMode.rebuilt(mob.level())&&state.getBlock() instanceof CropBlock)SourceCropBlight.set(mob.level(),pos,true);}
 void contactTop(BlockPos pos){var state=mob.level().getBlockState(pos);if(state.is(Blocks.DIRT)||state.is(Blocks.FARMLAND)){mob.level().setBlockAndUpdate(pos,Blocks.CLAY.defaultBlockState());fx(pos,true,true);return;}if(SourceChemicalMaterials.blockTicks(state,mob.profile().acidic())==0)destroy(pos);}
 void acidSupport(){var box=mob.getBoundingBox();int y=net.minecraft.util.Mth.floor(box.minY-.001);for(int x=net.minecraft.util.Mth.floor(box.minX);x<=net.minecraft.util.Mth.floor(box.maxX);x++)for(int z=net.minecraft.util.Mth.floor(box.minZ);z<=net.minecraft.util.Mth.floor(box.maxZ);z++){var pos=new BlockPos(x,y,z);var state=mob.level().getBlockState(pos);var shape=state.getShape(mob.level(),pos);if(!shape.isEmpty()&&box.minY-y-shape.max(Direction.Axis.Y)<=.01&&!(mob.level().getBlockState(pos.above()).getBlock() instanceof BaseFireBlock))contactTop(pos);}}
 void save(CompoundTag tag){tag.put("R195DissolveBlockPalette",savePalette(blocks));tag.put("R195DissolveDimensionPalette",savePalette(dimensions));}
 private static ListTag savePalette(List<String> entries){var list=new ListTag();for(var entry:entries)list.add(StringTag.valueOf(entry));return list;}
 void load(CompoundTag tag){if(tag.contains("R195DissolveBlockPalette",Tag.TAG_LIST)){blocks.clear();for(var entry:tag.getList("R195DissolveBlockPalette",Tag.TAG_STRING))blocks.add(entry.getAsString());if(blocks.isEmpty())blocks.add("minecraft:air");}dimensions.clear();for(var entry:tag.getList("R195DissolveDimensionPalette",Tag.TAG_STRING))dimensions.add(entry.getAsString());}
}
