package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.common.util.FakePlayer;

/** Adds one bounded step to the native positive countdown for an ITE co-op group. */
public final class IteSpawnerPace {
    private static final int BONUS_INTERVAL = 4;

    public static int countdown(int remaining, BaseSpawner spawner, ServerLevel level, BlockPos pos, int range) {
        if (remaining <= 0 || !bonusTick(level.getServer().getTickCount(), pos) || !WorldContentMode.ite(level)
                || !(spawner.getSpawnerBlockEntity() instanceof SpawnerBlockEntity block)
                || block.getLevel() != level || !block.getBlockPos().equals(pos)) {
            return remaining;
        }

        var players = level.getServer().getPlayerList();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double squaredRange = (double) range * range;
        UUID first = null;
        for (var player : level.players()) {
            if (player instanceof FakePlayer || player.isRemoved() || player.level() != level
                    || !EntitySelector.NO_SPECTATORS.test(player) || !EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)
                    || (range >= 0 && !(player.distanceToSqr(x, y, z) < squaredRange))) {
                continue;
            }
            UUID id = player.getUUID();
            if (players.getPlayer(id) != player) continue;
            if (first == null) {
                first = id;
            } else if (!first.equals(id)) {
                return remaining - 1;
            }
        }
        return remaining;
    }

    private static boolean bonusTick(int serverTick, BlockPos pos) {
        long offset = 31L * pos.getX() + 17L * pos.getY() + 13L * pos.getZ();
        return Math.floorMod(serverTick + offset, BONUS_INTERVAL) == 0;
    }

    private IteSpawnerPace() {}
}

