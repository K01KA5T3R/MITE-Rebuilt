package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Per-dimension saved counterpart to wr's scheduled source-to-level1 water16/lava48 changes. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class TemporaryVessels extends SavedData {
 public record Change(long due,boolean lava,boolean contained){}
 private final Map<BlockPos,Change> pending=new HashMap<>();
 public static TemporaryVessels get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TemporaryVessels::load,TemporaryVessels::new,"mite_temporary_vessels");}
 public static TemporaryVessels load(CompoundTag tag){var data=new TemporaryVessels();for(var raw:tag.getList("Changes",10)){var row=(CompoundTag)raw;data.pending.put(BlockPos.of(row.getLong("Pos")),new Change(row.getLong("Due"),row.getBoolean("Lava"),row.getBoolean("Contained")));}return data;}
 public CompoundTag save(CompoundTag tag){var rows=new ListTag();pending.forEach((pos,change)->{var row=new CompoundTag();row.putLong("Pos",pos.asLong());row.putLong("Due",change.due);row.putBoolean("Lava",change.lava);row.putBoolean("Contained",change.contained);rows.add(row);});tag.put("Changes",rows);return tag;}
 public void schedule(ServerLevel level,BlockPos pos,boolean lava,boolean contained){pending.put(pos.immutable(),new Change(level.getGameTime()+(lava?48:16),lava,contained));setDirty();}
 public boolean isTemporary(BlockPos pos){return pending.containsKey(pos);}
 public Change change(BlockPos pos){return pending.get(pos);}
 public void cancel(BlockPos pos){if(pending.remove(pos)!=null)setDirty();}
 public void advance(ServerLevel level){var iterator=pending.entrySet().iterator();while(iterator.hasNext()){var entry=iterator.next();var pos=entry.getKey();var change=entry.getValue();if(change.due>level.getGameTime()||!level.hasChunkAt(pos))continue;iterator.remove();setDirty();var fluid=level.getFluidState(pos);var wanted=change.lava?Fluids.LAVA:Fluids.WATER;if(!fluid.getType().isSame(wanted)||!fluid.isSource())continue;
   if(change.contained){var state=level.getBlockState(pos);if(state.getBlock() instanceof BucketPickup pickup)pickup.pickupBlock(level,pos,state);}
   else level.setBlock(pos,wanted.getFlowing(7,false).createLegacyBlock(),3);
  }}
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level)get(level).advance(level);}
}
