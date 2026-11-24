package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.AABB;
/** qx source revenge timestamp and same-species alert, excluding allies already pursuing a target. */
public final class OriginalHurtByGoal extends OriginalTargetGoal {
 private int timestamp;private final boolean alert;
 public OriginalHurtByGoal(PathfinderMob mob,boolean alert){super(mob,false,false);this.alert=alert;}
 public boolean canUse(){return mob.getLastHurtByMobTimestamp()!=timestamp&&suitable(mob.getLastHurtByMob(),false);}
 public void start(){mob.setTarget(mob.getLastHurtByMob());timestamp=mob.getLastHurtByMobTimestamp();if(alert){double range=range(mob);var box=new AABB(mob.getX(),mob.getY(),mob.getZ(),mob.getX()+1,mob.getY()+1,mob.getZ()+1).inflate(range,10,range);for(var other:mob.level().getEntitiesOfClass(PathfinderMob.class,box))if(other!=mob&&(mob instanceof SourceZombieOwner?(other instanceof SourceZombieOwner||other instanceof SourcePigman||Mobs.name(other).equals("revenant")):other.getType()==mob.getType())&&other.getTarget()==null&&!other.isAlliedTo(mob.getLastHurtByMob()))other.setTarget(mob.getLastHurtByMob());}super.start();}
}
