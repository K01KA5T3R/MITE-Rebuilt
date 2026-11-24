package dev.koikaster.mite;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** re's source targeting gates/caches for the source humanoid actors. */
public abstract class OriginalTargetGoal extends Goal {
 protected final PathfinderMob mob;private final boolean checkSight,checkPath;private int pathStatus,pathTicks,unseen;protected LivingEntity previous;
 private int cachedTick=Integer.MIN_VALUE;private final Map<Integer,Boolean> throughLeaves=new HashMap<>();
 protected OriginalTargetGoal(PathfinderMob mob,boolean sight,boolean path){this.mob=mob;checkSight=sight;checkPath=path;setFlags(EnumSet.of(Flag.TARGET));}
 public static float range(Mob mob){float value=(float)mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);if(((SourceMobAi)mob).mite$recentPlayerHitTicks()>0)value*=2;return value;}
 private boolean checksSight(){return checkSight&&!(OriginalDigging.watcher(mob)&&((OriginalDigging.Host)mob).originalDigging().enabled());}
 protected boolean sees(LivingEntity target,boolean ignoreLeaves){if(!ignoreLeaves)return mob.getSensing().hasLineOfSight(target);if(cachedTick!=mob.tickCount){cachedTick=mob.tickCount;throughLeaves.clear();}return throughLeaves.computeIfAbsent(target.getId(),id->{boolean result=OriginalVision.from(mob,target,mob.getEyePosition(),Double.MAX_VALUE,true);if(result&&target instanceof Player)OriginalDespawn.refresh(mob,-1200);return result;});}
 protected boolean suitable(LivingEntity target,boolean includeInvulnerable){if(target==null||target==mob||!target.isAlive()||!mob.canAttackType(target.getType())||target.isSpectator())return false;
  if(mob instanceof TamableAnimal owner&&owner.isTame()){if(target instanceof TamableAnimal other&&Objects.equals(owner.getOwnerUUID(),other.getOwnerUUID()))return false;if(target==owner.getOwner())return false;}else if(target instanceof Player player&&!includeInvulnerable&&player.getAbilities().invulnerable)return false;
  if(!mob.isWithinRestriction(target.blockPosition()))return false;if(checksSight()&&!sees(target,previous==target))return false;
  if(checkPath){if(--pathTicks<=0)pathStatus=0;if(pathStatus==0){pathTicks=10+mob.getRandom().nextInt(5);var path=mob.getNavigation().createPath(target,0);var end=path==null?null:path.getEndNode();int dx=end==null?0:end.x-target.getBlockX(),dz=end==null?0:end.z-target.getBlockZ();pathStatus=end!=null&&dx*dx+dz*dz<=2.25?1:2;}if(pathStatus==2)return false;}
  return true;
 }
 public boolean canContinueToUse(){var target=mob.getTarget();if(target==null||!target.isAlive())return false;double range=range(mob);if(mob.distanceToSqr(target)>range*range)return false;if(checksSight()){if(sees(target,false))unseen=0;else if(++unseen>60)return false;}return true;}
 public void start(){pathStatus=pathTicks=unseen=0;SourceLifecycle.targetStarted(mob);}
 public void stop(){previous=mob.getTarget();mob.setTarget(null);}
 public boolean requiresUpdateEveryTick(){return true;}
}
