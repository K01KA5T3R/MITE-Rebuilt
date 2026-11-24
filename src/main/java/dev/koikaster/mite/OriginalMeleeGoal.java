package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;

/** Staged direct qa control flow; awaiting complete species and AnimalWatcher wiring/new runtime cases. */
public final class OriginalMeleeGoal extends Goal {
 public interface Digger {boolean enabled();boolean setBlockToDig(BlockPos pos,boolean checkClipping);}
 private final PathfinderMob mob;private final Class<? extends LivingEntity> targetClass;private final double speed;private final boolean longMemory;private final Digger digger;
 public int attackCooldown,ticksSuppressed;private int repath;private Path path;
 public OriginalMeleeGoal(PathfinderMob mob,Class<? extends LivingEntity> targetClass,double speed,boolean longMemory,Digger digger){this.mob=mob;this.targetClass=targetClass;this.speed=speed;this.longMemory=longMemory;this.digger=digger;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 private boolean strike(LivingEntity target){return OriginalReach.npcStrike(mob,target);}
 private boolean cactusBlocksEdging(LivingEntity target){var center=target.position().add(0,target.getBbHeight()*.5f,0);var hit=OriginalRaycast.blocks(mob,mob.position().add(0,mob.getBbHeight()*.75f,0),center,false,true);if(hit==null)hit=OriginalRaycast.blocks(mob,mob.position().add(0,mob.getBbHeight()*.25f,0),center,false,true);return hit!=null&&hit.getType()==HitResult.Type.BLOCK&&mob.level().getBlockState(hit.getBlockPos()).is(Blocks.CACTUS)&&!mob.isInvulnerableTo(mob.damageSources().cactus());}
 public boolean canUse(){
  var target=mob.getTarget();if(target==null)return false;if(targetClass==Animal.class&&Mobs.name(target).equals("hellhound"))return false;if(!target.isAlive())return false;if(targetClass!=null&&!targetClass.isAssignableFrom(target.getClass()))return false;
  if(!mob.level().getEntitiesOfClass(FallingBlockEntity.class,mob.getBoundingBox().inflate(1,1,1).move(0,1,0)).isEmpty()){ticksSuppressed=Math.max(ticksSuppressed,10);attackCooldown=Math.max(attackCooldown,10);return false;}
  if(ticksSuppressed<1&&(mob.tickCount+mob.getId()*47)%10==0&&mob.distanceTo(target)<3&&!strike(target)&&!cactusBlocksEdging(target)){mob.getMoveControl().setWantedPosition(target.getX(),target.getY(),target.getZ(),2);mob.getLookControl().setLookAt(target,30,30);mob.getLookControl().tick();mob.setYRot(mob.getYHeadRot());}
  if(ticksSuppressed>0)--ticksSuppressed;attackCooldown=Math.max(attackCooldown-1,0);path=mob.getNavigation().createPath(target,0);if(strike(target))return true;return path!=null;
 }
 public boolean canContinueToUse(){var target=mob.getTarget();return target!=null&&target.isAlive()&&(!longMemory?!mob.getNavigation().isDone():mob.isWithinRestriction(BlockPos.containing(target.position())));}
 public void start(){mob.getNavigation().moveTo(path,speed);repath=0;}
 public void stop(){mob.getNavigation().stop();}
 public boolean canStrikeTargetNow(){if(ticksSuppressed>0||attackCooldown>0)return false;var target=mob.getTarget();return target!=null&&strike(target);}
 public void tick(){
  if(ticksSuppressed>0){--ticksSuppressed;return;}var target=mob.getTarget();if(target==null)return;mob.getLookControl().setLookAt(target,30,30);
  if((longMemory||mob.getSensing().hasLineOfSight(target))&&--repath<=0){repath=4+mob.getRandom().nextInt(7);mob.getNavigation().moveTo(target,speed);}attackCooldown=Math.max(attackCooldown-1,0);if(attackCooldown>0)return;
  if(strike(target)){attackCooldown=20;if(!mob.getMainHandItem().isEmpty())mob.swing(InteractionHand.MAIN_HAND);mob.doHurtTarget(target);}
  else if(digger!=null&&digger.enabled()){int attackerY=Mth.floor(mob.getY()+.5),targetY=Mth.floor(target.getY()+.5);if(targetY>attackerY){digger.setBlockToDig(new BlockPos(Mth.floor(target.getX()),targetY-1,Mth.floor(target.getZ())),true);attackCooldown=20;}}
 }
 public boolean requiresUpdateEveryTick(){return true;}public double movementSpeed(){return speed;}
}
