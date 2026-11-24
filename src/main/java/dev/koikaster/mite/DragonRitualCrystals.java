package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.AABB;

/** Canonical healing crystals on the existing spike tops. */
final class DragonRitualCrystals {
    static boolean isTowerCrystal(ServerLevel level, EndCrystal crystal) {
        var at = crystal.blockPosition();
        return level.getBlockState(at.below()).is(Blocks.BEDROCK)
            && SpikeFeature.getSpikesForLevel(level).stream().anyMatch(spike -> at.equals(slot(spike)));
    }

    static void restoreMissing(ServerLevel level) {
        for (var spike : SpikeFeature.getSpikesForLevel(level)) {
            BlockPos at = slot(spike);
            if (!level.hasChunkAt(at) || !level.getBlockState(at.below()).is(Blocks.BEDROCK)) continue;
            var existing = level.getEntitiesOfClass(EndCrystal.class, new AABB(at), crystal -> crystal.blockPosition().equals(at));
            if (!existing.isEmpty()) {
                for (var crystal : existing) {
                    crystal.setBeamTarget(null);
                    crystal.setInvulnerable(false);
                }
                continue;
            }
            if ((!level.isEmptyBlock(at) && !level.getBlockState(at).is(Blocks.FIRE)) || !level.isEmptyBlock(at.above())) continue;
            var crystal = EntityType.END_CRYSTAL.create(level);
            if (crystal == null) continue;
            crystal.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
            crystal.setShowBottom(true);
            level.addFreshEntity(crystal);
        }
    }

    private static BlockPos slot(SpikeFeature.EndSpike spike) {
        return new BlockPos(spike.getCenterX(), spike.getHeight() + 1, spike.getCenterZ());
    }

    private DragonRitualCrystals() {}
}
