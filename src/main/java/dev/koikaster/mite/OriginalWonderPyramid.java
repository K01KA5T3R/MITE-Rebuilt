package dev.koikaster.mite;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;

/** R195 aqh checks the rim of each layer below a reachable apex. */
final class OriginalWonderPyramid {
    interface Site {
        boolean sacred(BlockPos pos);
        boolean desert(BlockPos pos);
    }

    static BlockPos completedApex(BlockPos placement, int height, Site site) {
        var pending = new ArrayDeque<BlockPos>();
        var seen = new HashSet<BlockPos>();
        pending.push(placement.immutable());
        while (!pending.isEmpty()) {
            BlockPos candidate = pending.pop();
            if (!seen.add(candidate)) continue;
            boolean topmost = true;
            for (int dx = 1; dx >= -1; dx--) {
                for (int dz = 1; dz >= -1; dz--) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos higher = candidate.offset(dx, 1, dz);
                    if (site.sacred(higher)) {
                        pending.push(higher);
                        topmost = false;
                    }
                }
            }
            if (topmost && completeRims(candidate, height, site)) return candidate;
        }
        return null;
    }

    private static boolean completeRims(BlockPos apex, int height, Site site) {
        for (int depth = 1; depth < height; depth++) {
            for (int dx = -depth; dx <= depth; dx++) {
                if (dx == -depth || dx == depth) {
                    for (int dz = -depth; dz <= depth; dz++) {
                        if (!valid(apex.offset(dx, -depth, dz), site)) return false;
                    }
                } else if (!valid(apex.offset(dx, -depth, -depth), site)
                        || !valid(apex.offset(dx, -depth, depth), site)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean valid(BlockPos pos, Site site) {
        return site.sacred(pos) && site.desert(pos);
    }

    private OriginalWonderPyramid() {}
}

