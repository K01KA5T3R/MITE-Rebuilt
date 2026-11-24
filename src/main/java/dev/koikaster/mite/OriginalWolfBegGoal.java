package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
/** pi: main hand only; bone while wild, source ItemMeat in either tame state, full40..79ticks. */
public final class OriginalWolfBegGoal extends Goal {
 private final Wolf wolf;private Player player;private int time;
 public OriginalWolfBegGoal(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.LOOK));}
 private boolean peaceful(){return !OriginalWolfOwners.owner(wolf).attacking&&!OriginalWolfOwners.hostile(wolf);}
 private boolean item(){var stack=player.getMainHandItem();return !wolf.isTame()&&stack.is(Items.BONE)||OriginalMeatItems.includes(stack);}
 public boolean canUse(){if(!peaceful())return false;player=OriginalDespawn.closest(wolf,8,true);return player!=null&&item();}
 public boolean canContinueToUse(){return peaceful()&&player!=null&&player.isAlive()&&wolf.distanceToSqr(player)<=64&&time>0&&item();}
 public void start(){wolf.setIsInterested(true);time=40+wolf.getRandom().nextInt(40);}public void stop(){wolf.setIsInterested(false);player=null;}
 public void tick(){wolf.getLookControl().setLookAt(player.getX(),player.getEyeY(),player.getZ(),10,wolf.getMaxHeadXRot());--time;}
 public boolean requiresUpdateEveryTick(){return true;}
}
