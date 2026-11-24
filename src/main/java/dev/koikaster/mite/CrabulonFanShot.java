package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Plans the center spore using the air steps of ThrowableProjectile. */
record CrabulonFanShot(int outlet, Vec3 origin, Vec3 direction) {
    static final double SPEED = .82f;
    static final double OFFSET = .42;
    private static final double DRAG = .99f;
    private static final double[] HEIGHTS = {.38, .20, .65};
    private static final int MAX_TICKS = 26;

    static CrabulonFanShot plan(ServerLevel level, SourceCrabulon boss, Vec3 aim) {
        for (int outlet = 0; outlet < HEIGHTS.length; ++outlet) {
            CrabulonFanShot shot = plan(level, boss, aim, outlet);
            if (shot != null) return shot;
        }
        return null;
    }

    static CrabulonFanShot plan(ServerLevel level, SourceCrabulon boss, Vec3 aim, int outlet) {
        Vec3 origin = boss.position().add(0, boss.getBbHeight() * HEIGHTS[outlet], 0);
        Vec3 delta = aim.subtract(origin);
        if (delta.lengthSqr() < .25 || !level.hasChunkAt(BlockPos.containing(origin))
                || !level.hasChunkAt(BlockPos.containing(aim))) return null;

        // The air path first becomes reachable on the low arc. A fractional tick
        // interpolates the last movement segment, before that tick applies gravity.
        for (int tick = 1; tick <= MAX_TICKS; ++tick) {
            if (reachError(delta, tick) > 0) continue;
            double before = tick - 1, after = tick;
            for (int i = 0; i < 15; ++i) {
                double middle = (before + after) * .5;
                if (reachError(delta, middle) > 0) before = middle;
                else after = middle;
            }
            Vec3 direction = delta.add(0, drop(after), 0).normalize();
            Vec3 start = origin.add(direction.scale(OFFSET));
            if (!clear(level, boss, origin, start)) return null;

            Vec3 at = start;
            Vec3 motion = direction.scale(SPEED);
            for (int n = 0; n < tick; ++n) {
                Vec3 next = at.add(motion.scale(Math.min(1, after - n)));
                if (!clear(level, boss, at, next)) return null;
                at = next;
                motion = motion.scale(DRAG).add(0, -CrabulonSpore.FAN_GRAVITY, 0);
            }
            return new CrabulonFanShot(outlet, origin, direction);
        }
        return null;
    }

    private static boolean clear(ServerLevel level, Entity owner, Vec3 from, Vec3 to) {
        return level.hasChunkAt(BlockPos.containing(to))
            && level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, owner))
                .getType() == HitResult.Type.MISS;
    }

    private static double reachError(Vec3 delta, double ticks) {
        double reach = OFFSET + SPEED * travel(ticks);
        return delta.add(0, drop(ticks), 0).lengthSqr() - reach * reach;
    }

    private static double drop(double ticks) {
        return CrabulonSpore.FAN_GRAVITY * (ticks - travel(ticks)) / (1 - DRAG);
    }

    private static double travel(double ticks) {
        int whole = (int)ticks;
        double scale = Math.pow(DRAG, whole);
        return (1 - scale) / (1 - DRAG) + (ticks - whole) * scale;
    }
}
