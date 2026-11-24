package dev.koikaster.mite;
import java.util.Objects;
import java.util.function.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
/** Actual native target/path owner. Parent memory/flee decisions and world inputs
 * come from their production owners; no parallel target or path is stored. */
public final class SourceArachnidNativeHost implements SourceArachnidDecision.Host {
 private final PathfinderMob actor;private final SourceArachnidState state;
 
 private Entity idleLookTarget;private int idleLookTicks;private float idleYawVelocity;
 public SourceArachnidNativeHost(PathfinderMob actor){this.actor=Objects.requireNonNull(actor);state=SourceArachnidOwners.owner(actor);}

 public PathfinderMob actor(){return actor;}
 public boolean beforeFleeDecisions(){return state.parentLife.beforeDecisions();}
 public boolean fleeing(){return state.fleeing;}
 public Path path(){return actor.getNavigation().getPath();}
 public void path(Path value){SourceArachnidPathBudget.commit(actor,value);}
 public LivingEntity findPlayer(float range){return SourceArachnidAI.findPlayerToAttack(actor,range);}
 /** abw.findNearestSeenEntityWithinAABB accepts <=, so the last equal-distance
  * candidate wins. Source senses cache continues through the existing native bridge. */
 public LivingEntity findChicken(float range){
  if(SourceArachnidAI.peacefulDuringDay(actor)&&OriginalWeather.brightness(actor)>.5f&&SourceArachnidWorld.outdoors(actor))return null;
  Chicken best=null;double shortest=Double.MAX_VALUE;
  for(var chicken:actor.level().getEntitiesOfClass(Chicken.class,actor.getBoundingBox().inflate(range,range/4f,range))){if(!chicken.isAlive()||chicken==actor||!actor.getSensing().hasLineOfSight(chicken))continue;double distance=actor.distanceToSqr(chicken);if(distance<=shortest){best=chicken;shortest=distance;}}
  return best;
 }
 public float sourceRange(){return SourceArachnidAI.maximumTargetingRange(actor);}
 public boolean sourceLineOfStrike(LivingEntity target){Vec3 origin=actor.position().add(0,actor.getBbHeight()*.75f,0);for(float fraction:new float[]{.5f,.9f,.1f})if(OriginalRaycast.physicalLine(actor,origin,target.position().add(0,target.getBbHeight()*fraction,0)))return true;return false;}
 public void sourceAttackOrLeap(LivingEntity target,float distance){SourceArachnidAI.attackOrLeap(actor,target,state);}
 public Path route(Entity target,float maximum){return SourceArachnidPathBudget.entity(actor,target,maximum);}
 public Path route(BlockPos target,float maximum){return SourceArachnidPathBudget.block(actor,target,maximum);}
 public float walkWeight(BlockPos candidate){return (SourceArachnidWorld.cameFromSpawner(actor)?.7f:.5f)-SourceArachnidWorld.pointBrightness(actor.level(),candidate);}
 public long lastTickHarmedByCactus(){return SourceArachnidWorld.lastCactusTick(actor);}
 public boolean insideLeaves(){return actor.level().getBlockStates(actor.getBoundingBox()).anyMatch(state->state.is(BlockTags.LEAVES));}
 /** abw5157: current combined collision top, falling back exactly one block. */
 public double collisionTop(BlockPos waypoint){double top=top(waypoint);if(!Double.isNaN(top))return top;top=top(waypoint.below());return Double.isNaN(top)?waypoint.getY()-1:top;}
 private double top(BlockPos pos){var block=actor.level().getBlockState(pos);var kind=state.species();if(SourceArachnidProfiles.woodFamily(kind)&&block.is(BlockTags.LEAVES)&&(double)((float)pos.getY()+.99f)>actor.getY())return Double.NaN;var shape=block.getCollisionShape(actor.level(),pos,CollisionContext.of(actor));return shape.isEmpty()?Double.NaN:pos.getY()+shape.max(Direction.Axis.Y);}
 /** og.bl. Existing production despawn already has a native owner; no second
  * despawn call is made here. Native Navigation/MoveControl retain movement73A. */
 public void idleParentTick(){
  var random=actor.getRandom();actor.setXxa(0);actor.setZza(0);
  if(random.nextFloat()<.02f){var player=OriginalDespawn.closest(actor,8,false);if(player!=null){idleLookTarget=player;idleLookTicks=10+random.nextInt(20);}else idleYawVelocity=(random.nextFloat()-.5f)*20;}
  if(idleLookTarget!=null){actor.lookAt(idleLookTarget,10,actor.getMaxHeadXRot());if(idleLookTicks--<=0||idleLookTarget.isRemoved()||idleLookTarget.distanceToSqr(actor)>64)idleLookTarget=null;}
  else{if(random.nextFloat()<.05f)idleYawVelocity=(random.nextFloat()-.5f)*20;actor.setYRot(actor.getYRot()+idleYawVelocity);actor.setXRot(0);}
  if((actor.isInWater()||actor.isInLava())&&!preventedFromSwimming()&&random.nextFloat()<.8f)actor.getJumpControl().jump();
 }
 private boolean preventedFromSwimming(){int speed=actor.hasEffect(MobEffects.MOVEMENT_SPEED)?actor.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()+1:0;int slow=actor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?actor.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()+1:0;float combined=slow*-.2f+speed*.2f;if(combined<0)combined*=1-EnchantmentEffects.paralysisResistance(actor);return combined<-.5f;}
}
