package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Combat owner for the native three-skull ritual. */
final class WitherCombat {
    enum Move {
        ALTERNATE(8, 16, 5), BLUE(24, 3, 8), CHARGE(20, 19, 8), CROSS(14, 14, 6),
        MINE(19, 4, 7), CURTAIN(19, 26, 6), RETURN(13, 24, 5), RING(20, 32, 5),
        GALE(16, 25, 6), SWEEP(13, 21, 5), AIRBURST(18, 13, 6), GUARD(12, 12, 7);

        final int warning;
        final int strike;
        final int recovery;
        private static final Move[] BY_POSE = values();

        Move(int warning, int strike, int recovery) {
            this.warning = warning;
            this.strike = strike;
            this.recovery = recovery;
        }

        int pose() { return ordinal() + 1; }

        static Move fromPose(int pose) {
            return pose > 0 && pose <= BY_POSE.length ? BY_POSE[pose - 1] : null;
        }
    }

    static final int IDLE = 0;
    static final int STAGGERED = 13;
    static final float HEAVY_POWER = 2;
    private static final float MINE_POWER = 1;
    private static final double MINE_STRIKE_RADIUS = 3;
    private static final int STAGGER_TICKS = 36;
    private static final int STAGGER_IDLE_TICKS = 13;
    private static final int HEAD_BREAK_STAGGER_DESCENT = 16;
    private static final int CHARGE_STAGGER_DESCENT = 12;
    private static final int RING_DIVE_TICKS = 5;
    private static final float HEAD_TO_BREAK = 30;
    private static final double CHARGE_WIDTH = 1.85;
    private static final double CHARGE_OVERRUN = 6;
    private static final double AIRBORNE_CHARGE_SETUP_RANGE = 12;
    private static final SimpleParticleType WARNING = ParticleTypes.SOUL;
    private static final SimpleParticleType DARK = ParticleTypes.SMOKE;
    private static final SimpleParticleType STRIKE = ParticleTypes.SOUL_FIRE_FLAME;

    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private final List<WitherGroundWave> waves = new ArrayList<>();
    private final List<SourceWitherSkull> shots = new ArrayList<>();
    private final Set<UUID> struck = new HashSet<>();
    private final float[] headDamage = new float[2];
    private int broken;
    private boolean exposed;
    private int nextSide = 1;
    private int age;
    private int idle = 8;
    private boolean openedWithVolley;
    private int stagger;
    private int fallAllowance;
    private int ringImpactTick = -1;
    private int side;
    private Move move;
    private Vec3 landing;
    private UUID attackTarget;
    private Vec3 axis;
    private Vec3 origin;
    private Vec3 end;
    private Vec3 previous;
    private List<Vec3> sites = List.of();
    private BossAttackFocus focus;

    boolean attacking(WitherRitual ritual, SourceWitherBoss boss) {
        return move != null && age < move.warning + activeStrikeEnd() || !waves.isEmpty()
            || shots.stream().anyMatch(shot -> !shot.isRemoved() && ritual.inArena(boss, shot.position()));
    }

    void step(ServerLevel level, SourceWitherBrain brain, List<ServerPlayer> players) {
        var boss = brain.actor;
        boss.setAlternativeTarget(0, 0);
        if (players.isEmpty()) {
            suspend(boss);
            boss.setTarget(null);
            boss.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (!exposed && boss.getHealth() <= boss.getMaxHealth() * .5f) {
            exposed = true;
            boss.syncRitualHeads(broken, exposed);
        }
        waves.removeIf(wave -> !wave.step(level, boss, players));
        shots.removeIf(shot -> shot.isRemoved());
        var target = move == null ? players.stream().filter(player -> boss.distanceToSqr(player) < 52 * 52)
            .min(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null)
            : players.stream().filter(player -> player.getUUID().equals(attackTarget)).findFirst().orElse(null);
        boss.setTarget(target);
        if (stagger > 0) {
            Vec3 v = boss.getDeltaMovement();
            // Collision stops the descent and keeps a no-gravity Wither grounded across ticks.
            boss.setDeltaMovement(v.x * .62, -.54, v.z * .62);
            if (!boss.onGround() && fallAllowance > 0) {
                --fallAllowance;
                return;
            }
            fallAllowance = 0;
            if (--stagger == 0) boss.setRitualPosture(IDLE);
            return;
        }
        if (move != null) {
            if (target == null || !advance(level, brain, target, players)) {
                if (move == Move.CHARGE && struck.isEmpty()) stagger(boss, STAGGER_TICKS, CHARGE_STAGGER_DESCENT);
                else finish(boss, 7 + Integer.bitCount(broken) * 2);
            }
            return;
        }
        if (target == null) {
            boss.setDeltaMovement(boss.getDeltaMovement().scale(.65));
            return;
        }
        boss.getLookControl().setLookAt(target, 25, 25);
        if (exposed) hover(boss, target.position(), 3.6, 8.5);
        else airborneHover(level, boss, target.position());
        if (idle-- > 0) return;
        if (!openedWithVolley && start(Move.ALTERNATE, level, brain, target, players)) {
            openedWithVolley = true;
            order.used(Move.ALTERNATE);
            return;
        }
        for (Move candidate : order.candidates(boss.getRandom())) {
            if (!start(candidate, level, brain, target, players)) continue;
            order.used(candidate);
            return;
        }
        idle = 3;
    }

    private boolean start(Move candidate, ServerLevel level, SourceWitherBrain brain, ServerPlayer target,
                          List<ServerPlayer> players) {
        var boss = brain.actor;
        side = usableSide(nextSide);
        Vec3 targetBody = target.getBoundingBox().getCenter();
        landing = WitherGroundWave.surface(level, boss, target.position().add(0, .5, 0), 6);
        axis = BossAttackGeometry.flatDirection(target.position().subtract(boss.position()));
        if (axis.lengthSqr() < .5) axis = Vec3.directionFromRotation(0, boss.getYRot());
        double distance = boss.distanceTo(target);
        boolean local = candidate == Move.ALTERNATE || candidate == Move.RETURN || candidate == Move.SWEEP || candidate == Move.GUARD;
        if (local && side == 0 || !local && candidate != Move.CROSS && landing == null) return false;
        if (!waves.isEmpty() && candidate != Move.ALTERNATE && candidate != Move.RETURN && candidate != Move.GUARD) return false;
        if (distance > 32 || candidate == Move.CROSS && broken != 0) return false;
        origin = side == 0 ? brain.headPosition(0) : brain.headPosition(side);
        end = targetBody;
        sites = List.of();
        switch (candidate) {
            case ALTERNATE, RETURN, SWEEP -> {
                if (!BossAttackGeometry.visible(level, boss, origin, end) || !lineHasExit(level, players, origin, end, 1.1)) return false;
                Vec3 lateral = perpendicular(axis);
                if (candidate == Move.ALTERNATE) {
                    sites = List.of(end, end.add(lateral.scale(1.35)), end.subtract(lateral.scale(1.35)), end);
                } else if (candidate == Move.SWEEP) {
                    sites = List.of(end.add(lateral.scale(-6)), end.add(lateral.scale(-3.6)), end.add(lateral.scale(-1.1)),
                        end.add(lateral.scale(1.1)), end.add(lateral.scale(3.6)), end.add(lateral.scale(6)));
                    if (sites.stream().anyMatch(at -> !lineHasExit(level, players, origin, at, 1.2))) return false;
                }
            }
            case BLUE -> {
                if (distance > 23 || !BossAttackGeometry.visible(level, boss, brain.headPosition(0), landing.add(0, .13, 0))
                        || !hasExit(level, players, landing, SourceExplosionKernel.livingRadius(HEAVY_POWER))) return false;
            }
            case CHARGE -> {
                if (distance < 4.5 || distance > 23) return false;
                Vec3 setup = landing.subtract(axis.scale(Math.min(8, distance * .62))).add(0, .65, 0);
                Vec3 supported = WitherGroundWave.surface(level, boss, setup.add(0, 1.8, 0), 4.6);
                if (supported == null) return false;
                origin = supported.add(0, .7, 0);
                Vec3 runout = landing.add(axis.scale(CHARGE_OVERRUN));
                Vec3 stoppingFloor = WitherGroundWave.surface(level, boss, runout.add(0, 1.2, 0), 2.4);
                if (stoppingFloor == null) {
                    runout = landing.add(axis.scale(4));
                    stoppingFloor = WitherGroundWave.surface(level, boss, runout.add(0, 1.2, 0), 2.4);
                }
                if (stoppingFloor == null) return false;
                end = stoppingFloor.add(0, .7, 0);
                if (!exposed && boss.distanceToSqr(origin) > AIRBORNE_CHARGE_SETUP_RANGE * AIRBORNE_CHARGE_SETUP_RANGE) return false;
                if (!bodyPathClear(level, boss, boss.position(), origin) || !bodyPathClear(level, boss, origin, end)
                        || !lineHasExit(level, players, origin, end.add(0, 1.2, 0), CHARGE_WIDTH)) return false;
            }
            case CROSS -> {
                Vec3 offset = perpendicular(axis).scale(3.8);
                sites = List.of(end.add(offset), end.subtract(offset));
                if (!lineHasExit(level, players, brain.headPosition(1), sites.get(1), 1.15)
                        || !lineHasExit(level, players, brain.headPosition(2), sites.get(0), 1.15)) return false;
            }
            case MINE -> {
                if (!hasExit(level, players, landing, MINE_STRIKE_RADIUS)) return false;
                Vec3 mount = landing.add(0, 1.45, 0);
                if (!BossAttackGeometry.visible(level, boss, brain.headPosition(0), mount)) return false;
                var charge = new BossAttackFocus(level, mount);
                charge.setOwner(boss);
                if (!level.addFreshEntity(charge)) return false;
                focus = charge;
            }
            case CURTAIN -> {
                sites = ceilingTargets(level, boss, players);
                if (sites.size() < 3) return false;
            }
            case RING -> {
                if (distance > 19 || !exposed && boss.getY() - landing.y > 5.5 || !clearJump(level, players)
                        || !bodyPathClear(level, boss, boss.position(), landing)) return false;
                ringImpactTick = -1;
            }
            case GALE -> {
                origin = landing.subtract(axis.scale(12)).add(0, 1, 0);
                end = landing.add(axis.scale(12)).add(0, 1, 0);
                if (!lineHasExit(level, players, origin, end, 1.65)) return false;
            }
            case AIRBURST -> {
                end = landing.add(0, 3.4, 0);
                if (Math.abs(targetBody.y - end.y) >= 1.55 || !BossAttackGeometry.visible(level, boss, brain.headPosition(0), end)) return false;
            }
            case GUARD -> {
                if (distance > 13 || !lineHasExit(level, players, origin, end, 1.4)) return false;
            }
        }
        move = candidate;
        attackTarget = target.getUUID();
        age = 0;
        previous = boss.position();
        struck.clear();
        boss.setRitualPosture(move.pose());
        boss.setAlternativeTarget(1, 0);
        boss.setAlternativeTarget(2, 0);
        if (local) boss.setAlternativeTarget(side, target.getId());
        if (move == Move.CROSS) boss.setAlternativeTarget(nextSide, target.getId());
        level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE,
            2.6f, move == Move.CHARGE ? .59f : move == Move.BLUE ? .70f : .90f);
        warn(level, brain);
        return true;
    }

    private int activeStrikeEnd() {
        return move == Move.RING && ringImpactTick >= 0 ? ringImpactTick + move.strike - RING_DIVE_TICKS : move.strike;
    }

    private boolean advance(ServerLevel level, SourceWitherBrain brain, ServerPlayer target, List<ServerPlayer> players) {
        var boss = brain.actor;
        int tick = ++age - move.warning;
        if (age >= move.warning + activeStrikeEnd() + move.recovery) return false;
        if (move == Move.MINE && tick <= 0 && (focus == null || focus.isRemoved())) return false;
        Vec3 looking = move == Move.CHARGE ? end : target.getBoundingBox().getCenter();
        boss.getLookControl().setLookAt(looking.x, looking.y, looking.z, 26, 25);
        if (move != Move.CHARGE) brain.faceRitualTarget(looking, 30);
        if (move == Move.CHARGE) {
            if (tick < 0) {
                drift(boss, origin, .90, .30);
                if (!bodyPathClear(level, boss, boss.position(), boss.position().add(boss.getDeltaMovement()))) {
                    stopCharge(boss);
                    return true;
                }
            } else if (tick >= move.strike) boss.setDeltaMovement(boss.getDeltaMovement().scale(.73));
        } else if (move == Move.RING && ringImpactTick < 0) {
            if (!approachRingImpact(level, boss, tick)) return false;
        } else if (!exposed) {
            airborneHover(level, boss, landing == null ? end.add(0, -.9, 0) : landing);
        } else {
            var old = boss.getDeltaMovement();
            boss.setDeltaMovement(old.x * .81, old.y * .70, old.z * .81);
        }
        if (tick < 0) {
            if (age % 5 == 0) warn(level, brain);
            return true;
        }
        if (tick >= activeStrikeEnd()) return true;
        if (tick == 0 && move != Move.ALTERNATE && move != Move.BLUE && move != Move.CROSS && move != Move.SWEEP) {
            level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE,
                2.3f, move == Move.CHARGE ? .52f : .88f);
        }
        switch (move) {
            case ALTERNATE -> {
                if (tick % 5 == 0) sideShot(brain, usableSide(nextSide), sites.get(tick / 5));
            }
            case BLUE -> { if (tick == 0) track(brain.shootMarked(landing, (float)Math.atan2(axis.z, axis.x))); }
            case CHARGE -> charge(level, brain, players, tick);
            case CROSS -> {
                if (tick == 0 || tick == 4 || tick == 9 || tick == 13) {
                    int head = tick == 0 || tick == 9 ? nextSide : 3 - nextSide;
                    Vec3 at = sites.get(2 - head);
                    sideShot(brain, head, tick >= 9 ? at.lerp(end, .68) : at);
                    boss.setAlternativeTarget(head, boss.getTarget() == null ? 0 : boss.getTarget().getId());
                }
            }
            case MINE -> {
                if (tick == 0) {
                    focus.discard();
                    focus = null;
                    var burst = landing.add(0, 1.45, 0);
                    double radius = MINE_STRIKE_RADIUS;
                    BossCombatVisual.burst(level, boss, burst, radius);
                    // Apply the boss strike before the weaker terrain blast: normal hurt immunity
                    // prevents that blast from adding a second player hit in the same tick.
                    for (var player : players) {
                        if (player.position().distanceToSqr(burst) < radius * radius
                                && BossAttackGeometry.visible(level, boss, burst, player.getBoundingBox().getCenter())) {
                            hurt(level, boss, player, 17, false);
                        }
                    }
                    SourceExplosion.create(level, boss, burst.x, burst.y, burst.z, MINE_POWER, MINE_POWER, false, brain.griefing());
                }
            }
            case CURTAIN -> curtain(level, boss, players, tick);
            case RETURN -> returning(level, boss, players, tick);
            case RING -> {
                if (ringImpactTick >= 0) ring(level, boss, players, tick - ringImpactTick);
            }
            case GALE -> gale(level, boss, players, tick);
            case SWEEP -> { if (tick % 4 == 0) sideShot(brain, side, sites.get(tick / 4)); }
            case AIRBURST -> { if (tick == 8) airburst(level, boss, players); }
            case GUARD -> { if (tick == 3) strike(level, boss, players, brain.headPosition(side), end, 1.55, 14, false); }
        }
        return true;
    }

    private void warn(ServerLevel level, SourceWitherBrain brain) {
        // Charge at the emitting head, never draw the future flight corridor.
        for (int head = 0; head <= 2; ++head) {
            if (head != 0 && usableSide(head) == 0) continue;
            if ((move == Move.GUARD || move == Move.SWEEP || move == Move.RETURN) && head != side) continue;
            Vec3 at = chargeOrigin(brain, head);
            BossCombatVisual.cue(level, brain.actor, BossCombatVisual.Theme.WITHER, at,
                head == 0 && (move == Move.BLUE || move == Move.CHARGE) ? 1.1 : .72,
                Math.max(1, Math.min(5, move.warning - age)));
        }
    }

    static Vec3 chargeOrigin(SourceWitherBrain brain, int head) {
        return brain.headPosition(head).add(brain.actor.getLookAngle().scale(.7));
    }

    private void groundWarning(ServerLevel level, SourceWitherBoss boss, double radius) {
        BossAttackGeometry.point(level, WARNING, landing.add(0, .12, 0));
        for (int i = 0; i < 29; ++i) {
            Vec3 at = landing.add(BossAttackGeometry.flatDirection(i * Math.PI * 2 / 29).scale(radius));
            Vec3 surface = WitherGroundWave.surface(level, boss, at.add(0, 1.8, 0), 3.75);
            if (surface != null) BossAttackGeometry.point(level, WARNING, surface.add(0, .12, 0));
        }
    }

    private void charge(ServerLevel level, SourceWitherBrain brain, List<ServerPlayer> players, int tick) {
        var boss = brain.actor;
        if (tick == 0 && boss.position().distanceToSqr(origin) > 4) {
            finish(boss, 8);
            return;
        }
        Vec3 here = boss.position();
        if (tick > 0 && previous.distanceToSqr(here) > 1.0E-6)
            strike(level, boss, players, previous.add(0, 1.1, 0), here.add(0, 1.1, 0), CHARGE_WIDTH, 18, false);
        previous = here;
        double remaining = end.subtract(here).dot(axis);
        double speed = Mth.clamp(remaining * .40, 0, Mth.lerp(Math.min(1, tick / 6d), .68, 1.27));
        Vec3 motion = axis.scale(speed).add(0, Mth.clamp((end.y - here.y) * .30, -.26, .27), 0);
        if (!bodyPathClear(level, boss, here, here.add(motion))) {
            stopCharge(boss);
            return;
        }
        boss.setDeltaMovement(motion);
    }

    private void stopCharge(SourceWitherBoss boss) {
        boss.setDeltaMovement(Vec3.ZERO);
        stagger(boss, STAGGER_TICKS, CHARGE_STAGGER_DESCENT);
    }

    private static boolean bodyPathClear(ServerLevel level, SourceWitherBoss boss, Vec3 from, Vec3 to) {
        AABB body = boss.getBoundingBox().deflate(.04).move(from.subtract(boss.position()));
        Vec3 motion = to.subtract(from);
        AABB span = body.expandTowards(motion);
        BlockPos low = BlockPos.containing(span.minX, span.minY, span.minZ);
        BlockPos high = BlockPos.containing(span.maxX, span.maxY, span.maxZ);
        if (!level.hasChunksAt(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ())) return false;
        for (var shape : level.getBlockCollisions(boss, span)) {
            for (AABB block : shape.toAabbs()) {
                if (chargeBodyTouches(body, motion, block)) return false;
            }
        }
        return true;
    }

    static boolean chargeBodyTouches(AABB body, Vec3 motion, AABB obstacle) {
        if (body.intersects(obstacle)) return true;
        Vec3 center = body.getCenter();
        AABB expanded = obstacle.inflate(body.getXsize() * .5, body.getYsize() * .5, body.getZsize() * .5).deflate(1.0E-6);
        return expanded.contains(center.add(motion)) || expanded.clip(center, center.add(motion)).isPresent();
    }

    private void curtain(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players, int tick) {
        int index = tick / 5;
        if (index >= sites.size()) return;
        var floor = sites.get(index).add(0, .22, 0);
        var top = floor.add(0, 9.5, 0);
        double fall = tick % 5 / 5d;
        var front = top.lerp(floor, fall);
        if (BossAttackGeometry.visible(level, boss, top, front)) {
            strike(level, boss, players, front, top.lerp(floor, fall + .2), 1.7, 16, true);
        }
    }

    private void returning(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players, int tick) {
        Vec3 from = returnPath((double)tick / move.strike);
        Vec3 to = returnPath((tick + 1d) / move.strike);
        strike(level, boss, players, from, to, 1.10, 14, true);
        if (!BossAttackGeometry.visible(level, boss, from, to)) finish(boss, 6);
    }

    private Vec3 returnPath(double t) {
        Vec3 turn = end.add(axis.scale(6)).add(perpendicular(axis).scale(5.2)).add(0, 4.6, 0);
        Vec3 finish = end.subtract(axis.scale(5.4));
        // Two cubic arcs pass through the locked target with a shared tangent.
        // The old quadratic used that target only to derive its control point and could miss it entirely.
        double u = t <= .5 ? t * 2 : (t - .5) * 2;
        Vec3 first = t <= .5 ? origin : end;
        Vec3 controlA = t <= .5 ? origin.lerp(end, .4).add(perpendicular(axis).scale(2.6)) : end.add(axis.scale(4));
        Vec3 controlB = t <= .5 ? end.subtract(axis.scale(4)) : turn;
        Vec3 last = t <= .5 ? end : finish;
        Vec3 a = first.lerp(controlA, u), b = controlA.lerp(controlB, u), c = controlB.lerp(last, u);
        return a.lerp(b, u).lerp(b.lerp(c, u), u);
    }

    private boolean approachRingImpact(ServerLevel level, SourceWitherBoss boss, int tick) {
        if (tick >= move.strike || !bodyPathClear(level, boss, boss.position(), landing)) {
            boss.setDeltaMovement(Vec3.ZERO);
            return false;
        }
        if (tick >= RING_DIVE_TICKS && boss.onGround() && Math.abs(boss.getY() - landing.y) <= .12
                && boss.getBoundingBox().inflate(.15, 0, .15).contains(landing.add(0, .14, 0))) {
            Vec3 floor = WitherGroundWave.surface(level, boss, landing.add(0, .18, 0), .32);
            if (floor == null || Math.abs(floor.y - landing.y) > .05) return false;
            ringImpactTick = tick;
            boss.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        if (tick < 0) {
            double height = 2 + 4.1 * DragonRepertoire.smooth(age / (double)move.warning);
            drift(boss, landing.add(0, height, 0), .56, .28);
        } else {
            // Aim just below the surface so normal collision supplies the grounded contact.
            drift(boss, landing.add(0, -.25, 0), 1.22, .28);
        }
        return true;
    }

    private void ring(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players, int tick) {
        double radius = .70 * tick;
        Vec3 base = landing.add(0, .32, 0);
        if (radius < .9) BossCombatVisual.coreSegments(level, boss, base,
            visibleRing(level, boss, base, base, radius + .9, 0), .38);
        else BossCombatVisual.ringSegments(level, boss, base, visibleRing(level, boss, base, base, radius, .9), .9, .38);
        for (var player : players) {
            Vec3 feet = player.position();
            if (feet.y > landing.y + .70 || feet.y < landing.y - .55
                    || Math.abs(feet.subtract(landing).horizontalDistance() - radius) > .9) continue;
            if (BossAttackGeometry.visible(level, boss, base, feet.add(0, .26, 0))) hurt(level, boss, player, 15, false);
        }
    }

    private static List<Vec3> visibleRing(ServerLevel level, SourceWitherBoss boss, Vec3 source, Vec3 center, double radius, double halfWidth) {
        var segments = new ArrayList<Vec3>(56);
        for (int i = 0; i < 28; i++) {
            Vec3 first = center.add(BossAttackGeometry.flatDirection(i * Math.PI / 14).scale(radius));
            Vec3 last = center.add(BossAttackGeometry.flatDirection((i + 1) * Math.PI / 14).scale(radius));
            Vec3 mid = first.lerp(last, .5);
            // Each visible arc is checked independently; no full ring is drawn through cover.
            if (BossAttackGeometry.visible(level, boss, source, first)
                    && BossAttackGeometry.visible(level, boss, source, last)
                    && BossAttackGeometry.visible(level, boss, source, mid)
                    && BossAttackGeometry.visible(level, boss, first, last)) {
                Vec3 side = last.subtract(first).normalize().cross(new Vec3(0, 1, 0)).scale(halfWidth);
                boolean clear = true;
                for (Vec3 edge : new Vec3[]{first.add(side), first.subtract(side), last.add(side), last.subtract(side)})
                    clear &= BossAttackGeometry.visible(level, boss, source, edge);
                if (clear) { segments.add(first); segments.add(last); }
            }
        }
        return segments;
    }

    private void gale(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players, int tick) {
        Vec3 heart = landing.add(0, .85, 0);
        if (tick < 17 && tick % 3 == 0) {
            double radius = 9 * (1 - tick / 22d);
            var wind = new ArrayList<Vec3>(24);
            for (int i = 0; i < 12; ++i) {
                double angle = i * Math.PI / 6 + tick * .08;
                Vec3 from = heart.add(BossAttackGeometry.flatDirection(angle).scale(radius));
                Vec3 to = heart.add(BossAttackGeometry.flatDirection(angle + .18).scale(Math.max(.35, radius - 1.8)));
                Vec3 side = to.subtract(from).normalize().cross(new Vec3(0, 1, 0)).scale(.18);
                boolean clear = BossAttackGeometry.visible(level, boss, from, to);
                for (Vec3 edge : new Vec3[]{from.add(side), from.subtract(side), to.add(side), to.subtract(side)})
                    clear &= BossAttackGeometry.visible(level, boss, heart, edge);
                if (clear) { wind.add(from); wind.add(to); }
            }
            BossCombatVisual.windSegments(level, boss, heart, wind);
            for (var player : players) {
                Vec3 toward = heart.subtract(player.position());
                if (Math.abs(toward.y) > 2.6 || toward.horizontalDistanceSqr() > 81
                        || !BossAttackGeometry.visible(level, boss, heart, player.getEyePosition())) continue;
                player.setDeltaMovement(player.getDeltaMovement().add(BossAttackGeometry.flatDirection(toward).scale(.065)));
                player.hurtMarked = true;
            }
        } else if (tick >= 18 && tick < 23) {
            strike(level, boss, players, heart, origin, 1.65, 15, false);
            strike(level, boss, players, heart, end, 1.65, 15, false);
        }
    }

    private void airburst(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players) {
        // Layered blast boundary uses the same spherical radius and vertical limit as the hit below.
        for (double offset : new double[]{-1.2, 0, 1.2}) {
            Vec3 center = end.add(0, offset, 0);
            double radius = Math.sqrt(5.5 * 5.5 - offset * offset) - .1;
            BossCombatVisual.ringSegments(level, boss, end,
                visibleRing(level, boss, end, center, radius, .1), .1, .18);
        }
        for (var player : players) {
            Vec3 at = player.getBoundingBox().getCenter();
            if (Math.abs(at.y - end.y) < 1.55 && at.distanceToSqr(end) < 5.5 * 5.5
                    && BossAttackGeometry.visible(level, boss, end, at)) hurt(level, boss, player, 17, false);
        }
    }

    private void strike(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players,
                        Vec3 from, Vec3 toward, double width, float damage, boolean directSkull) {
        var to = BossAttackGeometry.shorten(level, boss, from, toward);
        // Only the active, wall-clipped strike is rendered. No future corridor is disclosed.
        BossCombatVisual.beam(level, boss, BossCombatVisual.Theme.WITHER, from, to, width, 3);
        for (var player : players) {
            var box = player.getBoundingBox();
            if (BossAttackGeometry.hits(box, from, to, width)
                    && BossAttackGeometry.visible(level, boss, from, box.getCenter())) hurt(level, boss, player, damage, directSkull);
        }
    }

    private void hurt(ServerLevel level, SourceWitherBoss boss, ServerPlayer player, float damage, boolean directSkull) {
        UUID target = player.getUUID();
        if (struck.contains(target) || !player.hurt(level.damageSources().mobAttack(boss), damage)) return;
        struck.add(target);
        if (!directSkull) return;
        int seconds = switch (level.getDifficulty()) { case NORMAL -> 10; case HARD -> 40; default -> 0; };
        if (seconds > 0) player.addEffect(new MobEffectInstance(MobEffects.WITHER, BossCombatTuning.witherDuration(seconds * 20), 1), boss);
    }

    private List<Vec3> ceilingTargets(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players) {
        List<Vec3> points = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            var offset = perpendicular(axis).scale(i * 4.6).add(axis.scale((i & 1) * 3.5));
            var point = WitherGroundWave.surface(level, boss, landing.add(offset).add(0, 2, 0), 5);
            if (point == null || !hasExit(level, players, point, 1.9)) continue;
            if (BossAttackGeometry.visible(level, boss, point.add(0, .18, 0), point.add(0, 9.7, 0))) points.add(point);
        }
        return points;
    }

    private boolean clearJump(ServerLevel level, List<ServerPlayer> players) {
        return players.stream().filter(player -> player.position().distanceToSqr(landing) < 20 * 20)
            .allMatch(player -> !level.getBlockCollisions(player, player.getBoundingBox().move(0, .9, 0)).iterator().hasNext());
    }

    private static boolean hasExit(ServerLevel level, List<ServerPlayer> players, Vec3 center, double radius) {
        for (var player : players) {
            Vec3 offset = player.position().subtract(center);
            if (Math.abs(offset.y) > 2.8 || offset.horizontalDistance() >= radius + .35) continue;
            Vec3 out = offset.horizontalDistanceSqr() < .02 ? new Vec3(1, 0, 0) : BossAttackGeometry.flatDirection(offset);
            boolean escaped = false;
            for (int i = 0; i < 4; ++i) {
                if (DragonCombat.escape(level, player, out, radius + .45 - Math.max(0, out.dot(offset)), radius + .55)) {
                    escaped = true;
                    break;
                }
                out = perpendicular(out);
            }
            if (!escaped) return false;
        }
        return true;
    }

    private static boolean lineHasExit(ServerLevel level, List<ServerPlayer> players, Vec3 from, Vec3 to, double width) {
        var side = perpendicular(BossAttackGeometry.flatDirection(to.subtract(from)));
        if (side.lengthSqr() < .5) side = new Vec3(1, 0, 0);
        for (var player : players) {
            if (BossAttackGeometry.hits(player.getBoundingBox(), from, to, width)
                    && !DragonCombat.escape(level, player, side, width + 1)
                    && !DragonCombat.escape(level, player, side.scale(-1), width + 1)) return false;
        }
        return true;
    }

    private static Vec3 perpendicular(Vec3 direction) { return new Vec3(-direction.z, 0, direction.x); }

    private static void airborneHover(ServerLevel level, SourceWitherBoss boss, Vec3 target) {
        Vec3 from = boss.position();
        Vec3 outward = BossAttackGeometry.flatDirection(from.subtract(target));
        if (outward.lengthSqr() < .5) outward = Vec3.directionFromRotation(0, boss.getYRot()).scale(-1);
        var body = boss.getBoundingBox().deflate(.05);
        Vec3 waypoint = WitherFlightPath.firstLeg(from, target, outward,
            at -> level.hasChunkAt(BlockPos.containing(at)) && level.noCollision(boss, body.move(at.subtract(from))),
            (start, end) -> level.noCollision(boss, WitherFlightPath.nearSweep(body.move(start.subtract(from)), end.subtract(start))));
        if (waypoint == null) boss.setDeltaMovement(boss.getDeltaMovement().scale(.6));
        else drift(boss, waypoint, .85, .35);
    }

    private static void hover(SourceWitherBoss boss, Vec3 target, double height, double range) {
        Vec3 offset = BossAttackGeometry.flatDirection(boss.position().subtract(target));
        if (offset.lengthSqr() < .5) offset = Vec3.directionFromRotation(0, boss.getYRot()).scale(-1);
        Vec3 column = target.add(offset.scale(range));
        for (int drop = 0; drop <= 6; drop += 3) {
            Vec3 at = column.add(0, Math.max(1.8, height - drop), 0);
            var box = boss.getBoundingBox().move(at.subtract(boss.position())).deflate(.05);
            if (boss.level().noCollision(boss, box)) {
                drift(boss, at, .85, .35);
                return;
            }
        }
        boss.setDeltaMovement(boss.getDeltaMovement().scale(.6));
    }

    private static void drift(SourceWitherBoss boss, Vec3 at, double maxSpeed, double blend) {
        Vec3 delta = at.subtract(boss.position());
        Vec3 desired = delta.normalize().scale(Math.min(delta.length() * .24, maxSpeed));
        boss.setDeltaMovement(boss.getDeltaMovement().lerp(desired, blend));
    }

    private void track(SourceWitherSkull shot) { if (shot != null) shots.add(shot); }

    private int usableSide(int preferred) {
        if ((broken & (1 << (preferred - 1))) == 0) return preferred;
        return broken == 3 ? 0 : 3 - preferred;
    }

    private void sideShot(SourceWitherBrain brain, int head, Vec3 target) {
        if (head == 0 || (broken & (1 << (head - 1))) != 0) return;
        track(brain.shootRitual(head, target));
        if (move == Move.ALTERNATE) {
            brain.actor.setAlternativeTarget(3 - head, 0);
            var player = brain.actor.getTarget();
            brain.actor.setAlternativeTarget(head, player == null ? 0 : player.getId());
            nextSide = 3 - head;
        }
    }

    void landed(Vec3 impact, float angle) { waves.add(new WitherGroundWave(impact, angle)); }

    void onDamage(SourceWitherBoss boss, DamageSource damage) {
        if (move != Move.CHARGE || age >= move.warning + 2 || !(damage.getEntity() instanceof Player player)
                || damage.getDirectEntity() != player || boss.distanceToSqr(player) > 5 * 5) return;
        stagger(boss, STAGGER_TICKS, CHARGE_STAGGER_DESCENT);
    }

    private void stagger(SourceWitherBoss boss, int duration, int descent) {
        finish(boss, STAGGER_IDLE_TICKS);
        clearHazards();
        if (boss.level() instanceof ServerLevel level) BossArenaPortals.release(level, boss.getUUID());
        stagger = duration;
        fallAllowance = descent;
        boss.setRitualPosture(STAGGERED);
        boss.setDeltaMovement(boss.getDeltaMovement().scale(.36).add(0, -.46, 0));
        boss.level().playSound(null, boss.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 2.6f, .62f);
    }

    boolean recovering() { return stagger > 0; }

    boolean canRegenerate(int tick) {
        if (recovering()) return false;
        return switch (broken) {
            case 0 -> true;
            case 1, 2 -> tick % 40 == 0;
            default -> false;
        };
    }

    int brokenHeads() { return broken; }
    boolean headsExposed() { return exposed; }

    void damageHead(SourceWitherBoss boss, int head, float damage) {
        if ((headDamage[head - 1] += damage) < HEAD_TO_BREAK) return;
        broken |= 1 << (head - 1);
        boss.syncRitualHeads(broken, exposed);
        boss.setAlternativeTarget(head, 0);
        if (broken == 3) stagger(boss, STAGGER_TICKS, HEAD_BREAK_STAGGER_DESCENT);
        else if (move == Move.CROSS || side == head && (move == Move.SWEEP || move == Move.GUARD || move == Move.ALTERNATE
                || move == Move.RETURN)) finish(boss, 11);
        if (boss.level() instanceof ServerLevel level) {
            Vec3 at = boss.sourceBrain().headPosition(head);
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 22, .28, .28, .28, .12);
            level.playSound(null, boss.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.1f, .78f);
        }
        boss.ritual().headBroke(boss);
    }

    float protectedDamage(SourceWitherBoss boss, DamageSource source, float damage, int part) {
        if (part != 0) return damage;
        float scale = !exposed ? 1 : switch (Integer.bitCount(broken)) { case 0 -> .73f; case 1 -> .88f; default -> 1; };
        if (move == Move.GUARD && age < move.warning && source.getEntity() != null) {
            var from = BossAttackGeometry.flatDirection(source.getEntity().position().subtract(boss.position()));
            if (axis.dot(from) > .28) scale *= .64f;
        }
        return damage * scale;
    }

    boolean deflectArrow(SourceWitherBoss boss, DamageSource damage) {
        if (!exposed || broken == 3) return false;
        if (broken == 0 || damage.getEntity() == null) return true;
        Vec3 attacker = damage.getEntity().position().subtract(boss.position());
        Vec3 protectedSide = boss.sourceBrain().headPosition(usableSide(1)).subtract(boss.position());
        return protectedSide.multiply(1, 0, 1).dot(attacker) > 0;
    }

    static float jawOpening(int posture, long age) {
        Move move = Move.fromPose(posture);
        if (move == null) return 0;
        if (move == Move.CHARGE) return age < move.warning ? Mth.clamp((age - 7) / 12f, 0, 1)
            : Mth.clamp(1 - (age - move.warning - 6) / 14f, 0, 1);
        if (move == Move.BLUE || move == Move.CURTAIN || move == Move.AIRBURST || move == Move.MINE) {
            return age < move.warning ? Mth.clamp(age / (float)move.warning, 0, 1) * .42f
                : Mth.clamp(1 - (age - move.warning) / 12f, 0, 1) * .65f;
        }
        return 0;
    }

    void suspend(SourceWitherBoss boss) {
        suspend(boss, false);
    }

    void suspend(SourceWitherBoss boss, boolean keepStagger) {
        if (move != null || (!keepStagger && stagger != 0)) finish(boss, 12);
        if (!keepStagger) {
            stagger = 0;
            fallAllowance = 0;
        }
        clearHazards();
    }

    private void clearHazards() {
        waves.clear();
        for (SourceWitherSkull shot : shots) if (!shot.isRemoved()) shot.discard();
        shots.clear();
    }

    private void finish(SourceWitherBoss boss, int delay) {
        if (side != 0 && (move == Move.SWEEP || move == Move.RETURN || move == Move.GUARD) && age >= move.warning) {
            nextSide = 3 - side;
        }
        if (focus != null) focus.discard();
        focus = null;
        move = null;
        sites = List.of();
        struck.clear();
        idle = delay;
        boss.setRitualPosture(IDLE);
        boss.setAlternativeTarget(1, 0);
        boss.setAlternativeTarget(2, 0);
    }

    void save(CompoundTag tag) {
        tag.putBoolean("MiteWitherHeadPhase", exposed);
        tag.putInt("MiteWitherBrokenHeads", broken);
        tag.putFloat("MiteWitherHeadLeft", headDamage[0]);
        tag.putFloat("MiteWitherHeadRight", headDamage[1]);
        tag.putInt("MiteWitherNextSide", nextSide);
        if (stagger > 0) {
            tag.putInt("MiteWitherStagger", stagger);
            tag.putInt("MiteWitherStaggerFallAllowance", fallAllowance);
        }
    }

    boolean isStaggered() {
        return stagger > 0;
    }

    void load(CompoundTag tag) {
        exposed = tag.getBoolean("MiteWitherHeadPhase");
        broken = tag.getInt("MiteWitherBrokenHeads") & 3;
        headDamage[0] = Mth.clamp(tag.getFloat("MiteWitherHeadLeft"), 0, HEAD_TO_BREAK);
        headDamage[1] = Mth.clamp(tag.getFloat("MiteWitherHeadRight"), 0, HEAD_TO_BREAK);
        nextSide = tag.getInt("MiteWitherNextSide") == 2 ? 2 : 1;
        stagger = Mth.clamp(tag.getInt("MiteWitherStagger"), 0, STAGGER_TICKS);
        if (stagger > 0) {
            fallAllowance = Mth.clamp(tag.getInt("MiteWitherStaggerFallAllowance"), 0, HEAD_BREAK_STAGGER_DESCENT);
            idle = STAGGER_IDLE_TICKS;
        }
    }
}
