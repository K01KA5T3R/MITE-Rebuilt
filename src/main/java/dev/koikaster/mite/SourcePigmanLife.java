package dev.koikaster.mite;

/** tn leaf, retaining its original order and the absence of an anger decrement.
 * Native entity/old-AI integration is a separate required part of this port. */
public final class SourcePigmanLife {
 public static final String ATTACK_SPEED_ID="49455A49-7EC5-45BA-B886-3B90B23A1718";
 public static final double BASE_SPEED=.5,ATTACK_BOOST=.45,BASE_ATTACK=8,FOLLOW_RANGE=40;
 public interface Host {
  boolean client();Object rawTarget();Object entityToAttack();void target(Object value);
  void removeAttackBoost();void addAttackBoost();int nextInt(int bound);float nextFloat();void angerSound(float pitch);
  Object closestPlayer(float range);boolean player(Object entity);Iterable<?> nearbyEntities();boolean pigman(Object entity);void provokePeer(Object peer,Object responsible);
 }
 public interface SpawnHost {boolean nonPeaceful();boolean noEntityCollision();boolean noBlockCollision();boolean noLiquid();}
 public interface Valuable {int subtype();int damage();boolean gold();}
 private int anger,soundDelay;private Object previousTarget;
 public int anger(){return anger;}public int soundDelay(){return soundDelay;}
 public void beforeParent(Host host){
  if(previousTarget!=host.rawTarget()&&!host.client()){
   host.removeAttackBoost();if(host.rawTarget()!=null)host.addAttackBoost();
  }
  previousTarget=host.rawTarget();
  if(soundDelay>0&&--soundDelay==0)host.angerSound(((host.nextFloat()-host.nextFloat())*.2f+1f)*1.8f);
 }
 public void provoke(Host host,Object responsible){host.target(responsible);anger=400+host.nextInt(400);soundDelay=host.nextInt(40);}
 public Object findPlayer(Host host,float range){if(anger<1)range/=4f;Object previous=host.entityToAttack(),target=host.closestPlayer(range);if(target!=null&&target!=previous)provoke(host,target);return target;}
 /** The original leaf still alerts after a lethal negative parent result. */
 public void afterParentDamage(Host host,boolean resultExists,boolean negativelyAffected,Object responsible){
  if(!resultExists)return;
  if(negativelyAffected&&host.player(responsible)){
   for(Object peer:host.nearbyEntities())if(host.pigman(peer))host.provokePeer(peer,responsible);
   provoke(host,responsible);
  }
 }
 public short savedAnger(){return (short)anger;}
 /** tn.a changes only Anger; do not reset transient fields when loading an existing object. */
 public void loadAnger(short saved){anger=saved;}
 public static boolean canSpawn(SpawnHost world){return world.nonPeaceful()&&world.noEntityCollision()&&world.noBlockCollision()&&world.noLiquid();}
 public static boolean valuable(Valuable stack){return stack.subtype()==0&&stack.damage()==0&&stack.gold();}
}
