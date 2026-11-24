package dev.koikaster.mite;

import net.minecraft.client.model.GuardianModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Guardian;

public final class ClientElderGuardianPose {
    private ClientElderGuardianPose() {}

    public static void pose(GuardianModel model, Guardian entity, float animation) {
        if (!(entity instanceof SourceElderGuardianBoss boss) || !model.root().hasChild("head")) return;
        var head = model.root().getChild("head");
        head.zRot = 0;
        var move = ElderGuardianCombat.Move.fromAction(boss.action());
        float age = boss.actionAge() + Mth.clamp(animation - boss.tickCount, 0, 1);
        if (move == null) return;
        float ready = smooth(age / move.warning);
        float release = smooth((age - move.warning) / 8);
        float strength = ready * (1 - smooth((age - move.warning - move.strike) / move.recovery));
        switch (move) {
            case BEAMS -> head.zRot = (float)Math.sin(age * .18f) * .13f * strength;
            case FOAM, ECHO -> head.xRot += (float)Math.sin(age * .23f) * .12f * strength;
            case VORTEX -> head.zRot = .36f * strength * (1 - release);
            case DART -> head.zRot = -.72f * strength * (1 - release * .72f);
            case BUBBLES -> head.xRot += .46f * strength * (1 - release);
            case DOWNFLOW -> head.xRot += .57f * strength;
            case PRISM -> head.zRot = -.28f * strength;
            case SPINES -> head.xRot -= .32f * strength * (1 - release);
            case CURTAIN -> head.zRot = .36f * strength;
            case FOCUS -> head.xRot += .18f * strength;
            case AMBUSH -> {
                head.zRot = -.45f * strength;
                head.xRot -= .57f * strength * release;
            }
        }
    }

    private static float smooth(float value) {
        float x = Mth.clamp(value, 0, 1);
        return x * x * (3 - 2 * x);
    }
}
