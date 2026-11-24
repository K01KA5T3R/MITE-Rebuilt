package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

record RedstoneGolemCrossbars(Vec3 lower, Vec3 upper, Vec3 side) {
    static final int UPPER_DELAY = 10;
    private static final double LOWER_HEIGHT = .35;
    private static final double UPPER_HEIGHT = .73;
    private static final double HALF_WIDTH = 1.75;
    private static final double CONTACT = .24;
    private static final double LATERAL_STEP = .45;

    static RedstoneGolemCrossbars plan(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer target,
                                       List<ServerPlayer> players, Vec3 mouth) {
        Vec3 delta = target.position().subtract(golem.position());
        double distance = delta.horizontalDistance();
        if (distance < 4.5 || distance > 11 || delta.y < -.8 || delta.y > 5) return null;
        Vec3 axis = BossAttackGeometry.flatDirection(delta);
        Vec3 side = new Vec3(-axis.z, 0, axis.x);
        Vec3 primary = RedstoneGolemPatterns.floor(level, golem, target.position(), .12);
        if (primary == null) return null;
        for (var other : players) {
            if (other == target || Math.abs(other.getY() - primary.y) < 2) continue;
            Vec3 secondary = RedstoneGolemPatterns.floor(level, golem, other.position(), .12);
            if (secondary == null) continue;
            var pair = pair(level, golem, target, mouth, axis, side, primary, secondary);
            if (pair != null) return pair;
        }
        // Use a second real floor when there is no opponent currently on it.
        double direction = delta.y < 1.7 ? 1 : -1;
        for (int horizontal : new int[]{3, -3}) {
            for (int height = 2; height <= 4; height++) {
                Vec3 offset = axis.scale(horizontal).add(0, height * direction, 0);
                Vec3 secondary = RedstoneGolemPatterns.floor(level, golem, primary.add(offset), .42);
                if (secondary == null) continue;
                var pair = pair(level, golem, target, mouth, axis, side, primary, secondary);
                if (pair != null) return pair;
            }
        }
        return null;
    }

    private static RedstoneGolemCrossbars pair(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer target,
                                               Vec3 mouth, Vec3 axis, Vec3 side, Vec3 first, Vec3 second) {
        Vec3 low = first.y < second.y ? first : second;
        Vec3 high = first.y < second.y ? second : first;
        Vec3 horizontal = high.subtract(low);
        if (horizontal.y < 2 || horizontal.y > 5 || Math.abs(horizontal.dot(axis)) < 1.5
                || Math.abs(horizontal.dot(side)) > 4 || Math.abs(low.y - golem.getY()) > 1.25) return null;
        if (!legalBand(level, golem, target, mouth, low, axis, side, LOWER_HEIGHT)
                || !legalBand(level, golem, target, mouth, high, axis, side, UPPER_HEIGHT)) return null;
        return new RedstoneGolemCrossbars(low, high, side);
    }

    private static boolean legalBand(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer player,
                                     Vec3 mouth, Vec3 ground, Vec3 axis, Vec3 side, double height) {
        var delta = ground.subtract(golem.position());
        if (delta.horizontalDistanceSqr() < 3.5 * 3.5 || delta.horizontalDistanceSqr() > 13 * 13
                || delta.dot(axis) < 2 || !BossAttackGeometry.visible(level, golem, mouth, ground.add(0, height, 0))) return false;
        return escape(level, golem, player, ground, side) || escape(level, golem, player, ground, side.reverse());
    }

    private static boolean escape(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer player, Vec3 ground, Vec3 side) {
        double escapeWidth = HALF_WIDTH + CONTACT + player.getBbWidth() * .5;
        int steps = (int)Math.ceil(escapeWidth / LATERAL_STEP);
        for (int step = 0; step <= steps; step++) {
            Vec3 point = ground.add(side.scale(step * LATERAL_STEP));
            Vec3 support = RedstoneGolemPatterns.floor(level, golem, point, .19);
            if (support == null || !RedstoneGolemPatterns.clearPlayerSpace(level, player, support)) return false;
        }
        return true;
    }

    Vec3 lookAt(boolean higher) {
        return higher ? upper.add(0, UPPER_HEIGHT, 0) : lower.add(0, LOWER_HEIGHT, 0);
    }

    void strike(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> players, Vec3 mouth, boolean higher) {
        Vec3 center = lookAt(higher);
        Vec3 support = higher ? upper : lower;
        if (!RedstoneGolemPatterns.sameGround(level, golem, support)
                || !BossAttackGeometry.visible(level, golem, mouth, center)) return;
        Vec3 left = end(level, golem, mouth, support, center, side);
        Vec3 right = end(level, golem, mouth, support, center, side.reverse());
        if (left.equals(center) && right.equals(center)) return;
        BossAttackGeometry.line(level, ParticleTypes.ELECTRIC_SPARK, left, right, .14);
        BossAttackGeometry.line(level, ParticleTypes.END_ROD, left, right, .45);
        level.playSound(null, BlockPos.containing(center), SoundEvents.FIREWORK_ROCKET_BLAST,
            SoundSource.HOSTILE, .85f, higher ? 1.45f : .9f);
        for (var player : players) {
            var box = player.getBoundingBox();
            if (box.minY > center.y + .15 || box.maxY < center.y - .15) continue;
            var wave = box.inflate(CONTACT, .15, CONTACT);
            Vec3 touch = wave.contains(left) ? left : wave.clip(left, right).orElse(null);
            if (touch != null && BossAttackGeometry.visible(level, golem, touch, box.getCenter())) golem.hurtPlayer(level, player, 11);
        }
    }

    private static Vec3 end(ServerLevel level, SourceRedstoneGolem golem, Vec3 mouth,
                            Vec3 support, Vec3 center, Vec3 side) {
        Vec3 last = center;
        for (int step = 1; step <= 5; step++) {
            Vec3 horizontal = side.scale(HALF_WIDTH * step / 5);
            Vec3 point = center.add(horizontal);
            if (!level.hasChunkAt(BlockPos.containing(point)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(point))
                    || RedstoneGolemPatterns.floor(level, golem, support.add(horizontal), .16) == null
                    || !BossAttackGeometry.visible(level, golem, mouth, point)) return last;
            Vec3 clipped = BossAttackGeometry.shorten(level, golem, last, point);
            if (clipped.distanceToSqr(point) > 1e-7) return clipped;
            last = point;
        }
        return last;
    }
}
