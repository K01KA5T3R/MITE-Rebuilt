package dev.koikaster.mite;

import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Ravager;

final class MythrilRavagerModel extends RavagerModel {
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart mouth;
    private final ModelPart leftFront;
    private final ModelPart rightFront;
    private final ModelPart leftHind;
    private final ModelPart rightHind;

    MythrilRavagerModel(ModelPart root) {
        super(root);
        neck = root.getChild("neck");
        head = neck.getChild("head");
        mouth = head.getChild("mouth");
        leftFront = root.getChild("left_front_leg");
        rightFront = root.getChild("right_front_leg");
        leftHind = root.getChild("left_hind_leg");
        rightHind = root.getChild("right_hind_leg");
    }

    @Override
    public void prepareMobModel(Ravager entity, float swing, float amount, float partialTick) {
        root().getAllParts().forEach(ModelPart::resetPose);
        super.prepareMobModel(entity, swing, amount, partialTick);
    }

    @Override
    public void setupAnim(Ravager entity, float swing, float amount, float ageInTicks, float yaw, float pitch) {
        super.setupAnim(entity, swing, amount, ageInTicks, yaw, pitch);
        if (!(entity instanceof MythrilRavager mount)) return;
        MythrilKnightCombat.Move move = mount.attackPose();
        if (move == null) return;
        float age = mount.attackPoseAge(ageInTicks - entity.tickCount);
        if (age < 0 || age >= move.activeUntil + move.recovery) return;
        float gathered = ease(age / move.windup);
        float recovery = 1 - ease((age - move.activeUntil) / move.recovery);
        float weight = gathered * recovery;
        float impact = ease((age - move.windup + 1) / 2) * (1 - ease((age - move.windup - 1) / 5));
        int side = mount.attackSide();
        switch (move) {
            case CHARGE -> {
                neck.xRot += .36f * weight;
                neck.z -= 2.0f * weight;
                head.xRot += .26f * weight;
                mouth.xRot += .19f * weight;
            }
            case HEADBUTT -> {
                neck.xRot += .28f * weight - .45f * impact;
                neck.z -= 3.4f * impact;
                head.xRot -= .29f * impact;
                mouth.xRot += .17f * impact;
            }
            case LOWER_SWEEP -> {
                neck.xRot += .20f * weight + .48f * impact;
                neck.z -= 4.1f * impact;
                head.xRot += .34f * weight + .42f * impact;
                mouth.xRot += .19f * impact;
                leftFront.xRot -= .23f * weight;
                rightFront.xRot -= .23f * weight;
            }
            case HOOK -> {
                neck.zRot -= .18f * side * weight;
                head.yRot += side * (.42f * weight + .65f * impact);
                head.xRot += .13f * weight;
                if (side > 0) rightFront.xRot -= .31f * weight;
                else leftFront.xRot -= .31f * weight;
            }
            case KICK -> {
                head.yRot += side * .60f * weight;
                head.xRot += .21f * weight;
                rightHind.xRot += .86f * impact;
                leftHind.xRot += .72f * impact;
                leftFront.xRot -= .17f * weight;
                rightFront.xRot -= .17f * weight;
            }
            case STOMP -> {
                float lift = gathered * (1 - ease((age - move.windup + 1) / 2));
                rightFront.xRot -= .98f * lift;
                leftFront.xRot -= .98f * lift;
                neck.xRot -= .31f * lift;
                head.xRot += .24f * impact;
            }
            case SIDESTEP -> {
                neck.zRot -= side * .31f * weight;
                head.yRot += side * .37f * weight;
                rightFront.zRot -= side * .25f * weight;
                leftFront.zRot -= side * .25f * weight;
            }
            case VOLLEY -> {
                neck.xRot += .12f * weight;
                rightFront.xRot -= .17f * weight;
                leftFront.xRot += .17f * weight;
            }
            case BRAKE -> {
                float brace = 1 - ease(age / 15);
                neck.xRot += .31f * brace;
                rightFront.xRot -= .38f * brace;
                leftFront.xRot += .22f * brace;
            }
            default -> { }
        }
    }

    private static float ease(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
