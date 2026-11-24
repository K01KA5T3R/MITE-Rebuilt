package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class BossAttackGeometry {
    private BossAttackGeometry() {}

    static Vec3 flatDirection(Vec3 direction) {
        return direction.multiply(1, 0, 1).normalize();
    }

    static Vec3 flatDirection(double radians) {
        return new Vec3(Math.cos(radians), 0, Math.sin(radians));
    }

    static Vec3 spread(Vec3 direction, double radians) {
        Vec3 across = direction.cross(new Vec3(0, 1, 0)).normalize();
        if (across.lengthSqr() < .001) across = new Vec3(1, 0, 0);
        return direction.scale(Math.cos(radians)).add(across.scale(Math.sin(radians)));
    }

    static Vec3 ground(ServerLevel level, Entity owner, Vec3 start, double depth) {
        if (!level.hasChunkAt(BlockPos.containing(start))) return null;
        var hit = trace(level, owner, start, start.add(0, -depth, 0));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    static boolean visible(ServerLevel level, Entity owner, Vec3 from, Vec3 to) {
        return trace(level, owner, from, to).getType() == HitResult.Type.MISS;
    }

    static Vec3 shorten(ServerLevel level, Entity owner, Vec3 from, Vec3 to) {
        return trace(level, owner, from, to).getLocation();
    }

    private static HitResult trace(ServerLevel level, Entity owner, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
    }

    static boolean hits(AABB box, Vec3 from, Vec3 to, double radius) {
        AABB expanded = box.inflate(radius);
        return expanded.contains(from) || expanded.clip(from, to).isPresent();
    }

    /** Find contact along the body's actual motion, including an initial overlap. */
    static Vec3 sweptBodyContact(AABB body, Vec3 displacement, AABB target) {
        var center = body.getCenter();
        var expanded = target.inflate(body.getXsize() * .5, body.getYsize() * .5, body.getZsize() * .5);
        return expanded.contains(center) ? center : expanded.clip(center, center.add(displacement)).orElse(null);
    }

    static void point(ServerLevel level, ParticleOptions effect, Vec3 point) {
        for (var player : level.players()) {
            if (player.distanceToSqr(point) <= 46 * 46) {
                level.sendParticles(player, effect, true, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    static void line(ServerLevel level, ParticleOptions effect, Vec3 from, Vec3 to, double spacing) {
        int samples = Math.max(1, Mth.ceil(from.distanceTo(to) / spacing));
        for (int i = 0; i <= samples; i++) point(level, effect, from.lerp(to, (double)i / samples));
    }

    static void ring(ServerLevel level, ParticleOptions effect, Vec3 center, double radius, int samples) {
        for (int i = 0; i < samples; ++i) {
            point(level, effect, center.add(flatDirection(i * Math.PI * 2 / samples).scale(radius)));
        }
    }
}
