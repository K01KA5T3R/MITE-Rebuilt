package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class RedstoneGolemPatterns {
    private static final double CURRENT_WIDTH = .46;
    private static final double CURRENT_STEP = .45;
    private static final double LANE_OFFSET = 1.35;
    private static final double NODE_RADIUS = .62;

    private RedstoneGolemPatterns() {}

    static final class GroundCurrent {
        final Vec3 axis;
        private Vec3 left;
        private Vec3 right;

        private GroundCurrent(Vec3 axis, Vec3 left, Vec3 right) {
            this.axis = axis;
            this.left = left;
            this.right = right;
        }

        boolean advance(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> fighters) {
            left = advanceLane(level, golem, fighters, left);
            right = advanceLane(level, golem, fighters, right);
            return left != null || right != null;
        }

        private Vec3 advanceLane(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> fighters, Vec3 from) {
            if (from == null || !sameGround(level, golem, from)) return null;
            Vec3 to = step(level, golem, from, axis, CURRENT_STEP);
            if (to == null) return null;
            Vec3 start = from.add(0, .16, 0);
            Vec3 end = to.add(0, .16, 0);
            BossAttackGeometry.line(level, ParticleTypes.ELECTRIC_SPARK, start, end, .20);
            BossAttackGeometry.point(level, ParticleTypes.END_ROD, end);
            for (var player : fighters) {
                var box = player.getBoundingBox();
                if (box.minY <= Math.max(from.y, to.y) + .42 && box.maxY >= Math.min(from.y, to.y)
                        && BossAttackGeometry.hits(box, start, end, CURRENT_WIDTH)
                        && BossAttackGeometry.visible(level, golem, end, player.position().add(0, .28, 0))) {
                    golem.hurtPlayer(level, player, 11);
                }
            }
            return to;
        }
    }

    static GroundCurrent groundCurrent(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer victim) {
        Vec3 delta = victim.position().subtract(golem.position());
        double distance = delta.horizontalDistance();
        if (distance < 4.5 || distance > 9.5 || Math.abs(delta.y) > .7) return null;
        Vec3 forward = BossAttackGeometry.spread(BossAttackGeometry.flatDirection(delta), Math.asin(LANE_OFFSET / distance));
        Vec3 start = golem.position().add(forward.scale(.9));
        Vec3 side = across(forward).scale(LANE_OFFSET);
        Vec3 left = floor(level, golem, start.add(side), .30);
        Vec3 right = floor(level, golem, start.subtract(side), .30);
        if (left == null || right == null || step(level, golem, left, forward, CURRENT_STEP) == null
                || step(level, golem, right, forward, CURRENT_STEP) == null) return null;
        if (!DragonCombat.escape(level, victim, across(forward), LANE_OFFSET, 1.5)) return null;
        Vec3 center = floor(level, golem, start, .30);
        for (int i = 0; i < Math.ceil((distance - .9) / CURRENT_STEP); i++) {
            if (center == null || !clearPlayerSpace(level, victim, center)) return null;
            center = step(level, golem, center, forward, CURRENT_STEP);
        }
        return center != null && clearPlayerSpace(level, victim, center) ? new GroundCurrent(forward, left, right) : null;
    }

    static boolean canLiftBeam(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer victim) {
        Vec3 delta = victim.position().subtract(golem.position());
        double distance = delta.horizontalDistance();
        if (distance < 4 || distance > 15 || delta.y < 1.7 || delta.y > 7
                || !sameGround(level, golem, victim.position())
                || !BossAttackGeometry.visible(level, golem, golem.getEyePosition().add(0, -.35, 0),
                    victim.getBoundingBox().getCenter())) return false;
        Vec3 side = across(BossAttackGeometry.flatDirection(delta));
        return DragonCombat.escape(level, victim, side, 1.65, 1.7)
            || DragonCombat.escape(level, victim, side.reverse(), 1.65, 1.7);
    }

    static Vec3 risingBeam(Vec3 locked, double progress) {
        Vec3 forward = BossAttackGeometry.flatDirection(locked);
        double pitch = Math.atan2(locked.y, locked.horizontalDistance());
        double radians = pitch - .26 + .50 * progress;
        return forward.scale(Math.cos(radians)).add(0, Math.sin(radians), 0);
    }

    static List<Vec3> dischargeNodes(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer victim, Vec3 mouth) {
        Vec3 feet = victim.position();
        Vec3 delta = feet.subtract(golem.position());
        double distance = delta.horizontalDistance();
        if (distance < 4 || distance > 12 || Math.abs(delta.y) > 1.35) return List.of();
        Vec3 forward = BossAttackGeometry.flatDirection(delta);
        Vec3 across = across(forward);
        Vec3 first = floor(level, golem, feet, .18);
        if (first == null) return List.of();
        for (Vec3 flank : List.of(across, across.reverse())) {
            if (!DragonCombat.escape(level, victim, flank.reverse(), 1.6, 1.7)) continue;
            Vec3 second = floor(level, golem, feet.add(forward.scale(2.25)).add(flank.scale(.75)), .42);
            Vec3 third = floor(level, golem, feet.add(forward.scale(-1.55)).add(flank.scale(1.9)), .42);
            if (second != null && third != null && List.of(first, second, third).stream()
                    .allMatch(node -> BossAttackGeometry.visible(level, golem, mouth, node.add(0, .18, 0)))) {
                return List.of(first, second, third);
            }
        }
        return List.of();
    }

    static void discharge(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> fighters,
                          Vec3 mouth, Vec3 ground) {
        Vec3 base = ground.add(0, .11, 0);
        if (!sameGround(level, golem, ground) || !BossAttackGeometry.visible(level, golem, mouth, base)) return;
        Vec3 tip = BossAttackGeometry.shorten(level, golem, base, base.add(0, 1.2, 0));
        BossAttackGeometry.ring(level, ParticleTypes.ELECTRIC_SPARK, base, NODE_RADIUS, 16);
        BossAttackGeometry.line(level, ParticleTypes.END_ROD, base, tip, .27);
        for (var player : fighters) {
            var box = player.getBoundingBox();
            double x = Math.max(box.minX, Math.min(base.x, box.maxX));
            double z = Math.max(box.minZ, Math.min(base.z, box.maxZ));
            if (box.minY <= tip.y && box.maxY >= ground.y && base.distanceToSqr(x, base.y, z) <= NODE_RADIUS * NODE_RADIUS
                    && BossAttackGeometry.visible(level, golem, base, box.getCenter())) golem.hurtPlayer(level, player, 11);
        }
    }

    private static Vec3 across(Vec3 forward) {
        return new Vec3(-forward.z, 0, forward.x);
    }

    static Vec3 floor(ServerLevel level, SourceRedstoneGolem golem, Vec3 around, double tolerance) {
        var at = BossAttackGeometry.ground(level, golem, around.add(0, tolerance + .015, 0), 2 * tolerance + .03);
        if (at == null || Math.abs(at.y - around.y) > tolerance
                || !level.getFluidState(BlockPos.containing(at.add(0, .05, 0))).isEmpty()
                || !level.getFluidState(BlockPos.containing(at.add(0, -.02, 0))).isEmpty()) return null;
        return at;
    }

    static boolean sameGround(ServerLevel level, SourceRedstoneGolem golem, Vec3 feet) {
        return floor(level, golem, feet, .07) != null;
    }

    private static Vec3 step(ServerLevel level, SourceRedstoneGolem golem, Vec3 from, Vec3 axis, double distance) {
        Vec3 next = floor(level, golem, from.add(axis.scale(distance)), .24);
        if (next == null) return null;
        double clearance = Math.max(from.y, next.y) + .20;
        return BossAttackGeometry.visible(level, golem, new Vec3(from.x, clearance, from.z),
            new Vec3(next.x, clearance, next.z)) ? next : null;
    }

    static boolean clearPlayerSpace(ServerLevel level, ServerPlayer player, Vec3 feet) {
        var box = player.getBoundingBox().move(feet.subtract(player.position()));
        return !level.getBlockCollisions(player, box.deflate(.015)).iterator().hasNext();
    }
}
