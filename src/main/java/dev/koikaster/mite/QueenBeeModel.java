package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

final class QueenBeeModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart bone;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;
    private final ModelPart stinger;

    QueenBeeModel(ModelPart root) {
        this.root = root;
        bone = root.getChild("bone");
        body = bone.getChild("body");
        leftWing = bone.getChild("left_wing");
        rightWing = bone.getChild("right_wing");
        leftAntenna = body.getChild("left_antenna");
        rightAntenna = body.getChild("right_antenna");
        stinger = body.getChild("stinger");
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(T actor, float step, float stride, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float flutter = Mth.cos(age * 2.58f);
        float pitchBias = .12f;
        float flap = 1.0f;
        float turn = .12f;
        float curl = 0;
        float crossSpread = 0;
        if (actor instanceof SourceQueenBee queen) {
            float partial = age - (int)age;
            float progress = queen.actionProgress(partial);
            var action = queen.actionPose();
            var move = queen.attackPose();
            turn = queen.bank() * .36f;
            switch (action) {
                case INTRO -> pitchBias += .28f * Mth.sin(progress * Mth.PI);
                case WINDUP -> {
                    pitchBias -= .39f * progress;
                    flap = 1 - .63f * progress;
                    curl = .65f * progress;
                    if (move == QueenBeeCombat.Move.STINGER_CROSS) crossSpread = Math.min(1, progress * 3.0f);
                }
                case ACTIVE -> {
                    if (move == QueenBeeCombat.Move.DIVE) pitchBias += .73f * (1 - progress * .65f);
                    else if (move == QueenBeeCombat.Move.TAIL_STRIKE) {
                        pitchBias += .54f;
                        curl = -.65f;
                    } else if (move == QueenBeeCombat.Move.REVERSAL) {
                        pitchBias += .28f;
                        flap = .37f + .5f * Mth.sin(progress * Mth.PI);
                    } else if (move == QueenBeeCombat.Move.STINGER_CROSS) {
                        crossSpread = Math.max(0, 1 - progress * 4.0f);
                    }
                }
                case RECOVERY -> {
                    pitchBias -= .32f * (1 - progress);
                    flap = .54f + progress * .46f;
                }
                case REST -> {}
            }
            if (queen.late()) flutter = Mth.cos(age * 3.10f);
        }
        bone.xRot = pitchBias + .52f * crossSpread + Mth.sin(age * .23f) * .048f;
        bone.zRot = turn;
        leftWing.yRot = -.18f - .36f * crossSpread;
        rightWing.yRot = .18f + .36f * crossSpread;
        float wingLift = .67f * crossSpread + flutter * .60f * flap * (1 - .85f * crossSpread);
        leftWing.zRot = -wingLift;
        rightWing.zRot = wingLift;
        leftAntenna.xRot = curl + .16f * Mth.cos(age * .28f);
        rightAntenna.xRot = curl + .16f * Mth.cos(age * .28f + .6f);
        stinger.xRot = -.48f * curl;
        stinger.zScale = 1.32f;
    }

    static LayerDefinition crownLayer() {
        var mesh = new MeshDefinition();
        var gold = CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3, -4.7f, -4.1f, 6, .85f, 3.5f)
            .texOffs(0, 6).addBox(-2.85f, -5.9f, -4.08f, 1.08f, 1.3f, .95f)
            .texOffs(4, 6).addBox(-.57f, -6.48f, -4.08f, 1.14f, 1.85f, .98f)
            .texOffs(8, 6).addBox(1.77f, -5.9f, -4.08f, 1.08f, 1.3f, .95f);
        mesh.getRoot().addOrReplaceChild("crown", gold, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    void renderCrown(ModelPart crown, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        pose.pushPose();
        root.translateAndRotate(pose);
        bone.translateAndRotate(pose);
        body.translateAndRotate(pose);
        crown.render(pose, buffer, light, overlay);
        pose.popPose();
    }
}
