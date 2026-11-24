package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Activated source callbacks. Native block state and chunk tick storage own
 * the source bit and the separate, serializable pending transport intent. */
public final class SourceSoilControl271 {
 public static void onPlace(BlockState state,Level level,BlockPos pos,BlockState old){
  if(!old.is(state.getBlock())&&state.getValue(SourceSoilProperties258.PENDING))level.setBlock(pos,state.setValue(SourceSoilProperties258.PENDING,false),0);
 }
 public static void placedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
  if(!(level instanceof ServerLevel server)||!(placer instanceof Player)||level.captureBlockSnapshots||level.restoringBlockSnapshots)return;
  var host=new World(server);var block=host.block(pos.getX(),pos.getY(),pos.getZ());
  if(block!=null&&block.undermines())SourceUndermineRules237.placed(host,block,pos.getX(),pos.getY(),pos.getZ(),SourceSoilProperties258.metadata(state),actor(placer),false);
 }
 public static void committedPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved,Player placer){
  state.onPlace(level,pos,old,moved);
  if(SoilGravity.affected(state))placedBy(level,pos,level.getBlockState(pos),placer,ItemStack.EMPTY);
  if(state.getBlock() instanceof SourceSmallMushroomBlock599&&level instanceof ServerLevel server)SourceMushroomWorld599.placed(server,pos,placer);
 }
 public static int afterHarvest(int result,ServerLevel level,ServerPlayer player,BlockPos pos,BlockState removed,boolean actualRemoval){
  if(!actualRemoval||player.isCreative()||removed.getBlock() instanceof TorchBlock)return result;
  undermine(level,pos.above(),player);
  for(var direction:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST})undermine(level,pos.relative(direction),player);
  return result;
 }
 public static void exploded(BlockState state,Level level,BlockPos pos,Explosion explosion){
  state.onBlockExploded(level,pos,explosion);
  if(level instanceof ServerLevel server&&!level.getBlockState(pos).is(state.getBlock()))schedule(server,pos.above());
 }
 public static void undermine(ServerLevel level,BlockPos pos,Player player){
  var world=new World(level);var block=world.block(pos.getX(),pos.getY(),pos.getZ());
  if(block!=null&&block.undermines())SourceUndermineRules237.undermined(world,block,actor(player),pos.getX(),pos.getY(),pos.getZ());
 }
 public static boolean tryFall(ServerLevel level,BlockPos pos){
  var world=new World(level);var block=world.block(pos.getX(),pos.getY(),pos.getZ());
  return block!=null&&block.undermines()&&SourceUndermineRules237.tryFall(world,block,pos.getX(),pos.getY(),pos.getZ());
 }
 public static boolean ordinaryFall(ServerLevel level,BlockPos pos){
  if(!SoilGravity.ordinary(level.getBlockState(pos)))return false;
  var world=new World(level);return SourceUndermineRules237.tryFall(world,world.block(pos.getX(),pos.getY(),pos.getZ()),pos.getX(),pos.getY(),pos.getZ());
 }
 public static void schedule(ServerLevel level,BlockPos pos){
  var world=new World(level);var block=world.block(pos.getX(),pos.getY(),pos.getZ());
  if(block!=null&&block.undermines())SourceUndermineRules237.schedule(world,block,pos.getX(),pos.getY(),pos.getZ());
 }
 public static void tick(ServerLevel level,BlockPos pos){
  var state=level.getBlockState(pos);if(!SoilGravity.affected(state)||!state.getValue(SourceSoilProperties258.PENDING))return;
  var world=new World(level);SourceUndermineRules237.tick(world,world.block(pos.getX(),pos.getY(),pos.getZ()),pos.getX(),pos.getY(),pos.getZ());
  state=level.getBlockState(pos);if(SoilGravity.affected(state)&&state.getValue(SourceSoilProperties258.PENDING))level.setBlock(pos,state.setValue(SourceSoilProperties258.PENDING,false),0);
 }
 public static void walked(Level rawLevel,BlockPos pos){
  if(!(rawLevel instanceof ServerLevel level)||!SoilGravity.affected(level.getBlockState(pos)))return;
  var world=new World(level);SourceUndermineRules237.walked(world,world.block(pos.getX(),pos.getY(),pos.getZ()),pos.getX(),pos.getY(),pos.getZ());
 }
 public static void collided(Level rawLevel,BlockPos pos){
  if(!(rawLevel instanceof ServerLevel level)||!SoilGravity.affected(level.getBlockState(pos)))return;
  var world=new World(level);SourceUndermineRules237.collided(world,world.block(pos.getX(),pos.getY(),pos.getZ()),pos.getX(),pos.getY(),pos.getZ());
 }
 private static SourceUndermineRules237.Actor actor(Entity entity){return entity==null?null:new SourceUndermineRules237.Actor(){public boolean player(){return entity instanceof Player;}public boolean creative(){return entity instanceof Player p&&p.isCreative();}};}
 private static final class Key implements SourceUndermineRules237.Block {
  final BlockState state;Key(BlockState state){this.state=state;}
  public boolean undermines(){return SoilGravity.affected(state);}public int bit(){return undermines()?1:0;}public int delay(SourceUndermineRules237.Host world){return 2;}
  public boolean newSandPhysics(){return false;}public boolean solid(int metadata){return SourceSoilSupport246.solid(state);}
 }
 private static final class World implements SourceUndermineRules237.Host {
  final ServerLevel level;World(ServerLevel level){this.level=level;}
  BlockPos pos(int x,int y,int z){return new BlockPos(x,y,z);}
  public Key block(int x,int y,int z){var state=level.getBlockState(pos(x,y,z));return state.isAir()?null:new Key(state);}
  public int metadata(int x,int y,int z){return SourceSoilProperties258.metadata(level.getBlockState(pos(x,y,z)));}
  public boolean client(){return false;}public boolean instant(){return false;}public boolean experimental(){return false;}public int floor(){return level.getMinBuildHeight();}
  public boolean loaded(int a,int b,int c,int d,int e,int f){return level.hasChunksAt(a,b,c,d,e,f);}
  public boolean dislodged(SourceUndermineRules237.Block falling,int metadata,int x,int y,int z){var p=pos(x,y,z);return SourceSoilSupport246.dislodged(level.getBlockState(p),((Key)falling).state,level,p);}
  public Object createFalling(SourceUndermineRules237.Block block,int x,int y,int z,int metadata){return SourceFallingTransport258.create(level,pos(x,y,z),((Key)block).state);}
  public void onStartFalling(SourceUndermineRules237.Block block,int x,int y,int z,Object entity){}
  public void spawn(Object entity){level.addFreshEntity((Entity)entity);}
  public void remove(int x,int y,int z){level.removeBlock(pos(x,y,z),false);}
  public void write(SourceUndermineRules237.Block block,int x,int y,int z,int metadata,int flags){
   var p=pos(x,y,z);var original=((Key)block).state;var current=level.getBlockState(p);
   // The source metadata clear must not recreate an origin already consumed
   // by its unloaded-area instant branch. Keep only a surviving same block.
   if(flags==0&&!current.is(original.getBlock()))return;
   var state=current.is(original.getBlock())?current:original;
   if(state.hasProperty(SourceSoilProperties258.UNDERMINED))state=state.setValue(SourceSoilProperties258.UNDERMINED,(metadata&1)!=0);
   if(flags!=0)state=SourceSoilProperties258.carried(state);level.setBlock(p,state,flags);
  }
  public void schedule(SourceUndermineRules237.Block block,int x,int y,int z,int delay){
   var p=pos(x,y,z);var state=level.getBlockState(p);if(!SoilGravity.affected(state))return;
   if(state.getValue(SourceSoilProperties258.PENDING))return;
   level.setBlock(p,state.setValue(SourceSoilProperties258.PENDING,true),0);
   // Native deduplication would let an existing twenty-tick bubble-column
   // check swallow the source two-tick intent. Also remove already queued
   // callbacks for this exact position, so an earlier bubble cannot run it.
   level.getBlockTicks().clearArea(new BoundingBox(p));level.scheduleTick(p,state.getBlock(),delay);
  }
  public void checkNotLegal(SourceUndermineRules237.Block block,int x,int y,int z){}
  public boolean parentPlaced(SourceUndermineRules237.Block block,int x,int y,int z,int metadata,SourceUndermineRules237.Actor actor,boolean testOnly){return true;}
 }
 private SourceSoilControl271(){}
}
