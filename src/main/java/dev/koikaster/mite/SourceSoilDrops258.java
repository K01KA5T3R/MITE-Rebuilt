package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** aqz and the effective source drop owners catalogued in245/250. The two
 * active reasons here are collided_with_entity and crushed, never harvest. */
public final class SourceSoilDrops258 {
 public static int drop(ServerLevel level,BlockPos pos,BlockState state,FallingBlockEntity body,boolean collided){
  var block=state.getBlock();
  if(block instanceof LiquidBlock||block instanceof BubbleColumnBlock||block instanceof BaseRailBlock||block instanceof BaseFireBlock)return 0;
  if(block instanceof AbstractGlassBlock)return emit(level,pos,state,new ItemStack(Content.GLASS_SHARD.get()),6,1,0);
  if(state.is(Blocks.GLASS_PANE)||block instanceof StainedGlassPaneBlock)return emit(level,pos,state,new ItemStack(Content.GLASS_SHARD.get()),1,1,0);
  if(state.is(Blocks.GLOWSTONE))return emit(level,pos,state,new ItemStack(Items.GLOWSTONE_DUST),2+level.random.nextInt(3),1,0);
  if(state.is(Blocks.BEACON))return collided?emit(level,pos,state,new ItemStack(Items.BEACON),1,1,0):0;
  if(block instanceof AbstractSkullBlock){
   if(!collided)return emit(level,pos,state,new ItemStack(Items.BONE_MEAL),1,1,0);
   var stack=new ItemStack(block.asItem());
   if(level.getBlockEntity(pos) instanceof SkullBlockEntity skull&&skull.getOwnerProfile()!=null)stack.getOrCreateTag().put("SkullOwner",NbtUtils.writeGameProfile(new CompoundTag(),skull.getOwnerProfile()));
   return emit(level,pos,state,stack,1,1,0);
  }
  if(block instanceof RedStoneWireBlock)return emit(level,pos,state,new ItemStack(Items.REDSTONE),1,1,5);
  if(block instanceof RedstoneTorchBlock||block instanceof SignBlock||block instanceof DiodeBlock)return emit(level,pos,state,new ItemStack(block.asItem()),1,1,0);
  if(block instanceof TripWireBlock)return emit(level,pos,state,new ItemStack(Items.STRING),1,1,0);
  if(block instanceof DeadBushBlock)return emit(level,pos,state,new ItemStack(Items.STICK),1,.05f,0);
  if(block instanceof NetherWartBlock)return emit(level,pos,state,new ItemStack(Items.NETHER_WART),state.getValue(NetherWartBlock.AGE)>=3?2+level.random.nextInt(3):1,1,0);
  if(block instanceof Agriculture.DeadCrop)return 0;
  if(block instanceof CropBlock crop){if(state.is(Blocks.POTATOES)&&crop.getAge(state)>0&&!crop.isMaxAge(state))return emit(level,pos,state,new ItemStack(Items.POISONOUS_POTATO),1,1,0);return 0;}
  if(block instanceof TallGrassBlock||block instanceof WebBlock||block instanceof SnowLayerBlock||block instanceof VineBlock||block instanceof StemBlock||block instanceof AttachedStemBlock||block instanceof SweetBerryBushBlock)return 0;
  if(block instanceof FlowerPotBlock)return 0;
  if(!collided)return 0;
  if(block instanceof SugarCaneBlock)return emit(level,pos,state,new ItemStack(Items.SUGAR_CANE),1,1,0);
  // Direct source counterparts use their exact native item aliases. Retained
  // modern counterparts retain their loot functions when hit without a tool.
  int count=0;for(var stack:Block.getDrops(state,level,pos,level.getBlockEntity(pos),body,ItemStack.EMPTY))count+=emit(level,pos,state,stack.copyWithCount(1),stack.getCount(),1,0);return count;
 }
 static int emit(ServerLevel level,BlockPos pos,BlockState state,ItemStack sample,int quantity,float chance,int xp){
  if(sample.isEmpty()||quantity<1||chance<=0)return 0;
  var shape=state.getShape(level,pos);var bounds=shape.isEmpty()?new AABB(0,0,0,1,1,1):shape.bounds();
  double cx=(bounds.minX+bounds.maxX)*.5,cy=(bounds.minY+bounds.maxY)*.5,cz=(bounds.minZ+bounds.maxZ)*.5;
  double minX=cx+(bounds.minX-cx)*.5,minY=cy+(bounds.minY-cy)*.5,minZ=cz+(bounds.minZ-cz)*.5,maxX=cx+(bounds.maxX-cx)*.5,maxY=cy+(bounds.maxY-cy)*.5,maxZ=cz+(bounds.maxZ-cz)*.5;
  int count=0;
  for(int i=0;i<quantity;i++)if(level.random.nextFloat()<chance){
   count++;
   if(level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)){
    var item=new ItemEntity(level,pos.getX()+level.random.nextFloat()*(maxX-minX)+minX,pos.getY()+level.random.nextFloat()*(maxY-minY)+minY,pos.getZ()+level.random.nextFloat()*(maxZ-minZ)+minZ,sample.copy());item.setDefaultPickUpDelay();level.addFreshEntity(item);
   }
  }
  if(xp>0&&count>0)ExperienceOrb.award(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),xp*count);
  return count;
 }
 private SourceSoilDrops258(){}
}
