package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;

/** tv.summonWolves and the animal branch of js.tryCreateNewLivingEntityCloseTo.
 * Existing sf ownership, source random tables and summoned FX are consumers,
 * not duplicated implementations of those already sealed subsystems. */
public final class SourceWitchSummons {
 private static final ThreadLocal<Object> WORLD_QUERY=new ThreadLocal<>();
 public static boolean allowWorldQuery(Object navigation){return WORLD_QUERY.get()==navigation;}
 static net.minecraft.world.level.pathfinder.Path worldPath(Wolf wolf,BlockPos target){
  var previous=WORLD_QUERY.get();WORLD_QUERY.set(wolf.getNavigation());
  try{return SourceArachnidPathBudget.block(wolf,target,32);}
  finally{if(previous==null)WORLD_QUERY.remove();else WORLD_QUERY.set(previous);}
 }
 static Wolf closeTo(ServerLevel level,BlockPos origin){
  OriginalRandomTables.initialize(level.getSeed());int index=level.random.nextInt();
  for(int attempt=0;attempt<16;attempt++){
   int dx=OriginalRandomTables.int_max[++index&32767]%33-16,dy=OriginalRandomTables.int_7_minus_3[++index&32767],dz=OriginalRandomTables.int_max[++index&32767]%33-16;
   var pos=origin.offset(dx,dy,dz);
   for(int i=0;i<8&&SourceTroopPlacement.passable(level,pos.below(),false);i++)pos=pos.below();
   for(int i=0;i<8&&!SourceTroopPlacement.passable(level,pos,false);i++)pos=pos.above();
   int vertical=pos.getY()-origin.getY(),squared=dx*dx+vertical*vertical+dz*dz;
   if(squared<64||squared>256||level.isOutsideBuildHeight(pos.below())||level.isOutsideBuildHeight(pos.above())||!level.hasChunkAt(pos.below())||!level.hasChunkAt(pos)||!level.hasChunkAt(pos.above())||SourceTroopPlacement.passable(level,pos.below(),true)||!SourceTroopPlacement.passable(level,pos,false)||!SourceTroopPlacement.passable(level,pos.above(),false))continue;
   // aci.animal accepts only grass, and rp.getCanSpawnHere(false) checks
   // grass again at the settled foot Y. No native light/biome/distance24 gate.
   if(!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK))continue;
   double y=SourceTroopPlacement.surfaceY(level,pos);if(!Double.isFinite(y))continue;
   Wolf wolf=EntityType.WOLF.create(level);if(wolf==null)continue;
   wolf.moveTo(pos.getX()+.5,y,pos.getZ()+.5,level.random.nextFloat()*360,0);
   if(!level.getBlockState(wolf.blockPosition().below()).is(Blocks.GRASS_BLOCK)||!level.isUnobstructed(wolf)||!level.noCollision(wolf)||level.containsAnyLiquid(wolf.getBoundingBox()))continue;
   var event=new MobSpawnEvent.PositionCheck(wolf,level,MobSpawnType.MOB_SUMMONED,null);MinecraftForge.EVENT_BUS.post(event);
   if(event.getResult()==Event.Result.DENY)continue;return wolf;
  }return null;
 }
 static int summon(SourceWitch witch,Object remembered){
  if(!(witch.level() instanceof ServerLevel world)||!(remembered instanceof Entity original)||!(world.getEntity(original.getId()) instanceof LivingEntity target)||target.isRemoved())return 0;
  var foot=target.blockPosition();int maximum=witch.getRandom().nextInt(3)+1,count=0;
  for(int attempt=0;attempt<16;attempt++){
   Wolf wolf=closeTo(world,foot);if(wolf==null)continue;
   // js calls the world path query before insertion, without rf.canNavigate.
   // Permit this one graph query while retaining native evaluator/geometry.
   var path=worldPath(wolf,foot);if(path==null||path.getEndNode()==null)continue;
   var end=path.getEndNode();int dx=end.x-foot.getX(),dy=end.y-foot.getY(),dz=end.z-foot.getZ();if(dx*dx+dy*dy+dz*dz>2)continue;
   OriginalDespawn.refresh(wolf,-9600);if(!world.addFreshEntity(wolf))continue;
   SourceSpawnInitialization.finish(wolf,world,MobSpawnType.MOB_SUMMONED,null);
   // Ordinary sf has one existing ally state. Mirror its old compatibility
   // marker for the sealed despawn consumer, without adding another owner.
   OriginalWolfOwners.owner(wolf).life.witchAlly=true;wolf.getPersistentData().putBoolean("is_witch_ally",true);
   wolf.setTarget(target);SourceSummonedFx.send(wolf);if(++count==maximum)break;
  }return count;
 }
 private SourceWitchSummons(){}
}
