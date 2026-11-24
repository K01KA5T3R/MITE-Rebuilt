package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A short, harmless ground step during recovery. Attack movement is handled separately. */
final class BossRecoveryStep {
    static final int DURATION = 5;
    private static final double FLOOR_TOLERANCE = .16;

    private BossRecoveryStep() {}

    static Vec3 plan(ServerLevel level, Mob boss, LivingEntity target, int side, double speed,
                     double forwardBias, double maxDistance) {
        Vec3 separation = target.position().subtract(boss.position());
        if (!boss.onGround() || Math.abs(separation.y) > .85 || separation.horizontalDistance() > maxDistance
                || !boss.hasLineOfSight(target)) return Vec3.ZERO;

        Vec3 forward = BossAttackGeometry.flatDirection(separation);
        if (forward.lengthSqr() < .5) return Vec3.ZERO;
        Vec3 across = new Vec3(-forward.z, 0, forward.x).scale(side);
        for (Vec3 lateral : new Vec3[]{across, across.reverse()}) {
            Vec3 step = lateral.add(forward.scale(forwardBias)).normalize().scale(speed);
            if (clearRoute(level, boss, step)) return step;
        }
        return Vec3.ZERO;
    }

    private static boolean clearRoute(ServerLevel level, Mob boss, Vec3 step) {
        for (int tick = 0; tick <= DURATION; tick++) {
            if (!clear(level, boss, step.scale(tick))) return false;
        }
        return true;
    }

    static boolean move(ServerLevel level, Mob boss, Vec3 step) {
        if (!boss.onGround() || step.lengthSqr() == 0
                || !clear(level, boss, Vec3.ZERO) || !clear(level, boss, step)) return false;
        Vec3 before = boss.position();
        boss.move(MoverType.SELF, step.add(0, -.04, 0));
        return boss.position().subtract(before).dot(step) >= step.lengthSqr() * .8;
    }

    private static boolean clear(ServerLevel level, Mob boss, Vec3 offset) {
        AABB feet = boss.getBoundingBox();
        AABB body = feet;
        for (Entity rider : boss.getPassengers()) body = body.minmax(rider.getBoundingBox());
        AABB next = body.move(offset);
        if (!level.hasChunksAt(BlockPos.containing(next.minX, next.minY, next.minZ),
                BlockPos.containing(next.maxX, next.maxY, next.maxZ))
                || !level.getWorldBorder().isWithinBounds(next)
                || level.containsAnyLiquid(next)
                || level.getBlockCollisions(boss, next.deflate(.025)).iterator().hasNext()) return false;

        AABB sweep = body.expandTowards(offset).deflate(.025);
        if (!level.getEntitiesOfClass(LivingEntity.class, sweep, other -> other != boss
                && other.isAlive() && !other.isSpectator() && other.getRootVehicle() != boss.getRootVehicle()).isEmpty()) {
            return false;
        }

        Vec3 center = boss.position().add(offset);
        double cornerX = feet.getXsize() * .32;
        double cornerZ = feet.getZsize() * .32;
        for (double dx : new double[]{-cornerX, cornerX}) {
            for (double dz : new double[]{-cornerZ, cornerZ}) {
                Vec3 probe = center.add(dx, FLOOR_TOLERANCE, dz);
                Vec3 floor = BossAttackGeometry.ground(level, boss, probe, FLOOR_TOLERANCE * 2);
                if (floor == null || Math.abs(floor.y - center.y) > FLOOR_TOLERANCE
                        || !level.getFluidState(BlockPos.containing(floor.add(0, .03, 0))).isEmpty()) return false;
            }
        }
        return true;
    }
}
