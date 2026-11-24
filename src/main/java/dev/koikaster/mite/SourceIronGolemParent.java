package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
/** Direct AbstractGolem boundaries, bypassing modern IronGolem leaf behavior. */
public interface SourceIronGolemParent {
 void mite$golemParentLiving();void mite$golemParentAi();
 void mite$golemParentPush(Entity target);void mite$golemParentEvent(byte event);
 boolean mite$golemParentHurt(DamageSource source,float amount);
 boolean mite$golemParentTargetType(EntityType<?> type);
 InteractionResult mite$golemParentInteract(Player player,InteractionHand hand);
 void mite$golemParentSave(CompoundTag tag);void mite$golemParentLoad(CompoundTag tag);
}
