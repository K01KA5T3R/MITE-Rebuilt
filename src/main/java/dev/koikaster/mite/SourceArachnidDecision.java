package dev.koikaster.mite;
import java.util.EnumSet;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
/** Source0.31. on.bl/bK decisions for EntityArachnid, preserving source branch
 * and random-call order around the human73A native navigation interface.
 * Host dependencies are mandatory; no placeholder is presented as integration. */
public final class SourceArachnidDecision extends Goal {
 public interface Host {
  PathfinderMob actor();
  /** OriginalParentFleeTransition.beforeDecisions(), wrapping the existing flags. */
  boolean beforeFleeDecisions();
  boolean fleeing();
  Path path();
  /** Native Navigation's single Path reference; no second source-path field. */
  void path(Path value);
  LivingEntity findPlayer(float sourceRange);
  LivingEntity findChicken(float sourceRange);
  float sourceRange();
  boolean sourceLineOfStrike(LivingEntity target);
  void sourceAttackOrLeap(LivingEntity target,float distance);
  Path route(Entity target,float sourceMaxDistance);
  Path route(BlockPos target,float sourceMaxDistance);
  /** tm.a(candidate): (.7 if came_from_spawner, otherwise .5)-abw.q(candidate).
   * Current OriginalWeather.brightness(actor) is NOT a candidate-point adapter. */
  float walkWeight(BlockPos candidate);
  long lastTickHarmedByCactus();
  boolean insideLeaves();
  double collisionTop(BlockPos waypoint);
  /** og.bl's idle-look/swim logic; must not duplicate the existing despawn hook. */
  void idleParentTick();
 }
 private final Host host;
 public SourceArachnidDecision(Host host){this.host=Objects.requireNonNull(host);setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));}
 @Override public boolean canUse(){return true;}
 @Override public boolean canContinueToUse(){return true;}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 private static float budget(PathfinderMob actor,Entity target,float range){return Math.max(4,Math.min(actor.distanceTo(target)*2,range));}
 @Override public void tick(){
  var actor=host.actor();var random=actor.getRandom();float range=Math.min(host.sourceRange(),24);boolean fleeing=host.beforeFleeDecisions();int offset=actor.tickCount+actor.getId()*47;
  if(!fleeing){
   var target=actor.getTarget();if(target!=null&&!target.isAlive()){actor.setTarget(null);target=null;}
   if(target==null){
    // Source acquired-player branch requests its full-range graph immediately.
    if(offset%10==0){target=host.findPlayer(range);actor.setTarget(target);}
    if(target!=null)host.path(host.route(target,range));
    else if(random.nextFloat()<.01f){target=host.findChicken(range);actor.setTarget(target);}
    // on.findTargetEntityItem returns null for all currently ported Arachnids.
    // No immediate prey path and no attack on the same newly-acquired tick.
   }else if(!target.isRemoved()){
    if(host.sourceLineOfStrike(target))host.sourceAttackOrLeap(target,actor.distanceTo(target));
   }else actor.setTarget(null);
  }
  var target=actor.getTarget();
  if(target!=null&&offset%8==0){
   float maximum=offset%40==0?range:budget(actor,target,range);var path=host.route(target,maximum);if(path!=null)host.path(path);
  }else if(!host.fleeing()&&host.path()==null&&target==null&&random.nextInt(150)==0)stroll();
  else{
   long stamp=host.lastTickHarmedByCactus();long age=stamp<1?-1:actor.level().getGameTime()-stamp;var path=host.path();
   if(age>=0&&age<10&&(path==null||path.isDone()))stroll();
  }
  var path=host.path();
  // Source draws even for an already-finished non-null path, before done-check.
  if(path!=null&&random.nextInt(host.fleeing()?1000:100)>0&&!path.isDone())follow(path,range);
  else{
   if(path!=null){
    target=host.findPlayer(range);if(target==null)target=host.findChicken(range);actor.setTarget(target);
    if(target!=null){host.path(host.route(target,range));host.idleParentTick();return;}
   }
   host.idleParentTick();host.path(null);
  }
 }
 /** on.bK's non-intensive source branch selects candidate ten, not best-of-ten.
  * path_entity remains null, so the original conditional weight-skip never runs. */
 private void stroll(){
  var actor=host.actor();var random=actor.getRandom();var origin=actor.blockPosition();BlockPos selected=origin;float lastWeight=-100;
  for(int attempt=0;attempt<10;attempt++){var candidate=origin.offset(random.nextInt(13)-6,random.nextInt(7)-3,random.nextInt(13)-6);lastWeight=host.walkWeight(candidate);selected=candidate;}
  if(lastWeight> -100)host.path(host.route(selected,10));
 }
 /** tq.bl also invokes the inherited on.bK after its own colony branch. */
 public void strollSource(){stroll();}
 private void follow(Path path,float range){
  var actor=host.actor();var random=actor.getRandom();Vec3 point=path.getNextEntityPos(actor);double radius=actor.getBbWidth()*2f;
  while(point!=null&&horizontalSquared(point,actor.position())<radius*radius){
   path.advance();
   if(path.isDone()){
    // include_last_point is only needed by tg's dropped-item branch, not Arachnid.
    point=null;host.path(null);var target=actor.getTarget();if(target!=null)host.path(host.route(target,budget(actor,target,range)));break;
   }
   point=path.getNextEntityPos(actor);
  }
  // Native Navigation/MoveControl owns movement. These source decisions retain
  // facing and jump requests instead of installing the old movement engine.
  if(point!=null){double dx=point.x-actor.getX(),dz=point.z-actor.getZ();actor.setYRot((float)(Math.atan2(dz,dx)*180/Math.PI)-90);double rise=host.collisionTop(BlockPos.containing(point))-actor.getBoundingBox().minY;if(actor.horizontalCollision&&rise>actor.maxUpStep())actor.getJumpControl().jump();}
  var target=actor.getTarget();if(target!=null){actor.lookAt(target,30,30);if(host.insideLeaves()&&target.getBoundingBox().minY>actor.getBoundingBox().minY)actor.getJumpControl().jump();}
  if(actor.horizontalCollision&&host.path()==null)actor.getJumpControl().jump();
  if(random.nextFloat()<.8f&&(actor.isInWater()||actor.isInLava()))actor.getJumpControl().jump();
 }
 private static double horizontalSquared(Vec3 a,Vec3 b){double dx=a.x-b.x,dz=a.z-b.z;return dx*dx+dz*dz;}
}
