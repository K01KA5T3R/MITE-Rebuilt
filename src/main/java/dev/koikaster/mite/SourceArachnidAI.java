package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
/** EntityArachnid/on/tm branches through modern Navigation/MoveControl (human73A). */
public final class SourceArachnidAI {
 /** og.canSpawnOnLeaves accepts exact Wood/BlackWidow, not Phase. */
 public static final SpawnPlacements.Type WOOD_PLACEMENT=SpawnPlacements.Type.create("MITE_SOURCE_WOOD_SPIDER",(level,pos,type)->level.getBlockState(pos.below()).is(BlockTags.LEAVES)?level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir():SpawnPlacements.Type.ON_GROUND.canSpawnAt(level,pos,type));
 public static boolean allowSpawn(String name,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
  boolean selected=SourceSpecialSpawns.takeSelection(name,level,reason,pos);
  if(!SourceSpecialSpawns.allow(name,level,reason,pos,random,selected))return false;
  if(!name.equals("wood_spider")&&!name.equals("black_widow_spider")&&level.getBlockState(pos.below()).is(BlockTags.LEAVES))return false;
  if(!selected&&reason!=MobSpawnType.SPAWNER&&name.equals("wood_spider")&&!woodHabitat(level,pos))return false;
  return !name.equals("demon_spider")||level.getFluidState(pos).isEmpty()&&level.getFluidState(pos.above()).isEmpty();
 }
 /** js581 + abw400/438. Modern log/leaf tags adapt the original ID17/18 families. */
 public static boolean woodHabitat(LevelReader level,BlockPos pos){return (level.canSeeSky(pos)||firstAbove(level,pos,true)||firstAbove(level,pos,false))&&near(level,pos,5,2,false)&&near(level,pos.above(5),5,5,true);}
 private static boolean firstAbove(LevelReader level,BlockPos pos,boolean leaves){for(int y=pos.getY()+1;y<level.getMaxBuildHeight();y++){BlockState state=level.getBlockState(new BlockPos(pos.getX(),y,pos.getZ()));if(state.is(leaves?BlockTags.LEAVES:BlockTags.LOGS))return true;if(!state.isAir())return false;}return false;}
 private static boolean near(LevelReader level,BlockPos origin,int horizontal,int vertical,boolean leaves){for(int x=origin.getX()-horizontal;x<=origin.getX()+horizontal;x++)for(int z=origin.getZ()-horizontal;z<=origin.getZ()+horizontal;z++)for(int y=origin.getY()-vertical;y<=origin.getY()+vertical;y++)if(level.getBlockState(new BlockPos(x,y,z)).is(leaves?BlockTags.LEAVES:BlockTags.LOGS))return true;return false;}
 public static boolean peacefulDuringDay(Mob mob){return SourceArachnidOwners.species(mob)==SourceArachnidState.Species.NORMAL;}
 private static boolean daylightPeaceful(Mob mob){return peacefulDuringDay(mob)&&OriginalWeather.brightness(mob)>.5f&&SourceArachnidWorld.outdoors(mob);}
 public static float maximumTargetingRange(Mob mob){float range=OriginalTargetGoal.range(mob);var species=SourceArachnidOwners.species(mob);return SourceArachnidProfiles.woodFamily(species)?SourceArachnidProfiles.woodRange(mob,range):range;}
 public static Player findPlayerToAttack(Mob mob,float range){
  if(daylightPeaceful(mob))return null;Player best=null;double shortest=-1;
  for(var player:mob.level().players()){
   if(player.isSpectator()||player.getAbilities().invulnerable||!player.isAlive())continue;
   double limit=range*KingSlimeRewards.detectionMultiplier(mob,player);if(player.isCrouching())limit*=(double).8f;if(player.isInvisible())limit*=(double)(.7f*Math.max(.1f,player.getArmorCoverPercentage()));
   double distance=player.distanceToSqr(mob.getX(),mob.getY()+(double)(mob.getBbHeight()/2f),mob.getZ());
   if(range>=0&&distance>=limit*limit||shortest!=-1&&distance>=shortest)continue;best=player;shortest=distance;
  }
  return best;
 }
 public static Chicken findChicken(Mob mob,float range){
  if(daylightPeaceful(mob))return null;Chicken selected=null;double shortest=0;
  for(var chicken:mob.level().getEntitiesOfClass(Chicken.class,mob.getBoundingBox().inflate(range,range/4f,range))){if(!chicken.isAlive()||!mob.getSensing().hasLineOfSight(chicken))continue;double distance=mob.distanceTo(chicken);if(selected==null||distance<shortest){selected=chicken;shortest=distance;}}
  return selected;
 }
 public static void peacefulAfterTick(Mob mob){if(mob.getTarget() instanceof Player&&daylightPeaceful(mob)&&!(mob.getLastHurtByMob() instanceof Player)&&mob.getRandom().nextInt(100)==0)mob.setTarget(null);}
 private static boolean strikeLine(Mob mob,LivingEntity target){var origin=mob.position().add(0,mob.getBbHeight()*.75f,0);for(float fraction:new float[]{.5f,.9f,.1f})if(OriginalRaycast.physicalLine(mob,origin,target.position().add(0,target.getBbHeight()*fraction,0)))return true;return false;}
 /** Arachnid.a: a successful leap branch does NOT also execute tm's ordinary strike. */
 public static void attackOrLeap(Mob mob, LivingEntity target, SourceArachnidState state) {
  float distance = mob.distanceTo(target);
  if (distance > 2 && distance < 6 && mob.getRandom().nextInt(10) == 0) {
   if (!mob.onGround()) return;
   double dx = target.getX() - mob.getX();
   double dz = target.getZ() - mob.getZ();
   float length = (float) Math.sqrt(dx * dx + dz * dz);
   // The attack window uses 3D distance, so an aligned target can have no horizontal direction.
   if (length == 0f) return;
   var motion = mob.getDeltaMovement();
   mob.setDeltaMovement(dx / length * .5 * .8f + motion.x * .2f, .4f, dz / length * .5 * .8f + motion.z * .2f);
   mob.setYRot((float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90);
   var spider = (PathfinderMob) mob;
   var path = SourceArachnidPathBudget.entity(spider, target, 8);
   if (path != null) SourceArachnidPathBudget.commit(spider, path);
  } else if (state.attackCooldown <= 0 && distance < 1.75f
      && target.getBoundingBox().maxY > mob.getBoundingBox().minY
      && target.getBoundingBox().minY < mob.getBoundingBox().maxY) {
   if (!mob.getMainHandItem().isEmpty()) mob.swing(InteractionHand.MAIN_HAND);
   state.attackCooldown = 20;
   mob.doHurtTarget(target);
  }
 }
 public static void install(Mob mob,SourceArachnidState state){
  if(state.installed)return;var access=(SourceMobAi)mob;access.mite$goals().removeAllGoals(goal->true);access.mite$targets().removeAllGoals(goal->true);
  access.mite$goals().addGoal(5,new SourceArachnidDecision(new SourceArachnidNativeHost((PathfinderMob)mob)));
  if(state.species()==SourceArachnidState.Species.DEMON)access.mite$goals().addGoal(4,new OriginalLeaveLavaGoal((PathfinderMob)mob,1));
  state.installed=true;
 }
 private SourceArachnidAI(){}
}
