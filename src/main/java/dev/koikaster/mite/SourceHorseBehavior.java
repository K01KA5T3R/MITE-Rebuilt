package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** New rs environment/riding owner, separate from the frozen hurt owner.
 * Source eating-rebellion is deliberately excluded by the accepted modern
 * feeding exception. Breeding/age/food nutrition are not replaced here.
 */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceHorseBehavior {
 static final class State {int rebelliousRiding;boolean installed;}
 static State state(AbstractHorse horse){var access=(SourceHorseBehaviorHost)horse;var value=(State)access.mite$horseBehavior();if(value==null){value=new State();access.mite$horseBehavior(value);}return value;}
 static boolean shy(AbstractHorse horse){return !horse.isBaby()&&!horse.isTamed()&&horse.getTemper()<=0;}
 public static boolean allowGrazing(Object raw,boolean eating){var horse=(AbstractHorse)raw;if(!eating||!SourceHorseOwners.handles(horse)||horse.level().isClientSide)return true;
  if(shy(horse)&&!horse.level().getEntitiesOfClass(Player.class,horse.getBoundingBox().inflate(16,2,16),p->true).isEmpty())return false;
  for(var wrapped:((SourceMobAi)horse).mite$goals().getAvailableGoals())if(wrapped.isRunning()&&(wrapped.getGoal() instanceof SourceHorseFleeGoal||wrapped.getGoal() instanceof SourceHorseEnvironmentGoal goal&&goal.kind==SourceHorseEnvironmentGoal.Kind.PREDATOR))return false;
  return SourceHorsePredators.query(horse,horse.getBoundingBox().inflate(16,2,16),false).isEmpty();
 }
 public static boolean allowRide(Object raw){var horse=(AbstractHorse)raw;if(!SourceHorseOwners.handles(horse)||horse.level().isClientSide)return true;var state=state(horse);if(horse.isTamed()){state.rebelliousRiding=0;return true;}if(state.rebelliousRiding>0){horse.makeMad();return false;}return true;}
 public static void afterAi(Object raw){var horse=(AbstractHorse)raw;if(!horse.level().isClientSide&&SourceHorseOwners.handles(horse)){var state=state(horse);if(horse.isTamed())state.rebelliousRiding=0;else if(state.rebelliousRiding>0)--state.rebelliousRiding;}}
 static void install(AbstractHorse horse){var state=state(horse);if(state.installed)return;state.installed=true;SourceHorseOwners.state(horse);var goals=((SourceMobAi)horse).mite$goals();Goal flee=null;
  for(var wrapped:List.copyOf(goals.getAvailableGoals())){var g=wrapped.getGoal();if(g instanceof SourceHorseFleeGoal){flee=g;goals.removeGoal(g);}else if(g instanceof RunAroundLikeCrazyGoal||g instanceof FollowParentGoal||g instanceof WaterAvoidingRandomStrollGoal||g instanceof TemptGoal||g instanceof RandomStandGoal)goals.removeGoal(g);}
  goals.addGoal(1,new SourceHorseRidingGoal(horse,state));if(flee!=null)goals.addGoal(1,flee);
  goals.addGoal(2,new SourceHorseEnvironmentGoal(horse,SourceHorseEnvironmentGoal.Kind.PREDATOR));goals.addGoal(3,new SourceHorseParentGoal(horse));
  goals.addGoal(4,new SourceHorseEnvironmentGoal(horse,SourceHorseEnvironmentGoal.Kind.RAIN));goals.addGoal(4,new SourceHorseEnvironmentGoal(horse,SourceHorseEnvironmentGoal.Kind.WATER));goals.addGoal(6,new Wander(horse));
 }
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof AbstractHorse horse&&SourceHorseOwners.handles(horse))install(horse);}
 /** Same qm/rh host specialization as rx: reuse its frozen source decision
  * verbatim rather than retain modern DefaultRandomPos. */
 static final class Wander extends Goal {
  final AbstractHorse horse;final double speed;OriginalOcelotWanderDecision.Point selected;
  Wander(AbstractHorse horse){this.horse=horse;double movement=horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);speed=.9*movement>.3?.3/movement:.9;setFlags(EnumSet.of(Flag.MOVE));}
  public boolean canUse(){selected=OriginalOcelotWanderDecision.select(new OriginalOcelotWanderDecision.Host(){
   public long lastCactusTick(){return horse.getPersistentData().getLong("MiteLastCactusHarm");}public long totalTime(){return horse.level().getGameTime();}public boolean navigationDone(){return horse.getNavigation().isDone();}public int nextInt(int bound){return horse.getRandom().nextInt(bound);}public int x(){return horse.getBlockX();}public int y(){return horse.getBlockY();}public int z(){return horse.getBlockZ();}public boolean hasHome(){return horse.hasRestriction();}public double homeDistanceSquared(){return horse.getRestrictCenter().distSqr(horse.blockPosition());}public float homeRadius(){return horse.getRestrictRadius();}public boolean withinHome(int x,int y,int z){return horse.isWithinRestriction(new BlockPos(x,y,z));}public float weight(int x,int y,int z){return horse.getWalkTargetValue(new BlockPos(x,y,z),horse.level());}
  });return selected!=null;}
  public boolean canContinueToUse(){return !horse.getNavigation().isDone();}public void start(){horse.getNavigation().moveTo(selected.x(),selected.y(),selected.z(),speed);}public boolean requiresUpdateEveryTick(){return true;}
 }
 private SourceHorseBehavior(){}
}
