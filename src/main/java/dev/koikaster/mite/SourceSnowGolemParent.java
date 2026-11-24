package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
/** Direct AbstractGolem boundaries; native SnowGolem heat/shear/loot is skipped. */
public interface SourceSnowGolemParent {
 void mite$snowParentLiving();boolean mite$snowParentHurt(DamageSource source,float amount);
 InteractionResult mite$snowParentInteract(Player player,InteractionHand hand);
 void mite$snowParentSave(CompoundTag tag);void mite$snowParentLoad(CompoundTag tag);
}
