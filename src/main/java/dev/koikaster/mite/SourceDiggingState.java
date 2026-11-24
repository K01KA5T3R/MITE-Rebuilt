package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;

/** EntityAnimalWatcher fields/save branches, owned by the source digging driver.
 * Keep signed progress/cooloff untouched. Source pause ticks are deliberately not persisted.
 */
public final class SourceDiggingState {
 public boolean destroying;public int x,y,z,progress,cooloff=40,pauseTicks;
 public void save(CompoundTag tag){if(destroying){tag.putBoolean("is_destroying_block",destroying);tag.putInt("destroy_block_x",x);tag.putInt("destroy_block_y",y);tag.putInt("destroy_block_z",z);tag.putInt("destroy_block_progress",progress);tag.putInt("destroy_block_cooloff",cooloff);}}
 public void load(CompoundTag tag){if(tag.contains("is_destroying_block")){destroying=tag.getBoolean("is_destroying_block");x=tag.getInt("destroy_block_x");y=tag.getInt("destroy_block_y");z=tag.getInt("destroy_block_z");progress=tag.getInt("destroy_block_progress");cooloff=tag.getInt("destroy_block_cooloff");}}
 /** Caller broadcasts original destruction stage-1 only when this returns true. */
 public boolean cancel(){if(!destroying)return false;destroying=false;progress=-1;cooloff=40;return true;}
 public void inactiveTick(){if(!destroying){cooloff=40;progress=-1;}}
}
