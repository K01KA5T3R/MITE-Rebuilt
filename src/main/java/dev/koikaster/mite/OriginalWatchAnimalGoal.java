package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;

/** EntityAIWatchAnimal: target-driven source selection, pursuit handoff, pause and ten-stage progress. */
public final class OriginalWatchAnimalGoal extends Goal {
 private final OriginalDigging digging;private final PathfinderMob mob;
 public OriginalWatchAnimalGoal(OriginalDigging digging){this.digging=digging;mob=digging.mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 private boolean passable(BlockPos pos,boolean liquids){return OriginalDigging.passable(mob.level(),pos,liquids);}
 private boolean restricted(BlockHitResult hit){var block=mob.level().getBlockState(hit.getBlockPos());return block.getBlock() instanceof FenceBlock&&digging.toolStrength(block)<=0&&!(mob.getTarget() instanceof Player);}
 private boolean tryColumn(BlockHitResult hit,int foot){if(hit.getType()!=HitResult.Type.BLOCK||restricted(hit))return false;for(var pos=hit.getBlockPos().above();pos.getY()>=foot;pos=pos.below())if(digging.setBlockToDig(pos,true))return true;return false;}
 private Path path(LivingEntity target){return mob.getNavigation().createPath(target.blockPosition(),0,16);}
 public boolean canUse(){
  var state=digging.state;if(digging.preventsDigging())return false;if(!digging.enabled()&&(mob.getTarget()==null||!mob.getSensing().hasLineOfSight(mob.getTarget())))return false;var target=mob.getTarget();if(target==null||mob.blockPosition().equals(target.blockPosition()))return false;
  if(state.destroying&&digging.canDestroy(digging.position(),true))return true;if(!state.destroying&&mob.getRandom().nextInt(20)!=0)return false;float distance=mob.distanceTo(target);if(distance>16)return false;int foot=Mth.floor(mob.getBoundingBox().minY);
  if(distance*distance>2)for(var pos=target.blockPosition().below();pos.getY()>=foot;pos=pos.below())if(digging.setBlockToDig(pos,true))return true;
  if(distance>8)return false;var raisedEye=mob.getEyePosition().add(0,1,0);boolean canSee=passable(BlockPos.containing(raisedEye),false)&&OriginalRaycast.physicalLine(mob,raisedEye,target.getEyePosition());if(distance>(canSee?8:OriginalDigging.frenzied(mob)?6:4))return false;
  path(target);if(!mob.getNavigation().isDone())return false;if(OriginalReach.npcStrike(mob,target))return false;var center=digging.center(target);
  if(passable(BlockPos.containing(target.getEyePosition()).above(),false)&&tryColumn(digging.trace(digging.eye(),center.add(0,1,0)),foot))return true;
  if(tryColumn(digging.trace(digging.eye(),center),foot))return true;
  var hit=digging.trace(digging.legs(),center);if(hit.getType()!=HitResult.Type.BLOCK||restricted(hit)||!passable(hit.getBlockPos().above(),false)&&!digging.blockWillFall(hit.getBlockPos().above()))return false;return digging.setBlockToDig(hit.getBlockPos(),false);
 }
 private OriginalMeleeGoal melee(){for(var wrapped:((SourceMobAi)mob).mite$goals().getAvailableGoals())if(wrapped.getGoal() instanceof OriginalMeleeGoal goal)return goal;return null;}
 private boolean couldGetCloser(){var target=mob.getTarget();if(target==null)return false;double before=mob.distanceTo(target);var path=path(target);if(path==null||path.getEndNode()==null)return false;var end=path.getEndNode();return new Vec3(end.x+.5,end.y,end.z+.5).distanceTo(target.position())<before-2;}
 private boolean couldHitByPathing(){var target=mob.getTarget();if(target==null)return false;var path=path(target);if(path==null||path.getEndNode()==null)return false;var end=path.getEndNode();var feet=new Vec3(end.x+.5,end.y,end.z+.5);return feet.distanceTo(target.position())<=1&&digging.trace(new Vec3(end.x+.5,end.y,end.z+.5),digging.center(target)).getType()==HitResult.Type.MISS;}
 public void start(){digging.state.destroying=true;}
 public boolean canContinueToUse(){
  if(digging.preventsDigging())return false;var melee=melee();if(melee!=null){if(melee.attackCooldown>0)--melee.attackCooldown;if(melee.canStrikeTargetNow()){melee.tick();return false;}}
  var state=digging.state;if(state.pauseTicks>0)return state.pauseTicks!=1||!couldGetCloser();if(!state.destroying||!digging.canDestroy(digging.position(),true))return false;var target=mob.getTarget();if(target==null||mob.blockPosition().equals(target.blockPosition()))return false;return (mob.tickCount+mob.getId()*47)%10!=0||!couldHitByPathing();
 }
 public void tick(){var state=digging.state;if(state.pauseTicks>0){--state.pauseTicks;return;}if(state.cooloff==10)mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);if(--state.cooloff>0)return;state.cooloff=digging.cooloff();digging.progress();}
 public void stop(){digging.cancel();}public boolean requiresUpdateEveryTick(){return true;}
}
