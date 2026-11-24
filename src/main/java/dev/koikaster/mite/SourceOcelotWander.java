package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Ocelot;
/** Ocelot-only source qm/rh replacement; frozen generic/Wolf wander is untouched. */
final class SourceOcelotWander extends Goal {
 final Ocelot cat;final double speed;OriginalOcelotWanderDecision.Point selected;
 SourceOcelotWander(Ocelot cat){this.cat=cat;double movement=cat.getAttributeValue(Attributes.MOVEMENT_SPEED);speed=.8*movement>.3?.3/movement:.8;setFlags(java.util.EnumSet.of(Flag.MOVE));}
 public boolean canUse(){selected=OriginalOcelotWanderDecision.select(new OriginalOcelotWanderDecision.Host(){
  public long lastCactusTick(){return cat.getPersistentData().getLong("MiteLastCactusHarm");}public long totalTime(){return cat.level().getGameTime();}public boolean navigationDone(){return cat.getNavigation().isDone();}public int nextInt(int bound){return cat.getRandom().nextInt(bound);}public int x(){return cat.getBlockX();}public int y(){return cat.getBlockY();}public int z(){return cat.getBlockZ();}public boolean hasHome(){return cat.hasRestriction();}public double homeDistanceSquared(){return cat.getRestrictCenter().distSqr(cat.blockPosition());}public float homeRadius(){return cat.getRestrictRadius();}public boolean withinHome(int x,int y,int z){return cat.isWithinRestriction(new BlockPos(x,y,z));}public float weight(int x,int y,int z){return cat.getWalkTargetValue(new BlockPos(x,y,z),cat.level());}
 });return selected!=null;}
 public boolean canContinueToUse(){return !cat.getNavigation().isDone();}public void start(){cat.getNavigation().moveTo(selected.x(),selected.y(),selected.z(),speed);}public boolean requiresUpdateEveryTick(){return true;}
}
