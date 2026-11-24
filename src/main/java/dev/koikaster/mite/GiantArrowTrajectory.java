package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Airborne 1.20.1 AbstractArrow motion, without a second damage or movement path. */
final class GiantArrowTrajectory {
    private static final double DRAG = .99f;
    private static final double GRAVITY = .05f;
    private static final double STEP_TOLERANCE = .075;

    record Flight(Vec3 origin, Vec3 initial, Vec3 landing, int ticks) {}

    private GiantArrowTrajectory() {}

    static Flight lob(ServerLevel level, Entity shooter, Vec3 origin, Vec3 landing) {
        if (clearSegment(level, shooter, origin, landing)) return null;
        for (int ticks = 13; ticks <= 32; ticks += 2) {
            var flight = flight(origin, landing, ticks);
            if (flight.initial().y < .32 || flight.initial().lengthSqr() > 2.4 * 2.4) continue;
            if (clearFlight(level, shooter, flight, .60, false)) return flight;
        }
        return null;
    }

    static Flight low(ServerLevel level, Entity shooter, Vec3 origin, Vec3 landing) {
        double distance = origin.distanceTo(landing);
        int ticks = Math.max(3, (int)Math.ceil(distance / 1.65));
        var flight = flight(origin, landing, ticks);
        return Math.abs(flight.initial().y) < .26 && clearFlight(level, shooter, flight, 0, true) ? flight : null;
    }

    static Flight layer(ServerLevel level, Entity shooter, Vec3 origin, Vec3 landing) {
        int first = Math.max(4, (int)Math.ceil(origin.distanceTo(landing) / 1.9));
        for (int ticks = first; ticks <= first + 4; ++ticks) {
            var flight = flight(origin, landing, ticks);
            if (flight.initial().lengthSqr() <= 2.05 * 2.05 && clearFlight(level, shooter, flight, 0, false)) {
                return flight;
            }
        }
        return null;
    }

    static boolean stillClear(ServerLevel level, Entity shooter, Flight flight, boolean low) {
        return clearFlight(level, shooter, flight, 0, low);
    }

    private static Flight flight(Vec3 origin, Vec3 landing, int ticks) {
        double horizontalFactor = 0;
        double gravityDrop = 0;
        double velocityMultiplier = 1;
        double gravitySpeed = 0;
        for (int tick = 0; tick < ticks; ++tick) {
            horizontalFactor += velocityMultiplier;
            gravityDrop += gravitySpeed;
            velocityMultiplier *= DRAG;
            gravitySpeed = gravitySpeed * DRAG + GRAVITY;
        }
        Vec3 initial = landing.subtract(origin).add(0, gravityDrop, 0).scale(1 / horizontalFactor);
        return new Flight(origin, initial, landing, ticks);
    }

    private static boolean clearFlight(ServerLevel level, Entity shooter, Flight flight, double lift, boolean low) {
        double peak = flight.origin().y;
        Vec3 from = flight.origin();
        Vec3 velocity = flight.initial();
        for (int tick = 0; tick < flight.ticks(); ++tick) {
            Vec3 to = from.add(velocity);
            if (!clearSegment(level, shooter, from, to) || (low && !lowCorridor(level, shooter, from, to))) return false;
            peak = Math.max(peak, to.y);
            velocity = velocity.scale(DRAG).add(0, -GRAVITY, 0);
            from = to;
        }
        return peak + STEP_TOLERANCE >= Math.max(flight.origin().y, flight.landing().y) + lift;
    }

    private static boolean lowCorridor(ServerLevel level, Entity shooter, Vec3 from, Vec3 to) {
        int samples = Math.max(1, (int)Math.ceil(from.distanceTo(to) * 2));
        for (int i = 0; i <= samples; ++i) {
            Vec3 point = from.lerp(to, (double)i / samples);
            Vec3 floor = BossAttackGeometry.ground(level, shooter, point, 1.25);
            if (floor == null || point.y - floor.y < .20 || !level.getFluidState(BlockPos.containing(floor.add(0, .02, 0))).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean clearSegment(ServerLevel level, Entity shooter, Vec3 from, Vec3 to) {
        BlockPos start = BlockPos.containing(from);
        BlockPos end = BlockPos.containing(to);
        return level.hasChunksAt(start, end) && level.getWorldBorder().isWithinBounds(end)
            && level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, shooter))
                .getType() == HitResult.Type.MISS;
    }
}
