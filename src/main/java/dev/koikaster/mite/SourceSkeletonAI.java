package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;

/** tr's source qn, qp and EntityAISeekFiringPosition. Existing source path,
 * visibility and random-table owners are reused without replaying their tests. */
final class SourceSkeletonAI {
 static final class RestrictSun extends Goal {
  private final SourceSkeleton mob;
  RestrictSun(SourceSkeleton mob){this.mob=mob;}
  public boolean canUse(){
   if(!mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty())return false;
   var target=mob.getTarget();boolean avoids=target==null||target.isInWater()||target.getLightLevelDependentMagicValue()<=0||mob.getMainHandItem().getItem() instanceof BowItem;
   return avoids&&SourceSpecialSpawns.sourceDay(mob.level());
  }
  public void start(){((GroundPathNavigation)mob.getNavigation()).setAvoidSun(true);}
  public void stop(){((GroundPathNavigation)mob.getNavigation()).setAvoidSun(false);}
 }
 static final class ArrowGoal extends Goal {
  private final SourceSkeleton mob;private LivingEntity target;private int cooldown=-1,visible;
  ArrowGoal(SourceSkeleton mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){var next=mob.getTarget();if(next==null)return false;target=next;return true;}
  public boolean canContinueToUse(){return canUse()||!mob.getNavigation().isDone();}
  public void start(){SourceSkeletonBowPose.hold(mob);}
  public void stop(){SourceSkeletonBowPose.stop(mob);target=null;visible=0;cooldown=-1;}
  public void tick(){
   if(target==null)return;SourceSkeletonBowPose.hold(mob);double distance=mob.distanceToSqr(target);boolean clear=projectileLine(mob,target);
   visible=clear?visible+1:0;
   if(distance<=900&&visible>=20)mob.getNavigation().stop();else mob.getNavigation().moveTo(target,1);
   mob.getLookControl().setLookAt(target,30,30);
   if(--cooldown==0){if(distance>900||!clear)return;float power=Math.max(.1f,Math.min(1,(float)Math.sqrt(distance)/15));
    if(OriginalDigging.passable(mob.level(),BlockPos.containing(mob.getEyePosition()),true)){mob.performRangedAttack(target,power);SourceSkeletonBowPose.shot(mob);}
    cooldown=mob.sourceFrenzied()?(int)(60*.67f):60;
   }else if(cooldown<0)cooldown=mob.sourceFrenzied()?(int)(60*.67f):60;
  }
  public boolean requiresUpdateEveryTick(){return true;}
 }
 /** Piercing policy: liquids/plants/open gates/bars are ignored, leaves are ignored when qn supplies a null projectile originator. */
 static boolean line(SourceSkeleton mob,Vec3 end){
  var origin=mob.getEyePosition();for(var from:new Vec3[]{origin.add(-.0001,0,0),origin.add(.0001,0,0),origin.add(0,0,-.0001),origin.add(0,0,.0001)}){
   var hit=mob.level().clip(new ClipContext(from,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,mob){
    public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos){
     var block=state.getBlock();if(block instanceof LiquidBlock||block instanceof TallGrassBlock||block instanceof FlowerBlock||block instanceof DoublePlantBlock||block instanceof CropBlock||block instanceof SugarCaneBlock||block instanceof VineBlock||block instanceof IronBarsBlock||block instanceof FenceGateBlock&&state.getValue(FenceGateBlock.OPEN)||block instanceof LeavesBlock)return net.minecraft.world.phys.shapes.Shapes.empty();
     return super.getBlockShape(state,level,pos);
    }});
   if(hit.getType()==HitResult.Type.BLOCK)return false;
  }return true;
 }
 static boolean projectileLine(SourceSkeleton mob,LivingEntity target){return line(mob,target.getEyePosition())||line(mob,target.position().add(0,target.getBbHeight()*.25,0));}
 static final class SeekFiringPosition extends OriginalItemMovementGoal {
  private final SourceSkeleton actor;private final List<Player> candidates=new ArrayList<>();private int index;
  SeekFiringPosition(SourceSkeleton mob){super(mob,1,true);actor=mob;index=sourceRandomIndex;OriginalEnvironmentPaths.initialize(mob);}
  public boolean canUse(){
   if(mob.getTarget()!=null||!OriginalRandomTables.chance_in_16[++index&32767])return false;candidates.clear();
   for(var player:mob.level().players()){if(candidates.size()>=16)break;if(mob.distanceToSqr(player)>900)continue;if(mob.getSensing().hasLineOfSight(player))return false;candidates.add(player);}
   return !candidates.isEmpty();
  }
  protected Path movementPath(){
   for(var player:candidates)for(int trial=0;trial<4;trial++){
    int dx=OriginalRandomTables.int_max[++index&32767]%33-16,dy=OriginalRandomTables.int_7_minus_3[++index&32767],dz=OriginalRandomTables.int_max[++index&32767]%33-16;
    var at=OriginalEnvironmentPaths.settle(mob,mob.blockPosition().offset(dx,dy,dz));
    if(!OriginalEnvironmentPaths.passable(mob,at,false)||OriginalEnvironmentPaths.occupied(mob,at)||!OriginalVision.from(mob,player,new Vec3(at.getX()+.5,at.getY()+mob.getEyeHeight(),at.getZ()+.5),900,false))continue;
    var path=mob.getNavigation().createPath(at,0,16);if(path==null||path.getEndNode()==null)continue;var end=path.getEndNode();var finalPos=new BlockPos(end.x,end.y,end.z);
    if(!OriginalVision.from(mob,player,new Vec3(end.x+.5,end.y+mob.getEyeHeight(),end.z+.5),900,false)||OriginalEnvironmentPaths.occupied(mob,finalPos))continue;
    return path;
   }return null;
  }
  public boolean canContinueToUse(){if(mob.getTarget()!=null)return false;if(OriginalRandomTables.chance_in_16[++index&32767])for(var player:candidates)if(mob.getSensing().hasLineOfSight(player))return false;return super.canContinueToUse();}
  public void stop(){super.stop();candidates.clear();}
 }
 private SourceSkeletonAI(){}
}
