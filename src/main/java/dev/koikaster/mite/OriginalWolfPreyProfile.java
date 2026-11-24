package dev.koikaster.mite;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;

/** rb/rc/rd predicates shared by native Wolf and Dire; Dire hostility/taming stays in its confirmed owner. */
public final class OriginalWolfPreyProfile {
 /** sf.preysUpon is a relationship predicate, not a nearest-target selector for arbitrary animals. */
 public static boolean preysUpon(Wolf wolf,Entity target){return !wolf.isBaby()&&!wolf.isTame()&&target instanceof Animal;}
 /** sf.a(target,owner), shared by both owner-defense targets. */
 public static boolean permittedOwnerTarget(LivingEntity target,LivingEntity owner){
  if(target instanceof Creeper||target instanceof Ghast)return false;
  if(target instanceof Wolf other&&other.isTame()&&owner.getUUID().equals(other.getOwnerUUID()))return false;
  if(target instanceof Player player&&owner instanceof Player owningPlayer&&!owningPlayer.canHarmPlayer(player))return false;
  return !(target instanceof AbstractHorse horse)||!horse.isTamed();
 }
 /** rb: start requires untamed AND canEat; continuation only requires canEat. */
 public static Goal prey(Wolf wolf,OriginalWolfFood food,Class<? extends LivingEntity> type,int chance,boolean sight){return gatedPrey(wolf,food,new OriginalNearestTargetGoal(wolf,type,chance,sight));}
 /** The exact source-family nearest-target adapter is wrapped here without widening its target set. */
 public static Goal gatedPrey(Wolf wolf,OriginalWolfFood food,Goal nearest){return new Goal(){
  {setFlags(nearest.getFlags());}
  public boolean canUse(){return food.canEat()&&!wolf.isTame()&&nearest.canUse();}
  public boolean canContinueToUse(){return food.canEat()&&nearest.canContinueToUse();}
  public void start(){nearest.start();}public void stop(){nearest.stop();}public void tick(){nearest.tick();}
  public boolean requiresUpdateEveryTick(){return true;}
 };}
 /** Exact livestock entries only. Installed into the empty selectors at the real constructor entry. */
 public static void installLivestockTargets(Wolf wolf,OriginalWolfFood food){var targets=((SourceMobAi)wolf).mite$targets();targets.addGoal(4,prey(wolf,food,Chicken.class,200,false));targets.addGoal(5,prey(wolf,food,Sheep.class,400,false));targets.addGoal(6,prey(wolf,food,Pig.class,600,false));targets.addGoal(7,prey(wolf,food,Cow.class,800,false));}
 /** Exact source defense entries. Installed in constructor order; no additional native defense goals are retained. */
 public static void installOwnerDefense(Wolf wolf){var targets=((SourceMobAi)wolf).mite$targets();targets.addGoal(1,new OwnerTarget(wolf,false));targets.addGoal(2,new OwnerTarget(wolf,true));targets.addGoal(3,new OriginalHurtByGoal(wolf,true));}
 public static Goal zombieFamilyNearest(Wolf wolf){return new OriginalWolfHostilePreyGoal(wolf,OriginalWolfHostilePreyGoal.Family.ZOMBIE);}
 public static Goal ghoulFamilyNearest(Wolf wolf){return new OriginalWolfHostilePreyGoal(wolf,OriginalWolfHostilePreyGoal.Family.GHOUL);}
 /** Direct source family installation: exact qy filtering first, then unchanged rb appetite/taming gate. */
 public static void installHostilePreyFamilies(Wolf wolf,OriginalWolfFood food){installHostilePreyFamilies(wolf,food,zombieFamilyNearest(wolf),ghoulFamilyNearest(wolf));}
 /** Both source hostile-family entries have priority5,chance3200,sight=true. Exact family adaptation is required. */
 public static void installHostilePreyFamilies(Wolf wolf,OriginalWolfFood food,Goal sourceZombieFamily,Goal sourceGhoulFamily){var targets=((SourceMobAi)wolf).mite$targets();targets.addGoal(5,gatedPrey(wolf,food,sourceZombieFamily));targets.addGoal(5,gatedPrey(wolf,food,sourceGhoulFamily));}
 private static final class OwnerTarget extends OriginalTargetGoal {
  private final Wolf wolf;private final boolean ownerAttacked;private LivingEntity selected;private int timestamp;
  private OwnerTarget(Wolf wolf,boolean ownerAttacked){super(wolf,false,false);this.wolf=wolf;this.ownerAttacked=ownerAttacked;}
  private int stamp(LivingEntity owner){return ownerAttacked?owner.getLastHurtMobTimestamp():owner.getLastHurtByMobTimestamp();}
  public boolean canUse(){if(!wolf.isTame())return false;var owner=wolf.getOwner();if(owner==null)return false;selected=ownerAttacked?owner.getLastHurtMob():owner.getLastHurtByMob();return stamp(owner)!=timestamp&&suitable(selected,false)&&permittedOwnerTarget(selected,owner);}
  public void start(){wolf.setTarget(selected);var owner=wolf.getOwner();if(owner!=null)timestamp=stamp(owner);super.start();}
 }
 private OriginalWolfPreyProfile(){}
}
