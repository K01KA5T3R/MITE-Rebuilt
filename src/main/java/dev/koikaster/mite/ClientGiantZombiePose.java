package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Monster;

public final class ClientGiantZombiePose {
    private ClientGiantZombiePose() {}

    public static void giant(HumanoidModel<?> model, Monster entity, float animation) {
        if (!(entity instanceof SourceGiantZombieBoss giant)) return;
        int action = giant.action();
        float age = giant.actionAge() + Mth.clamp(animation - entity.tickCount, 0, 1);
        model.body.xRot = 0;
        model.body.zRot = 0;
        model.head.zRot = 0;
        model.rightArm.y = model.leftArm.y = 2;
        if (action == SourceGiantZombieBoss.EMERGE) {
            float rise = smooth(age / 53);
            model.rightArm.xRot = -2.75f + rise * 1.35f;
            model.leftArm.xRot = -2.75f + smooth((age - 7) / 46) * 1.35f;
            model.leftArm.zRot = .33f * (1 - rise);
            model.rightArm.zRot = -.33f * (1 - rise);
            return;
        }
        if (action == SourceGiantZombieBoss.FALLEN) {
            float fall = GiantZombieCombat.fallenProgress(age);
            model.head.xRot *= 1 - fall;
            model.head.yRot *= 1 - fall;
            model.rightLeg.xRot = model.leftLeg.xRot = GiantZombieCombat.fallenLegPitch(fall);
            model.rightLeg.yRot = model.leftLeg.yRot = 0;
            model.rightLeg.zRot = model.leftLeg.zRot = 0;
            model.rightArm.xRot = GiantZombieCombat.fallenArmPitch(fall, 1);
            model.leftArm.xRot = GiantZombieCombat.fallenArmPitch(fall, -1);
            model.rightArm.y = model.leftArm.y = 2 + GiantZombieCombat.fallenShoulderDrop(fall) / .375f;
            model.rightArm.yRot = model.leftArm.yRot = 0;
            model.rightArm.zRot = model.leftArm.zRot = 0;
            return;
        }
        var move = GiantZombieCombat.Move.fromAction(action);
        if (move == null) return;
        float held = smooth(age / move.warning);
        float swing = smooth((age - move.warning) / Math.min(9, move.strike));
        float settle = smooth((age - move.warning - move.strike) / move.recovery);
        float energy = held * (1 - settle);
        switch (move) {
            case SLAM -> {
                float contact = smooth((age - move.warning) / 3);
                model.leftArm.xRot = model.rightArm.xRot = Mth.lerp(contact, -2.92f, -.68f) * energy;
                model.head.xRot += contact * .37f * energy;
            }
            case STOMP -> {
                // Plant the foot when the server releases the shock, not nine ticks afterwards.
                float plant = smooth((age - (move.warning - 5)) / 5);
                model.rightLeg.xRot = GiantZombieCombat.plantedLegPitch(move, age);
                model.body.zRot = .16f * energy * (1 - plant);
                model.rightArm.xRot = -.90f * energy;
                model.leftArm.xRot = -.65f * energy;
            }
            case STRIDE -> {
                model.leftLeg.xRot -= .84f * energy * (1 - swing);
                model.rightLeg.xRot += .66f * swing * energy;
            }
            case SWEEP -> {
                float sweep = Mth.clamp((age - move.warning) / 12, 0, 1);
                model.rightArm.xRot = -1.42f * energy;
                model.rightArm.yRot = Mth.lerp(sweep, -1.1f, 1.18f) * energy;
                model.body.zRot = Mth.lerp(sweep, -.24f, .27f) * energy;
            }
            case GRAB -> {
                float grip = smooth((age - move.warning) / 12);
                model.leftArm.xRot = model.rightArm.xRot = -1.28f * energy;
                model.rightArm.yRot = Mth.lerp(grip, -.86f, .41f) * energy;
                model.leftArm.yRot = -model.rightArm.yRot;
            }
            case FISSURE -> {
                model.leftLeg.xRot = GiantZombieCombat.plantedLegPitch(move, age);
                model.leftArm.zRot = .31f * energy;
            }
            case BOULDER -> {
                float lift=smooth(age/(move.warning-6));
                float release=smooth((age-(move.warning-6))/6);
                float follow=smooth((age-move.warning)/6);
                float throwing=Mth.lerp(release,Mth.lerp(lift,-.8f,-2.6f),-(float)Math.PI/2);
                model.rightArm.xRot=Mth.lerp(follow,throwing,-.35f)*(1-settle);
                model.rightArm.yRot=0;
                model.rightArm.zRot=0;
                model.leftArm.xRot = -.65f * energy;
                model.body.zRot = .12f * held * (1-release);
            }
            case SHOULDER -> {
                model.body.zRot = -.32f * energy;
                model.rightArm.xRot = -.52f * energy;
                model.leftArm.xRot = -1.46f * energy;
            }
            case BACK_KICK -> {
                float lift = smooth((age - (move.warning - 6)) / 6);
                model.rightLeg.xRot = 1.05f * lift * (1 - settle);
                model.rightLeg.yRot = Mth.lerp(Mth.clamp((age - move.warning) / 7, 0, 1), -1.12f, 1.12f) * energy;
                model.head.zRot = .17f * energy;
            }
            case KNEEL -> {
                float strikeTick = age - move.warning;
                float bend = GiantZombieCombat.kneelBend(strikeTick);
                float press = smooth((strikeTick + 1) / (GiantZombieCombat.KNEEL_CONTACT_TICK + 1));
                model.leftLeg.xRot = -bend;
                model.rightLeg.xRot = bend;
                model.leftLeg.yRot = model.rightLeg.yRot = 0;
                model.leftLeg.zRot = model.rightLeg.zRot = 0;
                model.leftArm.xRot = -.46f * bend;
                model.rightArm.xRot = Mth.lerp(press, -1.83f * bend, GiantZombieCombat.KNEEL_ARM_CONTACT) * (1 - settle);
                model.rightArm.yRot = 0;
                model.rightArm.zRot = 0;
                model.head.xRot += .16f * bend;
            }
            case HEAVE -> {
                // The first earth column erupts at release; finish the lift during the warning.
                float lift = smooth((age - (move.warning - 6)) / 6);
                model.rightArm.xRot = model.leftArm.xRot = Mth.lerp(lift, -.48f, -2.51f) * energy;
            }
            case COMBO -> {
                float strikeTick = age - move.warning;
                float rightWithdraw = 1 - smooth((strikeTick - GiantZombieCombat.COMBO_SWEEP_TICKS) / 6);
                float rightWeight = held * rightWithdraw;
                model.rightArm.xRot = Mth.lerp(smooth(strikeTick / 3), -1.95f, -1.30f) * rightWeight;
                model.rightArm.yRot = GiantZombieCombat.comboSweepYaw(strikeTick) * rightWeight;
                model.rightArm.zRot = .17f * rightWeight;

                float elbowTick = strikeTick - GiantZombieCombat.COMBO_ELBOW_START;
                float leftWeight = smooth((elbowTick + 10) / 8) * (1 - settle);
                float elbow = smooth((elbowTick + 2) / 2);
                model.leftArm.xRot = Mth.lerp(elbow, -.76f, -1.31f) * leftWeight;
                model.leftArm.yRot = Mth.lerp(elbow, 1.24f, GiantZombieCombat.COMBO_ELBOW_YAW) * leftWeight;
                model.leftArm.zRot = Mth.lerp(elbow, -.52f, -.15f) * leftWeight;
            }
        }
    }

    public static void giantScale(Giant entity, PoseStack pose, float partial) {
        if (!(entity instanceof SourceGiantZombieBoss giant)) return;
        float age = giant.actionAge() + partial;
        if (giant.action() == SourceGiantZombieBoss.EMERGE) {
            pose.translate(0, 2.02f * (1 - smooth(age / 53)), 0);
        } else if (giant.action() == GiantZombieCombat.Move.KNEEL.action()) {
            float bend = GiantZombieCombat.kneelBend(age - GiantZombieCombat.Move.KNEEL.warning);
            pose.translate(0, GiantZombieCombat.kneelDrop(bend) / 6, 0);
        } else if (giant.action() == SourceGiantZombieBoss.FALLEN) {
            pose.translate(0, GiantZombieCombat.fallenDrop(GiantZombieCombat.fallenProgress(age)) / 6, 0);
        }
    }

    private static float smooth(float value) {
        float x = Mth.clamp(value, 0, 1);
        return x * x * (3 - 2 * x);
    }
}
