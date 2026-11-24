package dev.koikaster.mite;

import java.util.function.BiConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class BossParticlePattern {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    static void beam(Vec3 from, Vec3 to, double radius, BiConsumer<Vec3, Vec3> sample) {
        Vec3 axis = to.subtract(from).normalize();
        Vec3 side = axis.cross(Math.abs(axis.y) > .9 ? new Vec3(1, 0, 0) : UP).normalize();
        Vec3 other = axis.cross(side);
        int steps = Mth.clamp(Mth.ceil(from.distanceTo(to) / .70), 1, 26);
        for (int i = 0; i <= steps; i++) {
            Vec3 center = from.lerp(to, i / (double)steps);
            sample.accept(center, center);
            if (radius == 0 || axis.lengthSqr() == 0) continue;
            Vec3 offset = (i % 2 == 0 ? side : other).scale(radius * .88);
            sample.accept(center, center.add(offset));
            sample.accept(center, center.subtract(offset));
        }
    }

    static void strip(Vec3 from, Vec3 to, double width, double height, boolean centered,
                      BiConsumer<Vec3, Vec3> sample) {
        Vec3 axis = to.subtract(from);
        Vec3 side = axis.cross(UP).normalize().scale(width * .90);
        Vec3 middle = from.lerp(to, .5);
        sample.accept(from, from.add(0, centered ? 0 : height * .25, 0));
        sample.accept(to, to.add(0, centered ? 0 : height * .25, 0));
        sample.accept(middle, middle.add(side));
        sample.accept(middle, middle.subtract(side));
        sample.accept(middle, middle.add(0, height * .85, 0));
        if (centered) sample.accept(middle, middle.add(0, -height * .85, 0));
    }

    static void sector(Vec3 center, Vec3 first, Vec3 last, double height, boolean centered,
                       BiConsumer<Vec3, Vec3> sample) {
        Vec3 rim = first.lerp(last, .5);
        sample.accept(center, center.lerp(rim, .38));
        sample.accept(center, center.lerp(rim, .70));
        sample.accept(center, center.lerp(first, .92));
        sample.accept(center, center.lerp(last, .92));
        sample.accept(center, rim.add(0, height * .85, 0));
        if (centered) sample.accept(center, rim.add(0, -height * .85, 0));
    }

    private BossParticlePattern() {}
}
