package dev.koikaster.mite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class MythrilKnightCombat {
    enum Move {
        CHARGE(9, 31, 15), HEADBUTT(6, 7, 11), HOOK(7, 8, 11), KICK(8, 9, 12),
        STOMP(9, 14, 15), SIDESTEP(7, 15, 12), VOLLEY(8, 18, 10),
        BASH(6, 7, 10), COUNTERSHOT(10, 16, 12), AIMED_SHOT(11, 12, 10),
        BRAKE(6, 7, 9), RIDGE_SHOT(9, 47, 12), LOWER_SWEEP(8, 9, 13);

        final int windup;
        final int activeUntil;
        final int recovery;

        Move(int windup, int activeUntil, int recovery) {
            this.windup = windup;
            this.activeUntil = activeUntil;
            this.recovery = recovery;
        }

        boolean needsMountedRider() {
            return this == VOLLEY;
        }
    }

    private record LowerSweep(Vec3 origin, Vec3 start, Vec3 lip, Vec3 end) {}

    private record Opening(Vec3 body, Vec3 aim, int side, Path path, LowerSweep lowerSweep) {
        Opening(Vec3 body, Vec3 aim, int side) {
            this(body, aim, side, null);
        }

        Opening(Vec3 body, Vec3 aim, int side, Path path) {
            this(body, aim, side, path, null);
        }
    }

    private static final class Attack {
        final Move move;
        final UUID target;
        final Vec3 body;
        final Vec3 aim;
        final int side;
        final long start;
        final Set<UUID> hit = new HashSet<>();
        Vec3 beforeStep;
        final Path path;
        final LowerSweep lowerSweep;
        int ridgeRelease = -1;
        boolean armed;
        boolean sidestepBlocked;
        Vec3 recoveryStep;

        Attack(Move move, ServerPlayer target, Opening opening, long start) {
            this.move = move;
            this.target = target.getUUID();
            body = opening.body();
            aim = opening.aim();
            side = opening.side();
            path = opening.path();
            lowerSweep = opening.lowerSweep();
            this.start = start;
        }
    }

    private static final int COUNTER_WINDOW = 5;
    private static final int SIDESTEP_LATERAL_TICKS = 4;
    private static final double SIDESTEP_LATERAL_CLEARANCE = 1.4;
    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private Attack attack;
    private long reconsiderAt = Long.MIN_VALUE;
    private final TargetMotion targetMotion = new TargetMotion();

    private static final class TargetMotion {
        private UUID player;
        private ServerLevel level;
        private Vec3 feet = Vec3.ZERO;
        private Vec3 previousStep = Vec3.ZERO;
        private long lastTick = Long.MIN_VALUE;

        Vec3 sample(ServerPlayer target, long now) {
            Vec3 position = target.position();
            if (now == lastTick && target.getUUID().equals(player) && target.serverLevel() == level) return Vec3.ZERO;
            long elapsed = now - lastTick;
            Vec3 step = Vec3.ZERO;
            Vec3 measured = Vec3.ZERO;
            if (target.getUUID().equals(player) && target.serverLevel() == level && elapsed > 0 && elapsed <= 2
                    && target.onGround() && !target.isPassenger() && !target.isInWaterOrBubble()) {
                var difference = position.subtract(feet);
                step = difference.multiply(1.0 / elapsed, 0, 1.0 / elapsed);
                // Ignore portal jumps, large corrections and single-frame changes in direction.
                if (step.lengthSqr() > .8 * .8 || Math.abs(difference.y) > 1.1) step = Vec3.ZERO;
                if (step.dot(previousStep) > .04 * .04) measured = step;
            }
            player = target.getUUID();
            level = target.serverLevel();
            feet = position;
            previousStep = step;
            lastTick = now;
            return measured;
        }

        void clear() {
            player = null;
            level = null;
            previousStep = Vec3.ZERO;
        }
    }

    boolean attacking() { return attack != null; }

    void tick(ServerLevel level, Mob actor, MythrilPillager mountedRider, ServerPlayer target,
              List<ServerPlayer> fighters) {
        long now = level.getGameTime();
        Vec3 targetVelocity = actor instanceof MythrilRavager ? targetMotion.sample(target, now) : Vec3.ZERO;
        if (attack == null) {
            if (reconsiderAt == Long.MIN_VALUE) reconsiderAt = now + 4;
            if (now < reconsiderAt) return;
            reconsiderAt = now + 4;
            for (Move move : order.candidates(actor.getRandom())) {
                Opening opening = opening(level, actor, mountedRider, target, move, targetVelocity);
                if (opening == null) continue;
                order.used(move);
                start(level, actor, mountedRider, target, move, opening, now);
                return;
            }
            approach(level, actor, target);
            return;
        }

        Attack current = attack;
        if (!(level.getPlayerByUUID(current.target) instanceof ServerPlayer locked)
                || !MythrilKnightEncounter.isFighter(level, actor, locked)
                || current.move.needsMountedRider() && mountedRider == null) {
            cancel(level, actor, mountedRider);
            reconsiderAt = now + 3;
            return;
        }
        if (actor.getTarget() != locked) {
            actor.setTarget(locked);
            if (actor.getTarget() != locked || !MythrilKnightEncounter.isFighter(level, actor, locked)) {
                cancel(level, actor, mountedRider);
                reconsiderAt = now + 3;
                return;
            }
        }
        if (current.move == Move.VOLLEY || current.move == Move.BRAKE) {
            if (mountedRider != null) aimAt(mountedRider, locked);
        }

        int age = (int)(now - current.start);
        Move move = current.move;
        if (age >= move.activeUntil + move.recovery) {
            finish(level, actor, mountedRider, now);
            return;
        }
        if (age < move.activeUntil && (move != Move.BRAKE || current.armed)) {
            BossArenaPortals.holdFollowing(level, actor, 32, 16);
        } else if (age == move.activeUntil) {
            BossArenaPortals.release(level, actor.getUUID());
        }
        if (age >= move.activeUntil + COUNTER_WINDOW) {
            recover(level, actor, locked, current, age - move.activeUntil - COUNTER_WINDOW);
            return;
        }
        if (move == Move.RIDGE_SHOT && age < move.activeUntil) {
            tickRidge(level, (MythrilPillager) actor, locked, current, now, age);
            return;
        }
        stop(actor);
        if (move == Move.AIMED_SHOT || move == Move.COUNTERSHOT && age >= 5 || move == Move.RIDGE_SHOT) {
            aimAt(actor, locked);
        } else {
            MythrilKnightEncounter.face(actor, current.body);
        }

        if (move == Move.CHARGE || move == Move.SIDESTEP) {
            if (age >= move.windup) tickRush(level, (MythrilRavager)actor, mountedRider, locked, fighters, now, age);
        } else if (age < move.activeUntil) {
            switch (move) {
                case HEADBUTT, HOOK, KICK, BASH -> {
                    if (age == move.windup) strike(level, actor, current, fighters);
                }
                case STOMP -> {
                    if (age >= move.windup) stomp(level, actor, current, fighters, age - move.windup);
                }
                case LOWER_SWEEP -> {
                    if (age == move.windup) lowerSweep(level, actor, current, fighters);
                }
                case BRAKE, VOLLEY -> tickMountedBow(level, actor, mountedRider, locked, current, age);
                case AIMED_SHOT, COUNTERSHOT -> tickFootBow(level, (MythrilPillager)actor, locked, current, age);
                default -> { }
            }
        }
    }

    private void start(ServerLevel level, Mob actor, MythrilPillager rider, ServerPlayer target, Move move,
                       Opening opening, long now) {
        attack = new Attack(move, target, opening, now);
        stop(actor);
        MythrilKnightEncounter.face(actor, opening.body());
        if (actor instanceof MythrilRavager mount) mount.showAttack(move, opening.side(), now);
        BossArenaPortals.holdFollowing(level, actor, 32, 16);
        switch (move) {
            case CHARGE -> actor.playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.05f);
            case HEADBUTT, HOOK, KICK, SIDESTEP, STOMP, LOWER_SWEEP -> actor.playSound(SoundEvents.RAVAGER_STEP, 1.1f, .85f);
            case VOLLEY, BRAKE -> {
                if (rider != null && rider.hasLineOfSight(target)) {
                    aimAt(rider, target);
                    attack.armed = draw(rider);
                }
                if (move == Move.BRAKE && !attack.armed) BossArenaPortals.release(level, actor.getUUID());
            }
            case AIMED_SHOT -> attack.armed = draw((MythrilPillager)actor);
            case BASH -> actor.setAggressive(true);
            case COUNTERSHOT -> attack.beforeStep = actor.position();
            case RIDGE_SHOT -> {
                ((SourceArachnidPathBudget) actor.getNavigation()).mite$sourceCommitPath(attack.path);
                if (!actor.getNavigation().moveTo(attack.path, 1.18)) finish(level, actor, rider, now);
            }
        }
    }

    void cancel(ServerLevel level, Mob actor, MythrilPillager mountedRider) {
        resetAttack(level, actor, mountedRider);
        targetMotion.clear();
    }

    private void resetAttack(ServerLevel level, Mob actor, MythrilPillager mountedRider) {
        if (attack != null) {
            stop(actor);
            attack = null;
        }
        if (actor instanceof MythrilRavager mount) mount.clearAttackPose();
        if (actor instanceof MythrilPillager rider) clearRider(rider);
        if (mountedRider != null) clearRider(mountedRider);
        BossArenaPortals.release(level, actor.getUUID());
        reconsiderAt = Long.MIN_VALUE;
    }

    private void finish(ServerLevel level, Mob actor, MythrilPillager rider, long now) {
        resetAttack(level, actor, rider);
        reconsiderAt = now + 3;
    }

    private static void recover(ServerLevel level, Mob actor, ServerPlayer target, Attack attack, int tick) {
        stop(actor);
        actor.getLookControl().setLookAt(target, 22, 18);
        if (tick >= BossRecoveryStep.DURATION || !actor.hasLineOfSight(target)) {
            attack.recoveryStep = Vec3.ZERO;
            return;
        }
        if (attack.recoveryStep == null) {
            boolean mounted = actor instanceof MythrilRavager;
            attack.recoveryStep = BossRecoveryStep.plan(level, actor, target, attack.side,
                mounted ? .22 : .18, mounted ? .28 : -.20, 6.5);
        }
        if (!BossRecoveryStep.move(level, actor, attack.recoveryStep)) attack.recoveryStep = Vec3.ZERO;
    }

    private static void approach(ServerLevel level, Mob actor, ServerPlayer target) {
        if (actor.distanceToSqr(target) > 2.45 * 2.45 || !actor.hasLineOfSight(target)) {
            actor.getNavigation().moveTo(target, 1.08);
        } else {
            stop(actor);
            int side = (actor.getId() & 1) == 0 ? 1 : -1;
            Vec3 step = BossRecoveryStep.plan(level, actor, target, side, .18, -.16, 2.45);
            BossRecoveryStep.move(level, actor, step);
        }
        actor.getLookControl().setLookAt(target, 22, 18);
    }

    private static void stop(Mob actor) {
        actor.getNavigation().stop();
        actor.setXxa(0);
        actor.setZza(0);
        actor.setDeltaMovement(actor.getDeltaMovement().multiply(.16, 1, .16));
    }

    private static Opening opening(ServerLevel level, Mob actor, MythrilPillager rider, ServerPlayer target,
                                   Move move, Vec3 targetVelocity) {
        Vec3 delta = target.position().subtract(actor.position());
        Vec3 towards = BossAttackGeometry.flatDirection(delta);
        if (towards.lengthSqr() < .5 || !actor.onGround()) return null;
        Vec3 body = Vec3.directionFromRotation(0, actor.yBodyRot);
        Vec3 side = sideOf(body);
        int flank = towards.dot(side) >= 0 ? 1 : -1;
        double distance = delta.horizontalDistance();
        double front = towards.dot(body);
        double height = Math.abs(delta.y);
        boolean mount = actor instanceof MythrilRavager;
        if (!mount && actor.isPassenger()) return null;
        boolean visible = actor.hasLineOfSight(target);
        if (move == Move.RIDGE_SHOT ? visible : !visible) return null;

        return switch (move) {
            case CHARGE -> mount && distance >= 4.5 && distance <= 15 && height < 1.65
                    && travelClear(level, actor, towards, 1.8) && lateralExit(level, target, towards, 2.4)
                    ? new Opening(towards, towards, 1) : null;
            case HEADBUTT -> mount && height < 1.25 && distance <= 3.5 && front > .45
                    ? new Opening(towards, towards, 1) : null;
            case HOOK -> mount && height < 1.25 && distance <= 3.5 && Math.abs(front) < .57
                    ? new Opening(body, side.scale(flank), flank) : null;
            case KICK -> mount && height < 1.1 && distance <= 3.3 && front < -.52
                    ? new Opening(body, body.reverse(), flank) : null;
            case STOMP -> mount && height < .6 && distance < 3.6
                    && level.noCollision(target, target.getBoundingBox().move(0, .65, 0))
                    ? new Opening(towards, towards, 1) : null;
            case SIDESTEP -> {
                Vec3 across = sideOf(towards);
                double lateralSpeed = targetVelocity.dot(across);
                int drift = lateralSpeed >= 0 ? 1 : -1;
                Vec3 step = across.scale(drift);
                Vec3 travel = towards.scale(.68).add(step.scale(.73)).normalize();
                yield mount && height < 1 && distance >= 3.25 && distance < 6 && Math.abs(lateralSpeed) > .11
                        && travelClear(level, actor, step, SIDESTEP_LATERAL_CLEARANCE)
                        && travelClear(level, actor, step.scale(SIDESTEP_LATERAL_CLEARANCE), travel, 1.65)
                        && lateralExit(level, target, travel, 1.8) ? new Opening(towards, travel, drift) : null;
            }
            case VOLLEY -> mount && rider != null && distance >= 4.5 && distance <= 19 && height < 8
                    && rider.hasLineOfSight(target) && lateralExit(level, target, towards, 1.4)
                    ? new Opening(towards, towards, 1) : null;
            case BASH -> !mount && height < 1.05 && distance < 2.65
                    ? new Opening(towards, towards, 1) : null;
            case COUNTERSHOT -> !mount && height < 1.2 && distance >= 2.1 && distance <= 6.2
                    && travelClear(level, actor, towards.reverse(), 1.55)
                    ? new Opening(towards, towards.reverse(), 1) : null;
            case AIMED_SHOT -> !mount && distance >= 4.0 && distance <= 19 && height < 9
                    ? new Opening(towards, towards, 1) : null;
            case RIDGE_SHOT -> !mount && height >= .8 && height < 4.5 && distance >= 4 && distance <= 12
                    ? ridgeOpening(level, (MythrilPillager) actor, target, towards) : null;
            case LOWER_SWEEP -> mount && front > .25
                    ? lowerSweepOpening(level, actor, target, towards, delta) : null;
            case BRAKE -> null;
        };
    }

    private static Opening lowerSweepOpening(ServerLevel level, Mob actor, ServerPlayer target, Vec3 heading, Vec3 delta) {
        double distance = delta.horizontalDistance();
        if (!lowerSweepHeight(-delta.y) || distance < 2.3 || distance > 3.85
                || !target.onGround() || target.isPassenger() || level.containsAnyLiquid(target.getBoundingBox())) {
            return null;
        }
        Vec3 across = sideOf(heading);
        if (!travelClear(level, target, across, 1.15) && !travelClear(level, target, across.reverse(), 1.15)) {
            return null;
        }

        Vec3 origin = actor.position();
        Vec3 targetFloor = BossAttackGeometry.ground(level, actor, target.position().add(0, .18, 0), .42);
        if (targetFloor == null || Math.abs(targetFloor.y - target.getY()) > .16) return null;

        double edge = -1;
        for (double reach = .55; reach <= Math.min(2.3, distance - .62); reach += .25) {
            Vec3 probe = origin.add(heading.scale(reach)).add(0, .20, 0);
            var column = BlockPos.containing(probe);
            if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)
                    || !level.getFluidState(column).isEmpty() || !level.getFluidState(column.below()).isEmpty()) {
                return null;
            }
            if (BossAttackGeometry.ground(level, actor, probe, .69) == null) {
                edge = reach;
                break;
            }
        }
        if (edge < .80) return null;

        Vec3 start = origin.add(heading.scale(.65)).add(0, 1.25, 0);
        Vec3 lip = origin.add(heading.scale(edge + .15)).add(0, .70, 0);
        Vec3 end = target.position().add(heading.scale(.23)).add(0, target.getBbHeight() * .43, 0);
        if (!BossAttackGeometry.visible(level, actor, start, lip)
                || !BossAttackGeometry.visible(level, actor, lip, end)) return null;
        return new Opening(heading, heading, 1, null, new LowerSweep(origin, start, lip, end));
    }

    private static void lowerSweep(ServerLevel level, Mob actor, Attack attack, List<ServerPlayer> fighters) {
        LowerSweep sweep = attack.lowerSweep;
        if (!actor.onGround() || actor.position().distanceToSqr(sweep.origin()) > .35 * .35
                || BossAttackGeometry.ground(level, actor, actor.position().add(0, .15, 0), .46) == null
                || !BossAttackGeometry.visible(level, actor, sweep.start(), sweep.lip())) return;
        actor.playSound(SoundEvents.RAVAGER_ATTACK, 1, .89f);
        Vec3 end = BossAttackGeometry.shorten(level, actor, sweep.lip(), sweep.end());
        if (sweep.lip().distanceToSqr(end) < .12 * .12) return;
        for (ServerPlayer player : fighters) {
            if (lowerSweepHeight(sweep.origin().y - player.getY())
                    && BossAttackGeometry.hits(player.getBoundingBox(), sweep.lip(), end, .25)
                    && BossAttackGeometry.visible(level, actor, sweep.lip(), player.getBoundingBox().getCenter())) {
                hit(level, actor, player, attack, 9);
            }
        }
    }

    private static boolean lowerSweepHeight(double drop) {
        return drop >= .95 && drop <= 2.40;
    }

    private void tickRush(ServerLevel level, MythrilRavager mount, MythrilPillager rider, ServerPlayer target,
                          List<ServerPlayer> fighters, long now, int age) {
        Attack current = attack;
        if (current.beforeStep != null) {
            Vec3 displacement = mount.position().subtract(current.beforeStep);
            AABB original = mount.getBoundingBox().move(displacement.reverse());
            if (displacement.horizontalDistanceSqr() > .001) {
                for (ServerPlayer player : fighters) {
                    if (!current.hit.contains(player.getUUID()) && mount.hasLineOfSight(player)
                            && BossAttackGeometry.sweptBodyContact(original, displacement, player.getBoundingBox()) != null) {
                        hit(level, mount, player, current, current.move == Move.CHARGE ? 11 : 8);
                    }
                }
            }
        }
        boolean sideStep = current.move == Move.SIDESTEP;
        if (sideStep && current.sidestepBlocked) return;
        Vec3 path = sideStep && age - current.move.windup < SIDESTEP_LATERAL_TICKS
            ? sideOf(current.body).scale(current.side) : current.aim;
        if (age >= current.move.activeUntil || !mount.onGround() || !travelClear(level, mount, path, .7)) {
            if (current.move == Move.CHARGE) {
                start(level, mount, rider, target, Move.BRAKE, new Opening(current.body, current.aim, 1), now);
            } else {
                current.beforeStep = null;
                current.sidestepBlocked = true;
            }
            return;
        }
        current.beforeStep = mount.position();
        double speed = current.move == Move.CHARGE ? .64 : .40;
        mount.setDeltaMovement(path.x * speed, mount.getDeltaMovement().y, path.z * speed);
    }

    private static void strike(ServerLevel level, Mob actor, Attack current, List<ServerPlayer> fighters) {
        if (!actor.onGround()) return;
        boolean bash = current.move == Move.BASH;
        if (bash) {
            actor.setAggressive(true);
            actor.swing(InteractionHand.MAIN_HAND, true);
        }
        actor.playSound(bash ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.RAVAGER_ATTACK, 1, 1.05f);
        double reach = switch (current.move) {
            case BASH -> 2.8;
            case KICK -> 3.45;
            default -> 3.75;
        };
        double minDot = switch (current.move) {
            case HOOK -> .55;
            case HEADBUTT -> .75;
            default -> .68;
        };
        Vec3 origin = actor.position().add(0, bash ? 1.05 : 1.25, 0);
        for (ServerPlayer player : fighters) {
            Vec3 delta = player.position().subtract(actor.position());
            if (Math.abs(delta.y) > 1.25 || delta.horizontalDistanceSqr() > reach * reach
                    || BossAttackGeometry.flatDirection(delta).dot(current.aim) < minDot) continue;
            if (BossAttackGeometry.visible(level, actor, origin, player.getBoundingBox().getCenter())) {
                hit(level, actor, player, current, bash ? 6 : 9);
            }
        }
    }

    private static void stomp(ServerLevel level, Mob actor, Attack current, List<ServerPlayer> fighters, int beat) {
        if (!actor.onGround()) return;
        if (beat == 0) actor.playSound(SoundEvents.RAVAGER_STEP, 1.5f, .60f);
        double outer = 1.55 + beat * .59;
        double inner = beat == 0 ? 0 : outer - .73;
        Vec3 ground = actor.position();
        for (int index = 0; index < 10; index++) {
            Vec3 tip = ground.add(BossAttackGeometry.flatDirection(index * Math.PI / 5).scale(outer));
            if (!connectedFloor(level, actor, ground, tip)) continue;
            var floor = level.getBlockState(BlockPos.containing(tip.add(0, -.17, 0)));
            if (floor.isAir()) continue;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                tip.x, tip.y + .16, tip.z, 3, .09, .04, .09, .08);
        }
        for (ServerPlayer player : fighters) {
            Vec3 feet = player.position();
            double radius = feet.subtract(ground).horizontalDistance();
            if (radius >= inner && radius <= outer && Math.abs(feet.y - ground.y) < .52
                    && connectedFloor(level, actor, ground, feet)
                    && BossAttackGeometry.visible(level, actor, ground.add(0, .22, 0), feet.add(0, .22, 0))) {
                hit(level, actor, player, current, 9);
            }
        }
    }

    private static boolean connectedFloor(ServerLevel level, Mob actor, Vec3 start, Vec3 end) {
        Vec3 flatEnd = new Vec3(end.x, start.y, end.z);
        int steps = Math.max(1, Mth.ceil(start.distanceTo(flatEnd) * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = start.lerp(flatEnd, (double)i / steps);
            Vec3 floor = BossAttackGeometry.ground(level, actor, p.add(0, .15, 0), .48);
            if (floor == null || !level.getFluidState(BlockPos.containing(p)).isEmpty()) return false;
            if (!BossAttackGeometry.visible(level, actor, start.add(0, .24, 0), p.add(0, .24, 0))) return false;
        }
        return true;
    }

    private static void hit(ServerLevel level, Mob actor, ServerPlayer player, Attack attack, float damage) {
        if (attack.hit.add(player.getUUID())) player.hurt(level.damageSources().mobAttack(actor), damage);
    }

    private static void tickMountedBow(ServerLevel level, Mob mount, MythrilPillager rider, ServerPlayer target,
                                       Attack attack, int age) {
        if (rider == null) return;
        if (attack.move == Move.VOLLEY && age == 10 && rider.hasLineOfSight(target)) {
            attack.armed = draw(rider);
        }
        if (age == attack.move.windup || attack.move == Move.VOLLEY && age == 17) {
            if (attack.armed) fire(level, rider, target, attack.move == Move.BRAKE ? 1.65f : 1.85f);
            attack.armed = false;
            if (attack.move == Move.BRAKE) BossArenaPortals.release(level, mount.getUUID());
        }
    }

    private static void tickFootBow(ServerLevel level, MythrilPillager rider, ServerPlayer target, Attack attack, int age) {
        if (attack.move == Move.COUNTERSHOT && age < 5) {
            if (rider.onGround() && travelClear(level, rider, attack.aim, .36)) {
                rider.setDeltaMovement(attack.aim.x * .31, rider.getDeltaMovement().y, attack.aim.z * .31);
            } else {
                attack.armed = false;
                return;
            }
        }
        if (attack.move == Move.COUNTERSHOT && age == 6 && rider.hasLineOfSight(target)) {
            // A blocked step must never degenerate into the standing shot.
            double travelled = rider.position().subtract(attack.beforeStep).dot(attack.aim);
            if (travelled >= .65) attack.armed = draw(rider);
        }
        if (age == attack.move.activeUntil - 1 && attack.armed) {
            fire(level, rider, target, attack.move == Move.AIMED_SHOT ? 2.1f : 1.65f);
            attack.armed = false;
        }
    }

    private static Opening ridgeOpening(ServerLevel level, MythrilPillager rider, ServerPlayer target, Vec3 heading) {
        // The move opens a blocked line from a reachable ledge. Flat retreats and visible shots use their own moves.
        var side = sideOf(heading);
        var targetBody = target.getBoundingBox().getCenter();
        int y = rider.getBlockY();
        for (int flank = 1; flank >= -1; flank -= 2) {
            var offset = side.scale(flank * 2.5).add(heading.scale(1.25));
            for (int dy = 1; dy >= -1; dy -= 2) {
                var at = BlockPos.containing(rider.getX() + offset.x, y + dy, rider.getZ() + offset.z);
                if (!level.hasChunkAt(at) || !level.hasChunkAt(at.below())
                        || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) continue;
                var foot = Vec3.atBottomCenterOf(at);
                var box = rider.getBoundingBox().move(foot.subtract(rider.position()));
                if (!SourceBiomePortal.fitsBorder(level, box) || level.getBlockCollisions(rider, box).iterator().hasNext()
                        || level.containsAnyLiquid(box)) continue;
                var eye = foot.add(0, rider.getEyeHeight(), 0);
                if (!BossAttackGeometry.visible(level, rider, eye, targetBody)) continue;
                var path = SourceArachnidPathBudget.block(rider, at, 8);
                if (path == null || !path.canReach() || !at.equals(path.getTarget()) || path.getNodeCount() > 8) continue;
                var end = path.getEndNode();
                if (end == null || !end.asBlockPos().equals(at)) continue;
                return new Opening(heading, heading, flank, path);
            }
        }
        return null;
    }

    private void tickRidge(ServerLevel level, MythrilPillager rider, ServerPlayer target, Attack current, long now, int age) {
        if (current.ridgeRelease >= 0) {
            stop(rider);
            aimAt(rider, target);
            if (age == current.ridgeRelease && current.armed) {
                if (!rider.onGround() || Math.abs(rider.getY() - current.path.getTarget().getY()) >= .18) {
                    finish(level, rider, null, now);
                    return;
                }
                fire(level, rider, target, 1.85f);
                current.armed = false;
            }
            return;
        }
        var goal = Vec3.atBottomCenterOf(current.path.getTarget());
        var error = goal.subtract(rider.position());
        if (rider.onGround() && Math.abs(error.y) < .18 && error.horizontalDistanceSqr() < .6 * .6) {
            stop(rider);
            if (!rider.hasLineOfSight(target) || !draw(rider)) {
                finish(level, rider, null, now);
                return;
            }
            aimAt(rider, target);
            current.armed = true;
            current.ridgeRelease = age + current.move.windup;
        } else if (age >= current.move.activeUntil - current.move.windup - 1
                || rider.getNavigation().isDone() || rider.isInWaterOrBubble()) {
            finish(level, rider, null, now);
        }
    }

    private static boolean draw(MythrilPillager rider) {
        if (!(rider.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)) return false;
        rider.startUsingItem(InteractionHand.MAIN_HAND);
        rider.setChargingCrossbow(true);
        rider.playSound(SoundEvents.CROSSBOW_LOADING_START, 1, 1.05f);
        return true;
    }

    private static void clearRider(MythrilPillager rider) {
        if (rider.isUsingItem()) rider.stopUsingItem();
        if (rider.isChargingCrossbow()) rider.setChargingCrossbow(false);
        rider.setAggressive(false);
    }

    private static void fire(ServerLevel level, MythrilPillager rider, ServerPlayer target, float speed) {
        if (rider.hasLineOfSight(target)) {
            Arrow bolt = new Arrow(level, rider);
            Vec3 aim = target.getBoundingBox().getCenter().subtract(bolt.position());
            Vec3 launch = aim.add(0, aim.horizontalDistance() * .05, 0);
            MythrilKnightEncounter.face(rider, launch);
            bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
            bolt.shoot(launch.x, launch.y, launch.z, speed, .45f);
            MythrilKnightEncounter.markBolt(bolt, rider);
            if (level.addFreshEntity(bolt)) rider.playSound(SoundEvents.CROSSBOW_SHOOT, 1, 1.05f);
        }
        clearRider(rider);
    }

    private static void aimAt(Mob actor, ServerPlayer target) {
        MythrilKnightEncounter.face(actor, target.getBoundingBox().getCenter().subtract(actor.getEyePosition()));
    }

    private static Vec3 sideOf(Vec3 direction) {
        return new Vec3(-direction.z, 0, direction.x);
    }

    private static boolean lateralExit(ServerLevel level, ServerPlayer target, Vec3 heading, double distance) {
        Vec3 side = sideOf(heading);
        return DragonCombat.escape(level, target, side, distance) || DragonCombat.escape(level, target, side.reverse(), distance);
    }

    private static boolean travelClear(ServerLevel level, LivingEntity actor, Vec3 direction, double distance) {
        return travelClear(level, actor, Vec3.ZERO, direction, distance);
    }

    private static boolean travelClear(ServerLevel level, LivingEntity actor, Vec3 startOffset, Vec3 direction, double distance) {
        AABB body = actor.getBoundingBox().deflate(.025);
        if (actor instanceof MythrilRavager mount && mount.getFirstPassenger() instanceof MythrilPillager rider) {
            body = body.minmax(rider.getBoundingBox().deflate(.025));
        }
        int steps = Mth.ceil(distance * 2.5);
        for (int i = 1; i <= steps; i++) {
            AABB next = body.move(startOffset.add(direction.scale(distance * i / steps)));
            if (!level.hasChunksAt(BlockPos.containing(next.minX, next.minY, next.minZ),
                    BlockPos.containing(next.maxX, next.maxY, next.maxZ))
                    || !level.getWorldBorder().isWithinBounds(next)
                    || level.getBlockCollisions(actor, next).iterator().hasNext() || level.containsAnyLiquid(next)) return false;
            AABB footing = new AABB(next.minX + .12, next.minY - .48, next.minZ + .12,
                next.maxX - .12, next.minY, next.maxZ - .12);
            if (!level.getBlockCollisions(actor, footing).iterator().hasNext()) return false;
        }
        return true;
    }
}
