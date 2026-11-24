package dev.koikaster.mite;

/** R195 tm.attackEntityAsMob and its aaw/abh callees. Native adapters supply
 * damage results; acceptance alone is not the seven-flag negative result. */
public final class SourceMobStrike {
 public record Result(boolean negativelyAffected,float healthLost){}
 public interface Host {
  boolean decoy();boolean immuneByGrace();float attack();boolean frenzied();float baseAttack();
  boolean livingTarget();float enchantmentDamage();int knockback();int fireAspect();
  Result hurt(float damage,boolean fireAspect);void push(int knockback);void ignite(int seconds);
  boolean burning();boolean hasHeldItem();float nextFloat();int difficulty();
  void thorns();int stun();double globalRandom();void stun(int ticks,int amplifier);
  boolean biologicalTarget();int vampiric();void heal(int amount);boolean playerTarget();void refreshDespawn(int ticks);
 }
 public interface ThornsHost {
  int level();boolean hasArmor();float nextFloat();int nextInt(int bound);void retaliate(int damage);void wear(int amount);
 }
 public static Result attack(Host h){
  if(h.decoy()||h.immuneByGrace())return null;
  float damage=h.attack();if(h.frenzied())damage+=h.baseAttack()*.5f;
  int knockback=0;if(h.livingTarget()){damage+=h.enchantmentDamage();knockback+=h.knockback();}
  int fire=h.fireAspect();var result=h.hurt(damage,fire>0);if(result==null)return null;
  if(result.negativelyAffected()){
   if(knockback>0)h.push(knockback);
   if(fire>0)h.ignite(fire*4);
   if(h.burning()&&!h.hasHeldItem()&&h.nextFloat()<h.difficulty()*.3f)h.ignite(2*h.difficulty());
   if(h.livingTarget()){
    h.thorns();int stun=h.stun();if(stun>h.globalRandom()*10)h.stun(stun*50,stun*5);
    h.heal(vampiric(result.healthLost(),h));
   }
   if(h.playerTarget())h.refreshDespawn(-9600);
  }
  return result;
 }
 private static int vampiric(float damage,Host h){
  if(damage<=0||!h.biologicalTarget())return 0;
  int potential=h.vampiric();if(h.globalRandom()*10>potential)return 0;
  int amount=(int)((double)(damage*.5f)*h.globalRandom());return amount<1?1:amount;
 }
 public static void thorns(ThornsHost h){
  int level=h.level();boolean proc=level>0&&h.nextFloat()<.15f*level;
  if(proc){h.retaliate(level>10?level-10:1+h.nextInt(4));if(h.hasArmor())h.wear(3);}
  else if(h.hasArmor())h.wear(1);
 }
 private SourceMobStrike(){}
}
