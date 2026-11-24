package dev.koikaster.mite;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Witch-local re/qx/qy. Does not run the native TargetGoal curse hook a second
 * time. Bat sightings are shared for20 actual target ticks, as nn's countdown. */
abstract class SourceWitchTargets extends Goal {
 private static final Map<LivingEntity,Integer> BAT_SIGHT=new WeakHashMap<>();
 final SourceWitch witch;final boolean sight;LivingEntity previous;int unseen;
 SourceWitchTargets(SourceWitch witch,boolean sight){this.witch=witch;this.sight=sight;setFlags(EnumSet.of(Flag.TARGET));}
 boolean checkSight(LivingEntity target){
  if(!sight)return false;Integer until=BAT_SIGHT.get(target);if(until!=null&&until>target.tickCount)return false;
  for(var bat:witch.level().getEntitiesOfClass(Bat.class,witch.getBoundingBox().inflate(32)))if(bat.getSensing().hasLineOfSight(target)){BAT_SIGHT.put(target,target.tickCount+20);return false;}
  return true;
 }
 boolean see(LivingEntity target,boolean ignoreLeaves){boolean result=ignoreLeaves?OriginalVision.from(witch,target,witch.getEyePosition(),Double.MAX_VALUE,true):witch.getSensing().hasLineOfSight(target);if(result&&target instanceof Player)OriginalDespawn.refresh(witch,-1200);return result;}
 boolean suitable(LivingEntity target){
  if(target==null||target==witch||!target.isAlive()||!witch.canAttackType(target.getType())||target.isSpectator()||target instanceof Player player&&player.getAbilities().invulnerable)return false;
  if(!witch.isWithinRestriction(target.blockPosition()))return false;
  if(checkSight(target)&&!see(target,previous==target))return false;
  OriginalCurseEffects.consider(witch,target);return true;
 }
 public boolean canContinueToUse(){var target=witch.getTarget();if(target==null||!target.isAlive())return false;double range=OriginalTargetGoal.range(witch);if(witch.distanceToSqr(target)>range*range)return false;if(checkSight(target)){if(see(target,false))unseen=0;else if(++unseen>60)return false;}return true;}
 public void start(){unseen=0;SourceLifecycle.targetStarted(witch);}public void stop(){previous=witch.getTarget();witch.setTarget(null);}
 public boolean requiresUpdateEveryTick(){return true;}
 static LivingEntity mount(LivingEntity target){return target!=null&&target.getVehicle() instanceof LivingEntity vehicle&&!vehicle.isInvulnerable()?vehicle:target;}
 static final class Hurt extends SourceWitchTargets {
  int timestamp;
  Hurt(SourceWitch witch){super(witch,false);}
  public boolean canUse(){return witch.getLastHurtByMobTimestamp()!=timestamp&&suitable(witch.getLastHurtByMob());}
  public void start(){witch.setTarget(witch.getLastHurtByMob());timestamp=witch.getLastHurtByMobTimestamp();super.start();}
 }
 static final class Nearest extends SourceWitchTargets {
  LivingEntity selected;
  Nearest(SourceWitch witch){super(witch,true);}
  LivingEntity nearest(){double range=OriginalTargetGoal.range(witch);var list=new ArrayList<LivingEntity>();for(var player:witch.level().getEntitiesOfClass(Player.class,witch.getBoundingBox().inflate(range,6,range)))if(KingSlimeRewards.canDetect(witch,player,range,6)&&suitable(player))list.add(player);list.sort(Comparator.comparingDouble(witch::distanceToSqr));return list.isEmpty()?null:mount(list.get(0));}
  public boolean canUse(){selected=nearest();return selected!=null;}
  public void start(){selected=mount(selected);witch.setTarget(selected);super.start();}
  public void tick(){if(witch.getRandom().nextInt(40)==0){var next=nearest();if(next!=null&&next!=selected&&see(next,false)){stop();selected=next;start();return;}}selected=mount(selected);}
 }
}
