package dev.koikaster.mite;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;

/** Keeps a live encounter ticking through its short return window. */
final class BossEncounterPresence {
    private static final TicketType<UUID> TICKET =
        TicketType.create("mite_boss_encounter_presence", Comparator.<UUID>naturalOrder(), 40);

    private final int radius;
    private ChunkPos chunk;
    private long refreshed = Long.MIN_VALUE;
    private RetreatTimer retreat;
    private RetreatTimer missingPartner;

    BossEncounterPresence(int radius) {
        this.radius = radius;
    }

    RetreatTimer retreat() {
        if (retreat == null) retreat = new RetreatTimer();
        return retreat;
    }

    RetreatTimer missingPartner() {
        if (missingPartner == null) missingPartner = new RetreatTimer();
        return missingPartner;
    }

    record RetreatKeys(String elapsed, String legacySince) {
        boolean presentIn(CompoundTag tag) {
            return tag.contains(elapsed, Tag.TAG_INT) || tag.contains(legacySince, Tag.TAG_LONG);
        }

        void clear(CompoundTag tag) {
            tag.remove(elapsed);
            tag.remove(legacySince);
        }
    }

    static final class RetreatTimer {
        private int elapsed = -1;
        private long legacySince = -1;
        private long sampledServerTick = -1;

        boolean expired(ServerLevel level, int grace) {
            long serverTick = Integer.toUnsignedLong(level.getServer().getTickCount());
            if (sampledServerTick != serverTick) {
                sampledServerTick = serverTick;
                if (legacySince >= 0) {
                    elapsed = elapsedAt(level, grace);
                    legacySince = -1;
                } else if (elapsed >= 0 && elapsed < grace) {
                    elapsed++;
                }
            }
            return elapsed >= grace;
        }

        int elapsedAt(ServerLevel level, int grace) {
            if (legacySince < 0) return elapsed;
            long now = level.getGameTime();
            return now >= legacySince ? (int)Math.min(now - legacySince, grace) : 0;
        }

        void observe(boolean attended) {
            if (attended) elapsed = -1;
            else if (elapsed < 0) elapsed = 0;
        }

        void load(CompoundTag tag, RetreatKeys keys, int grace) {
            boolean current = tag.contains(keys.elapsed(), Tag.TAG_INT);
            elapsed = current ? net.minecraft.util.Mth.clamp(tag.getInt(keys.elapsed()), -1, grace) : -1;
            legacySince = !current && tag.contains(keys.legacySince(), Tag.TAG_LONG)
                ? Math.max(-1L, tag.getLong(keys.legacySince())) : -1;
        }

        void save(CompoundTag tag, RetreatKeys keys) {
            keys.clear(tag);
            if (legacySince >= 0) tag.putLong(keys.legacySince(), legacySince);
            else if (elapsed >= 0) tag.putInt(keys.elapsed(), elapsed);
        }

        void inheritSample(RetreatTimer previous) {
            sampledServerTick = previous.sampledServerTick;
        }
    }

    void hold(ServerLevel level, LivingEntity boss) {
        var current = boss.chunkPosition();
        // DistanceManager expires this ticket even if the dimension clock stops.
        long now = Integer.toUnsignedLong(level.getServer().getTickCount());
        if (current.equals(chunk) && now >= refreshed && now - refreshed < 10) return;
        var chunks = level.getChunkSource();
        chunks.addRegionTicket(TICKET, current, radius, boss.getUUID());
        if (chunk != null && !current.equals(chunk)) {
            chunks.removeRegionTicket(TICKET, chunk, radius, boss.getUUID());
        }
        chunk = current;
        refreshed = now;
    }

    void release(ServerLevel level, UUID owner) {
        if (chunk == null) return;
        level.getChunkSource().removeRegionTicket(TICKET, chunk, radius, owner);
        chunk = null;
    }
}
