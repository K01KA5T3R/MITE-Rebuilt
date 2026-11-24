package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
/** Staged ABI. No native Magma/Slime transformer is active yet. */
public interface SourceMagmaNativeParent {
 void mite$magmaParentTick();
 void mite$magmaParentRemove(Entity.RemovalReason reason);
 SpawnGroupData mite$magmaParentSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag);
}
