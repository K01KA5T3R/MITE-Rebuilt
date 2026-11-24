package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Only placed-banner coordinates persist; encounter clocks and loaded chunks
 * are not serialized or held alive by this index. */
public final class BloodBannerSites extends SavedData {
 private final LinkedHashSet<Long> positions=new LinkedHashSet<>();
 public static BloodBannerSites get(ServerLevel level){return level.getDataStorage().computeIfAbsent(BloodBannerSites::load,BloodBannerSites::new,"mite_blood_banners");}
 public void include(BlockPos pos){if(positions.add(pos.asLong()))setDirty();}public void remove(BlockPos pos){if(positions.remove(pos.asLong()))setDirty();}public boolean contains(BlockPos pos){return positions.contains(pos.asLong());}public int size(){return positions.size();}
 public List<BloodBannerRules.Anchor> anchors(){return positions.stream().map(BlockPos::of).map(p->new BloodBannerRules.Anchor(p.getX(),p.getY(),p.getZ())).toList();}
 public static BloodBannerSites load(CompoundTag tag){var data=new BloodBannerSites();for(long value:tag.getLongArray("Positions"))data.positions.add(value);return data;}
 @Override public CompoundTag save(CompoundTag tag){tag.putLongArray("Positions",positions.stream().mapToLong(Long::longValue).toArray());return tag;}
}
