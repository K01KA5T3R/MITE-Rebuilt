package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

/** R195 aqz solidity flags, arh's WoodSpider leaf exemption and abw's two
 * collision queries. Native block shapes adapt the remaining source shapes;
 * neither sturdy-top nor opaque/full-cube flags decide the supporting block.
 */
public final class SourcePhaseNativeWorld implements OriginalPhaseSpiderLife.WorldAccess {
 private final PathfinderMob mob;
 public SourcePhaseNativeWorld(PathfinderMob mob){this.mob=Objects.requireNonNull(mob);}
 public boolean blockExists(int x,int y,int z){return y>=0&&y<256&&!mob.level().isOutsideBuildHeight(y)&&mob.level().hasChunk(x>>4,z>>4);}
 public boolean solid(int x,int y,int z){return solid(mob.level().getBlockState(new BlockPos(x,y,z)));}
 public boolean liquid(int x,int y,int z){return liquid(mob.level().getBlockState(new BlockPos(x,y,z)));}

 /** aqz.isSolid defaults true. Only original subclasses overriding that flag
  * are excluded, with Snow/Gate/Lily retaining their audited overrides. This
  * deliberately counts bedrock, leaves, ladders, doors and partial solid blocks.
  * Later block families inherit this explicit class-family adaptation.
  */
 public static boolean solid(BlockState state){
  Block block=state.getBlock();if(state.isAir())return false;
  if(block instanceof SnowLayerBlock)return state.getValue(SnowLayerBlock.LAYERS)>1;
  if(block instanceof FenceGateBlock)return !state.getValue(FenceGateBlock.OPEN);
  if(block instanceof WaterlilyBlock)return true;
  return !(block instanceof BushBlock||block instanceof BasePressurePlateBlock||block instanceof BaseRailBlock
   ||block instanceof ButtonBlock||block instanceof BaseFireBlock||block instanceof LeverBlock
   ||block instanceof LiquidBlock||block instanceof NetherPortalBlock||block instanceof RedStoneWireBlock
   ||block instanceof SugarCaneBlock||block instanceof SignBlock||block instanceof EndPortalBlock
   ||block instanceof TorchBlock||block instanceof TripWireHookBlock||block instanceof TripWireBlock
   ||block instanceof VineBlock||block instanceof WebBlock||block instanceof CarpetBlock);
 }
 /** Original liquid material belongs to a liquid block. Waterlogging is not
  * silently promoted to an original liquid block or a solid-state exclusion.
  */
 public static boolean liquid(BlockState state){return state.getBlock() instanceof LiquidBlock;}
 public boolean collisionFree(AABB box){
  var level=mob.level();var context=CollisionContext.of(mob);
  int minX=Mth.floor(box.minX),maxX=Mth.ceil(box.maxX)-1,minY=Mth.floor(box.minY),maxY=Mth.ceil(box.maxY)-1,minZ=Mth.floor(box.minZ),maxZ=Mth.ceil(box.maxZ)-1;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
   if(!level.hasChunk(x>>4,z>>4))continue;
   for(int y=minY-1;y<=maxY;y++){
    var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
    // arh compares to the entity's OLD foot Y, not the candidate bounding box.
    if(state.is(BlockTags.LEAVES)&&(double)((float)y+.99f)>mob.getY())continue;
    for(var part:state.getCollisionShape(level,pos,context).toAabbs())if(part.move(x,y,z).intersects(box))return false;
   }
  }
  // abw adds only E()/g() collidable bounds. Ordinary living mobs have neither;
  // use native collidable shapes, not a new "any living entity" occupancy ban.
  return level.getEntityCollisions(mob,box).isEmpty();
 }
 public boolean liquidFree(AABB box){
  for(int x=Mth.floor(box.minX);x<=Mth.ceil(box.maxX)-1;x++)for(int y=Mth.floor(box.minY);y<=Mth.ceil(box.maxY)-1;y++)for(int z=Mth.floor(box.minZ);z<=Mth.ceil(box.maxZ)-1;z++)if(liquid(x,y,z))return false;
  return true;
 }
}
