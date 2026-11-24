package dev.koikaster.mite;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/** Actual source constructor priority layout; leaf factories preserve original construction order. */
public final class OriginalWolfParentGoals {
 /** Factories create leaves at the original constructor position, preserving source RNG-index consumption. */
 public record Leaves(Supplier<? extends Goal> avoidFire,Supplier<? extends Goal> swim,Supplier<? extends Goal> sit,Supplier<? extends Goal> leap,Supplier<? extends Goal> modernBreeding,Supplier<? extends Goal> beg,Supplier<? extends Goal> watch,Supplier<? extends Goal> idle,Supplier<? extends Goal> panic,Supplier<? extends Goal> hostilePlayers,Supplier<? extends Goal> zombieFamily,Supplier<? extends Goal> ghoulFamily){
  public Leaves {for(var factory:new Supplier<?>[]{avoidFire,swim,sit,leap,modernBreeding,beg,watch,idle,panic,hostilePlayers,zombieFamily,ghoulFamily})Objects.requireNonNull(factory,"Incomplete source wolf constructor factory");}
 }
 private static Goal create(Supplier<? extends Goal> factory){return Objects.requireNonNull(factory.get(),"Source wolf constructor factory returned no goal");}
 /** Called by native/custom Wolf constructor entry while its selectors are still empty. */
 public static void installIntoEmptySelectors(Wolf wolf,OriginalWolfFood food,Leaves leaves,Consumer<Wolf> callToOwner){
  var access=(SourceMobAi)wolf;var goals=access.mite$goals();var targets=access.mite$targets();
  if(!goals.getAvailableGoals().isEmpty()||!targets.getAvailableGoals().isEmpty())throw new IllegalStateException("Source wolf installation requires deliberately prepared empty selectors");
  // Frozen OriginalWanderGoal captures its .3 movement cap at construction; profile assignment must precede it.
  if(Math.abs(wolf.getAttributeValue(Attributes.MOVEMENT_SPEED)-.4f)>1e-7)throw new IllegalStateException("Source wolf movement profile must be ready before goal construction");
  // Inherited rp entries precede sf entries of the same priority.
  food.install();goals.addGoal(2,create(leaves.avoidFire()));
  wolf.setPathfindingMalus(BlockPathTypes.WATER,8); // sf navigator.avoidsWater=true, before pp construction
  goals.addGoal(1,create(leaves.swim()));goals.addGoal(2,create(leaves.sit()));goals.addGoal(3,create(leaves.leap()));
  goals.addGoal(4,new OriginalMeleeGoal(wolf,null,1,true,null));
  goals.addGoal(5,new OriginalWolfFollowOwnerGoal(wolf,1,10,2,callToOwner));
  goals.addGoal(6,create(leaves.modernBreeding()));goals.addGoal(7,new OriginalWanderGoal(wolf,1));
  goals.addGoal(8,create(leaves.beg()));goals.addGoal(9,create(leaves.watch()));goals.addGoal(9,create(leaves.idle()));
  OriginalWolfPreyProfile.installOwnerDefense(wolf);
  OriginalWolfPreyProfile.installLivestockTargets(wolf,food);
  OriginalWolfPreyProfile.installHostilePreyFamilies(wolf,food,create(leaves.zombieFamily()),create(leaves.ghoulFamily()));
  targets.addGoal(3,create(leaves.hostilePlayers()));
  goals.addGoal(1,create(leaves.panic()));goals.addGoal(4,new OriginalLeaveWaterGoal(wolf,1));
 }
 private OriginalWolfParentGoals(){}
}
