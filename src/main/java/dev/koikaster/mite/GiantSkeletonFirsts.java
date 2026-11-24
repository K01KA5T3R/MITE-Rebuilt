package dev.koikaster.mite;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

final class GiantSkeletonFirsts extends SavedData {
    static final int FIRST_BONES = 5;
    private final Set<UUID> completed=new HashSet<>();

    static int bonesFor(ServerLevel level, UUID player, boolean offline) {
        var data=level.getServer().overworld().getDataStorage().computeIfAbsent(
            GiantSkeletonFirsts::load,GiantSkeletonFirsts::new,"mite_giant_skeleton_firsts");
        if (data.completed.contains(player) || BossRewardMailbox.get(level).firstSkeletonVictoryRecorded(player)) return 2;
        // An offline first win is recorded alongside its five bones in the same world file.
        if (!offline) {
            data.completed.add(player);
            data.setDirty();
        }
        return FIRST_BONES;
    }

    private static GiantSkeletonFirsts load(CompoundTag tag) {
        var data=new GiantSkeletonFirsts();
        var rows=tag.getList("Players",Tag.TAG_COMPOUND);
        for(int i=0;i<rows.size();i++) {
            var row=rows.getCompound(i);
            if(row.hasUUID("Player"))data.completed.add(row.getUUID("Player"));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        var rows=new ListTag();
        completed.stream().sorted().forEach(player->{
            var row=new CompoundTag();
            row.putUUID("Player",player);
            rows.add(row);
        });
        tag.put("Players",rows);
        return tag;
    }
}
