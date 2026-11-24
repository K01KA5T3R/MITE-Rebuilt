package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class GiantZombieCombat {
    enum Move {
        SLAM(19, 9, 19, 42), STOMP(15, 23, 6), STRIDE(19, 20, 6), SWEEP(12, 13, 7),
        GRAB(14, 12, 8), FISSURE(16, 23, 6), BOULDER(19, 18, 7, 36), SHOULDER(15, 15, 7, 42),
        BACK_KICK(12, 11, 8), KNEEL(14, 9, 12), HEAVE(17, 19, 6), COMBO(13, 36, 9);

        final int warning;
        final int strike;
        final int recovery;
        final int missRecovery;
        private static final Move[] BY_ACTION = values();

        Move(int warning, int strike, int recovery) {
            this(warning, strike, recovery, 0);
        }

        Move(int warning, int strike, int recovery, int missRecovery) {
            this.warning = warning;
            this.strike = strike;
            this.recovery = recovery;
            this.missRecovery = missRecovery;
        }

        int action() { return ordinal() + 1; }
        static Move fromAction(int action) { return action > 0 && action <= BY_ACTION.length ? BY_ACTION[action - 1] : null; }
    }

    private static final SimpleParticleType HIT = ParticleTypes.POOF;
    static final int STAGGER_LENGTH = 54;
    private static final float LEG_STAGGER = 42;
    private static final float COMBINED_LEG_STAGGER = 68;
    private static final double STOMP_CORE = 2.9;
    private static final double GRAB_WIDTH = 1.35;
    private static final double SLAM_RADIUS = 3.35;
    private static final double BOULDER_RADIUS = 4.1;
    private static final double BOULDER_BODY = 1.05;
    static final int KNEEL_CONTACT_TICK = 6;
    static final float KNEEL_ARM_CONTACT = -.60f;
    private static final double KNEEL_PALM_SIDE = 2.25;
    private static final double KNEEL_HALF_LENGTH = 1.05;
    private static final double KNEEL_HALF_WIDTH = 1.23;
    private static final double KNEEL_SHOCK_HEIGHT = .82;
    private static final double BACK_KICK_FOOT_WIDTH = .82;
    private static final double BACK_KICK_GROUND_WIDTH = .94;
    private static final double BACK_KICK_GROUND_HEIGHT = .72;
    static final int COMBO_SWEEP_TICKS = 8;
    static final int COMBO_ELBOW_START = 24;
    static final float COMBO_ELBOW_YAW = .30f;
    private static final double COMBO_WAVE_INNER = .85;
    private static final double COMBO_SWEEP_REACH = 6.2;
    private static final double COMBO_SWEEP_WIDTH = .93;
    private static final double COMBO_SWEEP_HEIGHT = .72;
    private static final double COMBO_ELBOW_REACH = 8;
    private static final double COMBO_ELBOW_WIDTH = 1.08;
    private static final double COMBO_ELBOW_HEIGHT = .86;
    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private final Set<UUID> struck = new HashSet<>();
    private final float[] balance = new float[2];
    private Move move;
    private Vec3 origin;
    private Vec3 center;
    private Vec3 axis;
    private Vec3 previous;
    private Vec3 boulderStart;
    private List<Vec3> steps = List.of();
    private int age;
    private int idle = 6;
    private int stalled;
    private int down;
    private int counterTicks;
    private int crowded;
    private boolean landedHit;

    boolean attacking() { return move != null && age < move.warning + move.strike; }

    float legDamageMultiplier() { return counterTicks > 0 ? .85f : .66f; }

    boolean step(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players) {
        if (counterTicks > 0) --counterTicks;
        if (down > 0) {
            brake(giant, .38);
            if (--down == 0) giant.action(0);
            return true;
        }
        var target = players.stream().min(Comparator.comparingDouble(giant::distanceToSqr)).orElseThrow();
        crowded = giant.distanceToSqr(target) <= STOMP_CORE * STOMP_CORE ? Math.min(crowded + 1, 22) : 0;
        if (move != null) {
            if (!advance(level, giant, players)) {
                int rest = switch (move) {
                    case SLAM, STOMP, COMBO -> 16;
                    default -> giant.getHealth() <= giant.getMaxHealth() * .5f ? 9 : 12;
                };
                finish(giant, rest);
            }
            return true;
        }
        if (idle > 0 || counterTicks > 0) {
            if (idle > 0) --idle;
            brake(giant, counterTicks > 0 ? 0 : .54);
            return true;
        }
        Vec3 heading = BossAttackGeometry.flatDirection(target.position().subtract(giant.position()));
        boolean reachable = giant.distanceToSqr(target) < 8 * 8 && Math.abs(target.getY() - giant.getY()) < 2.5
            && BossAttackGeometry.visible(level, giant, giant.position().add(0, 1.3, 0), target.getEyePosition());
        if (reachable || giant.distanceToSqr(target) > 5 * 5 && walk(level, giant, heading)) stalled = Math.max(0, stalled - 2);
        else if (++stalled > 190) return false;
        if (crowded == 22 && start(Move.STOMP, level, giant, target, players)) {
            order.used(Move.STOMP);
            stalled = 0;
            return true;
        }
        for (var candidate : order.candidates(giant.getRandom())) {
            if (!start(candidate, level, giant, target, players)) continue;
            order.used(candidate);
            stalled = 0;
            return true;
        }
        if (++stalled > 190) return false;
        idle = 4;
        return true;
    }

    private boolean start(Move candidate, ServerLevel level, SourceGiantZombieBoss giant, ServerPlayer target,
                          List<ServerPlayer> players) {
        origin = giant.position();
        Vec3 delta = target.position().subtract(origin);
        double range = delta.horizontalDistance();
        var facing = Vec3.directionFromRotation(0, giant.yBodyRot);
        axis = range < .15 ? facing : BossAttackGeometry.flatDirection(delta);
        if (candidate == Move.BACK_KICK) {
            if (range > 9 || facing.dot(axis) > -.48) return false;
            axis = facing.scale(-1);
        }
        if (candidate == Move.COMBO && giant.getHealth() > giant.getMaxHealth() * .5f) return false;
        if (candidate == Move.KNEEL) {
            if (Math.max(balance[0], balance[1]) < 9) return false;
            axis = kneelFacing(axis, range);
        }
        if (range > range(candidate) || Math.abs(delta.y) > (candidate == Move.BOULDER ? 14 : 5)) return false;
        if ((candidate == Move.STRIDE || candidate == Move.SHOULDER) && range < 5.3) return false;

        center = floor(level, giant, switch (candidate) {
            case STOMP, SWEEP, BACK_KICK, GRAB, COMBO, KNEEL -> origin;
            case SLAM -> origin.add(axis.scale(5.3));
            case STRIDE, SHOULDER -> origin.add(axis.scale(Math.min(range + 2.5, candidate == Move.STRIDE ? 14 : 9)));
            default -> target.position();
        });
        if (center == null || !giant.inArena(center)) return false;
        steps = List.of();
        if (candidate == Move.STRIDE || candidate == Move.SHOULDER) {
            steps = walkingPath(level, giant, center);
            if (steps.size() < 4 || players.stream().anyMatch(player -> !passageExit(level, player, origin, center, 2.3))) return false;
            center = steps.get(steps.size() - 1);
        }
        if (candidate == Move.SLAM && !canSlam(level, players)) return false;
        if (candidate == Move.BOULDER && !exits(level, players, center, BOULDER_RADIUS)) return false;
        if (candidate == Move.HEAVE && !canHeave(level, giant, players)) return false;
        if (candidate == Move.KNEEL && !canKneel(level, giant, target, players)) return false;
        if (candidate == Move.GRAB && !canGrab(level, giant, target, players)) return false;
        if (candidate == Move.COMBO && !canCombo(level, giant, target, players)) return false;
        if (candidate == Move.STOMP && !jumpSpace(level, players, center, 21)) return false;
        if (candidate == Move.FISSURE && players.stream().anyMatch(player -> !passageExit(level, player, origin,
                center.add(axis.scale(4)), 1.65))) return false;
        if (candidate == Move.BOULDER && !BossAttackGeometry.visible(level, giant, center.add(0, .25, 0), center.add(0, 7, 0))) return false;

        move = candidate;
        boulderStart = null;
        landedHit = false;
        if (move == Move.STOMP) crowded = 0;
        if (move == Move.KNEEL) brake(giant, 0);
        age = 0;
        previous = origin;
        struck.clear();
        giant.lockAttackFacing(move == Move.BACK_KICK ? axis.scale(-1) : axis);
        giant.action(move.action());
        giant.setYRot(yaw(move == Move.BACK_KICK ? axis.scale(-1) : axis, giant.getYRot(), Math.max(10, 180f / move.warning)));
        giant.yBodyRot = giant.getYRot();
        level.playSound(null, giant.blockPosition(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 3, .54f);
        warn(level, giant);
        return true;
    }

    private boolean advance(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players) {
        int tick = ++age - move.warning;
        if (age >= move.warning + move.strike + move.recovery) return false;
        if (tick == move.strike && !landedHit) counterTicks = move.missRecovery;
        brake(giant, counterTicks > 0 || move == Move.KNEEL ? 0 : tick < 0 ? .63 : .78);
        if (tick < 0) {
            // Complete the turn toward the locked attack axis during the wind-up.
            // A back kick keeps the target behind the body rather than turning to face it.
            giant.setYRot(yaw(move == Move.BACK_KICK ? axis.scale(-1) : axis,
                giant.getYRot(), Math.max(10, 180f / move.warning)));
            giant.yBodyRot = giant.getYRot();
            if (age % 5 == 0) warn(level, giant);
            return true;
        }
        if (tick >= move.strike) return true;
        if (tick == 0 || move == Move.COMBO && tick == 22) {
            level.playSound(null, giant.blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 3.6f, .68f);
        }
        switch (move) {
            case SLAM -> {
                if (tick == 3) {
                    impact(level, giant, players, center.add(right().scale(2.25)), SLAM_RADIUS, 18);
                    impact(level, giant, players, center.subtract(right().scale(2.25)), SLAM_RADIUS, 18);
                }
            }
            case STOMP -> stomp(level, giant, players, tick);
            case STRIDE, SHOULDER -> travel(level, giant, players, tick);
            case SWEEP -> sweep(level, giant, players, origin.add(0, 1.30, 0), axis, tick / 12d, 9.2, 16, .25);
            case GRAB -> grab(level, giant, players, tick);
            case FISSURE -> fissure(level, giant, players, tick);
            case BOULDER -> boulder(level, giant, players, tick);
            case BACK_KICK -> backKick(level, giant, players, tick);
            case KNEEL -> {
                if (tick == KNEEL_CONTACT_TICK) kneelImpact(level, giant, players);
            }
            case HEAVE -> heave(level, giant, players, tick);
            case COMBO -> combo(level, giant, players, tick);
        }
        return true;
    }

    private void warn(ServerLevel level, SourceGiantZombieBoss giant) {
        // Wind-up belongs to the body; never outline the future hit area or travel path.
        Vec3 feet=giant.position();
        switch(move) {
            case STOMP, STRIDE, BACK_KICK -> giant.dust(level,feet.add(right().scale(.85)),8,.55);
            case SLAM, HEAVE, BOULDER -> giant.dust(level,feet,10,1.1);
            default -> giant.dust(level,feet,5,.6);
        }
    }

    private void stomp(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        if (tick == 0) giant.dust(level, center, 36, 2.1);
        double radius = STOMP_CORE + tick * .78;
        Vec3 start = center.add(0, .24, 0);
        BossCombatVisual.earthRing(level,giant,start,stompSectors(level,giant,start,radius,tick==0?0:.94),tick==0);
        for (var player : players) {
            Vec3 feet = player.position();
            double distance = feet.subtract(center).horizontalDistance();
            if (feet.y < center.y - .45 || feet.y > center.y + .72
                    || (tick == 0 ? distance > STOMP_CORE : Math.abs(distance - radius) > .94)) continue;
            if (BossAttackGeometry.visible(level, giant, start, feet.add(0, .21, 0))) {
                hurt(level, giant, player, tick == 0 ? 18 : 15, tick == 0 ? .34 : 0);
            }
        }
    }

    private static List<Vec3> stompSectors(ServerLevel level, SourceGiantZombieBoss giant, Vec3 base,double radius,double width) {
        var points=new ArrayList<Vec3>(56);
        for(int i=0;i<28;i++) {
            Vec3 first=base.add(BossAttackGeometry.flatDirection(i*Math.PI/14).scale(radius));
            Vec3 last=base.add(BossAttackGeometry.flatDirection((i+1)*Math.PI/14).scale(radius));
            Vec3 normal=last.subtract(first).normalize().cross(new Vec3(0,1,0)).scale(width);
            boolean clear=BossAttackGeometry.visible(level,giant,first,last);
            for(Vec3 at:new Vec3[]{first,last,first.lerp(last,.5),first.add(normal),first.subtract(normal),last.add(normal),last.subtract(normal)}) {
                clear &= BossAttackGeometry.visible(level,giant,base,at);
                clear &= BossAttackGeometry.visible(level,giant,base.add(0,.48,0),at.add(0,.48,0));
            }
            if(clear){points.add(first);points.add(last);}
        }
        return points;
    }

    private void travel(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        int at = Math.min(steps.size() - 1, (int)(tick / (double)move.strike * steps.size()));
        Vec3 goal = steps.get(at);
        Vec3 delta = goal.subtract(giant.position());
        var forward = BossAttackGeometry.flatDirection(delta);
        if (delta.horizontalDistance() > .18 && passage(level, giant, goal) && giant.inArena(goal)) {
            double speed = Math.min(delta.horizontalDistance(), move == Move.SHOULDER ? .98 : .88);
            Vec3 v = giant.getDeltaMovement();
            giant.setDeltaMovement(v.x * .42 + forward.x * speed, v.y, v.z * .42 + forward.z * speed);
        }
        Vec3 here = giant.position();
        if (tick > 0 && previous.distanceToSqr(here) > .03) {
            double h = move == Move.SHOULDER ? 1.55 : .85;
            strike(level, giant, players, previous.add(0, h, 0), here.add(0, h, 0), 2.35, 17, .42);
        }
        if (tick == move.strike - 1 && here.distanceToSqr(center) < 9) impact(level, giant, players, here, 3.1, 16);
        previous = here;
    }

    private void sweep(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, Vec3 at, Vec3 facing,
                       double progress, double range, float damage, double shove) {
        double angle = Math.atan2(facing.z, facing.x) + Mth.lerp(Mth.clamp(progress, 0, 1), -1.12, 1.12);
        var tip = at.add(BossAttackGeometry.flatDirection(angle).scale(range));
        strike(level, giant, players, at.lerp(tip, .08), tip, 1.35, damage, shove);
    }

    private Vec3 grip(int side, double progress) {
        return origin.add(axis.scale(2.7 + 2.7 * progress))
            .add(right().scale(side * Mth.lerp(progress, 5.1, .45))).add(0, 1.8, 0);
    }

    private record GripPath(Vec3 start, Vec3 end) {
        boolean touches(AABB box) {
            return start.distanceToSqr(end) > 1.0E-6 && BossAttackGeometry.hits(box, start, end, GRAB_WIDTH);
        }
    }

    private GripPath gripPath(ServerLevel level, SourceGiantZombieBoss giant, int side, double progress) {
        Vec3 start = grip(side, 0);
        if (!BossAttackGeometry.visible(level, giant, origin.add(0, 1.8, 0), start)) return new GripPath(start, start);
        return new GripPath(start, BossAttackGeometry.shorten(level, giant, start, grip(side, progress)));
    }

    private boolean canGrab(ServerLevel level, SourceGiantZombieBoss giant, ServerPlayer target, List<ServerPlayer> players) {
        var left = gripPath(level, giant, -1, 1);
        var right = gripPath(level, giant, 1, 1);
        if (!left.touches(target.getBoundingBox()) && !right.touches(target.getBoundingBox())) return false;
        for (var player : players) {
            AABB box = player.getBoundingBox();
            if (!left.touches(box) && !right.touches(box)) continue;
            if (!canLeaveAttack(level, player, box, at -> left.touches(at) || right.touches(at))) return false;
        }
        return true;
    }

    private boolean canLeaveAttack(ServerLevel level, ServerPlayer player, AABB box, Predicate<AABB> threatened) {
        double forwardAngle = Math.atan2(axis.z, axis.x);
        for (int i = 0; i < 8; ++i) {
            Vec3 direction = BossAttackGeometry.flatDirection(forwardAngle + i * Math.PI / 4);
            for (double distance : new double[]{1.6, 3.2}) {
                AABB end = box.move(direction.scale(distance));
                if (!threatened.test(end) && DragonCombat.escape(level, player, direction, distance)) return true;
            }
        }
        return false;
    }

    private void grab(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        double t0 = DragonRepertoire.smooth(tick / 12d);
        double t1 = DragonRepertoire.smooth((tick + 1d) / 12d);
        for (int side : new int[]{-1, 1}) {
            // Trace from the first palm position so later steps cannot restart beyond a wall.
            GripPath path = gripPath(level, giant, side, t1);
            Vec3 from = grip(side, t0);
            if (path.start.distanceToSqr(path.end) > path.start.distanceToSqr(from) + 1.0E-6) {
                strike(level, giant, players, from, path.end, GRAB_WIDTH, 16, -.30);
            }
        }
    }

    private void fissure(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        if(previous==null)return;
        var next = floor(level, giant, origin.add(axis.scale(1.1 + tick * .82)));
        if (next == null || Math.abs(next.y - previous.y) > .8
                || !BossAttackGeometry.visible(level, giant, previous.add(0, .46, 0), next.add(0, .46, 0))) {
            previous=null;
            return;
        }
        strike(level, giant, players, previous.add(0, .38, 0), next.add(0, .38, 0), 1.55, 15, .28);
        giant.dust(level, next, 3, 1.1);
        BossCombatVisual.earthRift(level,giant,next.add(0,.05,0));
        previous = next;
    }

    private void boulder(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        double first = tick / (double)move.strike;
        double second = (tick + 1d) / move.strike;
        if (boulderStart == null) boulderStart = boulderRelease(giant.position(), axis);
        Vec3 release = boulderStart;
        Vec3 floor = center.add(0, .22, 0);
        Vec3 from = release.lerp(floor, first).add(0, Math.sin(Math.PI * first) * 4.7, 0);
        Vec3 to = release.lerp(floor, second).add(0, Math.sin(Math.PI * second) * 4.7, 0);
        if (tick > 0 && previous == null) return;
        var contact = boulderContact(level, giant, from, to);
        if (contact != null) {
            giant.dust(level, contact.getFirst(), 9, .85);
            if (contact.getSecond()) impact(level, giant, players, contact.getFirst().add(0,-BOULDER_BODY,0), BOULDER_RADIUS,18);
            previous = null;
            return;
        }
        BossCombatVisual.boulder(level, giant, from, to);
        strike(level, giant, players, from, to, 1.35, 17, .34);
        if (tick + 1 == move.strike) impact(level, giant, players, center, BOULDER_RADIUS, 18);
    }

    static Vec3 boulderRelease(Vec3 feet, Vec3 facing) {
        // Vanilla giant scale6: right-arm pivot(-5,2,0), palm(-1,10,0), pitch=-PI/2.
        // LivingEntityRenderer flips model Y and translates by-1.501 before the arm transform.
        Vec3 forward=BossAttackGeometry.flatDirection(facing);
        return feet.add(new Vec3(-forward.z,0,forward.x).scale(2.25))
            .add(forward.scale(3.75)).add(0,(1.501-2d/16)*6,0);
    }

    static com.mojang.datafixers.util.Pair<Vec3,Boolean> boulderContact(ServerLevel level, SourceGiantZombieBoss giant, Vec3 from, Vec3 to) {
        var swept=new net.minecraft.world.phys.AABB(from,to).inflate(BOULDER_BODY);
        com.mojang.datafixers.util.Pair<Vec3,Boolean> closest=null;
        double best=Double.POSITIVE_INFINITY;
        for(var shape:level.getBlockCollisions(giant,swept)) for(var box:shape.toAabbs()) {
            var expanded=box.inflate(BOULDER_BODY);
            var point=expanded.contains(from)?java.util.Optional.of(from):expanded.clip(from,to);
            if(point.isEmpty())continue;
            Vec3 at=point.get();double distance=from.distanceToSqr(at);
            if(distance>=best)continue;
            boolean floor=to.y<from.y && from.y>=expanded.maxY-1e-6 && Math.abs(at.y-expanded.maxY)<1e-5;
            closest=com.mojang.datafixers.util.Pair.of(at,floor);best=distance;
        }
        return closest;
    }

    static Vec3 backKickFoot(Vec3 feet, Vec3 bodyFacing, double tick) {
        // Native giant scale6, leg pivot(-1.9,12,0), sole(0,12,0), root Y1.501.
        double angle=Mth.lerp(Mth.clamp(tick/7,0,1),-1.12,1.12);
        double radius=Math.sin(1.05)*4.5;
        Vec3 forward=BossAttackGeometry.flatDirection(bodyFacing);
        Vec3 right=new Vec3(-forward.z,0,forward.x);
        return feet.add(right.scale(.7125-Math.sin(angle)*radius))
            .add(forward.scale(-Math.cos(angle)*radius)).add(0,(1.501-12d/16)*6-Math.cos(1.05)*4.5,0);
    }

    static Vec3 backKickGroundEnd(Vec3 feet, Vec3 rear, double tick) {
        double angle = Mth.lerp(Mth.clamp(tick / 7, 0, 1), -1.12, 1.12);
        Vec3 side = new Vec3(-rear.z, 0, rear.x);
        return feet.add(rear.scale(Math.cos(angle) * 7.5)).add(side.scale(Math.sin(angle) * 7.5));
    }

    private void backKick(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        if (tick >= 8) return;
        Vec3 feet = giant.position();
        Vec3 body = axis.scale(-1);
        Vec3 foot = backKickFoot(feet, body, tick);
        Vec3 prior = backKickFoot(feet, body, Math.max(0, tick - 1));
        Vec3 hipAtFootHeight = new Vec3(feet.x, foot.y, feet.z);
        if (!kickFootPathOpen(level, giant, hipAtFootHeight, foot)) return;
        if (kickFootPathOpen(level, giant, hipAtFootHeight, prior) && kickFootPathOpen(level, giant, prior, foot)) {
            strike(level, giant, players, prior, foot, BACK_KICK_FOOT_WIDTH, 16, .48);
        } else {
            strike(level, giant, players, foot, foot, BACK_KICK_FOOT_WIDTH, 16, .48);
        }

        Vec3 projectedFoot = new Vec3(foot.x, feet.y, foot.z);
        List<Vec3> footing = traceGroundStrip(level, giant, feet, projectedFoot, .48, BACK_KICK_GROUND_HEIGHT);
        if (footing.isEmpty()) return;
        Vec3 supportedFoot = footing.get(footing.size() - 1);
        if (supportedFoot.subtract(projectedFoot).horizontalDistanceSqr() > 1e-8) return;
        groundWave(level, giant, players, projectedFoot, backKickGroundEnd(feet, axis, tick), 0,
            BACK_KICK_GROUND_WIDTH, BACK_KICK_GROUND_HEIGHT, 16, .48, tick % 2 == 0);
    }

    private static boolean kickFootPathOpen(ServerLevel level, SourceGiantZombieBoss giant, Vec3 from, Vec3 to) {
        AABB reach = new AABB(from, to).inflate(BACK_KICK_FOOT_WIDTH);
        for (var shape : level.getBlockCollisions(giant, reach)) {
            for (AABB box : shape.toAabbs()) {
                if (BossAttackGeometry.hits(box, from, to, BACK_KICK_FOOT_WIDTH)) return false;
            }
        }
        return true;
    }

    private boolean canSlam(ServerLevel level, List<ServerPlayer> players) {
        var lateral = right().scale(2.25);
        Vec3 left = center.add(lateral), right = center.subtract(lateral);
        Predicate<AABB> threat = box -> {
            var feet = new Vec3((box.minX + box.maxX) * .5, box.minY, (box.minZ + box.maxZ) * .5);
            return inImpact(feet.subtract(left), SLAM_RADIUS) || inImpact(feet.subtract(right), SLAM_RADIUS);
        };
        return playersHaveExit(level, players, threat);
    }

    private boolean canHeave(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players) {
        List<Vec3> columns = new ArrayList<>();
        for (int tick = 0; tick < Move.HEAVE.strike; tick += 3) {
            for (var site : heaveSites(level, giant, tick)) {
                Vec3 base = site.add(0, .2, 0);
                columns.add(base);
                columns.add(BossAttackGeometry.shorten(level, giant, base, site.add(0, 2.7, 0)));
            }
        }
        if (columns.isEmpty()) return false;
        return playersHaveExit(level, players, box -> {
            for (int i = 0; i < columns.size(); i += 2) {
                if (BossAttackGeometry.hits(box, columns.get(i), columns.get(i + 1), 1.4)) return true;
            }
            return false;
        });
    }

    private boolean playersHaveExit(ServerLevel level, List<ServerPlayer> players, Predicate<AABB> threat) {
        for (var player : players) {
            var box = player.getBoundingBox();
            if (threat.test(box) && !canLeaveAttack(level, player, box, threat)) return false;
        }
        return true;
    }

    private List<Vec3> heaveSites(ServerLevel level, SourceGiantZombieBoss giant, int tick) {
        var sites = new ArrayList<Vec3>(3);
        var offset = right().scale(2.4 + tick * .34);
        for (int side : tick == 0 ? new int[]{-1, 0, 1} : new int[]{-1, 1}) {
            var at = floor(level, giant, center.add(offset.scale(side)));
            if (at != null && BossAttackGeometry.visible(level, giant, center.add(0, .28, 0), at.add(0, .28, 0))) {
                sites.add(at);
            }
        }
        return sites;
    }

    private void heave(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        if (tick % 3 != 0) return;
        for (var at : heaveSites(level, giant, tick)) {
            Vec3 base = at.add(0, .2, 0);
            Vec3 top = at.add(0, 2.7, 0);
            strike(level, giant, players, base, top, 1.4, 16, .26);
            BossCombatVisual.earthColumn(level, giant, base, BossAttackGeometry.shorten(level, giant, base, top));
            giant.dust(level, at, 14, .65);
        }
    }

    static float kneelBend(float strikeTick) {
        Move kneel = Move.KNEEL;
        if (strikeTick <= KNEEL_CONTACT_TICK) {
            return (float) DragonRepertoire.smooth((strikeTick + kneel.warning) / (kneel.warning + KNEEL_CONTACT_TICK));
        }
        return 1 - (float) DragonRepertoire.smooth((strikeTick - 8) / (kneel.strike + kneel.recovery - 8));
    }

    static double kneelDrop(float bend) {
        // The lower edge of each rotated leg stays at ground height as the hips sink.
        return Math.max(0, legDrop(bend));
    }

    private static double legDrop(float bend) { return 4.5 * (1 - Math.cos(bend)) - .75 * Math.sin(bend); }

    static float fallenProgress(float age) {
        return (float) (DragonRepertoire.smooth(age / 11) * DragonRepertoire.smooth((STAGGER_LENGTH - age) / 12));
    }

    static float fallenLegPitch(float progress) { return -1.55f * progress; }

    static double fallenDrop(float progress) { return legDrop(-fallenLegPitch(progress)); }

    static float fallenArmPitch(float progress, int side) { return (side > 0 ? .36f : .25f) * progress; }

    static float fallenShoulderDrop(float progress) { return .74f * progress; }

    static AABB fallenUpperBounds(Vec3 feet, Vec3 forward, double drop) {
        double halfX = 1.55 * Math.abs(forward.z) + 1.7 * Math.abs(forward.x);
        double halfZ = 1.55 * Math.abs(forward.x) + 1.7 * Math.abs(forward.z);
        return new AABB(feet.x - halfX, feet.y + 4.4 - drop, feet.z - halfZ,
            feet.x + halfX, feet.y + 12.15 - drop, feet.z + halfZ);
    }

    static AABB fallenArmBounds(Vec3 feet, Vec3 forward, double drop, int side, float progress) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        // Humanoid arm cube extends from -2 to +10 pixels vertically around its pivot.
        Vec3 pivot = feet.add(right.scale(side * 2.25)).add(0, 8.256 - drop - fallenShoulderDrop(progress), 0);
        float pitch = fallenArmPitch(progress, side);
        double sine = Math.sin(pitch), cosine = Math.cos(pitch);
        Vec3 shoulder = pivot.add(forward.scale(.75 * sine)).add(0, .75 * cosine, 0);
        Vec3 hand = pivot.subtract(forward.scale(3.75 * sine)).add(0, -3.75 * cosine, 0);
        double trimY = .75 * Math.abs(sine);
        double trimX = .75 * (Math.abs(right.x) + Math.abs(forward.x * cosine));
        double trimZ = .75 * (Math.abs(right.z) + Math.abs(forward.z * cosine));
        return new AABB(Math.min(shoulder.x, hand.x) - trimX, hand.y - trimY, Math.min(shoulder.z, hand.z) - trimZ,
            Math.max(shoulder.x, hand.x) + trimX, shoulder.y + trimY, Math.max(shoulder.z, hand.z) + trimZ);
    }

    static Vec3 kneelFacing(Vec3 forward, double targetRange) {
        double side = Math.min(1, KNEEL_PALM_SIDE / Math.max(targetRange, KNEEL_PALM_SIDE));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return forward.scale(Math.sqrt(1 - side * side)).subtract(right.scale(side));
    }

    static Vec3 kneelPalm(Vec3 feet, Vec3 forward) {
        // Giant arm pivot(-5,2,0), palm(-1,10,0), model scale 6, renderer origin at 1.501.
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double palmHeight = (1.501 - 2d / 16) * 6 - 3.75 * Math.cos(KNEEL_ARM_CONTACT) - kneelDrop(1);
        return feet.add(right.scale(KNEEL_PALM_SIDE)).add(forward.scale(-3.75 * Math.sin(KNEEL_ARM_CONTACT))).add(0, palmHeight, 0);
    }

    static float plantedLegPitch(Move move, float age) {
        float held = Mth.clamp(age / move.warning, 0, 1);
        held = held * held * (3 - 2 * held);
        float plant = Mth.clamp((age - (move.warning - 5)) / 5, 0, 1);
        plant = plant * plant * (3 - 2 * plant);
        float lift = move == Move.STOMP ? -.93f : -.73f;
        return lift * held * (1 - plant);
    }

    static AABB kneelingBounds(Vec3 hip, Vec3 forward, float pitch, double groundY) {
        double cosine = Math.cos(pitch);
        Vec3 sole = hip.subtract(forward.scale(4.5 * Math.sin(pitch))).add(0, -4.5 * cosine, 0);
        double trimY = .75 * Math.abs(Math.sin(pitch));
        double trimX = .75 * (Math.abs(forward.z) + Math.abs(forward.x * cosine));
        double trimZ = .75 * (Math.abs(forward.x) + Math.abs(forward.z * cosine));
        return new AABB(Math.min(hip.x, sole.x) - trimX, Math.max(groundY, sole.y - trimY), Math.min(hip.z, sole.z) - trimZ,
            Math.max(hip.x, sole.x) + trimX, hip.y + trimY, Math.max(hip.z, sole.z) + trimZ);
    }

    private static boolean kneelFooting(ServerLevel level, SourceGiantZombieBoss giant, Vec3 feet, Vec3 forward) {
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 hips = feet.add(0, 4.506 - kneelDrop(1), 0);
        for (int side : new int[]{1, -1}) {
            Vec3 sideOffset = right.scale(side * .7125);
            Vec3 projectedFoot = feet.add(sideOffset).subtract(forward.scale(side * 4.5 * Math.sin(1)));
            Vec3 support = floor(level, giant, projectedFoot);
            if (support == null || Math.abs(support.y - feet.y) > .3 || !giant.inArena(support)) return false;
            AABB leg = kneelingBounds(hips.add(sideOffset), forward, side, feet.y);
            if (level.getBlockCollisions(giant, leg).iterator().hasNext()) return false;
        }
        return true;
    }

    private record KneelPatch(Vec3 palm, Vec3 ground, List<Vec3> segments) {
        boolean touches(AABB box) {
            return groundSegmentsTouch(box, segments, KNEEL_HALF_WIDTH, KNEEL_SHOCK_HEIGHT);
        }
    }

    private boolean canKneel(ServerLevel level, SourceGiantZombieBoss giant, ServerPlayer target, List<ServerPlayer> players) {
        var patch = kneelPatch(level, giant, origin);
        if (patch == null || !patch.touches(target.getBoundingBox())) return false;
        for (var player : players) {
            AABB box = player.getBoundingBox();
            if (patch.touches(box) && !canLeaveAttack(level, player, box, patch::touches)) return false;
        }
        return true;
    }

    private KneelPatch kneelPatch(ServerLevel level, SourceGiantZombieBoss giant, Vec3 feet) {
        if (!kneelFooting(level, giant, feet, axis)) return null;
        Vec3 palm = kneelPalm(feet, axis);
        Vec3 ground = floor(level, giant, new Vec3(palm.x, feet.y, palm.z));
        if (ground == null || !giant.inArena(ground)
                || !BossAttackGeometry.visible(level, giant, feet.add(0, .66, 0), ground.add(0, .66, 0))) return null;
        Vec3 base = ground.add(0, .24, 0);
        double columnHeight = palm.y - base.y;
        if (columnHeight <= .3 || columnHeight > 4.5
                || !groundStripOpen(level, giant, base.subtract(axis.scale(.95)), base.add(axis.scale(.95)),
                    .95, columnHeight)) return null;

        List<Vec3> segments = new ArrayList<>(8);
        Vec3 previous = null;
        for (int step = 0; step <= 4; ++step) {
            Vec3 position = ground.add(axis.scale(KNEEL_HALF_LENGTH * (step * .5 - 1)));
            Vec3 surface = floor(level, giant, position);
            if (surface == null || !giant.inArena(surface) || Math.abs(surface.y - ground.y) > .60) return null;
            Vec3 next = surface.add(0, .24, 0);
            if (previous != null) {
                if (!groundStripOpen(level, giant, previous, next, KNEEL_HALF_WIDTH, KNEEL_SHOCK_HEIGHT)) return null;
                segments.add(previous);
                segments.add(next);
            }
            previous = next;
        }
        return new KneelPatch(palm, ground, segments);
    }

    private void kneelImpact(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players) {
        var patch = kneelPatch(level, giant, giant.position());
        if (patch == null) return;
        Vec3 base = patch.ground.add(0, .24, 0);
        BossCombatVisual.earthBrace(level, giant, base, patch.palm);
        BossCombatVisual.earthStrip(level, giant, base, patch.segments, KNEEL_HALF_WIDTH, KNEEL_SHOCK_HEIGHT);
        giant.dust(level, patch.ground, 22, 1.0);
        for (int i = 0; i < patch.segments.size(); i += 2) {
            hitGroundStrip(level, giant, players, patch.segments.get(i), patch.segments.get(i + 1), KNEEL_HALF_WIDTH,
                KNEEL_SHOCK_HEIGHT, 16, .37);
        }
    }

    static float comboSweepYaw(float strikeTick) {
        return Mth.lerp(Mth.clamp(strikeTick / (COMBO_SWEEP_TICKS - 1), 0, 1), -.98f, 1.08f);
    }

    private boolean canCombo(ServerLevel level, SourceGiantZombieBoss giant, ServerPlayer target, List<ServerPlayer> players) {
        var sweep = new ArrayList<Vec3>();
        for (int tick = 0; tick < COMBO_SWEEP_TICKS; ++tick) {
            sweep.addAll(liveGroundSegments(level, giant, origin, comboGroundEnd(comboSweepYaw(tick), COMBO_SWEEP_REACH),
                COMBO_WAVE_INNER, COMBO_SWEEP_WIDTH, COMBO_SWEEP_HEIGHT));
        }
        var elbow = liveGroundSegments(level, giant, origin, comboGroundEnd(COMBO_ELBOW_YAW, COMBO_ELBOW_REACH),
            COMBO_WAVE_INNER, COMBO_ELBOW_WIDTH, COMBO_ELBOW_HEIGHT);
        if (!comboThreatens(target.getBoundingBox(), sweep, elbow)) return false;
        Predicate<AABB> threatened = box -> comboThreatens(box, sweep, elbow);
        for (var player : players) {
            AABB box = player.getBoundingBox();
            if (!threatened.test(box)) continue;
            // The later elbow gives time for a second jump. Only offer the jump if the entire lift has clearance.
            double lift = 1.15;
            if (player.onGround() && !player.isInWaterOrBubble() && !threatened.test(box.move(0, lift, 0))
                    && !level.getBlockCollisions(player, box.expandTowards(0, lift, 0).deflate(.02)).iterator().hasNext()) continue;
            if (!canLeaveAttack(level, player, box, threatened)) return false;
        }
        return true;
    }

    static boolean comboThreatens(AABB box, List<Vec3> sweep, List<Vec3> elbow) {
        return groundSegmentsTouch(box, sweep, COMBO_SWEEP_WIDTH, COMBO_SWEEP_HEIGHT)
            || groundSegmentsTouch(box, elbow, COMBO_ELBOW_WIDTH, COMBO_ELBOW_HEIGHT);
    }

    private static boolean groundSegmentsTouch(AABB box, List<Vec3> segments, double width, double height) {
        for (int i = 0; i < segments.size(); i += 2) {
            if (groundStripTouches(box, segments.get(i), segments.get(i + 1), width, height)) return true;
        }
        return false;
    }

    private void combo(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players, int tick) {
        if (tick < COMBO_SWEEP_TICKS) {
            Vec3 end = comboGroundEnd(comboSweepYaw(tick), COMBO_SWEEP_REACH);
            groundWave(level, giant, players, origin, end, COMBO_WAVE_INNER, COMBO_SWEEP_WIDTH, COMBO_SWEEP_HEIGHT, 15, .18, tick % 3 == 0);
            return;
        }
        if (tick < COMBO_ELBOW_START) {
            if (tick % 4 == 1) giant.dust(level, giant.position(), 5, .6);
            return;
        }
        if (tick == COMBO_ELBOW_START) struck.clear();
        int elapsed = tick - COMBO_ELBOW_START;
        int travelTicks = Move.COMBO.strike - COMBO_ELBOW_START;
        double from = Mth.lerp(elapsed / (double) travelTicks, COMBO_WAVE_INNER, COMBO_ELBOW_REACH);
        double to = Mth.lerp((elapsed + 1d) / travelTicks, COMBO_WAVE_INNER, COMBO_ELBOW_REACH);
        groundWave(level, giant, players, origin, comboGroundEnd(COMBO_ELBOW_YAW, to), from,
            COMBO_ELBOW_WIDTH, COMBO_ELBOW_HEIGHT, 17, .45, true);
    }

    private Vec3 comboGroundEnd(float angle, double reach) {
        Vec3 heading = axis.scale(Math.cos(angle)).add(right().scale(Math.sin(angle)));
        return origin.add(heading.scale(reach));
    }

    private static List<Vec3> traceGroundStrip(ServerLevel level, SourceGiantZombieBoss giant, Vec3 start, Vec3 end,
                                               double width, double height) {
        Vec3 ground = floor(level, giant, start);
        if (ground == null || !giant.inArena(ground)) return List.of();
        List<Vec3> path = new ArrayList<>();
        path.add(ground.add(0, .24, 0));
        int steps = Mth.ceil(end.subtract(start).horizontalDistance() / .60);
        for (int step = 1; step <= steps; ++step) {
            Vec3 surface = floor(level, giant, start.lerp(end, step / (double) steps));
            if (surface == null || !giant.inArena(surface) || Math.abs(surface.y - ground.y) > .65) break;
            Vec3 next = surface.add(0, .24, 0);
            if (!groundStripOpen(level, giant, path.get(path.size() - 1), next, width, height)) break;
            path.add(next);
            ground = surface;
        }
        return path;
    }

    private void groundWave(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players,
                            Vec3 start, Vec3 end, double liveFrom, double width, double height,
                            float damage, double shove, boolean debris) {
        List<Vec3> liveSegments = liveGroundSegments(level, giant, start, end, liveFrom, width, height);
        if (liveSegments.isEmpty()) return;
        for (int i = 0; i < liveSegments.size(); i += 2) {
            hitGroundStrip(level, giant, players, liveSegments.get(i), liveSegments.get(i + 1), width, height, damage, shove);
        }
        BossCombatVisual.earthStrip(level, giant, liveSegments.get(0), liveSegments, width, height);
        if (debris) giant.dust(level, liveSegments.get(liveSegments.size() - 1).add(0, -.24, 0), 9, width * .62);
    }

    private static List<Vec3> liveGroundSegments(ServerLevel level, SourceGiantZombieBoss giant, Vec3 start, Vec3 end,
                                                double liveFrom, double width, double height) {
        List<Vec3> path = traceGroundStrip(level, giant, start, end, width, height);
        List<Vec3> liveSegments = new ArrayList<>();
        for (int step = 1; step < path.size(); ++step) {
            Vec3 from = path.get(step - 1);
            Vec3 to = path.get(step);
            double toDistance = to.subtract(start).horizontalDistance();
            if (toDistance <= liveFrom) continue;
            double fromDistance = from.subtract(start).horizontalDistance();
            if (fromDistance < liveFrom) from = from.lerp(to, (liveFrom - fromDistance) / (toDistance - fromDistance));
            liveSegments.add(from);
            liveSegments.add(to);
        }
        return liveSegments;
    }

    private void hitGroundStrip(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players,
                                Vec3 from, Vec3 to, double width, double height, float damage, double shove) {
        Vec3 middle = from.lerp(to, .5);
        for (ServerPlayer player : players) {
            if (struck.contains(player.getUUID())) continue;
            AABB bounds = player.getBoundingBox();
            if (!groundStripTouches(bounds, from, to, width, height)) continue;
            Vec3 body = bounds.getCenter();
            Vec3 contact = new Vec3(body.x, Mth.clamp(body.y, middle.y, middle.y + height), body.z);
            if (BossAttackGeometry.visible(level, giant, middle.add(0, height * .5, 0), contact)) {
                hurt(level, giant, player, damage, shove);
            }
        }
    }

    private static boolean groundStripOpen(ServerLevel level, SourceGiantZombieBoss giant,
                                           Vec3 from, Vec3 to, double width, double height) {
        Vec3 heading = BossAttackGeometry.flatDirection(to.subtract(from));
        double sideX = Math.abs(heading.z) * width;
        double sideZ = Math.abs(heading.x) * width;
        AABB bounds = new AABB(Math.min(from.x, to.x) - sideX, Math.min(from.y, to.y), Math.min(from.z, to.z) - sideZ,
            Math.max(from.x, to.x) + sideX, Math.max(from.y, to.y) + height, Math.max(from.z, to.z) + sideZ);
        for (var shape : level.getBlockCollisions(giant, bounds)) {
            for (AABB block : shape.toAabbs()) {
                if (groundStripTouches(block, from, to, width, height)) return false;
            }
        }
        return true;
    }

    static boolean groundStripTouches(AABB box, Vec3 from, Vec3 to, double width, double height) {
        Vec3 span = to.subtract(from);
        double length = span.horizontalDistance();
        if (length < 1e-6) return false;
        Vec3 direction = new Vec3(span.x / length, 0, span.z / length);
        Vec3 side = new Vec3(-direction.z, 0, direction.x);
        Vec3 offset = box.getCenter().subtract(from);
        double halfX = box.getXsize() * .5;
        double halfZ = box.getZsize() * .5;
        double along = offset.dot(direction);
        double alongExtent = halfX * Math.abs(direction.x) + halfZ * Math.abs(direction.z);
        double acrossExtent = halfX * Math.abs(side.x) + halfZ * Math.abs(side.z);
        if (along + alongExtent <= 0 || along - alongExtent >= length
                || Math.abs(offset.dot(side)) >= width + acrossExtent) return false;
        // The two world axes close the corners of the rectangle for oblique strips.
        double sideX = Math.abs(side.x) * width;
        double sideZ = Math.abs(side.z) * width;
        if (box.maxX <= Math.min(from.x, to.x) - sideX || box.minX >= Math.max(from.x, to.x) + sideX
                || box.maxZ <= Math.min(from.z, to.z) - sideZ || box.minZ >= Math.max(from.z, to.z) + sideZ) return false;
        double firstFloor = Mth.lerp(Mth.clamp((along - alongExtent) / length, 0, 1), from.y, to.y);
        double lastFloor = Mth.lerp(Mth.clamp((along + alongExtent) / length, 0, 1), from.y, to.y);
        return box.minY < Math.max(firstFloor, lastFloor) + height && box.maxY > Math.min(firstFloor, lastFloor);
    }

    private void impact(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players,
                        Vec3 at, double radius, float damage) {
        giant.dust(level, at, 30, radius * .63);
        BossAttackGeometry.ring(level, HIT, at.add(0, .34, 0), radius, 19);
        for (var player : players) {
            var offset = player.position().subtract(at);
            if (inImpact(offset, radius) && BossAttackGeometry.visible(level, giant, at.add(0, .58, 0), player.getEyePosition())) {
                hurt(level, giant, player, damage, .29);
            }
        }
    }

    private static boolean inImpact(Vec3 offset, double radius) {
        return offset.horizontalDistanceSqr() <= radius * radius && offset.y > -.8 && offset.y < 2.7;
    }

    private void strike(ServerLevel level, SourceGiantZombieBoss giant, List<ServerPlayer> players,
                        Vec3 from, Vec3 to, double width, float damage, double shove) {
        Vec3 end = BossAttackGeometry.shorten(level, giant, from, to);
        if (move != Move.BOULDER && move != Move.FISSURE && move != Move.HEAVE && move != Move.BACK_KICK) BossAttackGeometry.line(level, HIT, from, end, 1.0);
        for (var player : players) {
            var box = player.getBoundingBox();
            if (BossAttackGeometry.hits(box, from, end, width) && BossAttackGeometry.visible(level, giant, from, box.getCenter())) {
                hurt(level, giant, player, damage, shove);
            }
        }
    }

    private void hurt(ServerLevel level, SourceGiantZombieBoss giant, ServerPlayer player, float damage, double shove) {
        UUID target = player.getUUID();
        if (struck.contains(target) || !player.hurt(level.damageSources().mobAttack(giant), damage * .65f)) return;
        struck.add(target);
        landedHit = true;
        if (shove == 0) return;
        var toward = BossAttackGeometry.flatDirection(player.position().subtract(giant.position())).scale(shove);
        player.setDeltaMovement(player.getDeltaMovement().add(toward).add(0, Math.abs(shove) * .5, 0));
        player.hurtMarked = true;
    }

    private boolean walk(ServerLevel level, SourceGiantZombieBoss giant, Vec3 heading) {
        var goal = floor(level, giant, giant.position().add(heading.scale(1.3)));
        if (goal == null || !passage(level, giant, goal) || !giant.inArena(goal)) {
            brake(giant, .54);
            return false;
        }
        Vec3 v = giant.getDeltaMovement();
        giant.setDeltaMovement(Mth.lerp(.34, v.x, heading.x * .32), v.y, Mth.lerp(.34, v.z, heading.z * .32));
        giant.setYRot(yaw(heading, giant.getYRot(), 9));
        giant.yBodyRot = giant.getYRot();
        return true;
    }

    private List<Vec3> walkingPath(ServerLevel level, SourceGiantZombieBoss giant, Vec3 target) {
        List<Vec3> result = new ArrayList<>();
        Vec3 previous = origin;
        double distance = origin.subtract(target).horizontalDistance();
        for (double i = 1.15; i <= distance + .1; i += 1.15) {
            Vec3 step = floor(level, giant, new Vec3(origin.x, previous.y, origin.z).add(axis.scale(i)));
            if (step == null || !passage(level, giant, step) || !giant.inArena(step)
                    || step.y - previous.y > 1.46 || previous.y - step.y > 1.25) break;
            result.add(step);
            previous = step;
        }
        return result;
    }

    private static boolean passage(ServerLevel level, SourceGiantZombieBoss giant, Vec3 foot) {
        var body = giant.getBoundingBox().move(foot.subtract(giant.position())).deflate(.04);
        return !level.getBlockCollisions(giant, body).iterator().hasNext();
    }

    private static Vec3 floor(ServerLevel level, SourceGiantZombieBoss giant, Vec3 at) {
        return BossAttackGeometry.ground(level, giant, at.add(0, 1.75, 0), 5);
    }

    private static boolean jumpSpace(ServerLevel level, List<ServerPlayer> players, Vec3 center, double radius) {
        return players.stream().filter(player -> player.position().distanceToSqr(center) < radius * radius)
            .allMatch(player -> !level.getBlockCollisions(player, player.getBoundingBox().move(0, .86, 0)).iterator().hasNext());
    }

    private static boolean passageExit(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 end, double width) {
        Vec3 axis = BossAttackGeometry.flatDirection(end.subtract(origin));
        Vec3 side = new Vec3(-axis.z, 0, axis.x);
        return !BossAttackGeometry.hits(player.getBoundingBox(), origin.add(0, 1, 0), end.add(0, 1, 0), width)
            || DragonCombat.escape(level, player, side, width + .7)
            || DragonCombat.escape(level, player, side.scale(-1), width + .7);
    }

    private static boolean exits(ServerLevel level, List<ServerPlayer> players, Vec3 at, double radius) {
        for (var player : players) {
            if (player.position().distanceToSqr(at) > radius * radius) continue;
            Vec3 toward = BossAttackGeometry.flatDirection(player.position().subtract(at));
            if (toward.lengthSqr() < .5) toward = new Vec3(1, 0, 0);
            Vec3 side = new Vec3(-toward.z, 0, toward.x);
            if (!DragonCombat.escape(level, player, toward, radius + .42 - at.distanceTo(player.position()))
                    && !DragonCombat.escape(level, player, side, radius + .42)
                    && !DragonCombat.escape(level, player, side.scale(-1), radius + .42)) return false;
        }
        return true;
    }

    private Vec3 right() { return new Vec3(-axis.z, 0, axis.x); }

    private static float yaw(Vec3 toward, float old, float change) {
        return Mth.approachDegrees(old, (float)Math.toDegrees(Math.atan2(-toward.x, toward.z)), change);
    }

    private static double range(Move move) {
        return switch (move) {
            case BOULDER -> 32;
            case STRIDE, FISSURE, HEAVE -> 22;
            case STOMP, SHOULDER -> 14;
            default -> 10;
        };
    }

    private static void brake(SourceGiantZombieBoss giant, double factor) {
        var motion = giant.getDeltaMovement();
        giant.setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
    }

    void hurtLeg(SourceGiantZombieBoss giant, int side, float damage) {
        if ((balance[side] += damage) < LEG_STAGGER && balance[0] + balance[1] < COMBINED_LEG_STAGGER) return;
        balance[0] = balance[1] = 0;
        finish(giant, 10);
        counterTicks = 0;
        down = STAGGER_LENGTH;
        brake(giant, 0);
        giant.action(SourceGiantZombieBoss.FALLEN);
        if (giant.level() instanceof ServerLevel level) {
            BossArenaPortals.release(level, giant.getUUID());
            giant.dust(level, giant.position(), 36, 2.8);
            level.playSound(null, giant.blockPosition(), SoundEvents.ZOMBIE_HURT, SoundSource.HOSTILE, 3.4f, .50f);
        }
    }

    void suspend(SourceGiantZombieBoss giant) {
        if (move != null || down > 0) finish(giant, 11);
        down = 0;
        counterTicks = 0;
        brake(giant, .39);
    }

    private void finish(SourceGiantZombieBoss giant, int wait) {
        move = null;
        steps = List.of();
        struck.clear();
        idle = wait;
        giant.action(0);
    }

    void save(CompoundTag tag) {
        tag.putFloat("MiteGiantLeftLeg", balance[0]);
        tag.putFloat("MiteGiantRightLeg", balance[1]);
    }

    void load(CompoundTag tag) {
        balance[0] = readLegPressure(tag, "MiteGiantLeftLeg");
        balance[1] = readLegPressure(tag, "MiteGiantRightLeg");
        if (balance[0] + balance[1] >= COMBINED_LEG_STAGGER) {
            balance[0] = balance[1] = 0;
        }
    }

    private static float readLegPressure(CompoundTag tag, String key) {
        float value = tag.getFloat(key);
        return value >= 0 && value < LEG_STAGGER ? value : 0;
    }
}
