package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;

/** Read-only access to the native shape, installed by the new portal transformer. */
public interface SourcePortalShapeAccess {
 LevelAccessor mite$portalLevel();
 BlockPos mite$portalBottom();
 Direction mite$portalRight();
 Direction.Axis mite$portalAxis();
 int mite$portalWidth();
 int mite$portalHeight();
}
