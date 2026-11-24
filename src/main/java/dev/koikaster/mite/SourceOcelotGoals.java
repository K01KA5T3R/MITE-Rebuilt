package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Active wild-rx pg/qf/px/pw leaves and rb prey adapter. The tamed-cat form is
 * modern Cat under questionnaire V073, rather than an invented owner on Ocelot. */
final class SourceOcelotGoals {
 static final class Prey extends Goal {
  final Ocelot cat;final SourceOcelotFood food;final OriginalNearestTargetGoal source;
  Prey(Ocelot cat,SourceOcelotFood food,Class<? extends LivingEntity> kind){this.cat=cat;this.food=food;source=new OriginalNearestTargetGoal(cat,kind,750,false);setFlags(source.getFlags());}
  public boolean canUse(){return food.canEat()&&!SourceOcelotOwners.trusting(cat)&&source.canUse();}
  public boolean canContinueToUse(){return food.canEat()&&source.canContinueToUse();}
  public void start(){source.start();}public void stop(){source.stop();}public void tick(){source.tick();}public boolean requiresUpdateEveryTick(){return true;}
 }
 static final class Attack extends Goal {
  final Ocelot cat;LivingEntity target;int cooldown;
  Attack(Ocelot cat){this.cat=cat;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){var current=cat.getTarget();if(current==null)return false;target=current;return true;}
  public boolean canContinueToUse(){return target.isAlive()&&cat.distanceToSqr(target)<=225&&(!cat.getNavigation().isDone()||canUse());}
  public void stop(){target=null;cat.getNavigation().stop();}
  public void tick(){cat.getLookControl().setLookAt(target,30,30);double reach=cat.getBbWidth()*2f*cat.getBbWidth()*2f,distance=cat.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ()),speed=.8;if(distance>reach&&distance<16)speed=1.33;else if(distance<225)speed=.6;cat.getNavigation().moveTo(target,speed);cooldown=Math.max(cooldown-1,0);if(distance<=reach&&cooldown<=0){cooldown=20;cat.doHurtTarget(target);}}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 static final class AvoidPlayer extends Goal {
  final Ocelot cat;Player danger;Path path;
  AvoidPlayer(Ocelot cat){this.cat=cat;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){
   if(SourceOcelotOwners.trusting(cat))return false;danger=OriginalDespawn.closest(cat,16,true);if(danger==null)return false;
   var selected=randomAway(cat,danger.position(),16,7);if(selected==null||danger.distanceToSqr(selected)<danger.distanceToSqr(cat))return false;
   path=SourceArachnidPathBudget.block(cat,BlockPos.containing(selected),16);var end=path==null?null:path.getEndNode();return end!=null&&end.x==(int)selected.x&&end.z==(int)selected.z;
  }
  public boolean canContinueToUse(){return !cat.getNavigation().isDone();}
  public void start(){((SourceArachnidPathBudget)cat.getNavigation()).mite$sourceCommitPath(path);cat.getNavigation().moveTo(path,.8);}
  public void stop(){danger=null;}
  public void tick(){cat.getNavigation().setSpeedModifier(cat.distanceToSqr(danger)<49?1.33:.8);}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 /** Literal rh.c search rules, including conditional origin translation. */
 static Vec3 randomAway(Ocelot cat,Vec3 danger,int horizontal,int vertical){
  int x=cat.getBlockX(),y=cat.getBlockY(),z=cat.getBlockZ();double dx=cat.getX()-danger.x,dz=cat.getZ()-danger.z;var home=cat.getRestrictCenter();double extent=cat.getRestrictRadius()+horizontal;boolean restricted=cat.hasRestriction()&&home.distSqr(cat.blockPosition())+4<extent*extent;float best=-100;BlockPos selected=null;
  for(int attempt=0;attempt<10;attempt++){int tx=cat.getRandom().nextInt(horizontal*2+1)-horizontal,ty=cat.getRandom().nextInt(vertical*2+1)-vertical,tz=cat.getRandom().nextInt(horizontal*2+1)-horizontal;if(!(tx*dx+tz*dz>=0))continue;if(restricted){tx+=x;ty+=y;tz+=z;if(!cat.isWithinRestriction(new BlockPos(tx,ty,tz)))continue;}var at=new BlockPos(tx,ty,tz);float weight=cat.getWalkTargetValue(at,cat.level());if(weight>best){best=weight;selected=at;}}
  return selected==null?null:new Vec3(selected.getX(),selected.getY(),selected.getZ());
 }
 static final class Leap extends Goal {
  final Ocelot cat;LivingEntity target;
  Leap(Ocelot cat){this.cat=cat;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
  public boolean canUse(){target=cat.getTarget();if(target==null)return false;double d=cat.distanceToSqr(target);return d>=4&&d<=16&&cat.onGround()&&cat.getRandom().nextInt(5)==0;}
  public boolean canContinueToUse(){return !cat.onGround();}
  public void start(){double dx=target.getX()-cat.getX(),dz=target.getZ()-cat.getZ();float length=(float)Math.sqrt(dx*dx+dz*dz);var motion=cat.getDeltaMovement();cat.setDeltaMovement(motion.x+dx/length*.5*(double).8f+motion.x*(double).2f,.3f,motion.z+dz/length*.5*(double).8f+motion.z*(double).2f);double yaw=Math.atan2(dz,dx)*180/Math.PI-90;cat.setYRot((float)(yaw<0?yaw+360:yaw));}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 static final class Watch extends Goal {
  final Ocelot cat;Player player;int time;
  Watch(Ocelot cat){this.cat=cat;setFlags(EnumSet.of(Flag.LOOK));}
  public boolean canUse(){if(cat.getRandom().nextFloat()>=.02f)return false;player=OriginalDespawn.closest(cat,10,false);return player!=null;}
  public boolean canContinueToUse(){return player!=null&&player.isAlive()&&cat.distanceToSqr(player)<=100&&time>0;}
  public void start(){time=40+cat.getRandom().nextInt(40);}public void stop(){player=null;}
  public void tick(){cat.getLookControl().setLookAt(player.getX(),player.getEyeY(),player.getZ(),10,cat.getMaxHeadXRot());--time;}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 private SourceOcelotGoals(){}
}
