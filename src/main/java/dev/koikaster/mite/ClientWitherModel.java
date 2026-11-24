package dev.koikaster.mite;

import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.boss.wither.WitherBoss;

/** Keep the native head and texture when closed, then expose a hinged jaw during the charge. */
public final class ClientWitherModel {
    private static final String UPPER = "mite_wither_upper";
    private static final String JAW = "mite_wither_jaw";

    public static LayerDefinition layer(MeshDefinition mesh, int width, int height, CubeDeformation deformation) {
        var center = mesh.getRoot().getChild("center_head");
        center.addOrReplaceChild(UPPER, CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4, -4, -4, 8, 5, 8, deformation), PartPose.ZERO);
        center.addOrReplaceChild(JAW, CubeListBuilder.create().texOffs(0, 5)
            .addBox(-4, 0, -7.5f, 8, 3, 8, deformation), PartPose.offset(0, 1, 3.5f));
        return LayerDefinition.create(mesh, width, height);
    }

    public static void pose(WitherBossModel<?> model, WitherBoss entity, float ageInTicks) {
        var center = model.root().getChild("center_head");
        if (!center.hasChild(JAW)) return;
        var wither = entity instanceof SourceWitherBoss source ? source : null;
        float opening = wither == null ? 0 : wither.ritualJaw(ageInTicks - entity.tickCount);
        var right = model.root().getChild("right_head");
        var left = model.root().getChild("left_head");
        right.visible = wither == null || !wither.ritualHeadBroken(1);
        left.visible = wither == null || !wither.ritualHeadBroken(2);
        var move = wither == null ? null : WitherCombat.Move.fromPose(wither.ritualPosture());
        float progress = move == null ? 0 : Math.min(1, wither.ritualMoveAge() / (float)move.warning);
        float release = move == null ? 0 : Math.max(0, Math.min(1, (wither.ritualMoveAge() - move.warning) / (float)move.strike));
        var root = model.root();
        root.xRot = move == WitherCombat.Move.CHARGE ? opening * (release < .25f ? -.19f : .22f) : 0;
        root.zRot = move == WitherCombat.Move.CROSS || move == WitherCombat.Move.SWEEP
            ? (.15f - .30f * release) * progress : 0;
        if (wither != null && wither.ritualPosture() == WitherCombat.STAGGERED) {
            float stagger = Math.min(1, wither.ritualMoveAge() / 7f);
            root.xRot = .34f * stagger;
            root.zRot = -.18f * stagger;
        }
        center.zRot = move == WitherCombat.Move.GALE ? (float)Math.sin(ageInTicks * .27f) * .14f : 0;
        if (move == WitherCombat.Move.CHARGE) center.xRot -= .36f * opening;
        if (move == WitherCombat.Move.CURTAIN || move == WitherCombat.Move.AIRBURST) center.xRot -= .47f * progress;
        if (move == WitherCombat.Move.RING) center.xRot += .44f * progress;
        right.zRot = move == WitherCombat.Move.GUARD ? -.24f * progress : 0;
        left.zRot = move == WitherCombat.Move.GUARD ? .24f * progress : 0;
        boolean open = opening > .02f;
        center.skipDraw = open;
        center.getChild(UPPER).visible = open;
        var jaw = center.getChild(JAW);
        jaw.visible = open;
        jaw.xRot = opening * .62f;
    }

    private ClientWitherModel() {}
}
