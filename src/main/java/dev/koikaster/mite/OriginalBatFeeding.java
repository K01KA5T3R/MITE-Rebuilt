package dev.koikaster.mite;

/** R195 port. EntityVampireBat/EntityNightwing counters and successful-hit
 * consumers. No Nightwing weapon immunity is introduced by this class. */
public final class OriginalBatFeeding<T> {
 public record Hit(boolean negativelyAffected,float healthLost){}
 public interface World<T> {
  boolean vampire();boolean giant();boolean nightwing();
  float health();float maximumHealth();int ticksWithOffset();
  T target();void target(T target);T closestPrey(double radius,boolean requiresLineOfSight,boolean requiresPath);
  boolean player(T target);boolean creative(T target);boolean trueAnimal(T target);boolean villager(T target);
  boolean ownHalfBoxIntersects(T target);Hit attack(T target);void heal(float actualLost);
  void provokeLivingUntargetedOcelot(T target);void dimPlayerVision(T target,float amount,int resistanceKind);
  void parentCollision(T target);
 }
 private int attackCooldown,feedCooldown;
 public int savedFeedCooldown(){return feedCooldown;}
 public void loadFeedCooldown(short value){feedCooldown=value;}
 public boolean preysUpon(World<T> world,T target){
  if(!world.vampire()&&!world.nightwing())return false;
  if(world.vampire()&&feedCooldown>0)return world.player(target)&&!world.creative(target);
  return world.player(target)&&!world.creative(target)||world.trueAnimal(target)||world.villager(target);
 }
 /** Called after the actual source Bat base tick, preserving subclass ordering. */
 public void afterBaseTick(World<T> world){
  if(!world.vampire()&&!world.nightwing())return;
  if(attackCooldown>0)--attackCooldown;
  if(world.vampire()&&feedCooldown>0){
   if(world.health()<world.maximumHealth())feedCooldown=0;
   else if(--feedCooldown>0){T target=world.target();if(target!=null&&!preysUpon(world,target))world.target(null);}
  }
  if(world.ticksWithOffset()%20==0)world.target(world.closestPrey(32,true,false));
 }
 public void collided(World<T> world,T other){
  world.parentCollision(other);
  if(!world.vampire()&&!world.nightwing())return;
  if(attackCooldown>0||other!=world.target())return;
  if(world.ownHalfBoxIntersects(other)){
   Hit result=world.attack(other);
   if(result!=null){
    if(world.vampire()&&result.healthLost()>0){
     world.heal(result.healthLost());world.provokeLivingUntargetedOcelot(other);
     if(!world.giant()&&world.health()>=world.maximumHealth())feedCooldown=1200;
    }
    if(world.nightwing()&&result.negativelyAffected()&&world.player(other))world.dimPlayerVision(other,1.25f,4);
   }
  }
  attackCooldown=20;
 }
}
