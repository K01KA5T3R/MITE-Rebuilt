package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

final class RedstoneGolemAdvance {
    private static final double STEP = .33;
    private static final int STEPS = 7;
    private static final double CLEARANCE = .04;
    private static final double DOWNWARD_CONTACT = -.04;
    private static final double FOOT_HEIGHT = .16;
    private static final double SHOULDER_REACH = .63;
    private static final double SWING_RADIUS = .50;

    private RedstoneGolemAdvance() {}

    static Vec3 direction(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer target) {
        Vec3 delta = target.position().subtract(golem.position());
        double distance = delta.horizontalDistance();
        if (!golem.onGround() || distance < 2.0 || distance > 3.8 || Math.abs(delta.y) > .62) return null;
        Vec3 forward = BossAttackGeometry.flatDirection(delta);
        Vec3 side = across(forward);
        if (!DragonCombat.escape(level, target, side, 1.65, 1.65)
                && !DragonCombat.escape(level, target, side.reverse(), 1.65, 1.65)) return null;
        for (int i = 1; i <= STEPS; i++) {
            if (!clearStep(level, golem, forward.scale(i * STEP))) return null;
        }
        return forward;
    }

    static boolean move(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> fighters, Vec3 forward) {
        Vec3 step = forward.scale(STEP);
        if (!supported(level, golem, Vec3.ZERO) || !supported(level, golem, step)) return false;
        Vec3 before = golem.position();
        golem.move(MoverType.SELF, step.add(0, DOWNWARD_CONTACT, 0));
        Vec3 after = golem.position();
        double actual = after.subtract(before).dot(forward);
        if (actual < .015) return false;

        Vec3 shoulder = across(forward).scale(.29).add(0, 1.05, 0);
        Vec3 from = before.add(shoulder);
        Vec3 to = after.add(shoulder);
        if (actual >= STEP - CLEARANCE) to = to.add(forward.scale(SHOULDER_REACH));
        Vec3 end = BossAttackGeometry.shorten(level, golem, from, to);
        BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, end);
        for (var player : fighters) {
            var box = player.getBoundingBox();
            Vec3 center = box.getCenter();
            if (center.subtract(before).dot(forward) >= 0
                    && BossAttackGeometry.hits(box, from, end, SWING_RADIUS)
                    && BossAttackGeometry.visible(level, golem, from, center)) {
                golem.hurtPlayer(level, player, 11);
            }
        }
        return actual >= STEP - CLEARANCE;
    }

    private static boolean clearStep(ServerLevel level, SourceRedstoneGolem golem, Vec3 offset) {
        return supported(level, golem, offset)
            && !level.getBlockCollisions(golem, golem.getBoundingBox().move(offset).deflate(CLEARANCE)).iterator().hasNext();
    }

    private static boolean supported(ServerLevel level, SourceRedstoneGolem golem, Vec3 offset) {
        Vec3 foot = golem.position().add(offset);
        var box = golem.getBoundingBox();
        double x = box.getXsize() * .32;
        double z = box.getZsize() * .32;
        for (double dx : new double[]{-x, x}) {
            for (double dz : new double[]{-z, z}) {
                Vec3 at = foot.add(dx, 0, dz);
                Vec3 floor = BossAttackGeometry.ground(level, golem, at.add(0, FOOT_HEIGHT, 0), FOOT_HEIGHT * 2);
                if (floor == null || Math.abs(floor.y - foot.y) > FOOT_HEIGHT
                        || !level.getFluidState(BlockPos.containing(floor.add(0, .03, 0))).isEmpty()) return false;
            }
        }
        return true;
    }

    private static Vec3 across(Vec3 forward) {
        return new Vec3(-forward.z, 0, forward.x);
    }
}