package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Sharing the native portal renderer must not share its Nether mob source. */
public final class SourcePortalSpawns1172 {
    public static boolean allows(Object rawState, Object rawLevel, Object rawPos) {
        var state = (BlockState) rawState;
        var level = (Level) rawLevel;
        if (!state.is(Blocks.NETHER_PORTAL) || state.getValue(SourcePortalState.RUNEGATE)) return false;
        var destination = state.getValue(SourcePortalState.DESTINATION);
        if (destination == SourcePortalState.Destination.UNSET) {
            if (!SourcePortalState.supported(level.dimension())) return false;
            var frame = SourcePortalFrames.complete(level, (BlockPos) rawPos);
            if (frame.isEmpty() || SourceRuneGate.code(frame.get()).isPresent()) return false;
            destination = SourcePortalFrames.initial(level, frame.get());
        }
        if (destination == SourcePortalState.Destination.CORE || level.dimension() == CoreProgression.WORLD) return false;
        return destination == SourcePortalState.Destination.NETHER || level.dimension() == Level.NETHER;
    }

    private SourcePortalSpawns1172() {}
}
