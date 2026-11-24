package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Native entity type, state field, packets and persistence; original sr
 * centre coordinates and source tick own the carried soil lifecycle. */
public final class SourceFallingTransport258 {
 public static final byte SPLASH_EVENT=120;
 private static final float OFFSET=.98f/2f;
 private static final RandomSource PITCH=RandomSource.create();
 public static boolean eligible(BlockState state){return SoilGravity.falls(state);}
 public static FallingBlockEntity create(Level level,BlockPos pos,BlockState state){
  var body=new FallingBlockEntity(EntityType.FALLING_BLOCK,level);((SourceFallingAccess258)body).mite$fallingState258(SourceSoilProperties258.carried(state));
  double x=(float)pos.getX()+.5f,y=(float)pos.getY()+.5f- (double)OFFSET,z=(float)pos.getZ()+.5f;
  body.blocksBuilding=true;body.setPos(x,y,z);body.setDeltaMovement(Vec3.ZERO);body.xo=x;body.yo=y;body.zo=z;body.setStartPos(pos);return body;
 }
 public static FallingBlockEntity nativeFall(Level level,BlockPos pos,BlockState state){if(!eligible(state))return null;var body=create(level,pos,state);level.addFreshEntity(body);return body;}
 /** aqn slows an entity inside its block. Modern Entity.move additionally
  * applies the below-block factor while above it; sr had no such second path. */
 public static float speed(float original,Object raw){
  if(raw instanceof Entity body){
   var state=body.level().getBlockState(body.blockPosition());
   if(state.is(Blocks.SOUL_SAND))return 1f;
   if(state.is(Blocks.WATER)||state.is(Blocks.BUBBLE_COLUMN)||state.getBlock().getSpeedFactor()!=1f)return original;
   var below=BlockPos.containing(body.getX(),body.getY()-(double).500001f,body.getZ());
   if(body.level().getBlockState(below).is(Blocks.SOUL_SAND))return 1f;
  }return original;
 }
 public static boolean tick(Object raw){
  var body=(FallingBlockEntity)raw;if(!eligible(body.getBlockState()))return false;
  var state=new SourceFallingSoilRules243.State();var access=(SourceFallingAccess258)body;int flags=access.mite$fallingFlags258();
  state.block=BuiltInRegistries.BLOCK.getId(body.getBlockState().getBlock());state.metadata=SourceSoilProperties258.metadata(body.getBlockState());state.time=body.time;
  state.x=body.getX();state.y=body.getY()+(double)OFFSET;state.z=body.getZ();var motion=body.getDeltaMovement();state.vx=motion.x;state.vy=motion.y;state.vz=motion.z;
  state.yOffset=OFFSET;state.removed=body.isRemoved();state.ground=body.onGround();state.inWater=(flags&2)!=0;state.firstUpdate=(flags&1)==0;state.dropItem=body.dropItem;state.cancelDrop=access.mite$fallingCancelled258();
  SourceFallingSoilRules243.tick(new World(body),state);
  body.time=state.time;body.setDeltaMovement(state.vx,state.vy,state.vz);body.xo=state.previousX;body.yo=state.previousY-(double)OFFSET;body.zo=state.previousZ;
  access.mite$fallingFlags258((state.firstUpdate?0:1)|(state.inWater?2:0));return true;
 }
 public static void saved(Object raw,Object tag){var body=(FallingBlockEntity)raw;if(eligible(body.getBlockState()))((net.minecraft.nbt.CompoundTag)tag).putByte("Time",(byte)body.time);}
 public static void loaded(Object raw,Object tag){var body=(FallingBlockEntity)raw;var data=(net.minecraft.nbt.CompoundTag)tag;if(eligible(body.getBlockState())&&data.contains("Time",net.minecraft.nbt.Tag.TAG_BYTE))body.time=data.getByte("Time")&255;}
 public static VoxelShape collision(VoxelShape original,Object rawState,BlockGetter level,BlockPos pos,CollisionContext context){
  if(context instanceof EntityCollisionContext entityContext){
   var actor=entityContext.getEntity();var state=(BlockState)rawState;
   if(state.is(Blocks.SOUL_SAND)&&(actor instanceof FallingBlockEntity||actor instanceof ItemEntity))return Shapes.block();
   if(actor instanceof FallingBlockEntity body&&eligible(body.getBlockState())&&SourceSoilSupport246.dislodged(state,body.getBlockState(),level,pos))return Shapes.empty();
  }return original;
 }
 public static boolean event(Object raw,byte event){
  var body=(FallingBlockEntity)raw;if(event!=SPLASH_EVENT||!eligible(body.getBlockState()))return false;if(!body.level().isClientSide)return true;
  var random=((SourceFireRandom)body).mite$fireRandom();var motion=body.getDeltaMovement();float height=Mth.floor(body.getBoundingBox().minY),width=body.getBbWidth();
  for(int i=0;i<1f+width*20f;i++){float x=(random.nextFloat()*2f-1f)*width,z=(random.nextFloat()*2f-1f)*width;body.level().addParticle(ParticleTypes.BUBBLE,body.getX()+x,height+1f,body.getZ()+z,motion.x,motion.y-(double)(random.nextFloat()*.2f),motion.z);}
  for(int i=0;i<1f+width*20f;i++){float x=(random.nextFloat()*2f-1f)*width,z=(random.nextFloat()*2f-1f)*width;body.level().addParticle(ParticleTypes.SPLASH,body.getX()+x,height+1f,body.getZ()+z,motion.x,motion.y,motion.z);}
  float volume=(float)Math.sqrt(motion.x*motion.x*(double).2f+motion.y*motion.y+motion.z*motion.z*(double).2f)*.2f;if(volume>1f)volume=1f;volume*=2f;
  body.level().playLocalSound(body.getX(),body.getY()+(double)OFFSET,body.getZ(),SourceCreatureSounds.BLOCK_SPLASH.get(),SoundSource.BLOCKS,volume,1f+(PITCH.nextFloat()-PITCH.nextFloat())*.4f,false);return true;
 }
 private static final class World implements SourceFallingSoilRules243.Host {
  final FallingBlockEntity body;final Level level;final BlockState falling;
  World(FallingBlockEntity body){this.body=body;level=body.level();falling=body.getBlockState();}
  BlockPos pos(int x,int y,int z){return new BlockPos(x,y,z);}
  BlockState at(int x,int y,int z){return level.getBlockState(pos(x,y,z));}
  public boolean client(){return level.isClientSide;}public int floor(){return level.getMinBuildHeight();}public int ceiling(){return level.getMaxBuildHeight();}
  public int block(int x,int y,int z){var s=at(x,y,z);return s.isAir()?0:BuiltInRegistries.BLOCK.getId(s.getBlock());}
  public boolean liquid(int x,int y,int z){var b=at(x,y,z).getBlock();return b instanceof LiquidBlock||b instanceof BubbleColumnBlock;}
  public boolean water(int x,int y,int z){return liquid(x,y,z)&&at(x,y,z).getFluidState().is(FluidTags.WATER);}
  public boolean glass(int x,int y,int z){return SourceSoilSupport246.glass(at(x,y,z));}
  public boolean movingPiston(int x,int y,int z){return at(x,y,z).is(Blocks.MOVING_PISTON);}
  public boolean dislodged(int id,int metadata,int x,int y,int z){return SourceSoilSupport246.dislodged(at(x,y,z),falling,level,pos(x,y,z));}
  public boolean sandFalls(int metadata,int x,int y,int z){return SourceSoilSupport246.canFallBelow(at(x,y,z),Blocks.SAND.defaultBlockState(),level,pos(x,y,z));}
  public void move(SourceFallingSoilRules243.State s){body.time=s.time;body.setDeltaMovement(s.vx,s.vy,s.vz);body.move(MoverType.SELF,body.getDeltaMovement());s.x=body.getX();s.y=body.getY()+(double)OFFSET;s.z=body.getZ();s.ground=body.onGround();s.removed=body.isRemoved();var v=body.getDeltaMovement();s.vx=v.x;s.vy=v.y;s.vz=v.z;}
  public void discard(SourceFallingSoilRules243.State s){body.discard();s.removed=true;}
  public void splash(SourceFallingSoilRules243.State s){level.broadcastEntityEvent(body,SPLASH_EVENT);}
  public float randomFloat(){return ((SourceFireRandom)body).mite$fireRandom().nextFloat();}
  public void pop(SourceFallingSoilRules243.State s,float pitch){level.playSound(null,body.getX(),body.getY()+(double)OFFSET,body.getZ(),net.minecraft.sounds.SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.3f,pitch);}
  public int dropOccupied(SourceFallingSoilRules243.State s,int x,int y,int z,boolean collided){return SourceSoilDrops258.drop((ServerLevel)level,pos(x,y,z),at(x,y,z),body,collided);}
  public void remove(int x,int y,int z){level.removeBlock(pos(x,y,z),false);}
  public void destroyGlass(SourceFallingSoilRules243.State s,int x,int y,int z){var p=pos(x,y,z);SourceSoilDrops258.drop((ServerLevel)level,p,at(x,y,z),body,true);level.destroyBlock(p,false,body);}
  public boolean place(SourceFallingSoilRules243.State s,int x,int y,int z){return level.setBlock(pos(x,y,z),SourceSoilProperties258.carried(falling),3);}
  public void landed(SourceFallingSoilRules243.State s,int x,int y,int z){
   var p=pos(x,y,z);var sound=falling.is(Blocks.SOUL_SAND)?SoundType.SAND:falling.getSoundType(level,p,body);level.playSound(null,(float)x+.5f,(float)y+.5f,(float)z+.5f,sound.getBreakSound(),SoundSource.BLOCKS,(sound.getVolume()+1f)/8f,sound.getPitch()*.8f);
   if(falling.getBlock() instanceof Fallable fallable)fallable.onLand(level,p,falling,Blocks.AIR.defaultBlockState(),body);
   var below=p.below();var support=level.getBlockState(below);if(!support.isAir())support.getBlock().stepOn(level,below,support,body);
  }
  public void dropFalling(SourceFallingSoilRules243.State s){
   var item=new ItemStack(falling.getBlock());if(item.isEmpty())return;var dropped=new ItemEntity(level,body.getX(),body.getY()+(double)OFFSET,body.getZ(),item);dropped.setDefaultPickUpDelay();level.addFreshEntity(dropped);
  }
 }
 private SourceFallingTransport258(){}
}
