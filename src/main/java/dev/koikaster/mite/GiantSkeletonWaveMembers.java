package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;

/** Tracks the undefeated slots of one wave. */
final class GiantSkeletonWaveMembers {
    private static final String ARMY = "MiteGiantSkeletonArmy";
    private static final String RESERVES = "MiteGiantSkeletonReserves";
    private static final int MISSING_GRACE = 60;
    private static final int LOCAL_RADIUS = 3;

    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final ArrayList<GiantSkeletonWavePlan.Recruit> reserves = new ArrayList<>();

    static final class Member {
        private Long lastChunk;
        private EntityType<? extends Mob> type;
        private GiantSkeletonWavePlan.Role role = GiantSkeletonWavePlan.Role.FILL;
        private long missingSince = -1;

        EntityType<? extends Mob> type() { return type; }
        GiantSkeletonWavePlan.Role role() { return role; }

        void locate(ChunkPos chunk) {
            lastChunk = chunk.toLong();
        }

        void observe(Mob mob) {
            locate(mob.chunkPosition());
            if (type == null) type = GiantSkeletonWavePlan.savedType(EntityType.getKey(mob.getType()));
            if (mob.getPersistentData().contains(GiantSkeletonWavePlan.ROLE_TAG, Tag.TAG_STRING)) {
                role = GiantSkeletonWavePlan.Role.read(mob.getPersistentData().getString(GiantSkeletonWavePlan.ROLE_TAG));
            }
            missingSince = -1;
        }

        boolean missing(ServerLevel level, ChunkPos center, long now) {
            if (!regionReady(level, center)) {
                missingSince = -1;
                return false;
            }
            if (missingSince < 0 || missingSince > now) missingSince = now;
            return now - missingSince >= MISSING_GRACE;
        }

        private boolean regionReady(ServerLevel level, ChunkPos center) {
            if (lastChunk != null) {
                var known = new ChunkPos(lastChunk);
                if (Math.abs(known.x - center.x) <= LOCAL_RADIUS
                        && Math.abs(known.z - center.z) <= LOCAL_RADIUS) {
                    return level.areEntitiesLoaded(lastChunk);
                }
            }
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (!level.areEntitiesLoaded(ChunkPos.asLong(center.x + x, center.z + z))) return false;
                }
            }
            return true;
        }
    }

    boolean contains(UUID id) { return members.containsKey(id); }
    boolean isEmpty() { return members.isEmpty() && reserves.isEmpty(); }
    List<GiantSkeletonWavePlan.Recruit> reserves() { return reserves; }
    Map<UUID, Member> entries() { return members; }
    boolean defeated(UUID id) { return members.remove(id) != null; }

    void add(Mob mob) {
        var member = new Member();
        member.observe(mob);
        members.put(mob.getUUID(), member);
    }

    void replace(UUID previous, Mob replacement) {
        members.remove(previous);
        add(replacement);
    }

    void locate(UUID id, ChunkPos chunk) {
        var member = members.get(id);
        if (member != null) member.locate(chunk);
    }

    void save(CompoundTag tag) {
        var rows = new ListTag();
        members.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var row = new CompoundTag();
            row.putUUID("Id", entry.getKey());
            var member = entry.getValue();
            if (member.lastChunk != null) row.putLong("Chunk", member.lastChunk);
            if (member.type != null) row.putString("Type", EntityType.getKey(member.type).toString());
            if (member.role != GiantSkeletonWavePlan.Role.FILL) row.putString("Role", member.role.name());
            rows.add(row);
        });
        tag.put(ARMY, rows);
        if (!reserves.isEmpty()) {
            var standby = new ListTag();
            for (var recruit : reserves) {
                var row = new CompoundTag();
                row.putString("Type", EntityType.getKey(recruit.type()).toString());
                row.putString("Role", recruit.role().name());
                standby.add(row);
            }
            tag.put(RESERVES, standby);
        }
    }

    void load(CompoundTag tag) {
        members.clear();
        reserves.clear();
        for (var value : tag.getList(RESERVES, Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            var type = GiantSkeletonWavePlan.savedType(ResourceLocation.tryParse(row.getString("Type")));
            if (type != null) reserves.add(new GiantSkeletonWavePlan.Recruit(type,
                GiantSkeletonWavePlan.Role.read(row.getString("Role"))));
        }
        for (var value : tag.getList(ARMY, Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            if (!row.hasUUID("Id")) continue;
            var member = new Member();
            if (row.contains("Chunk", Tag.TAG_LONG)) member.lastChunk = row.getLong("Chunk");
            if (row.contains("Type", Tag.TAG_STRING)) {
                member.type = GiantSkeletonWavePlan.savedType(ResourceLocation.tryParse(row.getString("Type")));
            }
            member.role = GiantSkeletonWavePlan.Role.read(row.getString("Role"));
            members.putIfAbsent(row.getUUID("Id"), member);
        }
    }
}
