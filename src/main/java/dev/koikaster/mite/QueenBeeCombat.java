package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class QueenBeeCombat {
    private static final double HONEY_GAP_COSINE = .77;
    enum Action { INTRO, REST, WINDUP, ACTIVE, RECOVERY }

    enum Move {
        REVERSAL(13, 34, 9), DIVE(16, 16, 12), SWEEP(11, 16, 8),
        STINGER_FAN(12, 17, 8), STINGER_BURST(10, 13, 7), STINGER_RAIN(17, 19, 10),
        HONEY_SPIT(12, 10, 7), GUARDS(15, 6, 8), WING_GUST(10, 9, 8),
        TAIL_STRIKE(9, 8, 10), HONEY_RING(15, 13, 11), STINGER_CROSS(13, 15, 8);

        final int windup;
        final int active;
        final int recovery;

        Move(int windup, int active, int recovery) {
            this.windup = windup;
            this.active = active;
            this.recovery = recovery;
        }
    }

    private record Plan(Vec3 floor, Vec3 aim, Vec3 forward, Vec3 side, Vec3 stage, Vec3 end) {}

    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private final QueenBeeHazards hazards = new QueenBeeHazards();
    private final Set<UUID> struck = new HashSet<>();
    private Action action = Action.REST;
    private Move move;
    private Plan plan;
    private UUID target;
    private Vec3 launch = Vec3.ZERO;
    private Vec3 turnOrigin = Vec3.ZERO;
    private Vec3 recoveryPoint = Vec3.ZERO;
    private int age, length, missedDashes, attacksSinceLowApproach;
    private boolean late, lowRecovery, connected;

    boolean wantsTarget() { return action == Action.REST; }
    boolean lowRecovery() { return action == Action.RECOVERY && lowRecovery; }
    UUID target() { return target; }

    boolean dangerous() {
        return action == Action.WINDUP || action == Action.ACTIVE || hazards.dangerous();
    }

    void suspend(ServerLevel level, SourceQueenBee bee) {
        if (action != Action.REST) enter(bee, Action.REST, 0);
        target = null;
        attacksSinceLowApproach = 0;
        hazards.clear();
        BossArenaPortals.release(level, bee.getUUID());
    }

    void stickyImpact(ServerLevel level, SourceQueenBee bee, Vec3 point) {
        hazards.honeyAt(level, bee, point, 1.25);
    }

    boolean tick(ServerLevel level, SourceQueenBee bee, ServerPlayer player, List<ServerPlayer> players) {
        hazards.tick(level, bee, players);
        ++age;
        switch (action) {
            case REST -> {
                if (age > length && player != null) return select(level, bee, player);
            }
            case WINDUP -> {
                if (players.stream().noneMatch(candidate -> candidate.getUUID().equals(target))) {
                    recover(level, bee, false);
                } else if (age >= length) {
                    launch = bee.position();
                    double stageError = launch.distanceToSqr(plan.stage);
                    if (move == Move.REVERSAL && stageError > 8.0 || move == Move.HONEY_RING && stageError > 1.0) {
                        recover(level, bee, false);
                    } else {
                        if (move == Move.REVERSAL || move == Move.DIVE || move == Move.HONEY_RING) hazards.clearStingers();
                        enter(bee, Action.ACTIVE, move.active);
                        releaseSound(level, bee);
                    }
                } else {
                    double speed = move == Move.REVERSAL || move == Move.DIVE ? .92 : .64;
                    bee.flyToward(plan.stage, speed, move == Move.SWEEP ? .33f : -.14f);
                    Vec3 toward = plan.aim.subtract(bee.position());
                    bee.face(move == Move.TAIL_STRIKE ? toward.scale(-1) : toward);
                    if (move == Move.HONEY_RING && age % 4 == 2) warnHoneyRing(level, bee);
                }
            }
            case ACTIVE -> {
                if (age > length || act(level, bee, players)) {
                    boolean opened = (move == Move.REVERSAL && missedDashes >= 2)
                        || (move == Move.DIVE && !connected)
                        || (move == Move.SWEEP && !connected && dodgedSweepInside(players));
                    recover(level, bee, opened);
                }
            }
            case RECOVERY -> {
                bee.flyToward(recoveryPoint, lowRecovery ? .80 : .34, lowRecovery ? .34f : .09f);
                if (age >= length) enter(bee, Action.REST, 2);
            }
            case INTRO -> throw new IllegalStateException("The encounter owns its opening flight");
        }
        return false;
    }

    private boolean select(ServerLevel level, SourceQueenBee bee, ServerPlayer player) {
        Vec3 focus = player.position();
        Vec3 floor = QueenBeeArena.floor(level, bee, focus.add(0, 1.4, 0), 5.0);
        Vec3 delta = focus.subtract(bee.position());
        double distance = delta.horizontalDistance();
        Vec3 forward = BossAttackGeometry.flatDirection(delta);
        if (forward.lengthSqr() < .001) forward = Vec3.directionFromRotation(0, player.getYRot());
        if (delta.lengthSqr() > 16 * 16 || !BossAttackGeometry.visible(level, bee, bee.getEyePosition(), player.getEyePosition())) {
            reposition(level, bee, focus, forward);
            return false;
        }
        late = bee.late();
        Vec3 side = new Vec3(-forward.z, 0, forward.x).scale(bee.getRandom().nextBoolean() ? 1 : -1);
        Vec3 aim = player.getBoundingBox().getCenter();
        boolean aboveFloor = floor == null || focus.y - floor.y > 2.5;
        List<Move> choices = order.candidates(bee.getRandom());
        if (aboveFloor || Math.abs(delta.y) > 3.4) {
            choices = new ArrayList<>(choices);
            choices.sort(Comparator.comparingInt(move -> move == Move.STINGER_BURST ? 0 : aimedStinger(move) ? 1 : 2));
        } else if (attacksSinceLowApproach >= 2) {
            choices = new ArrayList<>(choices);
            choices.sort(Comparator.comparing(move -> !opensGroundWindow(move)));
        }
        for (Move candidate : choices) {
            Vec3 attackAim = aim;
            if (aimedStinger(candidate)) {
                if (!eligible(candidate, delta.length(), 0)) continue;
            } else {
                if (floor == null || aboveFloor && candidate != Move.STINGER_RAIN && candidate != Move.GUARDS
                        || !eligible(candidate, distance, bee.getY() - floor.y)) continue;
                attackAim = floor.add(0, .9, 0);
            }
            Plan formed = plan(level, bee, candidate, floor, attackAim, forward, side);
            if (formed == null) continue;
            plan = formed;
            move = candidate;
            order.used(move);
            attacksSinceLowApproach = opensGroundWindow(move) ? 0 : Math.min(2, attacksSinceLowApproach + 1);
            target = player.getUUID();
            struck.clear();
            missedDashes = 0;
            lowRecovery = false;
            connected = false;
            enter(bee, Action.WINDUP, Math.max(8, move.windup - (late ? 1 : 0)));
            level.playSound(null, bee.blockPosition(), move == Move.TAIL_STRIKE ? SoundEvents.BEE_STING : SoundEvents.BEE_LOOP_AGGRESSIVE,
                SoundSource.HOSTILE, 1.10f, move == Move.DIVE ? .63f : .95f);
            return true;
        }
        reposition(level, bee, focus, forward);
        return false;
    }

    private static boolean opensGroundWindow(Move move) {
        return move == Move.REVERSAL || move == Move.DIVE;
    }

    private static boolean aimedStinger(Move move) {
        return move == Move.STINGER_FAN || move == Move.STINGER_BURST || move == Move.STINGER_CROSS;
    }

    private boolean eligible(Move candidate, double distance, double height) {
        return switch (candidate) {
            case REVERSAL -> distance <= 11.0 && height < 8.0;
            case DIVE -> distance <= 10.0 && height < 10.0;
            case SWEEP -> distance <= 8.0 && height < 6.0;
            case STINGER_FAN, STINGER_BURST -> distance >= 2.6;
            case STINGER_RAIN -> distance <= 12.5 && (late || distance >= 6);
            case HONEY_SPIT -> distance >= 3.4 && hazards.honeySpace();
            case GUARDS -> distance <= 14 && hazards.guardSpace();
            case WING_GUST -> distance <= 8.5 && height < 5;
            case TAIL_STRIKE -> distance <= 4.6 && height < 4.0;
            case HONEY_RING -> late && distance <= 7.5 && height < 5;
            case STINGER_CROSS -> late && distance >= 4.1;
        };
    }

    private Plan plan(ServerLevel level, SourceQueenBee bee, Move candidate, Vec3 floor, Vec3 aim, Vec3 forward, Vec3 side) {
        Vec3 stage = floor;
        Vec3 end = floor;
        switch (candidate) {
            case REVERSAL -> {
                stage = floor.add(side.scale(-6.1)).add(0, .54, 0);
                end = floor.add(side.scale(6.1)).add(0, .54, 0);
                if (bee.position().distanceTo(stage) > bee.position().distanceTo(end)) {
                    Vec3 swap = stage;
                    stage = end;
                    end = swap;
                    side = side.scale(-1);
                }
                Vec3 returnOffset = forward.scale(1.25);
                if (!QueenBeeArena.clearPath(level, bee, stage, end)
                        || !QueenBeeArena.clearPath(level, bee, end.add(returnOffset), stage.add(returnOffset))) return null;
            }
            case DIVE -> {
                stage = floor.subtract(forward.scale(4.2)).add(0, 6.0, 0);
                end = floor.add(forward.scale(1.25)).add(0, .42, 0);
                if (!QueenBeeArena.clearPath(level, bee, stage, end)) return null;
            }
            case SWEEP -> {
                stage = floor.subtract(forward.scale(4.9)).subtract(side.scale(1.6)).add(0, 1.05, 0);
                end = floor.add(forward.scale(2.0)).add(side.scale(4.5)).add(0, .82, 0);
            }
            case WING_GUST -> stage = floor.subtract(forward.scale(4.15)).add(0, 1.10, 0);
            case TAIL_STRIKE -> stage = floor.subtract(forward.scale(2.60)).add(0, .85, 0);
            case HONEY_RING -> {
                stage = floor.subtract(forward.scale(3.5)).add(0, .7, 0);
                end = stage.add(0, -.7, 0);
            }
            case STINGER_RAIN -> stage = floor.subtract(forward.scale(2.0)).add(0, 7.2, 0);
            case HONEY_SPIT, GUARDS -> stage = floor.subtract(forward.scale(5.0)).add(0, 3.0, 0);
            case STINGER_FAN, STINGER_BURST, STINGER_CROSS -> {
                stage = aim.subtract(forward.scale(6.6)).add(0, 2.1, 0);
                if (!stingerLane(level, bee, stage, aim) || !QueenBeeArena.clearPath(level, bee, bee.position(), stage)) {
                    stage = bee.position();
                    if (!stingerLane(level, bee, stage, aim)) return null;
                }
                return new Plan(null, aim, forward, side, stage, stage);
            }
        }
        if (!QueenBeeArena.clearPath(level, bee, bee.position(), stage)) return null;
        return new Plan(floor, aim, forward, side, stage, end);
    }

    private static boolean stingerLane(ServerLevel level, SourceQueenBee bee, Vec3 stage, Vec3 aim) {
        return BossAttackGeometry.visible(level, bee, stage.add(0, bee.getBbHeight() * .44, 0), aim);
    }

    private void reposition(ServerLevel level, SourceQueenBee bee, Vec3 focus, Vec3 direction) {
        Vec3 desired = focus.subtract(direction.scale(3.5)).add(0, 1.75, 0);
        Vec3 side = new Vec3(-direction.z, 0, direction.x);
        for (Vec3 option : List.of(desired, desired.add(side.scale(2.6)), desired.subtract(side.scale(2.6)),
                bee.position().add(0, 1.6, 0))) {
            Vec3 diff = option.subtract(bee.position());
            if (diff.lengthSqr() < .01) continue;
            Vec3 next = bee.position().add(diff.normalize().scale(Math.min(.56, diff.length())));
            if (!QueenBeeArena.clearPath(level, bee, bee.position(), next)) continue;
            bee.flyToward(next, .56, .11f);
            return;
        }
    }

    private boolean act(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        return switch (move) {
            case REVERSAL -> reversal(level, bee, players);
            case DIVE -> dive(level, bee, players);
            case SWEEP -> sweep(level, bee, players);
            case WING_GUST -> {
                if (age == 1 || age == 4 || age == 7) gust(level, bee, players);
                yield false;
            }
            case TAIL_STRIKE -> {
                if (age <= 3) tail(level, bee, players);
                yield false;
            }
            case HONEY_RING -> {
                honeyRing(level, bee, players);
                yield false;
            }
            case STINGER_FAN, STINGER_BURST, STINGER_RAIN, HONEY_SPIT, GUARDS, STINGER_CROSS -> {
                releaseHazards(level, bee);
                yield false;
            }
        };
    }

    private boolean reversal(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        if (age <= 14) {
            double speed = 1.0 + Math.min(5, age) * .15;
            dash(level, bee, players, plan.end, speed, -.45f, late ? 10.1f : 9.2f, late);
            if (age == 14) {
                if (struck.isEmpty()) ++missedDashes;
                struck.clear();
                turnOrigin = bee.position();
            }
        } else if (age <= 20) {
            Vec3 turn = turnOrigin.add(plan.forward.scale(1.25));
            bee.flyToward(turn, .34, .72f);
            bee.face(age < 18 ? plan.side : plan.side.scale(-1));
            if (age == 20) releaseSound(level, bee);
        } else {
            Vec3 end = plan.stage.add(plan.forward.scale(1.25));
            double speed = 1.08 + Math.min(5, age - 20) * .13;
            dash(level, bee, players, end, speed, .44f, late ? 10.5f : 9.6f, late);
            if (age == length && struck.isEmpty()) ++missedDashes;
        }
        return false;
    }

    private boolean dive(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        double progress = Math.min(1, age / 12.0);
        Vec3 point = launch.lerp(plan.end, progress).add(0, .7 * Math.sin(progress * Math.PI), 0);
        dash(level, bee, players, point, 1.28, -.54f, late ? 10.9f : 10.1f, late);
        if (age < 12) return false;
        if (bee.position().distanceToSqr(plan.end) < 2.1) {
            Vec3 center = bee.position().add(0, .21, 0);
            effectRing(level, center, 2.35, 13);
            for (ServerPlayer player : players) {
                if (player.position().subtract(center).horizontalDistanceSqr() <= 2.35 * 2.35
                        && Math.abs(player.getY() - center.y) < 1.15) hit(level, bee, player, center, 8.3f, .57, late);
            }
        }
        return true;
    }

    private boolean sweep(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        double progress = Math.min(1, age / 14.0);
        Vec3 point = launch.lerp(plan.end, progress).add(plan.side.scale(-2.65 * Math.sin(progress * Math.PI)));
        dash(level, bee, players, point, .95, .58f, 8.5f, false);
        return age >= 14;
    }

    private boolean dodgedSweepInside(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (!player.getUUID().equals(target)) continue;
            Vec3 offset = player.position().subtract(plan.floor);
            // The active curve bends away from plan.side. A short step toward it stays in counterattack range.
            return offset.dot(plan.side) >= 1.5 && offset.horizontalDistanceSqr() <= 16 && Math.abs(offset.y) <= 1.2;
        }
        return false;
    }

    private void dash(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players, Vec3 end, double speed, float bank,
                      float damage, boolean poison) {
        double middle = bee.getBbHeight() * .50;
        var bodyBefore = bee.getBoundingBox();
        Vec3 before = bee.position().add(0, middle, 0);
        Vec3 after = bee.flyToward(end, speed, bank).add(0, middle, 0);
        if (after.distanceToSqr(before) < .012) return;
        level.sendParticles(ParticleTypes.FALLING_HONEY, before.x, before.y, before.z, 4, .50, .22, .50, .075);
        if (age % 2 == 0) level.sendParticles(ParticleTypes.CLOUD, after.x, after.y, after.z, 4, .38, .20, .38, .06);
        Vec3 displacement = after.subtract(before);
        for (ServerPlayer player : players) {
            Vec3 contact = BossAttackGeometry.sweptBodyContact(bodyBefore, displacement, player.getBoundingBox());
            if (contact != null) hit(level, bee, player, contact, damage, .66, poison);
        }
    }

    private void gust(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        Vec3 origin = bee.position().add(0, bee.getBbHeight() * .46, 0);
        Vec3 forward = BossAttackGeometry.flatDirection(plan.aim.subtract(origin));
        double reach = age == 1 ? 2.9 : age == 4 ? 4.9 : 7.2;
        for (int index = -4; index <= 4; ++index) {
            Vec3 point = origin.add(forward.yRot(index * .128f).scale(reach));
            Vec3 clipped = BossAttackGeometry.shorten(level, bee, origin, point);
            level.sendParticles(ParticleTypes.CLOUD, clipped.x, clipped.y, clipped.z, 3, .19, .17, .19, .026);
        }
        for (ServerPlayer player : players) {
            Vec3 diff = player.position().add(0, .7, 0).subtract(origin);
            if (Math.abs(diff.y) <= 1.55 && diff.horizontalDistanceSqr() <= reach * reach
                    && BossAttackGeometry.flatDirection(diff).dot(forward) > .82) {
                hit(level, bee, player, origin, 6.9f, .87, false);
            }
        }
    }

    private void tail(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        Vec3 center = bee.position().add(0, bee.getBbHeight() * .46, 0);
        Vec3 forward = plan.aim.subtract(center).normalize();
        Vec3 origin = BossAttackGeometry.shorten(level, bee, center, center.add(forward.scale(bee.getBbWidth() * .38)));
        Vec3 end = BossAttackGeometry.shorten(level, bee, origin, origin.add(forward.scale(2.85)));
        if (age == 1) {
            level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 11, .24, .24, .24, .07);
            level.playSound(null, bee.blockPosition(), SoundEvents.BEE_STING, SoundSource.HOSTILE, 1.25f, .78f);
        }
        for (ServerPlayer player : players) {
            if (BossAttackGeometry.hits(player.getBoundingBox(), origin, end, .58)) {
                hit(level, bee, player, origin, late ? 11.0f : 8.0f, .44, late);
            }
        }
    }

    private void warnHoneyRing(ServerLevel level, SourceQueenBee bee) {
        Vec3 origin = plan.end.add(0, .25, 0);
        var contacts = new ArrayList<Vec3>(32);
        for (int index = 0; index < 32; ++index) {
            Vec3 radial = BossAttackGeometry.flatDirection(index * Math.PI / 16);
            if (inHoneyGap(radial, plan.side)) continue;
            Vec3 pulse = honeyPulse(level, bee, origin, radial, 3.25);
            if (pulse == null) continue;
            contacts.add(pulse.add(0, .22, 0));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HONEY_BLOCK.defaultBlockState()),
                pulse.x, pulse.y + .08, pulse.z, 2, 0, .10, 0, .012);
            level.sendParticles(ParticleTypes.FALLING_HONEY, pulse.x, pulse.y + .42, pulse.z, 2, 0, .10, 0, .01);
        }
        BossCombatVisual.honeyRing(level, bee, origin, contacts, true);
    }

    private void honeyRing(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        if (age > 11) return;
        Vec3 origin = plan.end.add(0, .25, 0);
        double range = .75 + age * .62;
        Vec3 gap = plan.side;
        var contacts = new ArrayList<Vec3>(16);
        for (int i = 0; i < 16; ++i) {
            Vec3 radial = BossAttackGeometry.flatDirection(i * Math.PI / 8);
            if (inHoneyGap(radial, gap)) continue;
            Vec3 pulse = honeyPulse(level, bee, origin, radial, range);
            if (pulse == null) continue;
            contacts.add(pulse.add(0, .12, 0));
            double spread = radial.dot(gap) > .62 ? 0 : .21;
            level.sendParticles(ParticleTypes.FALLING_HONEY, pulse.x, pulse.y, pulse.z, 4, spread, .16, spread, .03);
            if (age % 2 == 1) level.sendParticles(ParticleTypes.CLOUD, pulse.x, pulse.y, pulse.z, 3, spread, .10, spread, .035);
            for (ServerPlayer player : players) {
                Vec3 offset = player.position().subtract(pulse);
                if (offset.horizontalDistanceSqr() < .81 && offset.y < .77 && offset.y > -.9
                        && !inHoneyGap(player.position().subtract(origin), gap)) {
                    hit(level, bee, player, origin, 10.4f, .57, true);
                }
            }
        }
        BossCombatVisual.honeyRing(level, bee, origin, contacts, false);
    }

    private static boolean inHoneyGap(Vec3 offset, Vec3 gap) {
        return BossAttackGeometry.flatDirection(offset).dot(gap) > HONEY_GAP_COSINE;
    }

    private static Vec3 honeyPulse(ServerLevel level, SourceQueenBee bee, Vec3 origin, Vec3 radial, double range) {
        Vec3 sample = origin.add(radial.scale(range));
        if (!BossAttackGeometry.visible(level, bee, origin, sample)) return null;
        Vec3 floor = QueenBeeArena.floor(level, bee, sample.add(0, .55, 0), 2.0);
        if (floor == null) return null;
        // Keep the honey ring on ground connected to the release point.
        int segments = (int)Math.ceil(range * 2);
        for (int i = 0; i < segments; ++i) {
            Vec3 path = origin.lerp(sample, i / (double) segments);
            if (QueenBeeArena.floor(level, bee, path.add(0, .55, 0), 2.0) == null) return null;
        }
        return floor.add(0, .18, 0);
    }

    private void releaseHazards(ServerLevel level, SourceQueenBee bee) {
        if (aimedStinger(move)) bee.face(plan.aim.subtract(bee.position()));
        Vec3 origin = bee.position().add(0, bee.getBbHeight() * .44, 0);
        switch (move) {
            case STINGER_FAN -> {
                if (age == 1 || age == 7 || age == 13) hazards.fan(level, bee, origin, plan.aim, (age - 7) / 6, late);
            }
            case STINGER_BURST -> {
                if (age == 1 || age == 5 || age == 9) {
                    Vec3 shot = plan.aim.add(plan.side.scale((age - 5) * .19));
                    hazards.stinger(level, bee, origin, shot, 1.03, late, 7.1f);
                }
            }
            case STINGER_RAIN -> {
                if (age == 1 || age == 8 || age == 15) hazards.rain(level, bee, plan.floor, plan.forward, plan.side, (age - 8) / 7, late);
            }
            case HONEY_SPIT -> {
                if (age == 1 || age == 6) {
                    Vec3 point = plan.floor.add(plan.side.scale(age == 1 ? -1.45 : 1.45));
                    hazards.lobHoney(level, bee, origin, point);
                }
            }
            case GUARDS -> {
                if (age == 1) hazards.guards(level, bee, origin, plan.forward, plan.side, target);
            }
            case STINGER_CROSS -> {
                if (age == 1 || age == 8) hazards.cross(level, bee, origin, plan.aim, plan.side, age == 1 ? 1 : -1, late);
            }
            default -> throw new IllegalStateException("No projectile action for " + move);
        }
    }

    private boolean hit(ServerLevel level, SourceQueenBee bee, ServerPlayer player, Vec3 origin, float damage, double push, boolean poison) {
        if (struck.contains(player.getUUID()) || !BossAttackGeometry.visible(level, bee, origin, player.getBoundingBox().getCenter())
                || !player.hurt(level.damageSources().mobAttack(bee), damage)) return false;
        struck.add(player.getUUID());
        connected = true;
        if (poison) player.addEffect(new MobEffectInstance(MobEffects.POISON, BossCombatTuning.queenPoisonDuration(48)), bee);
        Vec3 inward = BossAttackGeometry.flatDirection(origin.subtract(player.position()));
        if (push > 0) player.knockback(push, inward.x, inward.z);
        return true;
    }

    private void recover(ServerLevel level, SourceQueenBee bee, boolean opened) {
        lowRecovery = opened || move == Move.TAIL_STRIKE;
        Vec3 floor = QueenBeeArena.floor(level, bee, bee.position().add(0, .7, 0), 10.0);
        Vec3 low = floor == null ? bee.position() : floor.add(0, lowRecovery ? .42 : 1.18, 0);
        if (opened) {
            var dodger = target == null ? null : level.getServer().getPlayerList().getPlayer(target);
            if (dodger != null && dodger.level() == level && bee.canTarget(dodger)) {
                Vec3 accessible = accessibleRecovery(level, bee, dodger);
                if (accessible != null) low = accessible;
            }
        }
        recoveryPoint = low;
        int descent = opened ? Math.min(21, (int)Math.ceil(bee.position().distanceTo(low) / .80)) : 0;
        int counterattackTicks = move == Move.SWEEP ? 12 : 25;
        enter(bee, Action.RECOVERY, opened ? counterattackTicks + descent : move.recovery);
        if (opened) level.playSound(null, bee.blockPosition(), SoundEvents.BEE_HURT, SoundSource.HOSTILE, 1.2f, .65f);
    }

    private static Vec3 accessibleRecovery(ServerLevel level, SourceQueenBee bee, ServerPlayer dodger) {
        Vec3 anchor = dodger.position();
        Vec3 away = BossAttackGeometry.flatDirection(bee.position().subtract(anchor));
        if (away.lengthSqr() < .001) away = Vec3.directionFromRotation(0, dodger.getYRot());
        double clearance = (bee.getBbWidth() + dodger.getBbWidth()) * .5 + .15;
        for (int turn : new int[]{0, 1, -1, 2, -2, 3, -3, 4}) {
            Vec3 direction = away.yRot(turn * (float)(Math.PI / 4));
            // Separate the two collision boxes along their largest horizontal axis.
            double distance = clearance / Math.max(Math.abs(direction.x), Math.abs(direction.z));
            Vec3 near = anchor.add(direction.scale(distance));
            Vec3 support = QueenBeeArena.floor(level, bee, near.add(0, 1.2, 0), 2.4);
            if (support == null || Math.abs(support.y - anchor.y) > .65) continue;
            Vec3 accessible = support.add(0, .42, 0);
            if (BossAttackGeometry.visible(level, dodger, dodger.getEyePosition(), accessible.add(0, 1.15, 0))
                    && QueenBeeArena.clearPath(level, bee, bee.position(), accessible)) return accessible;
        }
        return null;
    }

    private void enter(SourceQueenBee bee, Action next, int duration) {
        action = next;
        length = duration;
        age = 0;
        bee.pose(next == Action.REST ? null : move, next, duration);
    }

    private void releaseSound(ServerLevel level, SourceQueenBee bee) {
        boolean heavy = move == Move.REVERSAL || move == Move.DIVE || move == Move.HONEY_RING;
        level.playSound(null, bee.blockPosition(), heavy ? SoundEvents.BEE_LOOP_AGGRESSIVE : SoundEvents.BEE_STING,
            SoundSource.HOSTILE, heavy ? 1.7f : 1.15f, heavy ? .63f : 1.25f);
    }

    private static void effectRing(ServerLevel level, Vec3 origin, double radius, int samples) {
        for (int i = 0; i < samples; ++i) {
            Vec3 point = origin.add(BossAttackGeometry.flatDirection(i * 2 * Math.PI / samples).scale(radius));
            level.sendParticles(ParticleTypes.LANDING_HONEY, point.x, point.y, point.z, 3, .14, .13, .14, .035);
            level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 2, .19, .10, .19, .046);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void ironStagePressure(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer && (event.getSource().getEntity() instanceof SourceQueenBee
                || event.getSource().getEntity() instanceof QueenBeeGuard)) event.setAmount(event.getAmount() * .76f);
    }
}
