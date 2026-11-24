package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** sd constructor's qa/qe/qc/qd/qh/qm/px/ql and qw/qx/qy. Native POI,
 * gossip and universal-anger selectors are not installed. Native pathfinding
 * carries the source's declared search requests, as in the existing ports. */
final class SourceIronGolemGoals {
 static void install(SourceIronGolem mob){
  var access=(SourceMobAi)(Object)mob;var goals=access.mite$goals();var targets=access.mite$targets();
  if(!goals.getAvailableGoals().isEmpty()||!targets.getAvailableGoals().isEmpty())throw new IllegalStateException("Golem source selectors require an empty native constructor");
  goals.addGoal(1,new OriginalMeleeGoal(mob,null,1,true,null));goals.addGoal(2,new Approach(mob));goals.addGoal(3,new Visit(mob));goals.addGoal(4,new Home(mob));goals.addGoal(5,new Flower(mob));goals.addGoal(6,new OriginalWanderGoal(mob,.6));goals.addGoal(7,new Watch(mob));goals.addGoal(8,new Idle(mob));
  targets.addGoal(1,new Defend(mob));targets.addGoal(2,new OriginalHurtByGoal(mob,false));targets.addGoal(3,new Nearest(mob));
 }
 private abstract static class EveryTick extends Goal {public boolean requiresUpdateEveryTick(){return true;}}
 private static final class Approach extends EveryTick {
  final SourceIronGolem mob;LivingEntity target;Vec3 pos;
  Approach(SourceIronGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){target=mob.getTarget();if(target==null||target.distanceToSqr(mob)>1024)return false;pos=DefaultRandomPos.getPosTowards(mob,16,7,target.position(),Math.PI/2);return pos!=null;}
  public boolean canContinueToUse(){return !mob.getNavigation().isDone()&&target.isAlive()&&target.distanceToSqr(mob)<1024;}
  public void start(){mob.getNavigation().moveTo(pos.x,pos.y,pos.z,.9);}public void stop(){target=null;}
 }
 private static final class Home extends EveryTick {
  final SourceIronGolem mob;Vec3 pos;
  Home(SourceIronGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){if(mob.isWithinRestriction())return false;var p=mob.getRestrictCenter();pos=DefaultRandomPos.getPosTowards(mob,16,7,new Vec3(p.getX(),p.getY(),p.getZ()),Math.PI/2);return pos!=null;}
  public boolean canContinueToUse(){return !mob.getNavigation().isDone();}public void start(){mob.getNavigation().moveTo(pos.x,pos.y,pos.z,1);}
 }
 private static final class Visit extends EveryTick {
  final SourceIronGolem mob;final List<SourceVillageDoors.Door> visited=new ArrayList<>();SourceVillageDoors.Door selected;Path path;
  Visit(SourceIronGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){
   if(visited.size()>15)visited.remove(0);if(mob.level().isDay()||!(mob.level() instanceof ServerLevel l))return false;var village=SourceVillageDoors.get(l).closest(mob.blockPosition(),0);if(village==null)return false;
   selected=null;int distance=Integer.MAX_VALUE;for(var d:village.doors){int next=d.distance(mob.blockPosition());if(next>=distance||visited.stream().anyMatch(old->old.pos.equals(d.pos)))continue;selected=d;distance=next;}if(selected==null)return false;
   var nav=(GroundPathNavigation)mob.getNavigation();boolean old=nav.canOpenDoors();nav.setCanOpenDoors(false);try{path=nav.createPath(selected.pos,0);}finally{nav.setCanOpenDoors(old);}if(path!=null)return true;
   var p=DefaultRandomPos.getPosTowards(mob,10,7,new Vec3(selected.pos.getX(),selected.pos.getY(),selected.pos.getZ()),Math.PI/2);if(p==null)return false;nav.setCanOpenDoors(false);try{path=nav.createPath(p.x,p.y,p.z,0);}finally{nav.setCanOpenDoors(old);}return path!=null;
  }
  public boolean canContinueToUse(){float range=mob.getBbWidth()+4;return !mob.getNavigation().isDone()&&mob.distanceToSqr(selected.pos.getX(),selected.pos.getY(),selected.pos.getZ())>(double)(range*range);}
  public void start(){mob.getNavigation().moveTo(path,.6);}public void stop(){if(mob.getNavigation().isDone()||mob.distanceToSqr(selected.pos.getX(),selected.pos.getY(),selected.pos.getZ())<16)visited.add(selected);}
 }
 private static final class Flower extends EveryTick {
  final SourceIronGolem mob;Villager target;int ticks;
  Flower(SourceIronGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){if(!mob.level().isDay()||mob.getRandom().nextInt(8000)!=0)return false;target=null;double best=Double.MAX_VALUE;for(var v:mob.level().getEntitiesOfClass(Villager.class,mob.getBoundingBox().inflate(6,2,6))){double d=v.distanceToSqr(mob);if(d<best){target=v;best=d;}}return target!=null;}
  public boolean canContinueToUse(){return ticks>0;}public void start(){ticks=400;mob.offerFlower(true);}public void stop(){mob.offerFlower(false);target=null;}public void tick(){mob.getLookControl().setLookAt(target,30,30);ticks--;}
 }
 private static final class Watch extends EveryTick {
  final SourceIronGolem mob;Player target;int ticks;
  Watch(SourceIronGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.LOOK));}
  public boolean canUse(){if(mob.getRandom().nextFloat()>=.02f)return false;target=OriginalDespawn.closest(mob,6,false);return target!=null;}
  public boolean canContinueToUse(){return target.isAlive()&&mob.distanceToSqr(target)<=36&&ticks>0;}
  public void start(){ticks=40+mob.getRandom().nextInt(40);}public void stop(){target=null;}public void tick(){mob.getLookControl().setLookAt(target.getX(),target.getEyeY(),target.getZ(),10,mob.getMaxHeadXRot());ticks--;}
 }
 private static final class Idle extends EveryTick {
  final SourceIronGolem mob;double dx,dz;int ticks;
  Idle(SourceIronGolem mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){return mob.getRandom().nextFloat()<.02f;}public boolean canContinueToUse(){return ticks>=0;}
  public void start(){double angle=Math.PI*2*mob.getRandom().nextDouble();dx=Math.cos(angle);dz=Math.sin(angle);ticks=20+mob.getRandom().nextInt(20);}
  public void tick(){ticks--;mob.getLookControl().setLookAt(mob.getX()+dx,mob.getEyeY(),mob.getZ()+dz,10,mob.getMaxHeadXRot());}
 }
 private static final class Defend extends OriginalTargetGoal {
  final SourceIronGolem guard;LivingEntity target;
  Defend(SourceIronGolem mob){super(mob,false,true);guard=mob;}
  public boolean canUse(){var village=guard.sourceVillage();if(village==null)return false;target=SourceVillageLife.nearestAggressor(village,guard);if(!suitable(target,false)){if(mob.getRandom().nextInt(20)!=0)return false;target=SourceVillageLife.nearestHostile((ServerLevel)mob.level(),village,guard);return suitable(target,false);}return true;}
  public void start(){mob.setTarget(target);super.start();}
 }
 private static final class Nearest extends OriginalTargetGoal {
  LivingEntity selected;
  Nearest(SourceIronGolem mob){super(mob,false,true);}
  LivingEntity mount(LivingEntity target){return target!=null&&target.getVehicle() instanceof LivingEntity v&&!v.isInvulnerable()?v:target;}
  LivingEntity find(){double range=range(mob);var candidates=new ArrayList<LivingEntity>();for(var actor:mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(range,6,range)))if(actor instanceof Enemy&&suitable(actor,false))candidates.add(actor);candidates.sort(Comparator.comparingDouble(mob::distanceToSqr));return candidates.isEmpty()?null:mount(candidates.get(0));}
  public boolean canUse(){selected=find();return selected!=null;}public void start(){selected=mount(selected);mob.setTarget(selected);super.start();}
  public void tick(){if(mob.getRandom().nextInt(40)==0){var next=find();if(next!=null&&next!=selected&&sees(next,false)){stop();selected=next;start();return;}}selected=mount(selected);}
 }
 private SourceIronGolemGoals(){}
}
