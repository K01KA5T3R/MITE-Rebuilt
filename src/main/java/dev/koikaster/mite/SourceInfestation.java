package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/** aqs metadata0..4 and tq colony wake, including the real Netherspawn leaf.
 * The inherited wake's post-replacement metadata0 behavior is preserved. */
public final class SourceInfestation {
 public static final BooleanProperty DEEPSLATE=BooleanProperty.create("deepslate");
 public static final RegistryObject<Block> COPPER_STONE=Content.BLOCKS.register("copperspine_infested_stone",CopperStone::new);
 public static final RegistryObject<Block> NETHER_STONE=Content.BLOCKS.register("netherspawn_infested_netherrack",NetherStone::new);
 static void register(){}
 static boolean infested(BlockState state){return state.getBlock() instanceof InfestedBlock||state.is(COPPER_STONE.get())||state.is(NETHER_STONE.get());}
 static boolean ordinaryStone(BlockState state){return state.is(Blocks.INFESTED_STONE)||state.is(Blocks.INFESTED_DEEPSLATE);}
 static boolean copperNearby(ServerLevel level,BlockPos pos){for(var at:BlockPos.betweenClosed(pos.offset(-3,-3,-3),pos.offset(3,3,3))){if(!level.hasChunkAt(at))continue;var state=level.getBlockState(at);if(state.is(Blocks.COPPER_ORE)||state.is(Blocks.DEEPSLATE_COPPER_ORE))return true;}return false;}
 /** aqs.convertVeinMetadata propagates only through adjacent metadata0 blocks. */
 static void convertVein(ServerLevel level,BlockPos origin){
  var queue=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();queue.add(origin.immutable());
  while(!queue.isEmpty()){var at=queue.removeFirst();if(!seen.add(at)||!level.hasChunkAt(at)||!ordinaryStone(level.getBlockState(at)))continue;boolean deep=level.getBlockState(at).is(Blocks.INFESTED_DEEPSLATE);level.setBlock(at,COPPER_STONE.get().defaultBlockState().setValue(DEEPSLATE,deep),2);for(var direction:Direction.values())queue.add(at.relative(direction));}
 }
 public static void update(Object rawLevel,Object rawPos){
  var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;if(level.dimension()==Level.NETHER||!ordinaryStone(level.getBlockState(pos)))return;
  boolean nearby=false;for(var player:level.players())if(!player.isSpectator()&&player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<256){nearby=true;break;}if(!nearby)return;
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(!level.hasChunk((pos.getX()>>4)+x,(pos.getZ()>>4)+z))return;
  if(copperNearby(level,pos))convertVein(level,pos);
 }
 public static void playerDestroyed(Object rawLevel,Object rawPlayer,Object rawPos,Object rawState){if(rawLevel instanceof ServerLevel level)release(level,(BlockPos)rawPos,(BlockState)rawState,(Player)rawPlayer);}
 static SourceSilverfish release(ServerLevel level,BlockPos pos,BlockState state,LivingEntity target){
  boolean copper=state.is(COPPER_STONE.get())||ordinaryStone(state)&&copperNearby(level,pos);
  if(copper&&ordinaryStone(state))for(var direction:Direction.values())convertVein(level,pos.relative(direction));
  var type=state.is(NETHER_STONE.get())?SourceNetherspawnContent.NETHERSPAWN.get():copper?SourceSilverfishContent.COPPERSPINE.get():level.dimension()==WorldEvents.UNDERWORLD?SourceSilverfishContent.HOARY.get():net.minecraft.world.entity.EntityType.SILVERFISH;
  var entity=(SourceSilverfish)type.create(level);if(entity==null)throw new IllegalStateException("Source silverfish factory missing");
  entity.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);level.addFreshEntity(entity);entity.spawnAnim();entity.setTarget(target);entity.playAmbientSound();OriginalDespawn.refresh(entity,-9600);return entity;
 }
 static void summon(SourceSilverfish actor){
  if(!(actor.level() instanceof ServerLevel level))return;var center=actor.blockPosition();
  for(int y=0;y<=5&&y>=-5;y=y<=0?1-y:-y)for(int x=0;x<=10&&x>=-10;x=x<=0?1-x:-x)for(int z=0;z<=10&&z>=-10;z=z<=0?1-z:-z){
   var pos=center.offset(x,y,z);if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);if(!infested(state))continue;
   if(level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))level.destroyBlock(pos,false,actor);
   else{var host=state.getBlock() instanceof InfestedBlock block?block.hostStateByInfested(state):(state.hasProperty(DEEPSLATE)&&state.getValue(DEEPSLATE)?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();level.setBlock(pos,host,3);}
   // tq creates its second BlockBreakInfo AFTER replacement/destruction. Its
   // metadata is consequently0, even when the original host had another subtype.
   release(level,pos,Blocks.INFESTED_STONE.defaultBlockState(),null);
   if(actor.getRandom().nextInt(4)==0)return;
  }
 }
 public static final class CopperStone extends Block {
  CopperStone(){super(BlockBehaviour.Properties.copy(Blocks.STONE).strength(0,.75f));registerDefaultState(stateDefinition.any().setValue(DEEPSLATE,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(DEEPSLATE);}
  @Override public void playerDestroy(Level level,Player player,BlockPos pos,BlockState state,BlockEntity blockEntity,ItemStack tool){super.playerDestroy(level,player,pos,state,blockEntity,tool);playerDestroyed(level,player,pos,state);}
 }
 public static final class NetherStone extends Block {
  NetherStone(){super(BlockBehaviour.Properties.copy(Blocks.NETHERRACK).strength(0,.75f));}
  @Override public void playerDestroy(Level level,Player player,BlockPos pos,BlockState state,BlockEntity blockEntity,ItemStack tool){super.playerDestroy(level,player,pos,state,blockEntity,tool);playerDestroyed(level,player,pos,state);}
 }
 private SourceInfestation(){}
}
