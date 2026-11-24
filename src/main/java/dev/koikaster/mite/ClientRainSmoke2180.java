package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.AABB;

final class ClientRainSmoke2180 extends TextureSheetParticle {
    private final LegacyRainSmoke2180 smoke;
    private final ClientSourceIcon1001.World world;

    ClientRainSmoke2180(ParticleEngine engine, ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        smoke = new LegacyRainSmoke2180(x, y, z, OriginalRandomTables.float_1, new Random().nextInt(), Math::random);
        world = new ClientSourceIcon1001.World(level);
        setSprite(((ClientFertilizerAtlas933)engine).mite$fertilizerAtlas().getSprite(Content.id("source_particles")));
        syncPosition();
    }

    private void syncPosition() {
        var motion = smoke.motion;
        x = motion.x;
        y = motion.y;
        z = motion.z;
        var box = motion.box;
        setBoundingBox(new AABB(box.a, box.b, box.c, box.d, box.e, box.f));
    }

    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (smoke.tick(world)) remove();
        syncPosition();
    }

    @Override public void render(VertexConsumer vertices, Camera camera, float partial) {
        var viewer = camera.getEntity();
        var rotations = SourceIconCamera1012.coefficients(viewer.getXRot(), viewer.getYRot(),
            Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT);
        var origin = camera.getPosition();
        int light = getLightColor(partial);
        smoke.render(new SourceIconQuad1007.Camera(origin.x, origin.y, origin.z, rotations.rotationX(),
            rotations.rotationXZ(), rotations.rotationZ(), rotations.rotationYZ(), rotations.rotationXY()), partial,
            new SourceIconQuad1007.Sink() {
                private float shade;
                @Override public void color(float red, float green, float blue) { shade = red; }
                @Override public void vertex(double x, double y, double z, double u, double v) {
                    // The old standalone texture repeats its final frame at negative U. The atlas must stay within this sprite.
                    float atlasU = sprite.getU((u < 0D ? u + 1D : u) * 16D);
                    float atlasV = sprite.getV(v * 16D);
                    vertices.vertex(x, y, z).uv(atlasU, atlasV).color(shade, shade, shade, 1F).uv2(light).endVertex();
                }
            });
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
}
