package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** tv's new-goal AI, not the legacy on.bl controller used by some other mobs.
 * Modern Raider parent village/celebration goals remain as the approved adapter.
 */
final class SourceWitchGoals {
 static void install(SourceWitch witch){
  var access=(SourceMobAi)(Object)witch;var goals=access.mite$goals();var targets=access.mite$targets();
  goals.addGoal(1,OriginalHellhoundProfile.swimGoal(witch));
  goals.addGoal(2,new Ranged(witch));goals.addGoal(2,new OriginalWanderGoal(witch,1));
  goals.addGoal(3,OriginalHellhoundProfile.watchGoal(witch));goals.addGoal(3,OriginalHellhoundProfile.idleGoal(witch));
  targets.addGoal(1,new SourceWitchTargets.Hurt(witch));targets.addGoal(2,new SourceWitchTargets.Nearest(witch));
  goals.addGoal(3,new Home(witch));goals.addGoal(3,new Roam(witch));
 }
 static boolean line(SourceWitch witch,Vec3 end){
  var eye=witch.getEyePosition();for(var from:new Vec3[]{eye.add(-.0001,0,0),eye.add(.0001,0,0),eye.add(0,0,-.0001),eye.add(0,0,.0001)}){
   var hit=witch.level().clip(new ClipContext(from,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,witch){
    @Override public VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos){var block=state.getBlock();
     if(block instanceof LiquidBlock||block instanceof TallGrassBlock||block instanceof FlowerBlock||block instanceof CropBlock||block instanceof WebBlock||block instanceof SugarCaneBlock||block instanceof VineBlock||block instanceof IronBarsBlock||block instanceof FenceGateBlock&&state.getValue(FenceGateBlock.OPEN)||block instanceof LeavesBlock)return Shapes.empty();
     return super.getBlockShape(state,level,pos);
    }
   });if(hit.getType()==HitResult.Type.BLOCK)return false;
  }return true;
 }
 static final class Ranged extends Goal {
  final SourceWitch witch;LivingEntity target;int attackTime=-1,visible;
  Ranged(SourceWitch witch){this.witch=witch;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){var next=witch.getTarget();if(next==null)return false;target=next;return true;}
  public boolean canContinueToUse(){return canUse()||!witch.getNavigation().isDone();}
  public void stop(){target=null;visible=0;attackTime=-1;}
  int interval(){return SourceSpecialSpawns.bloodUp(witch.level())?(int)(60*.67f):60;}
  public void tick(){
   double distance=witch.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());
   boolean sight=line(witch,target.getEyePosition())||line(witch,target.position().add(0,target.getBbHeight()*.25f,0));visible=sight?visible+1:0;
   if(distance<=400&&visible>=20)witch.getNavigation().stop();else witch.getNavigation().moveTo(target,1);
   witch.getLookControl().setLookAt(target,30,30);
   if(--attackTime==0){if(distance>400||!sight)return;float factor=(float)Math.sqrt(distance)/10;factor=Math.max(.1f,Math.min(1,factor));
    if(SourceTroopPlacement.passable(witch.level(),BlockPos.containing(witch.getX(),witch.getEyeY(),witch.getZ()),true))witch.performRangedAttack(target,factor);
    attackTime=interval();
   }else if(attackTime<0)attackTime=interval();
  }
  public boolean requiresUpdateEveryTick(){return true;}
 }
 static final class Home extends OriginalItemMovementGoal {
  final SourceWitch witch;
  Home(SourceWitch witch){super(witch,1,true);this.witch=witch;}
  double distance(){var home=witch.home();return witch.distanceToSqr(home.getX(),home.getY(),home.getZ());}
  public boolean canUse(){return witch.getRandom().nextInt(40)<=0&&!(distance()<64);}
  protected Path movementPath(){return OriginalEnvironmentPaths.toward(witch,witch.home(),16);}
  public boolean canContinueToUse(){return !(distance()<64)&&super.canContinueToUse();}
 }
 static final class Roam extends Goal {
  final SourceWitch witch;
  Roam(SourceWitch witch){this.witch=witch;setFlags(EnumSet.of(Flag.MOVE));}
  boolean playerAtHome(){var home=witch.home();return witch.level().players().stream().anyMatch(p->!p.isSpectator()&&p.isAlive()&&p.distanceToSqr(home.getX(),home.getY(),home.getZ())<256);}
  public boolean canUse(){return !SourceSpecialSpawns.sourceDay(witch.level())&&!playerAtHome()&&witch.getRandom().nextInt(100)<=0;}
  public boolean canContinueToUse(){var home=witch.home();if(SourceSpecialSpawns.sourceDay(witch.level())||witch.distanceToSqr(home.getX(),home.getY(),home.getZ())>4096||playerAtHome()||witch.getRandom().nextInt(4000)==0)return false;return !witch.getNavigation().isDone();}
  public void start(){witch.getNavigation().moveTo(OriginalParentAwayPath.find(witch,witch.home(),16,48,true),1);}
  public void stop(){witch.getNavigation().stop();}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 private SourceWitchGoals(){}
}
