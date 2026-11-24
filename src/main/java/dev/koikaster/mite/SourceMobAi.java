package dev.koikaster.mite;
import net.minecraft.world.entity.ai.goal.GoalSelector;
/** Native goal-selector access for source AI installation and nearby digging suppression. */
public interface SourceMobAi {GoalSelector mite$goals();GoalSelector mite$targets();int mite$recentPlayerHitTicks();}
