package dev.koikaster.mite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.BlockSnapshot;

/** World-level Wonder result and the accepted sacred sandstone transactions that contribute to it. */
final class SourceWonderTournament extends SavedData {
    private final Map<UUID, Long> placedByPlayer = new HashMap<>();
    private long placedTotal;
    private long completedTick = -1;
    private long completedPos;
    private String completedDimension = "";

    static SourceWonderTournament state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SourceWonderTournament::load,
                SourceWonderTournament::new, "mite_r195_wonder");
    }

    static boolean finished(MinecraftServer server) {
        return state(server).completedTick >= 0;
    }

    static void acceptedPlacement(ServerPlayer player, List<BlockSnapshot> snapshots) {
        MinecraftServer server = player.getServer();
        SourceTournament.Config config = SourceTournament.config(server);
        if (config == null || config.mode() != SourceTournament.Mode.WONDER) return;
        SourceWonderTournament data = state(server);
        if (data.completedTick >= 0) return;
        ServerLevel level = player.serverLevel();
        var sacred = Content.SACRED_SANDSTONE.get();
        var accounted = new HashSet<BlockPos>();
        for (BlockSnapshot snapshot : snapshots) {
            BlockPos pos = snapshot.getPos();
            if (snapshot.getReplacedBlock().is(sacred) || !level.getBlockState(pos).is(sacred) || !accounted.add(pos)) continue;
            data.placedTotal++;
            data.placedByPlayer.merge(player.getUUID(), 1L, Long::sum);
            data.setDirty();
            if (config.pyramidHeight() > level.getHeight()) continue;
            BlockPos apex = OriginalWonderPyramid.completedApex(pos, config.pyramidHeight(), new OriginalWonderPyramid.Site() {
                public boolean sacred(BlockPos point) {
                    return !level.isOutsideBuildHeight(point) && level.getBlockState(point).is(sacred);
                }
                public boolean desert(BlockPos point) {
                    return level.getBiome(point).is(Biomes.DESERT);
                }
            });
            if (apex != null) {
                data.completedTick = server.overworld().getGameTime();
                data.completedPos = apex.asLong();
                data.completedDimension = level.dimension().location().toString();
                server.overworld().getDataStorage().save();
                SourceTournament.wonderCompleted(server);
                return;
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("PlacedTotal", placedTotal);
        var players = new ListTag();
        for (var entry : placedByPlayer.entrySet()) {
            var row = new CompoundTag();
            row.putUUID("Player", entry.getKey());
            row.putLong("Placed", entry.getValue());
            players.add(row);
        }
        tag.put("Players", players);
        if (completedTick >= 0) {
            tag.putLong("CompletedTick", completedTick);
            tag.putLong("CompletedPos", completedPos);
            tag.putString("CompletedDimension", completedDimension);
        }
        return tag;
    }

    static SourceWonderTournament load(CompoundTag tag) {
        var data = new SourceWonderTournament();
        data.placedTotal = Math.max(0, tag.getLong("PlacedTotal"));
        for (Tag raw : tag.getList("Players", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            if (row.hasUUID("Player") && row.getLong("Placed") > 0) {
                data.placedByPlayer.put(row.getUUID("Player"), row.getLong("Placed"));
            }
        }
        if (tag.contains("CompletedTick", Tag.TAG_LONG)) {
            data.completedTick = tag.getLong("CompletedTick");
            data.completedPos = tag.getLong("CompletedPos");
            data.completedDimension = tag.getString("CompletedDimension");
        }
        return data;
    }
}

