package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
/** nn root save/load and abw.d successful world-add coupling, including native player-add API. */
public final class OriginalOrigin {
 public static BlockPos position(Entity entity){var state=(SourceSpawnOrigin)entity;return new BlockPos(state.mite$originX(),state.mite$originY(),state.mite$originZ());}
 public static void capture(Object raw){var entity=(Entity)raw;if(!entity.level().isClientSide)((SourceSpawnOrigin)entity).mite$setOrigin(entity.getBlockX(),entity.getBlockY(),entity.getBlockZ());}
 public static void spawned(boolean added, Object raw) {
  CaveSpawnBudget.finishSpawn((Entity) raw, added);
  if (added) capture(raw);
 }
 public static void save(Object raw,Object data){var state=(SourceSpawnOrigin)raw;var tag=(CompoundTag)data;tag.putInt("spawn_x",state.mite$originX());tag.putInt("spawn_y",state.mite$originY());tag.putInt("spawn_z",state.mite$originZ());}
 public static void load(Object raw,Object data){var tag=(CompoundTag)data;((SourceSpawnOrigin)raw).mite$setOrigin(tag.getInt("spawn_x"),tag.getInt("spawn_y"),tag.getInt("spawn_z"));}
 private OriginalOrigin(){}
}
