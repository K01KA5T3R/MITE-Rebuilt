package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** sb constructor order. Shared source helpers retain their existing owners;
 * only this new consumer supplies the snow range/speed/cooldown and predicate. */
final class SourceSnowGolemGoals {
 static void install(SourceSnowGolem mob){var ai=(SourceMobAi)(Object)mob;var g=ai.mite$goals();var t=ai.mite$targets();if(!g.getAvailableGoals().isEmpty()||!t.getAvailableGoals().isEmpty())throw new IllegalStateException("Snow source selectors require empty native constructor");
  g.addGoal(0,new SourceZombieIdleGoals.Swim(mob));g.addGoal(1,new Ranged(mob));g.addGoal(2,new OriginalWanderGoal(mob,1));g.addGoal(3,new Watch(mob));g.addGoal(4,new SourceZombieIdleGoals.Look(mob));t.addGoal(1,new Targets(mob));g.addGoal(4,new OriginalRainShelterGoal(mob,1,true));OriginalRepairOwners.owner(mob);g.addGoal(2,new SourceSnowAvoidFire(mob));g.addGoal(1,new OriginalLeaveWaterGoal(mob,1));
 }
 static final class Ranged extends Goal implements SourceRangedAttackRules.Host {
  final SourceSnowGolem mob;LivingEntity target;final SourceRangedAttackRules.State state=new SourceRangedAttackRules.State();Ranged(SourceSnowGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){var next=mob.getTarget();if(next==null)return false;target=next;return true;}public boolean canContinueToUse(){return canUse()||!mob.getNavigation().isDone();}public void stop(){target=null;state.cooldown=-1;state.visible=0;}public boolean requiresUpdateEveryTick(){return true;}public void tick(){if(target!=null)SourceRangedAttackRules.tick(this);}
  public SourceRangedAttackRules.State state(){return state;}public double distanceSquared(){return mob.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());}public boolean piercingLine(){return SourceBlazeRaycast.blocks(mob,mob.getEyePosition(),target.getEyePosition(),null)==null||SourceBlazeRaycast.blocks(mob,mob.getEyePosition(),target.position().add(0,(double)(target.getBbHeight()*.25f),0),null)==null;}
  public float range(){return 10;}public int cooldown(){return 20;}public void stopNavigation(){mob.getNavigation().stop();}public void pursue(){mob.getNavigation().moveTo(target,1.25);}public void look(){mob.getLookControl().setLookAt(target,30,30);}public boolean eyePassable(){return OriginalDigging.passable(mob.level(),BlockPos.containing(mob.getEyePosition()),true);}public void shoot(float power){mob.performRangedAttack(target,power);}public boolean frenzied(){return false;} // og.isFrenzied; rv/sb does not inherit tm.

 }
 static final class Watch extends Goal {final SourceSnowGolem mob;Player selected;int ticks;Watch(SourceSnowGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.LOOK));}public boolean canUse(){if(mob.getRandom().nextFloat()>=.02f)return false;selected=OriginalDespawn.closest(mob,6,false);return selected!=null;}public boolean canContinueToUse(){return selected.isAlive()&&mob.distanceToSqr(selected)<=36&&ticks>0;}public void start(){ticks=40+mob.getRandom().nextInt(40);}public void stop(){selected=null;}public void tick(){mob.getLookControl().setLookAt(selected.getX(),selected.getEyeY(),selected.getZ(),10,mob.getMaxHeadXRot());ticks--;}public boolean requiresUpdateEveryTick(){return true;}}
 static final class Targets extends OriginalTargetGoal {LivingEntity selected;Targets(SourceSnowGolem mob){super(mob,true,false);}LivingEntity mount(LivingEntity target){return target!=null&&target.getVehicle() instanceof LivingEntity vehicle&&!vehicle.isInvulnerable()?vehicle:target;}LivingEntity nearest(){double r=range(mob);var all=new ArrayList<Mob>();for(var target:mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(r,6,r)))if(target instanceof Enemy&&suitable(target,false))all.add(target);all.sort(Comparator.comparingDouble(mob::distanceToSqr));return all.isEmpty()?null:mount(all.get(0));}public boolean canUse(){selected=nearest();return selected!=null;}public void start(){selected=mount(selected);mob.setTarget(selected);super.start();}public void tick(){if(mob.getRandom().nextInt(40)==0){var next=nearest();if(next!=null&&next!=selected&&sees(next,false)){stop();selected=next;start();return;}}selected=mount(selected);}}
 private SourceSnowGolemGoals(){}
}
