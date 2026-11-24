package dev.koikaster.mite;

import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.IronGolem;

final class RedstoneGolemModel extends IronGolemModel<IronGolem> {
    private final ModelPart left;
    private final ModelPart right;

    RedstoneGolemModel(ModelPart root) {
        super(root);
        left = root.getChild("left_arm");
        right = root.getChild("right_arm");
    }

    @Override
    public void prepareMobModel(IronGolem golem, float limbSwing, float swingAmount, float partialTick) {
        left.yRot = left.zRot = right.yRot = right.zRot = 0;
        super.prepareMobModel(golem, limbSwing, swingAmount, partialTick);
        if (!(golem instanceof SourceRedstoneGolem boss)) return;
        float age = boss.attackPoseAge(partialTick);
        if (age < 0) return;
        byte pose = boss.attackPose();
        if (pose == SourceRedstoneGolem.RETURN_POSE) {
            float ready = ease(age / 10);
            float send = ease((age - 10) / 3);
            float draw = ease((age - 23) / 9);
            float rest = 1 - ease((age - 35) / 9);
            right.xRot = (-.62f * ready - 1.52f * send + 1.06f * draw) * rest;
            right.yRot = (.21f * ready - .41f * send) * rest;
            left.xRot = -.49f * ready * rest;
        } else if (pose == SourceRedstoneGolem.CROSSBARS_POSE) {
            float initial = ease(age / 10) * (1 - ease((age - 12) / 6));
            float high = ease((age - 14) / 7) * (1 - ease((age - 23) / 13));
            right.xRot = -1.04f * initial + .26f * high;
            right.zRot = -.58f * initial;
            left.xRot = -.35f * initial - 2.07f * high;
            left.zRot = .49f * high;
        } else if (pose == SourceRedstoneGolem.ADVANCE_POSE) {
            float ready = ease(age / 8);
            float push = ease((age - 10) / 4);
            float rest = 1 - ease((age - 18) / 10);
            right.xRot = (-.72f * ready - 1.17f * push) * rest;
            right.zRot = (-.26f * ready + .18f * push) * rest;
            left.xRot = -.38f * ready * rest;
        } else if (pose == SourceRedstoneGolem.LOB_POSE) {
            float lift = ease(age / 12);
            float release = ease((age - 12) / 4);
            float lower = 1 - ease((age - 16) / 13);
            right.xRot = (-2.4f * lift + .73f * release) * lower;
            right.zRot = -.26f * lift * lower;
            left.xRot = -.62f * lift * lower;
        } else if (pose == SourceRedstoneGolem.CURRENT_POSE) {
            float set = ease(age / 10) * (1 - ease((age - 35) / 9));
            float press = ease((age - 10) / 4) * set;
            left.xRot = right.xRot = -.86f * set + .65f * press;
            left.zRot = .51f * set;
            right.zRot = -.51f * set;
        } else if (pose == SourceRedstoneGolem.LIFT_POSE) {
            float charge = ease(age / 11) * (1 - ease((age - 24) / 13));
            float lift = ease((age - 12) / 11) * charge;
            left.xRot = right.xRot = -1.12f * charge - .70f * lift;
            left.yRot = -.19f * charge;
            right.yRot = .19f * charge;
        } else if (pose == SourceRedstoneGolem.NODES_POSE) {
            float charge = ease(age / 12) * (1 - ease((age - 29) / 11));
            float pulse = age >= 15 && age < 30 ? 1 - ease(((age - 15) % 5) / 4) : 0;
            left.xRot = -.75f * charge - .4f * pulse;
            right.xRot = -1.58f * charge + .7f * pulse;
        } else if (boss.isClamping()) {
            float lift = ease(age / 7) * (1 - ease((age - 16) / 9));
            float open = ease(age / 7) - ease((age - 12) / 4);
            left.xRot = right.xRot = -1.35f * lift;
            left.zRot = .32f * open;
            right.zRot = -.32f * open;
            float close = ease((age - 12) / 4) * (1 - ease((age - 16) / 9));
            left.yRot = -.28f * close;
            right.yRot = .28f * close;
        } else {
            float raised = ease(age / 11) * (1 - ease((age - 15) / 1));
            float planted = ease((age - 15) / 1) * (1 - ease((age - 17) / 11));
            right.xRot = -2.75f * raised - .55f * planted;
            left.xRot = -.22f * (raised + planted);
        }
    }

    private static float ease(float progress) {
        float t = Mth.clamp(progress, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
