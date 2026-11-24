package dev.koikaster.mite;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Chooses a local flight leg without steering the Wither into solid terrain. */
final class WitherFlightPath {
    private static final double RANGE = 10.5;
    private static final double LOOK_AHEAD = 2.5;
    private static final int[] ALTITUDES = {9, 6, 3};
    private static final int[] ANGLES = {0, 45, -45, 90, -90, 135, -135, 180};

    static Vec3 firstLeg(Vec3 from, Vec3 target, Vec3 outward, Predicate<Vec3> room,
                         BiPredicate<Vec3, Vec3> clear) {
        for (int height : ALTITUDES) {
            for (int degrees : ANGLES) {
                Vec3 destination = target.add(outward.yRot((float)Math.toRadians(degrees)).scale(RANGE)).add(0, height, 0);
                if (!room.test(destination)) continue;
                if (clear.test(from, destination)) return destination;
                if (Math.abs(destination.y - from.y) < .25) continue;

                Vec3 above = new Vec3(from.x, destination.y, from.z);
                if (room.test(above) && clear.test(from, above) && clear.test(above, destination)) return above;

                Vec3 around = new Vec3(destination.x, from.y, destination.z);
                if (room.test(around) && clear.test(from, around) && clear.test(around, destination)) return around;
            }
        }
        return null;
    }

    static AABB nearSweep(AABB body, Vec3 travel) {
        double distance = travel.length();
        return body.expandTowards(distance > LOOK_AHEAD ? travel.scale(LOOK_AHEAD / distance) : travel);
    }

    private WitherFlightPath() {}
}
