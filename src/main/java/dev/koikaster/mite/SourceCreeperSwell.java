package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
/** qs: health/navigation trigger, all-player knee/eye visibility, cactus trap branch. */
public final class SourceCreeperSwell extends Goal {
 private final SourceCreeper actor;private LivingEntity target;
 SourceCreeperSwell(SourceCreeper actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE));}
 private boolean cactus(){if(actor.cactusCountdown()<=0)return false;var center=actor.blockPosition();for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))if(actor.level().getBlockState(pos).is(Blocks.CACTUS))return true;return false;}
 @Override public boolean canUse(){if(actor.getSwellDir()>0||cactus())return true;var target=actor.getTarget();if(target==null)return false;float threshold=actor.getNavigation().isDone()?16:actor.getHealth()<actor.getMaxHealth()?9:4.5f;if(actor.infernal())threshold*=2;return actor.distanceToSqr(target)<(double)threshold;}
 @Override public void start(){actor.getNavigation().stop();target=actor.getTarget();}
 @Override public void stop(){target=null;}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void tick(){
  if(cactus()){actor.setSwellDir(1);return;}if(target==null){actor.setSwellDir(-1);return;}
  float fraction=Mth.clamp(actor.getHealth()/actor.getMaxHealth(),.4f,1),limit=(actor.infernal()?36f:16f)/fraction;
  for(var player:actor.level().players())if(player.getHealth()>0&&actor.distanceToSqr(player)<=(double)limit){var knee=actor.position().add(0,(double)(actor.getBbHeight()/4f),0);if(OriginalVision.from(actor,player,knee,limit,false)||OriginalVision.from(actor,player,actor.getEyePosition(),limit,false)){actor.setSwellDir(1);return;}}
  actor.setSwellDir(-1);
 }
}
