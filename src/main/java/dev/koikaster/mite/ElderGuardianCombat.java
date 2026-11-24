package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class ElderGuardianCombat {
    enum Move {
        BEAMS(36, 22, 12, 19), FOAM(24, 25, 11, 15), VORTEX(30, 25, 13, 18.75f), DART(24, 17, 12, 17),
        BUBBLES(28, 24, 10, 15.5f), DOWNFLOW(30, 24, 11, 16), PRISM(35, 20, 11, 16.5f), SPINES(20, 16, 14, 15.5f),
        ECHO(26, 20, 12, 15), CURTAIN(36, 24, 11, 16), FOCUS(30, 7, 11, 16.5f), AMBUSH(34, 17, 14, 17.5f);

        final int warning;
        final int strike;
        final int recovery;
        private final float damage;
        private static final Move[] BY_ACTION = values();

        Move(int warning, int strike, int recovery, float damage) {
            this.warning = warning;
            this.strike = strike;
            this.recovery = recovery;
            this.damage = damage;
        }

        int action() { return ordinal() + 1; }

        boolean isLunge() { return this == DART || this == AMBUSH; }
        static Move fromAction(int action) { return action > 0 && action <= BY_ACTION.length ? BY_ACTION[action - 1] : null; }
    }

    private record Ray(Vec3 from, Vec3 to) {}

    private static final SimpleParticleType WARN = ParticleTypes.GLOW;
    private static final SimpleParticleType HIT = ParticleTypes.BUBBLE_POP;
    private static final double BEAM_SPACING = 1.75;
    private static final double BEAM_SWING = .58;
    private static final double BEAM_WIDTH = .86;
    private static final double FOAM_START = 1.45;
    private static final double FOAM_SPEED = .42;
    private static final double FOAM_DEPTH = .69;
    private static final double FOAM_WIDTH = .72;
    private static final double VORTEX_RANGE = 5.8;
    private static final double VORTEX_CORE = 2.7;
    private static final double VORTEX_ESCAPE = VORTEX_CORE + .6;
    private static final int LUNGE_COUNTER_TICKS = 44;
    private static final int BEAM_COUNTER_TICKS = 54;
    private static final int BEAM_COAST_TICKS = 18;
    private static final double BEAM_COUNTER_DISTANCE = 3.2;
    private static final int BETWEEN_MOVES_TICKS = 4;
    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private final Set<UUID> hit = new HashSet<>();
    private Move move;
    private Vec3 center;
    private Vec3 origin;
    private Vec3 axis;
    private Vec3 previous;
    private Vec3 exit;
    private List<Ray> lines = List.of();
    private List<Vec3> sites = List.of();
    private BossAttackFocus focus;
    private Path pursuit;
    private BlockPos pursuitTarget;
    private long repathAt;
    private int age;
    private int idle = 11;
    private int vulnerable;
    private Vec3 recoveryApproach;

    boolean attacking() { return move != null && age < move.warning + move.strike; }

    void step(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players) {
        if (players.isEmpty()) {
            suspend(boss);
            boss.setDeltaMovement(boss.getDeltaMovement().scale(.65));
            return;
        }
        if (move != null) {
            if (!advance(level, boss, players)) finish(boss, BETWEEN_MOVES_TICKS);
            return;
        }
        if (vulnerable > 0) {
            boss.retract(true);
            recover(level, boss);
            if (--vulnerable == 0) recoveryApproach = null;
            return;
        }
        Comparator<ServerPlayer> distance = Comparator.comparingDouble(boss::distanceToSqr);
        Vec3 eyes = boss.getEyePosition();
        var target = players.stream().filter(player -> pathWater(level, boss, eyes, player.getBoundingBox().getCenter()))
            .min(distance).orElse(null);
        if (target == null) {
            pursue(level, boss, players.stream().min(distance).orElseThrow());
            return;
        }
        pursuit = null;
        Vec3 eye = target.getEyePosition();
        boss.getLookControl().setLookAt(eye.x, eye.y, eye.z, 30, 30);
        patrol(level, boss, target);
        if (idle-- > 0) return;
        for (Move candidate : order.candidates(boss.getRandom())) {
            if (!start(level, boss, target, players, candidate)) continue;
            order.used(candidate);
            return;
        }
        idle = 7;
    }

    private boolean start(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer target, List<ServerPlayer> players,
                          Move candidate) {
        origin = boss.getEyePosition();
        center = target.getBoundingBox().getCenter();
        double distance = origin.distanceTo(center);
        if (distance > 27 || !SourceElderGuardianBoss.water(level, center)) return false;
        axis = BossAttackGeometry.flatDirection(center.subtract(origin));
        if (axis.lengthSqr() < .5) axis = Vec3.directionFromRotation(0, boss.getYRot());
        Vec3 lateral = side();
        lines = List.of();
        sites = List.of();
        exit = null;
        switch (candidate) {
            case BEAMS -> {
                if (distance < 6 || !planBeams(level, boss, target, players, lateral)) return false;
            }
            case FOAM -> {
                if (!canLeaveDepth(level, boss, players, center) || nearbyWater(level, boss, center, 4) < 2) return false;
            }
            case VORTEX -> {
                if (!pathWater(level, boss, origin, center) || !hasVortexEscape(level, boss, players, center)) return false;
            }
            case DART -> {
                if (distance < 4.5 || distance > 14) return false;
                exit = lungeEnd(candidate);
                if (!swimPath(level, boss, boss.position(), exit) || !hasSideExit(level, boss, players, center, 2.2)) return false;
            }
            case BUBBLES -> {
                List<Ray> columns = new ArrayList<>();
                for (int i = -2; i <= 2; ++i) {
                    Vec3 base = center.add(lateral.scale(i * 3.1)).add(0, -2.1, 0);
                    Ray ray = fixedRay(level, boss, base, base.add(0, 5.7, 0));
                    if (ray.from.distanceToSqr(ray.to) > 7 && hasSideExit(level, boss, players, base.add(0, 2.1, 0), 1.75)) columns.add(ray);
                }
                if (columns.size() < 2) return false;
                lines = columns;
            }
            case DOWNFLOW -> {
                if (!SourceElderGuardianBoss.water(level, center.add(0, 3.2, 0)) || !hasSideExit(level, boss, players, center, 4.4)) return false;
            }
            case PRISM -> {
                lines = reflections(level, boss);
                if (lines.size() < 4 || !hasSideExit(level, boss, players, center, 2.25)) return false;
            }
            case SPINES -> {
                if (distance > 9 || !hasSideExit(level, boss, players, center, 1.8)) return false;
                Vec3 out = center.add(axis.scale(3.4));
                lines = List.of(fixedRay(level, boss, origin, out), fixedRay(level, boss, origin, out.add(lateral.scale(3.6))),
                    fixedRay(level, boss, origin, out.subtract(lateral.scale(3.6))));
            }
            case ECHO -> {
                sites = wallSites(level, boss);
                if (sites.size() < 3 || !hasSideExit(level, boss, players, center, 2.3)) return false;
            }
            case CURTAIN -> {
                if (!hasSideExit(level, boss, players, center, 3.4) || nearbyWater(level, boss, center, 4) < 3) return false;
            }
            case FOCUS -> {
                if (!hasSideExit(level, boss, players, center, 3.6) || !pathWater(level, boss, origin, center)) return false;
                Vec3 mount = center.add(lateral.scale(1.75)).add(0, .35, 0);
                if (!SourceElderGuardianBoss.water(level, mount)) return false;
                var orb = new BossAttackFocus(level, mount);
                orb.setOwner(boss);
                if (!level.addFreshEntity(orb)) return false;
                focus = orb;
                center = mount;
            }
            case AMBUSH -> {
                Vec3 underwaterExit = findAmbushExit(level, boss, lungeEnd(candidate));
                if (underwaterExit == null || !hasSideExit(level, boss, players, center, 1.9)) return false;
                exit = underwaterExit;
            }
        }
        move = candidate;
        age = 0;
        previous = boss.position();
        hit.clear();
        boss.action(move.action());
        boss.retract(candidate.isLunge());
        level.playSound(null, boss.blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE,
            2.4f, candidate == Move.BEAMS || candidate == Move.PRISM ? .66f : .88f);
        warn(level, boss);
        return true;
    }

    private boolean advance(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players) {
        int tick = ++age - move.warning;
        if (tick == move.strike && move.isLunge() && hit.isEmpty()) {
            vulnerable = LUNGE_COUNTER_TICKS;
            return false;
        }
        if (tick == move.strike && (move == Move.BEAMS || move == Move.PRISM) && hit.isEmpty()) {
            vulnerable = BEAM_COUNTER_TICKS;
            recoveryApproach = center.subtract(0, boss.getBbHeight() * .5, 0);
            return false;
        }
        if (age >= move.warning + move.strike + move.recovery) return false;
        if (move == Move.FOCUS && tick <= 2 && (focus == null || focus.isRemoved())) return false;
        boss.getLookControl().setLookAt(center.x, center.y, center.z, 30, 30);
        if (move == Move.AMBUSH && tick < 0) swim(level, boss, exit, .55, .29);
        else if (!move.isLunge() || tick < 0 || tick >= move.strike) {
            boss.setDeltaMovement(boss.getDeltaMovement().scale(.72));
        }
        if (tick < 0) {
            if (age % 5 == 0) warn(level, boss);
            return true;
        }
        if (tick >= move.strike) return true;
        if (tick == 0) {
            if ((move == Move.BEAMS || move == Move.PRISM) && boss.getEyePosition().distanceToSqr(origin) > 4) return false;
            level.playSound(null, boss.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 2.2f, .70f);
        }
        switch (move) {
            case BEAMS -> beams(level, boss, players, tick);
            case FOAM -> foam(level, boss, players, tick);
            case VORTEX -> vortex(level, boss, players, tick);
            case DART -> lunge(level, boss, players, tick, exit);
            case BUBBLES -> bubbles(level, boss, players, tick);
            case DOWNFLOW -> downflow(level, boss, players, tick);
            case PRISM -> prism(level, boss, players, tick);
            case SPINES -> spines(level, boss, players, tick);
            case ECHO -> echo(level, boss, players, tick);
            case CURTAIN -> curtain(level, boss, players, tick);
            case FOCUS -> {
                if (tick == 2) {
                    focus.discard();
                    focus = null;
                    pulse(level, boss, players, center, 3.5);
                }
            }
            case AMBUSH -> {
                if (tick == 0 && boss.position().distanceToSqr(exit) > 5) return false;
                lunge(level, boss, players, tick, lungeEnd(move));
            }
        }
        return true;
    }

    private void recover(ServerLevel level, SourceElderGuardianBoss boss) {
        if (recoveryApproach != null && vulnerable > BEAM_COUNTER_TICKS - BEAM_COAST_TICKS) {
            Vec3 from = boss.position();
            Vec3 away = from.subtract(recoveryApproach);
            if (away.lengthSqr() > BEAM_COUNTER_DISTANCE * BEAM_COUNTER_DISTANCE) {
                Vec3 stop = recoveryApproach.add(away.normalize().scale(BEAM_COUNTER_DISTANCE));
                if (clearSwimTurn(level, boss, from, stop)) {
                    swim(level, boss, stop, .31, .38);
                    return;
                }
            }
        }
        boss.setDeltaMovement(boss.getDeltaMovement().scale(.68));
    }

    private void warn(ServerLevel level, SourceElderGuardianBoss boss) {
        int ticks = Math.max(1, Math.min(5, move.warning - age));
        switch (move) {
            case BEAMS -> {
                for (Ray ray : lines) {
                    if (pathWater(level, boss, origin, ray.from)) waterCue(level, boss, ray.from, .72, ticks);
                }
            }
            case BUBBLES -> {
                for (Ray ray : lines) waterCue(level, boss, ray.from, .55, ticks);
            }
            case PRISM -> {
                waterCue(level, boss, boss.getEyePosition(), .85, ticks);
                waterCue(level, boss, lines.get(0).to, .95, ticks);
            }
            case SPINES, DART -> waterCue(level, boss, boss.getEyePosition(), .93, ticks);
            case FOAM -> waterRing(level, boss, WARN, center, FOAM_START, 22);
            case VORTEX -> waterCue(level, boss, center, 1.4, ticks);
            case DOWNFLOW -> waterCue(level, boss, center.add(0, 3.15, 0), 1.1, ticks);
            case ECHO -> {
                for (Vec3 site : sites) BossAttackGeometry.point(level, WARN, site);
            }
            case CURTAIN -> {
                Vec3 opening = center.subtract(axis.scale(5));
                waterCue(level, boss, opening.subtract(side().scale(3.8)), .85, ticks);
                waterCue(level, boss, opening.add(side().scale(3.8)), .85, ticks);
            }
            case FOCUS -> waterCue(level, boss, center, 1.25, ticks);
            case AMBUSH -> waterCue(level, boss, exit.add(0, .45, 0), .95, ticks);
        }
    }

    private static void waterCue(ServerLevel level, SourceElderGuardianBoss boss, Vec3 at, double radius, int ticks) {
        if (!boss.contains(at) || !SourceElderGuardianBoss.water(level, at)) return;
        BossCombatVisual.cue(level, boss, BossCombatVisual.Theme.ELDER, at, radius, ticks);
        level.sendParticles(ParticleTypes.BUBBLE, at.x, at.y, at.z, 5, .35, .25, .35, .05);
    }

    private boolean planBeams(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer target,
                              List<ServerPlayer> players, Vec3 lateral) {
        Vec3 near = origin.add(axis.scale(.72));
        Vec3 aim = center.add(center.subtract(near).normalize().scale(5));
        double preferred = boss.getRandom().nextBoolean() ? BEAM_SPACING : -BEAM_SPACING;
        for (double gap : new double[]{preferred, -preferred}) {
            var pair = beamPair(near, aim, lateral, gap);
            var limits = new ArrayList<Ray>(4);
            for (int i = 0; i < pair.size(); ++i) {
                Ray ray = pair.get(i);
                Ray straight = fixedRay(level, boss, ray.from, ray.to);
                if (!pathWater(level, boss, origin, ray.from) || straight.from.distanceToSqr(straight.to) < 16) break;
                limits.add(straight);
                limits.add(fixedRay(level, boss, ray.from, beamEnd(ray, i, BEAM_SWING, lateral)));
            }
            if (limits.size() != 4 || !beamCovers(target.getBoundingBox(), limits)
                    || !pathWater(level, boss, near, center)
                    || !beamExit(level, boss, target, limits, lateral.scale(gap))) continue;
            boolean safe = true;
            for (var player : players) {
                if (player == target || !beamCovers(player.getBoundingBox(), limits)) continue;
                double lane = player.getBoundingBox().getCenter().subtract(center).dot(lateral);
                Vec3 toGap = lateral.scale(gap - lane);
                if (toGap.lengthSqr() <= 3.65 * 3.65 && beamExit(level, boss, player, limits, toGap)) continue;
                Vec3 away = lateral.scale(lane > gap ? 2.25 : -2.25);
                if (beamExit(level, boss, player, limits, away)) continue;
                safe = false;
                break;
            }
            if (!safe) continue;
            lines = pair;
            return true;
        }
        return false;
    }

    private static List<Ray> beamPair(Vec3 near, Vec3 aim, Vec3 lateral, double gap) {
        Vec3 first = lateral.scale(gap + BEAM_SPACING);
        Vec3 second = lateral.scale(gap - BEAM_SPACING);
        return List.of(new Ray(near.add(first), aim.add(first)), new Ray(near.add(second), aim.add(second)));
    }

    private static boolean beamCovers(AABB box, List<Ray> rays) {
        return rays.stream().anyMatch(ray -> BossAttackGeometry.hits(box, ray.from, ray.to, BEAM_WIDTH));
    }

    private static boolean beamExit(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer player,
                                    List<Ray> rays, Vec3 delta) {
        return !beamCovers(player.getBoundingBox().move(delta), rays) && playerRoute(level, boss, player, delta);
    }

    private void beams(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        if (tick >= 18) return;
        double offset = BEAM_SWING * Math.sin(Math.PI * tick / 18);
        Vec3 lateral = side();
        for (int i = 0; i < lines.size(); ++i) {
            Ray ray = lines.get(i);
            if (pathWater(level, boss, origin, ray.from)) {
                strike(level, boss, players, ray.from, beamEnd(ray, i, offset, lateral), BEAM_WIDTH);
            }
        }
    }

    private static Vec3 beamEnd(Ray ray, int lane, double offset, Vec3 lateral) {
        return ray.to.add(lateral.scale(lane == 0 ? offset : -offset));
    }

    private void foam(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        double radius = FOAM_START + tick * FOAM_SPEED;
        if (tick % 2 == 0) releasedWave(level, boss, center, radius, FOAM_WIDTH, FOAM_DEPTH, false);
        for (var player : players) {
            Vec3 offset = player.getBoundingBox().getCenter().subtract(center);
            if (Math.abs(offset.y) <= FOAM_DEPTH && Math.abs(offset.horizontalDistance() - radius) <= FOAM_WIDTH
                    && pathWater(level, boss, center, center.add(offset))) hurt(level, boss, player);
        }
    }

    private void vortex(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        if (tick == 22) {
            pulse(level, boss, players, center, VORTEX_CORE);
        } else if (tick < 21 && tick % 3 == 0) {
            waterRing(level, boss, HIT, center, VORTEX_RANGE - tick * .20, 23);
            for (var player : players) {
                Vec3 at = player.getBoundingBox().getCenter();
                Vec3 motion = center.subtract(at);
                if (motion.lengthSqr() > VORTEX_RANGE * VORTEX_RANGE || !pathWater(level, boss, center, at)) continue;
                Vec3 current = motion.normalize();
                double force = .028 + tick * .0011;
                if (player.isSwimming() && player.getLookAngle().dot(current) < -.55) force *= .12;
                SourceExplosionImpulse.send(player, current.scale(force));
            }
        }
    }

    private void lunge(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick, Vec3 destination) {
        Vec3 here = boss.position();
        if (tick > 0 && here.distanceToSqr(previous) > .015) {
            strike(level, boss, players, previous.add(0, .75, 0), here.add(0, .75, 0), 1.1);
        }
        if (tick + 1 < move.strike) swim(level, boss, destination, Math.min(1.15, .55 + tick * .08), .42);
        previous = here;
    }

    private void bubbles(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        int index = tick / 4;
        if (index >= lines.size()) return;
        Ray ray = lines.get(index);
        double progress = tick % 4 * .25;
        Vec3 from = ray.from.lerp(ray.to, progress);
        if (pathWater(level, boss, ray.from, from)) strike(level, boss, players, from, ray.from.lerp(ray.to, progress + .25), .95);
    }

    private void downflow(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        double y = 3.15 - tick * .26;
        Vec3 layer = center.add(0, y, 0);
        if (tick % 2 == 0) releasedWave(level, boss, layer, 4.1, 0, .69, true);
        for (var player : players) {
            Vec3 at = player.getBoundingBox().getCenter();
            Vec3 above = new Vec3(at.x, center.y + 3.15, at.z);
            if (Math.abs(at.y - layer.y) < .69 && at.subtract(layer).horizontalDistanceSqr() <= 4.1 * 4.1
                    && pathWater(level, boss, above, at) && pathWater(level, boss, layer, at)) hurt(level, boss, player);
        }
    }

    private void prism(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        Ray inbound = lines.get(0);
        if (!pathWater(level, boss, inbound.from, inbound.to)) return;
        if (tick < 8) BossCombatVisual.beam(level, boss, BossCombatVisual.Theme.ELDER, inbound.from, inbound.to, .46, 3);
        if (tick >= 6 && tick < 17) {
            for (int i = 1; i < lines.size(); ++i) {
                Ray ray = lines.get(i);
                if (tick >= 4 + 2 * i) strike(level, boss, players, ray.from, ray.to, .75);
            }
        }
    }

    private void spines(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        if (tick > 10) {
            boss.retract(true);
            return;
        }
        if (tick % 3 == 0 && tick / 3 < lines.size()) {
            Ray ray = lines.get(tick / 3);
            strike(level, boss, players, ray.from, ray.to, .82);
        }
    }

    private void echo(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        int index = tick / 3;
        if (tick % 3 == 0 && index < sites.size()) pulse(level, boss, players, sites.get(index), 1.75);
    }

    private void curtain(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, int tick) {
        Vec3 base = center.add(axis.scale(-5 + tick * .43));
        for (int sign : new int[]{-1, 1}) {
            Vec3 from = base.add(side().scale(sign * 1.55));
            Vec3 end = base.add(side().scale(sign * 6.2));
            strike(level, boss, players, from, end, .88);
        }
    }

    private void pulse(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, Vec3 at, double radius) {
        if (move == Move.VORTEX) releasedVortex(level, boss, at, radius);
        else releasedWave(level, boss, at, radius, 0, .42, true);
        for (var player : players) {
            Vec3 point = player.getBoundingBox().getCenter();
            if (point.distanceToSqr(at) <= radius * radius && pathWater(level, boss, at, point)) hurt(level, boss, player);
        }
    }

    private void strike(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players,
                        Vec3 from, Vec3 toward, double width) {
        var ray = fixedRay(level, boss, from, toward);
        if (ray.from.distanceToSqr(ray.to) < .01) return;
        BossCombatVisual.beam(level, boss, BossCombatVisual.Theme.ELDER, ray.from, ray.to, width, 3);
        for (var player : players) {
            var box = player.getBoundingBox();
            if (BossAttackGeometry.hits(box, ray.from, ray.to, width) && pathWater(level, boss, from, box.getCenter())) {
                hurt(level, boss, player);
            }
        }
    }

    private void hurt(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer player) {
        if (!hit.contains(player.getUUID()) && player.hurt(level.damageSources().mobAttack(boss), move.damage)) hit.add(player.getUUID());
    }

    private static void releasedWave(ServerLevel level, SourceElderGuardianBoss boss, Vec3 center,
                                     double radius, double halfWidth, double halfHeight, boolean filled) {
        var endpoints = new ArrayList<Vec3>(56);
        Vec3 first = new Vec3(1, 0, 0);
        double minimum = filled ? .12 : radius - .02;
        double firstRadius = waterRadius(level, boss, center, first, radius);
        for (int i = 1; i <= 28; ++i) {
            Vec3 last = BossAttackGeometry.flatDirection(i * Math.PI / 14);
            double lastRadius = waterRadius(level, boss, center, last, radius);
            double span = Math.min(firstRadius, lastRadius);
            if (span >= minimum) {
                Vec3 middle = first.add(last).normalize();
                span = Math.min(span, waterRadius(level, boss, center, middle, radius));
                if (span >= minimum) {
                    endpoints.add(center.add(first.scale(span)));
                    endpoints.add(center.add(last.scale(span)));
                }
            }
            first = last;
            firstRadius = lastRadius;
        }
        BossCombatVisual.waterWave(level, boss, center, endpoints, halfWidth, halfHeight, filled);
    }

    private static void releasedVortex(ServerLevel level, SourceElderGuardianBoss boss, Vec3 center,
                                       double radius) {
        var endpoints = new ArrayList<Vec3>(60);
        addVortexRay(level, boss, center, center.add(0, radius, 0), endpoints);
        addVortexRay(level, boss, center, center.add(0, -radius, 0), endpoints);
        for (int layer = -2; layer <= 2; layer++) {
            double elevation = layer * Math.PI / 6;
            double height = Math.sin(elevation) * radius;
            double horizontal = Math.cos(elevation) * radius;
            int rays = 8 - 2 * Math.abs(layer);
            for (int i = 0; i < rays; i++) {
                double angle = i * Math.PI * 2 / rays;
                Vec3 end = center.add(Math.cos(angle) * horizontal, height, Math.sin(angle) * horizontal);
                addVortexRay(level, boss, center, end, endpoints);
            }
        }
        BossCombatVisual.waterBurst(level, boss, center, endpoints);
    }

    private static void addVortexRay(ServerLevel level, SourceElderGuardianBoss boss, Vec3 center,
                                     Vec3 target, List<Vec3> endpoints) {
        Vec3 end = fixedRay(level, boss, center, target).to;
        if (center.distanceToSqr(end) < .01) return;
        endpoints.add(center);
        endpoints.add(end);
    }

    private static double waterRadius(ServerLevel level, SourceElderGuardianBoss boss, Vec3 center,
                                      Vec3 direction, double radius) {
        return fixedRay(level, boss, center, center.add(direction.scale(radius))).to.distanceTo(center);
    }

    private static Ray fixedRay(ServerLevel level, SourceElderGuardianBoss boss, Vec3 from, Vec3 toward) {
        if (!boss.contains(from) || !SourceElderGuardianBoss.water(level, from)) return new Ray(from, from);
        Vec3 end = BossAttackGeometry.shorten(level, boss, from, toward);
        int samples = Math.max(1, Mth.ceil(from.distanceTo(end) * 2));
        for (int i = 1; i <= samples; ++i) {
            Vec3 sample = from.lerp(end, i / (double)samples);
            if (!boss.contains(sample) || !SourceElderGuardianBoss.water(level, sample)) {
                return new Ray(from, from.lerp(end, (i - 1d) / samples));
            }
        }
        return new Ray(from, end);
    }

    private static boolean pathWater(ServerLevel level, SourceElderGuardianBoss boss, Vec3 from, Vec3 toward) {
        return boss.contains(from) && SourceElderGuardianBoss.water(level, from)
            && fixedRay(level, boss, from, toward).to.distanceToSqr(toward) < .05;
    }

    private static void waterRing(ServerLevel level, SourceElderGuardianBoss boss, ParticleOptions particle,
                                  Vec3 from, double radius, int samples) {
        for (int i = 0; i < samples; ++i) {
            Vec3 at = from.add(BossAttackGeometry.flatDirection(i * Math.PI * 2 / samples).scale(radius));
            if (pathWater(level, boss, from, at)) BossAttackGeometry.point(level, particle, at);
        }
    }

    private List<Ray> reflections(ServerLevel level, SourceElderGuardianBoss boss) {
        for (int i = 0; i < 8; ++i) {
            Vec3 along = BossAttackGeometry.flatDirection(i * Math.PI / 4);
            var contact = level.clip(new ClipContext(center, center.add(along.scale(8)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, boss));
            if (contact.getType() != HitResult.Type.BLOCK || !contact.getDirection().getAxis().isHorizontal()) continue;
            var wall = level.getBlockState(contact.getBlockPos());
            if (!(wall.is(Blocks.PRISMARINE) || wall.is(Blocks.PRISMARINE_BRICKS) || wall.is(Blocks.DARK_PRISMARINE))) continue;
            Vec3 normal = Vec3.atLowerCornerOf(contact.getDirection().getNormal());
            Vec3 spot = contact.getLocation().add(normal.scale(.34));
            if (!pathWater(level, boss, origin, spot)) continue;
            var result = new ArrayList<Ray>();
            result.add(new Ray(origin, spot));
            for (int side = -1; side <= 1; ++side) {
                Vec3 toward = spot.add(normal.yRot(side * .43f).scale(9));
                Ray ray = fixedRay(level, boss, spot, toward);
                if (ray.from.distanceToSqr(ray.to) > 4) result.add(ray);
            }
            if (result.size() == 4) return result;
        }
        return List.of();
    }

    private List<Vec3> wallSites(ServerLevel level, SourceElderGuardianBoss boss) {
        List<Vec3> found = new ArrayList<>();
        for (int i = 0; i < 8; ++i) {
            Vec3 at = center.add(BossAttackGeometry.flatDirection(i * Math.PI / 4).scale(7));
            var hit = level.clip(new ClipContext(center, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            Vec3 point = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.50));
            if (found.stream().anyMatch(existing -> existing.distanceToSqr(point) < 1.8)) continue;
            if (pathWater(level, boss, center, point)) found.add(point);
            if (found.size() >= 6) break;
        }
        return found;
    }

    private Vec3 lungeEnd(Move attack) {
        return attack == Move.DART ? center.add(axis.scale(2)).add(0, -.75, 0) : center.add(0, .55, 0);
    }

    private Vec3 findAmbushExit(ServerLevel level, SourceElderGuardianBoss boss, Vec3 destination) {
        for (int sign : new int[]{1, -1}) {
            Vec3 at = center.add(side().scale(sign * 5.1)).add(0, -2.2, 0);
            if (swimPath(level, boss, boss.position(), at) && swimPath(level, boss, at, destination)) return at;
        }
        return null;
    }

    private static boolean swimPath(ServerLevel level, SourceElderGuardianBoss boss, Vec3 from, Vec3 to) {
        if (!pathWater(level, boss, from.add(0, .65, 0), to.add(0, .65, 0))) return false;
        int steps = Math.max(1, Mth.ceil(from.distanceTo(to)));
        for (int i = 1; i <= steps; ++i) {
            Vec3 at = from.lerp(to, i / (double)steps);
            var box = boss.getBoundingBox().move(at.subtract(boss.position())).deflate(.035);
            if (level.getBlockCollisions(boss, box).iterator().hasNext()) return false;
        }
        return true;
    }

    private static void swim(ServerLevel level, SourceElderGuardianBoss boss, Vec3 destination, double maximum, double blend) {
        Vec3 from = boss.position();
        Vec3 delta = destination.subtract(from);
        Vec3 velocity = boss.getDeltaMovement().lerp(delta.normalize().scale(Math.min(delta.length() * .42, maximum)), blend);
        if (!swimPath(level, boss, from, from.add(velocity))) velocity = Vec3.ZERO;
        boss.setDeltaMovement(velocity);
    }

    private static void patrol(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer player) {
        Vec3 toward = player.position().subtract(boss.position());
        Vec3 axis = BossAttackGeometry.flatDirection(toward);
        Vec3 target = player.position().subtract(axis.scale(6.7)).add(0, .48, 0);
        swim(level, boss, target, .36, .22);
        boss.retract(boss.getDeltaMovement().lengthSqr() > .025);
    }

    private void pursue(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer player) {
        double footprint = (int)(boss.getBbWidth() + 1) * .5;
        BlockPos destination = BlockPos.containing(player.getX() - footprint, player.getY(), player.getZ() - footprint);
        long now = level.getGameTime();
        if (now >= repathAt && (pursuit == null || pursuit.isDone() || !destination.equals(pursuitTarget))) {
            pursuit = boss.getNavigation().createPath(destination, 1);
            pursuitTarget = destination;
            repathAt = now + 12;
        }
        Vec3 from = boss.position();
        while (pursuit != null && !pursuit.isDone()) {
            Vec3 waypoint = pursuit.getNextEntityPos(boss);
            double distance = from.distanceToSqr(waypoint);
            if (distance < .16) {
                int nextIndex = pursuit.getNextNodeIndex() + 1;
                if (nextIndex >= pursuit.getNodeCount()
                        || clearSwimTurn(level, boss, from, pursuit.getEntityPosAtNode(boss, nextIndex))) {
                    pursuit.advance();
                    continue;
                }
            }
            Vec3 heading = alignSwimWaypoint(level, boss, from, waypoint);
            if (heading == null) {
                pursuit = null;
                break;
            }
            boss.getLookControl().setLookAt(heading.x, heading.y + boss.getEyeHeight(), heading.z, 30, 30);
            swim(level, boss, heading, .36, .28);
            boss.retract(boss.getDeltaMovement().lengthSqr() > .025);
            return;
        }
        boss.setDeltaMovement(boss.getDeltaMovement().scale(.55));
        boss.retract(false);
    }

    private static boolean clearSwimTurn(ServerLevel level, SourceElderGuardianBoss boss, Vec3 from, Vec3 to) {
        if (!pathWater(level, boss, from.add(0, .65, 0), to.add(0, .65, 0))) return false;
        AABB body = boss.getBoundingBox().move(from.subtract(boss.position())).deflate(.001);
        for (var shape : level.getBlockCollisions(boss, body.expandTowards(to.subtract(from)))) {
            for (AABB block : shape.toAabbs()) {
                // Expand the obstacle by the body offsets so the feet can be tested as a single ray.
                var blocked = new AABB(block.minX - (body.maxX - from.x), block.minY - (body.maxY - from.y),
                    block.minZ - (body.maxZ - from.z), block.maxX + (from.x - body.minX),
                    block.maxY + (from.y - body.minY), block.maxZ + (from.z - body.minZ));
                if (blocked.contains(from) || blocked.contains(to) || blocked.clip(from, to).isPresent()) return false;
            }
        }
        return true;
    }

    private static Vec3 alignSwimWaypoint(ServerLevel level, SourceElderGuardianBoss boss, Vec3 from, Vec3 waypoint) {
        if (clearSwimTurn(level, boss, from, waypoint)) return waypoint;
        Vec3 best = null;
        double closest = from.distanceToSqr(waypoint);
        for (Vec3 aligned : List.of(new Vec3(waypoint.x, from.y, from.z), new Vec3(from.x, waypoint.y, from.z),
                new Vec3(from.x, from.y, waypoint.z))) {
            double remaining = aligned.distanceToSqr(waypoint);
            if (remaining >= closest - 1.0E-5 || !clearSwimTurn(level, boss, from, aligned)) continue;
            best = aligned;
            closest = remaining;
        }
        return best;
    }

    private static boolean canLeaveDepth(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players, Vec3 center) {
        double radius = FOAM_START + (Move.FOAM.strike - 1) * FOAM_SPEED + FOAM_WIDTH;
        for (var player : players) {
            Vec3 at = player.getBoundingBox().getCenter();
            Vec3 offset = at.subtract(center);
            if (Math.abs(offset.y) > FOAM_DEPTH || offset.horizontalDistanceSqr() > radius * radius
                    || !pathWater(level, boss, center, at)) continue;
            if (!playerRoute(level, boss, player, new Vec3(0, 1.15, 0))
                    && !playerRoute(level, boss, player, new Vec3(0, -1.15, 0))) return false;
        }
        return true;
    }

    private static boolean hasSideExit(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players,
                                       Vec3 center, double needed) {
        for (var player : players) {
            var at = player.getBoundingBox().getCenter();
            if (at.distanceToSqr(center) > (needed + 1.7) * (needed + 1.7)) continue;
            boolean open = false;
            for (int i = 0; i < 6; ++i) {
                Vec3 axis = i < 4 ? BossAttackGeometry.flatDirection(i * Math.PI / 2) : new Vec3(0, i == 4 ? 1 : -1, 0);
                if (playerRoute(level, boss, player, axis.scale(needed))) { open = true; break; }
            }
            if (!open) return false;
        }
        return true;
    }

    private static boolean hasVortexEscape(ServerLevel level, SourceElderGuardianBoss boss, List<ServerPlayer> players,
                                           Vec3 center) {
        for (var player : players) {
            Vec3 at = player.getBoundingBox().getCenter();
            Vec3 offset = at.subtract(center);
            double clearance = VORTEX_ESCAPE * VORTEX_ESCAPE - offset.lengthSqr();
            if (clearance <= 0 || !pathWater(level, boss, center, at)) continue;
            if (offset.lengthSqr() > .01 && vortexRoute(level, boss, player, offset, offset.normalize(), clearance)) continue;
            boolean open = false;
            for (Direction direction : Direction.values()) {
                if (vortexRoute(level, boss, player, offset, Vec3.atLowerCornerOf(direction.getNormal()), clearance)) {
                    open = true;
                    break;
                }
            }
            if (!open) return false;
        }
        return true;
    }

    private static boolean vortexRoute(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer player,
                                       Vec3 offset, Vec3 direction, double clearance) {
        double outward = offset.dot(direction);
        if (outward < 0) return false;
        // Use the first point beyond the damaging core and its safety margin.
        double distance = Math.sqrt(outward * outward + clearance) - outward;
        return playerRoute(level, boss, player, direction.scale(distance));
    }

    private static boolean playerRoute(ServerLevel level, SourceElderGuardianBoss boss, ServerPlayer player, Vec3 delta) {
        var body = player.getBoundingBox();
        var center = body.getCenter();
        return pathWater(level, boss, center, center.add(delta))
            && !level.getBlockCollisions(player, body.expandTowards(delta).deflate(.02)).iterator().hasNext();
    }

    private static int nearbyWater(ServerLevel level, SourceElderGuardianBoss boss, Vec3 center, double radius) {
        int count = 0;
        for (int i = 0; i < 4; ++i) if (pathWater(level, boss, center,
                center.add(BossAttackGeometry.flatDirection(i * Math.PI / 2).scale(radius)))) ++count;
        return count;
    }

    private Vec3 side() { return new Vec3(-axis.z, 0, axis.x); }

    void suspend(SourceElderGuardianBoss boss) {
        if (move != null || vulnerable > 0) finish(boss, 9);
        vulnerable = 0;
        recoveryApproach = null;
        pursuit = null;
        repathAt = 0;
    }

    private void finish(SourceElderGuardianBoss boss, int delay) {
        if (focus != null) focus.discard();
        focus = null;
        if (vulnerable > 0 && move != null && move.isLunge()) boss.setDeltaMovement(Vec3.ZERO);
        move = null;
        lines = List.of();
        sites = List.of();
        idle = delay;
        boss.action(0);
        boss.retract(vulnerable > 0);
    }
}
