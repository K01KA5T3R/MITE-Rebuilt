package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.level.gameevent.GameEvent;

/** ub constructor's14 retained tasks. V060 removes qv/py/pz. Native paths and
 * random-position construction are adapters; source priorities, gates and
 * clocks replace modern Villager activities. */
final class SourceVillagerGoals {
 static void install(SourceVillager mob){
  var access=(SourceMobAi)(Object)mob;var goals=access.mite$goals();if(!goals.getAvailableGoals().isEmpty()||!access.mite$targets().getAvailableGoals().isEmpty())throw new IllegalStateException("076 requires empty native NPC selectors");
  var nav=(GroundPathNavigation)mob.getNavigation();nav.setCanOpenDoors(true);nav.setCanPassDoors(true);nav.setCanFloat(true);
  goals.addGoal(0,new Swim(mob));goals.addGoal(1,new AvoidZombie(mob));goals.addGoal(2,new Indoors(mob));goals.addGoal(3,new RestrictDoor(mob));goals.addGoal(4,new OpenPortal(mob));goals.addGoal(5,new Home(mob));goals.addGoal(7,new Flower(mob));goals.addGoal(8,new Play(mob));goals.addGoal(9,new Watch(mob,Player.class,3,1,true));goals.addGoal(9,new Watch(mob,Villager.class,5,.02f,true));goals.addGoal(9,new OriginalWanderGoal(mob,.6));goals.addGoal(10,new Watch(mob,Mob.class,8,.02f,false));goals.addGoal(4,new OriginalRainShelterGoal(mob,.6f,true));goals.addGoal(2,new Predators(mob));
 }
 private static boolean day(SourceVillager mob){return SourceSpecialSpawns.sourceDay(mob.level());}
 private static SourceVillageDoors.Village village(SourceVillager mob,int range){return mob.level() instanceof ServerLevel l?SourceVillageDoors.get(l).closest(mob.blockPosition(),range):null;}
 private abstract static class EveryTick extends Goal {public boolean requiresUpdateEveryTick(){return true;}}
 private static final class Swim extends EveryTick {
  final SourceVillager mob;Swim(SourceVillager mob){this.mob=mob;setFlags(EnumSet.of(Flag.JUMP));}
  boolean buoyant(){if(mob.horizontalCollision)return true;float slow=mob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?-(mob.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()+1)*.2f:0,fast=mob.hasEffect(MobEffects.MOVEMENT_SPEED)?(mob.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()+1)*.2f:0;var tag=mob.getPersistentData();if(tag.contains("MiteSourceWebTick")&&mob.tickCount-tag.getInt("MiteSourceWebTick")<=1)slow-=.75f;double result=slow+fast;if(result<0)result*=1-EnchantmentEffects.paralysisResistance(mob);return (float)result>=-.5f;}
  public boolean canUse(){if(!buoyant())return false;var fluid=mob.level().getFluidState(mob.blockPosition());boolean feet=fluid.is(FluidTags.WATER)&&fluid.getHeight(mob.level(),mob.blockPosition())>mob.getY()-mob.getBlockY(),full=fluid.is(FluidTags.WATER)&&fluid.isSource();if(mob.isInWater()&&!feet&&!full)return mob.isInLava();return mob.isInWater()||mob.isInLava();}
  public boolean canContinueToUse(){return buoyant()&&canUse();}
  public void tick(){if(!buoyant())return;var head=BlockPos.containing(mob.getX(),mob.getEyeY()+.5,mob.getZ());if(mob.level().getFluidState(head).is(FluidTags.WATER)){var velocity=mob.getDeltaMovement();mob.setDeltaMovement(velocity.x,(double).1f,velocity.z);}else if(mob.getRandom().nextFloat()<.8f)mob.getJumpControl().jump();}
 }
 private static final class AvoidZombie extends EveryTick {
  final SourceVillager mob;LivingEntity threat;Path path;AvoidZombie(SourceVillager mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){threat=null;for(var actor:mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(8,3,8)))if((actor instanceof Zombie||Mobs.name(actor).equals("revenant"))&&actor.isAlive()&&OriginalVision.see(mob,actor)){threat=actor;break;}if(threat==null)return false;var pos=DefaultRandomPos.getPosAway(mob,16,7,threat.position());if(pos==null||threat.distanceToSqr(pos)<threat.distanceToSqr(mob))return false;path=mob.getNavigation().createPath(pos.x,pos.y,pos.z,0);if(path==null)return false;var end=path.getEndNode();return SourceVillagerPathRules.endFits(end!=null,end==null?0:end.x,end==null?0:end.z,pos.x,pos.z);}
  public boolean canContinueToUse(){return !mob.getNavigation().isDone();}public void start(){mob.getNavigation().moveTo(path,.6);}public void stop(){threat=null;}public void tick(){mob.getNavigation().setSpeedModifier(.6);}
 }
 private static final class Indoors extends EveryTick {
  final SourceVillager mob;SourceVillageDoors.Door door;int lastX=-1,lastZ=-1;Indoors(SourceVillager mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){
   if(day(mob)&&mob.level().getRainLevel(1)<=.2f||!mob.level().dimensionType().hasSkyLight()||mob.getRandom().nextInt(50)!=0)return false;
   if(lastX!=-1&&mob.distanceToSqr(lastX,mob.getY(),lastZ)<4)return false;var village=village(mob,14);if(village==null)return false;door=null;int best=Integer.MAX_VALUE;
   for(var d:village.doors){int distance=d.distance(mob.blockPosition()),score=distance>256?distance*1000:d.usage;if(score<best){best=score;door=d;}}
   if(door==null)return false;var inside=door.inside();var path=mob.getNavigation().createPath(inside,0,16);if(path==null||path.getEndNode()==null)return false;var end=path.getEndNode();float dx=inside.getX()-end.x,dy=inside.getY()-end.y,dz=inside.getZ()-end.z;return dx*dx+dy*dy+dz*dz<2;
  }
  public boolean canContinueToUse(){return !mob.getNavigation().isDone();}
  public void start(){lastX=-1;var p=door.inside();if(mob.distanceToSqr(p.getX(),door.pos.getY(),p.getZ())>256){var pos=DefaultRandomPos.getPosTowards(mob,14,3,new Vec3(p.getX()+.5,p.getY(),p.getZ()+.5),Math.PI/2);if(pos!=null)mob.getNavigation().moveTo(pos.x,pos.y,pos.z,(double).6f);}else mob.getNavigation().moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,(double).6f);}
  public void stop(){lastX=door.inside().getX();lastZ=door.inside().getZ();door=null;}
 }
 private static final class RestrictDoor extends EveryTick {
  final SourceVillager mob;SourceVillageDoors.Door door;RestrictDoor(SourceVillager mob){this.mob=mob;}
  public boolean canUse(){if(day(mob))return false;var v=village(mob,16);if(v==null)return false;door=null;int best=Integer.MAX_VALUE;for(var d:v.doors){int next=d.distance(mob.blockPosition());if(next<best){best=next;door=d;}}return door!=null&&door.insideDistance(mob.blockPosition())<2.25;}
  public boolean canContinueToUse(){return !day(mob)&&!door.removed&&(mob.getBlockX()-door.pos.getX())*door.insideX+(mob.getBlockZ()-door.pos.getZ())*door.insideZ>=0;}
  public void start(){var nav=(GroundPathNavigation)mob.getNavigation();nav.setCanOpenDoors(false);nav.setCanPassDoors(false);}
  public void stop(){var nav=(GroundPathNavigation)mob.getNavigation();nav.setCanOpenDoors(true);nav.setCanPassDoors(true);door=null;}
  public void tick(){door.usage++;}
 }
 private static final class OpenPortal extends EveryTick {
  final SourceVillager mob;BlockPos pos;boolean passed;float dx,dz;int ticks;
  OpenPortal(SourceVillager mob){this.mob=mob;}
  boolean find(BlockPos at){var state=mob.level().getBlockState(at);if(state.is(BlockTags.WOODEN_DOORS)||state.is(BlockTags.WOODEN_TRAPDOORS)||state.getBlock() instanceof FenceGateBlock){pos=at;return true;}return false;}
  public boolean canUse(){if(!mob.horizontalCollision)return false;var nav=(GroundPathNavigation)mob.getNavigation();var path=nav.getPath();if(path==null||path.isDone()||!nav.canOpenDoors())return false;for(int i=0;i<Math.min(path.getNextNodeIndex()+2,path.getNodeCount());i++){var n=path.getNode(i);if(mob.distanceToSqr(n.x,mob.getY(),n.z)<=2.25&&(find(new BlockPos(n.x,n.y+1,n.z))||find(new BlockPos(n.x,n.y,n.z))))return true;}return find(BlockPos.containing(mob.getX(),mob.getY()+1,mob.getZ()))||find(mob.blockPosition());}
  // qi.c does not call pm.c: plane-vector defaults remain zero. Do not add
  // native OpenDoorGoal's early passed-plane stop or open fence gates.
  public boolean canContinueToUse(){return ticks>0&&!passed;}
  void open(boolean value){var state=mob.level().getBlockState(pos);if(state.getBlock() instanceof DoorBlock door&&state.is(BlockTags.WOODEN_DOORS))door.setOpen(mob,mob.level(),state,pos,value);else if(state.getBlock() instanceof TrapDoorBlock&&state.is(BlockTags.WOODEN_TRAPDOORS)&&state.getValue(TrapDoorBlock.OPEN)!=value){mob.level().setBlock(pos,state.setValue(TrapDoorBlock.OPEN,value),2);mob.level().playSound(null,pos,value?SoundEvents.WOODEN_TRAPDOOR_OPEN:SoundEvents.WOODEN_TRAPDOOR_CLOSE,SoundSource.BLOCKS,1,.9f+mob.level().random.nextFloat()*.1f);mob.level().gameEvent(mob,value?GameEvent.BLOCK_OPEN:GameEvent.BLOCK_CLOSE,pos);}}
  public void start(){ticks=20;open(true);}public void stop(){open(false);}public void tick(){ticks--;float x=(float)((double)((float)pos.getX()+.5f)-mob.getX()),z=(float)((double)((float)pos.getZ()+.5f)-mob.getZ());if(dx*x+dz*z<0)passed=true;}
 }
 private static final class Home extends EveryTick {
  final SourceVillager mob;Vec3 pos;Home(SourceVillager mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){if(mob.isWithinRestriction())return false;var at=mob.getRestrictCenter();pos=DefaultRandomPos.getPosTowards(mob,16,7,new Vec3(at.getX(),at.getY(),at.getZ()),Math.PI/2);return pos!=null;}
  public boolean canContinueToUse(){return !mob.getNavigation().isDone();}public void start(){mob.getNavigation().moveTo(pos.x,pos.y,pos.z,.6);}
 }
 private static final class Flower extends EveryTick {
  final SourceVillager mob;IronGolem golem;int draw;boolean approaching;Flower(SourceVillager mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){if(mob.getAge()>=0||!day(mob))return false;for(var actor:mob.level().getEntitiesOfClass(IronGolem.class,mob.getBoundingBox().inflate(6,2,6)))if(actor.getOfferFlowerTick()>0){golem=actor;break;}return golem!=null;}
  public boolean canContinueToUse(){return golem.getOfferFlowerTick()>0;}public void start(){draw=mob.getRandom().nextInt(320);approaching=false;golem.getNavigation().stop();}public void stop(){golem=null;mob.getNavigation().stop();}
  public void tick(){mob.getLookControl().setLookAt(golem,30,30);if(golem.getOfferFlowerTick()==draw){mob.getNavigation().moveTo(golem,.5);approaching=true;}if(approaching&&mob.distanceToSqr(golem)<4){golem.offerFlower(false);mob.getNavigation().stop();}}
 }
 private static final class Play extends EveryTick {
  final SourceVillager mob;LivingEntity other;int ticks;Play(SourceVillager mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){if(mob.getAge()>=0||mob.getRandom().nextInt(400)!=0)return false;double best=Double.MAX_VALUE;for(var actor:mob.level().getEntitiesOfClass(Villager.class,mob.getBoundingBox().inflate(6,3,6))){if(actor==mob||actor instanceof SourceVillager owner&&owner.state().playing||actor.getAge()>=0)continue;double d=actor.distanceToSqr(mob);if(d>best)continue;other=actor;best=d;}return other!=null||DefaultRandomPos.getPos(mob,16,3)!=null;}
  public boolean canContinueToUse(){return ticks>0;}public void start(){if(other!=null)mob.state().playing=true;ticks=1000;}public void stop(){mob.state().playing=false;other=null;}
  public void tick(){ticks--;if(other!=null){if(mob.distanceToSqr(other)>4)mob.getNavigation().moveTo(other,.32);}else if(mob.getNavigation().isDone()){var pos=DefaultRandomPos.getPos(mob,16,3);if(pos!=null)mob.getNavigation().moveTo(pos.x,pos.y,pos.z,.32);}}
 }
 private static final class Watch extends EveryTick {
  final SourceVillager mob;final Class<? extends LivingEntity> type;final float range,chance;LivingEntity target;int ticks;
  Watch(SourceVillager mob,Class<? extends LivingEntity> type,float range,float chance,boolean move){this.mob=mob;this.type=type;this.range=range;this.chance=chance;setFlags(move?EnumSet.of(Flag.MOVE,Flag.LOOK):EnumSet.of(Flag.LOOK));}
  public boolean canUse(){if(mob.getRandom().nextFloat()>=chance)return false;target=null;if(type==Player.class)target=OriginalDespawn.closest(mob,range,false);else{double best=Double.MAX_VALUE;for(var actor:mob.level().getEntitiesOfClass(type,mob.getBoundingBox().inflate(range,3,range))){if(actor==mob)continue;double d=mob.distanceToSqr(actor);if(d<best){best=d;target=actor;}}}return target!=null;}
  public boolean canContinueToUse(){return target.isAlive()&&mob.distanceToSqr(target)<=(double)(range*range)&&ticks>0;}public void start(){ticks=40+mob.getRandom().nextInt(40);}public void stop(){target=null;}public void tick(){mob.getLookControl().setLookAt(target.getX(),target.getEyeY(),target.getZ(),10,40);ticks--;}
 }
 static boolean predator(Mob actor,SourceVillager prey){return actor instanceof Silverfish||actor instanceof Mobs.Undead earth&&earth.earthBody()||actor instanceof SourcePredatoryBat bat&&bat.sourcePreysUpon(prey);}
 private static final class Predators extends OriginalItemMovementGoal {
  final SourceVillager npc;List<LivingEntity> predators;double nearest;int index;
  Predators(SourceVillager mob){super(mob,.6f,true);npc=mob;index=sourceRandomIndex;OriginalEnvironmentPaths.initialize(mob);}
  double nearest(int x,int y,int z){double result=Double.MAX_VALUE;for(var actor:predators){float dx=(float)x-(float)actor.getX(),dy=(float)y-(float)actor.getY(),dz=(float)z-(float)actor.getZ();double d=dx*dx+dy*dy+dz*dz;if(d<result)result=d;}return result;}
  public boolean canUse(){if(mob.getRandom().nextInt(10)>0)return false;predators=new ArrayList<>();for(var actor:mob.level().getEntities(mob,new AABB(mob.getX()-32,mob.getY()-4,mob.getZ()-32,mob.getX()+32,mob.getY()+4,mob.getZ()+32)))if(actor instanceof Mob enemy&&predator(enemy,npc))predators.add(enemy);nearest=nearest(mob.getBlockX(),mob.getBlockY(),mob.getBlockZ());return nearest<256;}
  protected Path movementPath(){int x=mob.getBlockX(),y=mob.getBlockY(),z=mob.getBlockZ();double best=nearest;Path selected=null;for(int i=0;i<16;i++){int dx=OriginalRandomTables.int_max[++index&32767]%65-32,dy=OriginalRandomTables.int_7_minus_3[++index&32767],dz=OriginalRandomTables.int_max[++index&32767]%65-32;var pos=OriginalEnvironmentPaths.settle(mob,new BlockPos(x+dx,y+dy,z+dz));double d=nearest(pos.getX(),pos.getY(),pos.getZ());if(!(d>best))continue;var path=mob.getNavigation().createPath(pos,0,32);if(path==null)continue;var end=path.getEndNode();d=nearest(end.x,end.y,end.z);if(!(d>best))continue;best=d;selected=path;}return selected;}
  protected float movementSpeed(){for(var actor:predators)if(actor instanceof Mob enemy&&enemy.getTarget()==mob)return speed*1.2f;return speed;}
  public void stop(){super.stop();predators=null;}
 }
 private SourceVillagerGoals(){}
}
