package dev.koikaster.mite;

import java.util.function.DoubleSupplier;

/** The R195 beg/bel smoke selected by the bfe lava rain branch. */
final class LegacyRainSmoke2180 {
    final SourceIconMotion1014.State motion;
    final int lifetime;
    final float shade;
    private final float scale;
    private double oldX, oldY, oldZ;
    private boolean rendered;
    int age, tile;

    LegacyRainSmoke2180(double x, double y, double z, float[] table, int index, DoubleSupplier random) {
        motion = new SourceIconMotion1014.State(x, y, z);
        scale = (table[(index + 3) & 32767] + 1F) * .75F;
        double dx = jitter(table[(index + 5) & 32767]);
        double dy = jitter(table[(index + 6) & 32767]);
        double dz = jitter(table[(index + 7) & 32767]);
        float speed = (table[(index + 8) & 32767] * (float)random.getAsDouble() + 1F) * .15F;
        float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        motion.vx = dx / length * speed * (double).4F * (double).1F;
        motion.vy = (dy / length * speed * (double).4F + (double).1F) * (double).1F;
        motion.vz = dz / length * speed * (double).4F * (double).1F;
        shade = (float)(random.getAsDouble() * (double).3F);
        lifetime = (int)(8D / (random.getAsDouble() * .8D + .2D));
    }

    private static double jitter(float value) {
        return (double)(value * .8F) - (double).4F;
    }

    boolean tick(SourceIconMotion1014.World world) {
        rememberPosition();
        boolean expired = age++ >= lifetime;
        tile = 7 - age * 8 / lifetime;
        motion.vy += .004D;
        SourceIconMotion1014.move(motion, world, motion.vx, motion.vy, motion.vz);
        if (motion.y == oldY) {
            motion.vx *= 1.1D;
            motion.vz *= 1.1D;
        }
        motion.vx *= (double).96F;
        motion.vy *= (double).96F;
        motion.vz *= (double).96F;
        if (motion.onGround) {
            motion.vx *= (double).7F;
            motion.vz *= (double).7F;
        }
        return expired;
    }

    private void rememberPosition() {
        oldX = motion.x;
        oldY = motion.y;
        oldZ = motion.z;
    }

    void render(SourceIconQuad1007.Camera camera, float partial, SourceIconQuad1007.Sink sink) {
        if (!rendered) {
            rememberPosition();
            rendered = true;
        }
        float growth = Math.min(1F, Math.max(0F, ((float)age + partial) / (float)lifetime * 32F));
        float size = .1F * (scale * growth);
        float x = (float)(motion.x * partial + oldX * (1D - partial) - camera.x());
        float y = (float)(motion.y * partial + oldY * (1D - partial) - camera.y());
        float z = (float)(motion.z * partial + oldZ * (1D - partial) - camera.z());
        float horizontalX = camera.rotationX() * size;
        float horizontalZ = camera.rotationZ() * size;
        float verticalX = camera.rotationYZ() * size;
        float verticalY = camera.rotationXZ() * size;
        float verticalZ = camera.rotationXY() * size;
        float u0 = (float)(tile % 16) / 16F, u1 = u0 + .0624375F;
        float v0 = (float)(tile / 16) / 16F, v1 = v0 + .0624375F;
        sink.color(shade, shade, shade);
        sink.vertex(x - horizontalX - verticalX, y - verticalY, z - horizontalZ - verticalZ, u1, v1);
        sink.vertex(x - horizontalX + verticalX, y + verticalY, z - horizontalZ + verticalZ, u1, v0);
        sink.vertex(x + horizontalX + verticalX, y + verticalY, z + horizontalZ + verticalZ, u0, v0);
        sink.vertex(x + horizontalX - verticalX, y - verticalY, z + horizontalZ - verticalZ, u0, v1);
    }
}
