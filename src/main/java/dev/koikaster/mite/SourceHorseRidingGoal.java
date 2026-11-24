package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.event.ForgeEventFactory;

/** qq with its actual50tick draw, rh5/4 search and4000tick riding rebellion. */
final class SourceHorseRidingGoal extends Goal {
 final AbstractHorse horse;final SourceHorseBehavior.State state;BlockPos selected;
 SourceHorseRidingGoal(AbstractHorse horse,SourceHorseBehavior.State state){this.horse=horse;this.state=state;setFlags(EnumSet.of(Flag.MOVE));}
 public boolean canUse(){if(horse.isTamed()||!horse.isVehicle())return false;int x=horse.getBlockX(),y=horse.getBlockY(),z=horse.getBlockZ();double extent=horse.getRestrictRadius()+5;boolean restricted=horse.hasRestriction()&&horse.getRestrictCenter().distSqr(horse.blockPosition())+4<extent*extent;float best=-100;selected=null;for(int i=0;i<10;i++){int dx=horse.getRandom().nextInt(11)-5,dy=horse.getRandom().nextInt(9)-4,dz=horse.getRandom().nextInt(11)-5;if(restricted){dx+=x;dy+=y;dz+=z;if(!horse.isWithinRestriction(new BlockPos(dx,dy,dz)))continue;}var at=new BlockPos(dx,dy,dz);float weight=horse.getWalkTargetValue(at,horse.level());if(weight>best){best=weight;selected=at;}}return selected!=null;}
 public void start(){horse.getNavigation().moveTo(selected.getX(),selected.getY(),selected.getZ(),1.2);}
 public boolean canContinueToUse(){return !horse.getNavigation().isDone()&&horse.isVehicle();}
 public void tick(){if(horse.getRandom().nextInt(50)!=0)return;var rider=horse.getFirstPassenger();if(rider==null)return;if(rider instanceof Player player){int temper=horse.getTemper(),max=horse.getMaxTemper();if(max>0&&horse.getRandom().nextInt(max)<temper&&!ForgeEventFactory.onAnimalTame(horse,player)){horse.tameWithName(player);state.rebelliousRiding=0;return;}horse.modifyTemper(5);state.rebelliousRiding=4000;}
  if(rider instanceof LivingEntity&&horse.getRandom().nextInt(2)==0)rider.fallDistance=3; // source fallback on ejection
  rider.stopRiding();horse.makeMad();horse.level().broadcastEntityEvent(horse,(byte)6);
 }
 public boolean requiresUpdateEveryTick(){return true;}
}
