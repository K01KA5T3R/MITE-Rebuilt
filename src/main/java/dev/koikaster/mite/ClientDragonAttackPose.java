package dev.koikaster.mite;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public final class ClientDragonAttackPose {
    private ClientDragonAttackPose() {}

    public static float jaw(float natural, EnderDragon entity, float partial) {
        if (!(entity instanceof SourceEnderDragon dragon)) return natural;
        int posture = dragon.ritualPosture();
        double age = dragon.ritualMoveAge() + partial;
        return switch (posture) {
            case DragonCombat.WARNING -> Mth.lerp(ease(age / 7), natural, .43f);
            case DragonCombat.DIVE -> Mth.lerp(ease((age - DragonCombat.DESCENT_TICKS) / 11), .72f, .20f);
            case DragonBreathAttack.TRACK -> Mth.lerp(ease(age / DragonBreathAttack.TRACK_TICKS), natural, .38f);
            case DragonBreathAttack.LOCK -> Mth.lerp(ease(age / DragonBreathAttack.LOCK_TICKS), .38f, .71f);
            case DragonBreathAttack.RECOVERY -> Mth.lerp(ease(age / 10), .71f, natural);
            case DragonAltarAttack.CHARGE -> Mth.lerp(ease(age / 16), natural, .64f);
            default -> {
                var move = DragonRepertoire.Move.fromPosture(posture);
                if (move == DragonRepertoire.Move.PECK) {
                    yield age < move.warning ? Mth.lerp(ease(age / move.warning), natural, .92f)
                        : Mth.lerp(ease((age - move.warning) / 5), .92f, .07f);
                }
                if (move == DragonRepertoire.Move.BURST) {
                    yield age < move.warning ? Mth.lerp(ease(age / move.warning), natural, .70f)
                        : Mth.lerp(ease((age - move.warning) / 10), .70f, natural);
                }
                if (move == DragonRepertoire.Move.EMBERS) {
                    yield age < move.warning ? Mth.lerp(ease(age / move.warning), natural, .56f)
                        : Mth.lerp(ease((age - move.warning) / 16), .82f, natural);
                }
                yield natural;
            }
        };
    }

    private static float ease(double value) {
        double t = Mth.clamp(value, 0, 1);
        return (float)(t * t * (3 - 2 * t));
    }
}
