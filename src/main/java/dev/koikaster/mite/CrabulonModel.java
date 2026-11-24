package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

final class CrabulonModel extends HierarchicalModel<SourceCrabulon> {
    private static final int ATLAS_SIZE = 256;
    private static final int LEG_COUNT = 6;
    private static final int MUSHROOM_COUNT = 7;
    // LivingEntityRenderer offsets a model by 1.501 blocks before applying its parts.
    private static final float FLOOR = 24;

    private enum Surface {
        SHELL(0, 0), ARMOR(128, 0), JOINT(0, 64), RIDGE(128, 64),
        RED(0, 128), BROWN(64, 128), STEM(128, 128), GILL(192, 128), EYE(0, 192);

        private final int u;
        private final int v;

        Surface(int u, int v) {
            this.u = u;
            this.v = v;
        }

        CubeListBuilder on(CubeListBuilder mesh) {
            return mesh.texOffs(u, v);
        }

        CubeListBuilder mesh() {
            return on(CubeListBuilder.create());
        }
    }

    private final ModelPart root;
    private final ModelPart hull;
    private final ModelPart eyes;
    private final ModelPart leftClaw;
    private final ModelPart rightClaw;
    private final ModelPart leftPincer;
    private final ModelPart rightPincer;
    private final ModelPart[] legs = new ModelPart[LEG_COUNT];
    private final ModelPart[] feet = new ModelPart[LEG_COUNT];
    private final ModelPart[] caps = new ModelPart[MUSHROOM_COUNT];
    private final Matrix4f hullPose = new Matrix4f();
    private final Matrix4f footPose = new Matrix4f();

    CrabulonModel(ModelPart root) {
        this.root = root;
        hull = root.getChild("hull");
        eyes = hull.getChild("eyes");
        leftClaw = hull.getChild("left_claw");
        rightClaw = hull.getChild("right_claw");
        leftPincer = leftClaw.getChild("palm").getChild("pincer");
        rightPincer = rightClaw.getChild("palm").getChild("pincer");
        for (int i = 0; i < LEG_COUNT; ++i) {
            legs[i] = hull.getChild("leg_" + i);
            feet[i] = legs[i].getChild("foot");
        }
        for (int i = 0; i < MUSHROOM_COUNT; ++i) {
            caps[i] = hull.getChild("mushroom_" + i).getChild("cap");
        }
    }

    static LayerDefinition layer() {
        var mesh = new MeshDefinition();
        var body = Surface.SHELL.mesh()
            .addBox(-13, -5, -11, 26, 11, 23)
            .addBox(-12, -5, 11.6f, 24, 7, 3);
        Surface.JOINT.on(body).addBox(-10.5f, 4.8f, -10.5f, 21, 3, 19)
            .addBox(-9, -2, -14, 18, 7, 5);
        Surface.ARMOR.on(body).addBox(-15.5f, -6, -9.8f, 31, 4, 21)
            .addBox(-12.5f, -8.5f, -8, 25, 4, 17)
            .addBox(-10.8f, -6.9f, 8.5f, 21.6f, 4, 5)
            .addBox(-11.2f, -7, -13, 22.4f, 3, 5);
        Surface.RIDGE.on(body).addBox(-8, -9.8f, -5, 16, 2.5f, 12)
            .addBox(-1.5f, -5, -16.2f, 3, 6, 4)
            .addBox(-7, 3.5f, -14.5f, 14, 2.8f, 3);
        var hull = mesh.getRoot().addOrReplaceChild("hull", body, PartPose.offset(0, 11, 0));
        hull.addOrReplaceChild("eyes", Surface.EYE.mesh()
            .addBox(-6.55f, -.9f, -14.16f, 2.45f, 1.25f, .25f)
            .addBox(4.1f, -.9f, -14.16f, 2.45f, 1.25f, .25f), PartPose.ZERO);

        for (int side : new int[]{1, -1}) {
            hull.addOrReplaceChild(side > 0 ? "left_brow" : "right_brow",
                Surface.RIDGE.mesh().addBox(-4.1f, -1.35f, -1.4f, 8.2f, 2.5f, 4.1f),
                PartPose.offsetAndRotation(side * 6.9f, -2.7f, -13.4f, -.14f, side * -.10f, side * -.21f));
            hull.addOrReplaceChild(side > 0 ? "left_shoulder" : "right_shoulder",
                Surface.ARMOR.mesh().addBox(-4, -2.6f, -7, 8, 5.2f, 13),
                PartPose.offsetAndRotation(side * 12.5f, -3.2f, -1.5f, .14f, side * -.20f, side * .30f));
            addClaw(hull, side);
            for (int i = 0; i < 3; ++i) addLeg(hull, side, i);
        }

        addMushroom(hull, 0, true, -6.8f, -8.1f, -5.3f, 5.8f, 7.8f, -.12f, .13f);
        addMushroom(hull, 1, true, 5.7f, -9.0f, 4.5f, 7.4f, 10.2f, .10f, -.11f);
        addMushroom(hull, 2, true, -10.9f, -6.3f, 6.1f, 3.7f, 5.2f, -.18f, -.21f);
        addMushroom(hull, 3, false, 8.2f, -8.2f, -5.9f, 3.3f, 7.8f, .11f, .25f);
        addMushroom(hull, 4, false, -1.6f, -9.8f, 3.9f, 3.9f, 7.4f, -.08f, -.32f);
        addMushroom(hull, 5, false, -6.2f, -7.2f, 10.5f, 3.2f, 6.1f, .17f, .12f);
        addMushroom(hull, 6, false, 11.6f, -6.1f, 8.9f, 3.0f, 5.8f, -.20f, -.17f);
        return LayerDefinition.create(mesh, ATLAS_SIZE, ATLAS_SIZE);
    }

    private static void addClaw(PartDefinition hull, int side) {
        var arm = Surface.JOINT.mesh().addBox(-3.0f, -1.8f, -8.7f, 6, 4, 10.2f);
        Surface.ARMOR.on(arm).addBox(-3.7f, -2.9f, -7.2f, 7.4f, 3.8f, 6.2f);
        var claw = hull.addOrReplaceChild(side > 0 ? "left_claw" : "right_claw", arm,
            PartPose.offsetAndRotation(side * 10.7f, 2.2f, -7.0f, 0, side * -.21f, 0));
        var wrist = Surface.SHELL.mesh().addBox(-5.2f, -2.1f, -5.2f, 10.4f, 6, 8.2f);
        Surface.ARMOR.on(wrist).addBox(-5.7f, -4.3f, -4.9f, 11.4f, 5, 7.8f)
            .addBox(-4.4f, -4.9f, -3.7f, 8.8f, 1.9f, 5.8f);
        Surface.RIDGE.on(wrist).addBox(-4.3f, -3.6f, -5.55f, 8.6f, 3.7f, 1.7f);
        var palm = claw.addOrReplaceChild("palm", wrist, PartPose.offset(side * .9f, 1.1f, -8.6f));
        var fixed = Surface.ARMOR.mesh().addBox(-2.5f, -2, -5.9f, 5, 4.2f, 7.7f);
        Surface.RIDGE.on(fixed).addBox(-1.8f - side * .65f, -1.75f, -7.0f, 3.6f, 3.3f, 2.9f);
        palm.addOrReplaceChild("fixed_finger", fixed,
            PartPose.offsetAndRotation(side * 3.0f, .10f, -4.2f, .08f, side * .26f, 0));
        var moving = Surface.SHELL.mesh().addBox(-1.85f, -1.3f, -5.7f, 3.7f, 3.5f, 6.6f);
        Surface.ARMOR.on(moving).addBox(-1.5f + side * .55f, -1.8f, -6.45f, 3, 3.0f, 3.25f);
        palm.addOrReplaceChild("pincer", moving,
            PartPose.offsetAndRotation(side * -3.4f, .28f, -4.05f, -.06f, side * .06f, 0));
    }

    private static void addLeg(PartDefinition hull, int side, int index) {
        float shinLength = shinLength(index);
        int id = side == 1 ? index : index + 3;
        var upper = Surface.JOINT.mesh().addBox(side > 0 ? -1 : -8.2f, -1.3f, -1.7f, 9.2f, 3.1f, 3.4f);
        Surface.ARMOR.on(upper).addBox(side > 0 ? .4f : -6.8f, -2, -1.45f, 6.4f, 2.2f, 2.9f);
        var thigh = hull.addOrReplaceChild("leg_" + id, upper,
            PartPose.offsetAndRotation(side * 10.8f, 6.0f, index * 7.0f - 6.4f, 0,
                side * (1 - index) * .34f, side * .20f));
        var lower = Surface.JOINT.mesh().addBox(-1.35f, -.5f, -1.25f, 2.7f, shinLength, 2.5f);
        Surface.SHELL.on(lower).addBox(-.87f + side * .32f, shinLength - 1.1f, -.85f, 1.74f, 1.1f, 1.7f);
        thigh.addOrReplaceChild("foot", lower, PartPose.offsetAndRotation(side * 7.35f, .25f, 0, .08f, 0, side * -.48f));
    }

    private static float shinLength(int index) {
        return index == 1 ? 4.8f : 4.5f;
    }

    private static void addMushroom(PartDefinition hull, int id, boolean red, float x, float y, float z,
                                    float stemHeight, float width, float tilt, float yaw) {
        float stem = red ? 1.7f : 1.3f;
        var plant = hull.addOrReplaceChild("mushroom_" + id,
            Surface.STEM.mesh().addBox(-stem * .5f, -stemHeight, -stem * .5f, stem, stemHeight + .7f, stem),
            PartPose.offsetAndRotation(x, y, z, tilt, yaw, tilt * .7f));
        float depth = width * (red ? .86f : .92f);
        float rimHeight = red ? 1.95f : 1.0f;
        float crownHeight = red ? 1.8f : .65f;
        var skin = red ? Surface.RED : Surface.BROWN;
        var cap = skin.mesh().addBox(-width * .5f, -rimHeight, -depth * .5f, width, rimHeight, depth)
            .addBox(-width * .38f, -rimHeight - crownHeight, -depth * .38f, width * .76f, crownHeight + .1f, depth * .76f);
        Surface.GILL.on(cap).addBox(-width * .43f, -.12f, -depth * .43f, width * .86f, .55f, depth * .86f);
        plant.addOrReplaceChild("cap", cap, PartPose.offset(0, -stemHeight, 0));
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SourceCrabulon crab, float step, float stride, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        var action = crab.actionPose();
        var move = crab.attackPose();
        float progress = crab.actionProgress(age - crab.tickCount);
        float ease = progress * progress * (3 - 2 * progress);
        float effort = Mth.sin(progress * Mth.PI);
        walk(step, Math.min(stride, .88f));

        if (action == CrabulonCombat.Action.INTRO) {
            float rising = 1 - ease;
            hull.y += rising * 2.6f;
            hull.xRot -= rising * .17f;
            foldLegs(rising * .23f);
            if (crab.onGround()) liftBuriedFeet();
            return;
        }
        if (move != null) animateAction(action, move, ease, effort, progress);
        if (move == CrabulonCombat.Move.FAN) {
            float strength = switch (action) {
                case WINDUP -> ease;
                case ACTIVE -> 1;
                case RECOVERY -> 1 - ease;
                default -> 0;
            };
            float aimPitch = Mth.clamp(pitch * Mth.DEG_TO_RAD, -.52f, .52f);
            hull.xRot += aimPitch * strength;
        }
        if (crab.onGround()) liftBuriedFeet();
    }

    private void animateAction(CrabulonCombat.Action action, CrabulonCombat.Move move, float ease, float effort, float progress) {
        switch (action) {
            case WINDUP -> {
                if (move == CrabulonCombat.Move.CLAW || move == CrabulonCombat.Move.BREACH) {
                    swingClaw(1, -.94f * ease, -.54f * ease, .56f * ease);
                    hull.zRot -= .15f * ease;
                } else if (move == CrabulonCombat.Move.HOOK) {
                    swingClaw(-1, -.78f * ease, .58f * ease, .60f * ease);
                    hull.zRot += .15f * ease;
                } else {
                    hull.y += 1.6f * effort;
                    hull.xRot -= .17f * effort;
                    swingClaw(1, -.36f * effort, 0, .17f * effort);
                    swingClaw(-1, -.36f * effort, 0, .17f * effort);
                }
            }
            case ACTIVE -> animateMove(move, ease, effort, progress);
            case RECOVERY -> {
                if (move == CrabulonCombat.Move.CLAW || move == CrabulonCombat.Move.HOOK) {
                    float open = 1 - ease;
                    int side = move == CrabulonCombat.Move.CLAW ? 1 : -1;
                    hull.zRot += side * .18f * open;
                    swingClaw(side, .36f * open, side * .36f * open, 0);
                    swingClaw(-side, -.11f * open, -side * .26f * open, .13f * open);
                }
            }
            default -> { }
        }
    }

    private void liftBuriedFeet() {
        hullPose.translation(hull.x, hull.y, hull.z).rotateZYX(hull.zRot, hull.yRot, hull.xRot);
        for (int i = 0; i < LEG_COUNT; ++i) {
            var thigh = legs[i];
            var foot = feet[i];
            footPose.set(hullPose)
                .translate(thigh.x, thigh.y, thigh.z).rotateZYX(thigh.zRot, thigh.yRot, thigh.xRot)
                .translate(foot.x, foot.y, foot.z).rotateZYX(foot.zRot, foot.yRot, foot.xRot);
            float length = shinLength(i % 3);
            float toeX = i < 3 ? .32f : -.32f;
            float shin = lowestY(footPose, 0, length - .5f, 1.35f, 1.25f);
            float toe = lowestY(footPose, toeX, length, .87f, .85f);
            float buried = Math.max(shin, toe) - FLOOR;
            if (buried > 0) thigh.y -= buried / hullPose.m11();
        }
    }

    private static float lowestY(Matrix4f pose, float x, float bottom, float halfWidth, float halfDepth) {
        return pose.m31() + pose.m01() * x + pose.m11() * bottom
            + Math.abs(pose.m01()) * halfWidth + Math.abs(pose.m21()) * halfDepth;
    }

    private void walk(float step, float stride) {
        for (int i = 0; i < LEG_COUNT; ++i) {
            int side = i < 3 ? 1 : -1;
            float phase = step * 1.6f + ((i % 3 + (i < 3 ? 0 : 1)) % 2) * Mth.PI;
            float lift = Math.max(0, Mth.sin(phase)) * stride;
            legs[i].yRot += side * Mth.cos(phase) * stride * .30f;
            legs[i].zRot -= side * lift * .27f;
            feet[i].zRot += side * lift * .23f;
        }
    }

    private void animateMove(CrabulonCombat.Move move, float ease, float effort, float progress) {
        switch (move) {
            case CLAW, BREACH -> {
                swingClaw(1, Mth.lerp(ease, -.94f, .45f), Mth.lerp(ease, -.54f, .56f), .56f * (1 - ease));
                hull.zRot += Mth.lerp(ease, -.15f, .16f);
            }
            case HOOK -> {
                swingClaw(-1, Mth.lerp(ease, -.78f, .37f), Mth.lerp(ease, .58f, -.46f), .60f * (1 - ease));
                hull.zRot += Mth.lerp(ease, .15f, -.15f);
            }
            case SKITTER -> {
                hull.y += .85f * effort;
                float roll = Mth.sin(progress * Mth.TWO_PI);
                hull.zRot += .14f * roll;
                foldLegs(.12f * effort);
            }
            case ALTERNATE -> {
                float cycle = Mth.sin(progress * Mth.TWO_PI);
                swingClaw(1, -Math.max(0, cycle) * .99f, 0, Math.max(0, cycle) * .37f);
                swingClaw(-1, Math.min(0, cycle) * .99f, 0, -Math.min(0, cycle) * .37f);
                hull.zRot += cycle * .14f;
            }
            case HOPS, OVERLEAP, STOMP -> {
                hull.xRot += effort * .21f;
                swingClaw(1, -.60f * effort, -.14f * effort, .22f * effort);
                swingClaw(-1, -.60f * effort, .14f * effort, .22f * effort);
                foldLegs(.29f * effort);
            }
            case CAP_VOLLEY, FAN, CLOUD, BROOD, UNDERCUT -> {
                if (move != CrabulonCombat.Move.FAN) {
                    hull.xRot += (move == CrabulonCombat.Move.UNDERCUT ? .05f : .14f) * effort;
                }
                for (int i = 0; i < MUSHROOM_COUNT; ++i) {
                    caps[i].xRot += .065f * effort * Mth.sin(i + progress * Mth.TWO_PI);
                }
            }
        }
    }

    private void swingClaw(int side, float pitch, float yaw, float opening) {
        var claw = side > 0 ? leftClaw : rightClaw;
        var pincer = side > 0 ? leftPincer : rightPincer;
        claw.xRot += pitch;
        claw.yRot += yaw;
        pincer.yRot += side * opening;
    }

    private void foldLegs(float amount) {
        for (int i = 0; i < LEG_COUNT; ++i) {
            int side = i < 3 ? 1 : -1;
            legs[i].zRot -= side * amount;
            feet[i].zRot += side * amount;
        }
    }

    void renderEyes(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        pose.pushPose();
        root.translateAndRotate(pose);
        hull.translateAndRotate(pose);
        eyes.render(pose, vertices, light, overlay);
        pose.popPose();
    }
}
