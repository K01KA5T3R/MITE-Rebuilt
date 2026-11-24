package dev.koikaster.mite;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ai.goal.Goal;
/** pr: same original rs family across the five modern identities, no extra flags. */
final class SourceHorseParentGoal extends Goal {
 final AbstractHorse horse;AbstractHorse parent;int delay;
 SourceHorseParentGoal(AbstractHorse horse){this.horse=horse;}
 public boolean canUse(){if(horse.getAge()>=0)return false;double best=Double.MAX_VALUE;AbstractHorse found=null;for(var peer:horse.level().getEntitiesOfClass(AbstractHorse.class,horse.getBoundingBox().inflate(8,4,8),SourceHorseOwners::handles)){if(peer.getAge()<0)continue;double sq=horse.distanceToSqr(peer);if(sq>best)continue;best=sq;found=peer;}if(found==null||best<9)return false;parent=found;return true;}
 public boolean canContinueToUse(){if(parent==null||!parent.isAlive())return false;double sq=horse.distanceToSqr(parent);return sq>=9&&sq<=256;}
 public void start(){delay=0;}public void stop(){parent=null;}public void tick(){if(--delay>0)return;delay=10;horse.getNavigation().moveTo(parent,1);}public boolean requiresUpdateEveryTick(){return true;}
}
