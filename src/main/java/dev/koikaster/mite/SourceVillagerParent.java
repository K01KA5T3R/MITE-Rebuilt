package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
/** Direct AbstractVillager boundaries bypass modern Villager brain/gossip. */
public interface SourceVillagerParent {
 void mite$villagerParentTick();void mite$villagerParentAi();
 boolean mite$villagerParentHurt(DamageSource source,float amount);
 void mite$villagerParentRevenge(LivingEntity actor);void mite$villagerParentDie(DamageSource source);
 InteractionResult mite$villagerParentInteract(Player player,InteractionHand hand);
 void mite$villagerParentEvent(byte event);
 void mite$villagerParentSave(CompoundTag tag);void mite$villagerParentLoad(CompoundTag tag);
 void mite$villagerParentThunder(ServerLevel level,LightningBolt bolt);
}
