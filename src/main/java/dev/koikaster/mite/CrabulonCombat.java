package dev.koikaster.mite;

import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class CrabulonCombat {
    enum Action { INTRO, REST, APPROACH, WINDUP, ACTIVE, RECOVERY }

    enum Move {
        CLAW(8, 5, 11), HOOK(9, 5, 12), SKITTER(10, 13, 8), HOPS(15, 61, 13),
        CAP_VOLLEY(13, 10, 8), CLOUD(16, 11, 10), FAN(11, 10, 9), BROOD(15, 10, 9),
        UNDERCUT(11, 10, 10), OVERLEAP(17, 47, 19), ALTERNATE(16, 24, 14),
        BREACH(16, 7, 10), STOMP(11, 10, 12);

        final int windup;
        final int active;
        final int recovery;

        Move(int windup, int active, int recovery) {
            this.windup = windup;
            this.active = active;
            this.recovery = recovery;
        }
    }

    static final double VISIBLE_ATTACK_HEIGHT = 3.4;
    private static final double STOMP_HALF_WIDTH = .58;

    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private final CrabulonHazards hazards = new CrabulonHazards();
    private final Set<UUID> struck = new HashSet<>();
    private Action action = Action.REST;
    private Move move;
    private Move lastAttack;
    private UUID target;
    private int age;
    private int length;
    private int blockedApproaches;
    private int hops;
    private int flightStarted;
    private int waveStarted;
    private Vec3 aim = Vec3.ZERO;
    private Vec3 bodyAim = Vec3.ZERO;
    private CrabulonFanShot fanShot;
    private Vec3 heading = Vec3.ZERO;
    private Vec3 launch = Vec3.ZERO;
    private Vec3 landing = Vec3.ZERO;

    boolean wantsTarget() { return action == Action.REST; }
    UUID target() { return target; }
    boolean cancellable() { return action == Action.APPROACH || action == Action.WINDUP; }

    boolean dangerous() {
        return action == Action.WINDUP || action == Action.ACTIVE || hazards.dangerous();
    }

    boolean isOpenBelly(SourceCrabulon boss, ServerPlayer player) {
        if (action != Action.RECOVERY || move != Move.CLAW && move != Move.HOOK) return false;
        Vec3 offset = BossAttackGeometry.flatDirection(player.position().subtract(boss.position()));
        return offset.dot(clawSide(move, heading)) <= -.35;
    }

    void suspend(ServerLevel level, SourceCrabulon boss) {
        if (action != Action.REST) enter(boss, Action.REST, 0);
        target = null;
        fanShot = null;
        boss.setXRot(0);
        blockedApproaches = 0;
        hazards.clear();
        BossArenaPortals.release(level, boss.getUUID());
    }

    boolean tick(ServerLevel level, SourceCrabulon boss, ServerPlayer selected, List<ServerPlayer> players) {
        hazards.tick(level, boss, players);
        if (selected == null && cancellable()) {
            fanShot = null;
            boss.setXRot(0);
            enter(boss, Action.REST, 0);
            return false;
        }
        if (move == Move.FAN && fanShot != null && action != Action.REST) {
            faceFan(boss, fanShot);
        }
        ++age;
        switch (action) {
            case REST -> { return select(level, boss, selected); }
            case APPROACH -> approach(level, boss, selected);
            case WINDUP -> {
                if (isClaw(move) && age <= length - 4) trackClaw(boss, selected);
                if (age >= length) begin(level, boss);
            }
            case ACTIVE -> {
                if (age > length || act(level, boss, players)) recover(boss);
            }
            case RECOVERY -> {
                if (age >= length) enter(boss, Action.REST, 0);
            }
            case INTRO -> throw new IllegalStateException("The encounter owns the introduction");
        }
        return false;
    }

    void broodLanded(ServerLevel level, SourceCrabulon boss, Vec3 floor) {
        hazards.cloudAt(level, boss, floor, 1.65, 36);
    }

    private boolean select(ServerLevel level, SourceCrabulon boss, ServerPlayer player) {
        if (!boss.onGround() || player == null) return false;
        Vec3 delta = player.position().subtract(boss.position());
        double distance = delta.horizontalDistance();
        Vec3 toward = BossAttackGeometry.flatDirection(delta);
        if (toward.lengthSqr() < .001) toward = Vec3.directionFromRotation(0, boss.getYRot());
        boolean visible = BossAttackGeometry.visible(level, boss, boss.getEyePosition(), player.getEyePosition());
        float health = boss.getHealth() / boss.getMaxHealth();
        target = player.getUUID();
        aim = player.position();
        bodyAim = player.getBoundingBox().getCenter();
        launch = boss.position();
        heading = toward;
        move = null;
        fanShot = fanInRange(delta.lengthSqr()) ? CrabulonFanShot.plan(level, boss, bodyAim) : null;
        boolean fanClear = fanShot != null;
        if (Math.abs(delta.y) >= 2.2 && fanClear) {
            move = Move.FAN;
        } else if (!visible && distance <= 10 && Math.abs(delta.y) < 2.2 && blockedApproaches >= 2) {
            move = Move.BREACH;
        } else if (visible && Math.abs(delta.y) <= VISIBLE_ATTACK_HEIGHT) {
            boolean wide = distance >= 3.5 && CrabulonArena.wide(level, boss, heading);
            var choices = order.candidates(boss.getRandom());
            if (distance <= clawReach(boss) && clawHeight(boss, player)
                    && BossAttackGeometry.visible(level, boss, boss.position().add(0, .9, 0), bodyAim)) {
                choices = choices.stream().sorted(Comparator.comparingInt(this::closePriority)).toList();
            }
            for (Move candidate : choices) {
                if (!eligible(candidate, boss, player, distance, delta.y, health, wide, fanClear)) continue;
                if ((candidate == Move.HOPS || candidate == Move.OVERLEAP)
                        && leapLanding(level, boss, candidate, 0) == null) continue;
                if (candidate == Move.CLOUD && !hazards.canCloud(level, boss, aim)) continue;
                if (candidate == Move.BROOD && !hazards.canBrood(level, boss, aim, heading)) continue;
                move = candidate;
                break;
            }
        } else if (fanClear) {
            move = Move.FAN;
        }
        if (move == Move.FAN) {
            faceFan(boss, fanShot);
        } else {
            fanShot = null;
            boss.setXRot(0);
            face(boss, heading);
        }
        if (move == null) {
            enter(boss, Action.APPROACH, 14);
            return false;
        }
        order.used(move);
        lastAttack = move;
        struck.clear();
        hops = 0;
        flightStarted = -1;
        waveStarted = -1;
        boss.setDeltaMovement(boss.getDeltaMovement().multiply(.13, 1, .13));
        int windup = Math.max(8, move.windup - (health < .35 ? 2 : 0));
        enter(boss, Action.WINDUP, windup);
        warn(level, boss);
        return true;
    }

    static boolean fanInRange(double distanceSquared) {
        return distanceSquared >= 3.2 * 3.2 && distanceSquared <= 15 * 15;
    }

    private int closePriority(Move candidate) {
        if (!isClaw(candidate) && candidate != Move.STOMP) return 2;
        return candidate == lastAttack ? 1 : 0;
    }

    private void trackClaw(SourceCrabulon boss, ServerPlayer player) {
        bodyAim = player.getBoundingBox().getCenter();
        Vec3 forward = BossAttackGeometry.flatDirection(player.position().subtract(boss.position()));
        if (forward.lengthSqr() < .001) return;
        heading = forward;
        face(boss, heading);
    }

    private static boolean isClaw(Move move) { return move == Move.CLAW || move == Move.HOOK; }

    private static double clawReach(SourceCrabulon boss) { return boss.getBbWidth() * .5 + 3.45; }

    private static boolean clawHeight(SourceCrabulon boss, ServerPlayer player) {
        var box = player.getBoundingBox();
        return box.minY < boss.getY() + 2.05 && box.maxY > boss.getY() - .48;
    }

    private static boolean eligible(Move move, SourceCrabulon boss, ServerPlayer player,
                                    double range, double dy, float health, boolean wide, boolean fanClear) {
        return switch (move) {
            case CLAW -> range <= clawReach(boss) && clawHeight(boss, player);
            case HOOK -> range >= 2.5 && range <= clawReach(boss) && clawHeight(boss, player);
            case SKITTER -> range >= 2.2 && range <= 7.8 && Math.abs(dy) < 1.65;
            case HOPS -> wide && range <= 10 && Math.abs(dy) < 2.2;
            case CAP_VOLLEY -> range >= 4.3 && range <= 15;
            case CLOUD -> range >= 3 && range <= 12 && health <= .78f;
            case FAN -> fanClear;
            case BROOD -> range >= 3.5 && range <= 12 && health <= .70f;
            case UNDERCUT -> range >= 2 && range <= 9 && health <= .67f && Math.abs(dy) < 1.8;
            case OVERLEAP -> wide && range <= 7.6 && health <= .36f && Math.abs(dy) < 1.8;
            case ALTERNATE -> range >= 2.4 && range <= 10 && health <= .36f && Math.abs(dy) < 2;
            case STOMP -> range <= boss.getBbWidth() * .5 + .7 && Math.abs(dy) < 1.5;
            case BREACH -> false;
        };
    }

    private void approach(ServerLevel level, SourceCrabulon boss, ServerPlayer player) {
        aim = player.position();
        if (age >= length || boss.position().distanceToSqr(aim) < 3.6 * 3.6 && Math.abs(boss.getY() - aim.y) < 1.65
                && BossAttackGeometry.visible(level, boss, boss.getEyePosition(), aim.add(0, 1.5, 0))) {
            double before = aim.subtract(launch).horizontalDistance();
            double after = aim.subtract(boss.position()).horizontalDistance();
            if (before - after < .85) ++blockedApproaches;
            else blockedApproaches = 0;
            boss.setDeltaMovement(boss.getDeltaMovement().multiply(.20, 1, .20));
            enter(boss, Action.REST, 0);
            return;
        }
        Vec3 forward = BossAttackGeometry.flatDirection(aim.subtract(boss.position()));
        if (forward.lengthSqr() < .001) forward = heading;
        Vec3 side = lateral(forward);
        double bias = blockedApproaches % 2 == 0 ? 1 : -1;
        for (Vec3 choice : new Vec3[]{forward, forward.add(side.scale(.86 * bias)).normalize(),
                side.scale(bias), side.scale(-bias)}) {
            Vec3 step = choice.scale(.30);
            if (CrabulonArena.nearbyFloor(level, boss, boss.position().add(step.scale(2))) != null) {
                groundStep(boss, step);
                face(boss, forward);
                return;
            }
        }
        boss.setDeltaMovement(boss.getDeltaMovement().multiply(.12, 1, .12));
    }

    private void warn(ServerLevel level, SourceCrabulon boss) {
        Vec3 position = boss.position().add(0, 1.05, 0);
        if (move == Move.CLAW || move == Move.HOOK || move == Move.BREACH) {
            position = position.add(heading.scale(.55)).add(clawSide(move, heading).scale(.95));
        } else if (move == Move.FAN) {
            position = fanShot.origin().add(fanShot.direction().scale(.55));
        }
        level.sendParticles(ParticleTypes.MYCELIUM, position.x, position.y, position.z,
            12, .42, .24, .42, .06);
        level.playSound(null, boss.blockPosition(), move == Move.BREACH ? SoundEvents.STONE_HIT : SoundEvents.SPIDER_AMBIENT,
            SoundSource.HOSTILE, 1.4f, move == Move.OVERLEAP || move == Move.ALTERNATE ? .59f : 1.13f);
    }

    private void begin(ServerLevel level, SourceCrabulon boss) {
        launch = boss.position();
        enter(boss, Action.ACTIVE, move.active);
        if (move == Move.CAP_VOLLEY) hazards.volley(level, boss, aim, heading, false);
        if (move == Move.CLOUD && !hazards.cloud(level, boss, aim, 3.05, 51)) recover(boss);
        if (move == Move.FAN) {
            var actual = CrabulonFanShot.plan(level, boss, bodyAim, fanShot.outlet());
            if (actual == null) {
                recover(boss);
                return;
            }
            fanShot = actual;
            faceFan(boss, actual);
            if (!hazards.fan(level, boss, actual, false)) recover(boss);
        }
        if (move == Move.BROOD && !hazards.brood(level, boss, aim, heading)) recover(boss);
    }

    /** Returns when the active motion ended before its upper time bound. */
    private boolean act(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players) {
        switch (move) {
            case CLAW -> {
                if (age <= 4) crescent(level, boss, players);
            }
            case HOOK -> {
                if (age <= 4) hook(level, boss, players);
            }
            case SKITTER -> {
                if (skitter(level, boss)) return true;
            }
            case HOPS, OVERLEAP -> { return airborne(level, boss, players); }
            case UNDERCUT -> {
                Vec3 direction = heading.yRot((float)((age - 5) * .105));
                wave(level, boss, players, launch, direction, .8 + age * .69, .85, 7.0f);
            }
            case ALTERNATE -> {
                if (age == 13) struck.clear();
                int offset = age <= 12 ? age : age - 12;
                float side = age <= 12 ? -.57f : .57f;
                if (offset == 1) impact(level, boss, boss.position(), 1.25);
                wave(level, boss, players, launch, heading.yRot(side), .85 + offset * .66, 1.25, 9.0f);
            }
            case STOMP -> stomp(level, boss, players);
            case BREACH -> {
                if (age == 2) {
                    CrabulonArena.breakObstruction(level, boss, heading);
                    impact(level, boss, boss.position().add(heading.scale(boss.getBbWidth() * .5)), 1.3);
                }
                if (age >= 2 && age <= 4) crescent(level, boss, players);
            }
            case CAP_VOLLEY -> {
                if (age == 7) hazards.volley(level, boss, aim, heading, true);
            }
            case FAN -> {
                if (age == 6 && fanShot != null) {
                    var next = CrabulonFanShot.plan(level, boss, bodyAim, fanShot.outlet());
                    if (next != null) hazards.fan(level, boss, next, true);
                }
            }
            case CLOUD, BROOD -> {
                // Their server-owned hazards carry the active checks.
            }
        }
        return false;
    }

    private void crescent(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players) {
        Vec3 origin = boss.position().add(0, .82, 0);
        Vec3 edge = heading.yRot(.97f - age * .36f);
        double clawReach = clawReach(boss);
        if (age == 1 || age == 3) sliceParticles(level, boss, origin, edge, clawReach - .3);
        for (ServerPlayer player : players) {
            Vec3 offset = player.position().subtract(boss.position());
            double reach = offset.horizontalDistance();
            if (reach > clawReach || !clawHeight(boss, player)
                    || reach > .01 && BossAttackGeometry.flatDirection(offset).dot(edge) < .55) continue;
            strike(level, boss, player, origin, move == Move.CLAW ? 14.0f : 8.5f, .56);
        }
    }

    private void hook(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players) {
        double reach = boss.getBbWidth() * .5 + 4;
        Vec3 origin = boss.position().add(clawSide(Move.HOOK, heading).scale(boss.getBbWidth() * .26)).add(0, 1.0, 0);
        Vec3 contact = new Vec3(bodyAim.x, Mth.clamp(bodyAim.y, boss.getY() - .2, boss.getY() + 2.05), bodyAim.z);
        Vec3 direction = contact.subtract(origin).normalize();
        Vec3 end = BossAttackGeometry.shorten(level, boss, origin, origin.add(direction.scale(reach)));
        if (age == 1 || age == 3) sliceTrail(level, origin, end);
        for (ServerPlayer player : players) {
            if (BossAttackGeometry.hits(player.getBoundingBox(), origin, end, .65)
                    && strike(level, boss, player, origin, 12.0f, 0)) {
                Vec3 toward = BossAttackGeometry.flatDirection(player.position().subtract(origin));
                player.knockback(.72, toward.x, toward.z);
            }
        }
    }

    private boolean skitter(ServerLevel level, SourceCrabulon boss) {
        Vec3 side = lateral(heading).scale((boss.getId() & 1) == 0 ? 1 : -1);
        Vec3 step = age <= 5 ? side.scale(.54) : heading.scale(.56).add(side.scale(-.23));
        if (CrabulonArena.nearbyFloor(level, boss, boss.position().add(step)) == null
                || age > 1 && boss.horizontalCollision) return true;
        groundStep(boss, step);
        if (age % 2 == 1) impact(level, boss, boss.position(), .7);
        return false;
    }

    boolean skitterStepPending() {
        return action == Action.ACTIVE && move == Move.SKITTER && age >= 5;
    }

    void finishSkitterStep(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players, AABB start) {
        Vec3 displacement = boss.getBoundingBox().getCenter().subtract(start.getCenter());
        if (displacement.horizontalDistanceSqr() < 1.0e-6) return;
        var body = start.inflate(.08, 0, .08);
        for (ServerPlayer player : players) {
            Vec3 contact = BossAttackGeometry.sweptBodyContact(body, displacement, player.getBoundingBox());
            if (contact != null) strike(level, boss, player, contact, 7.0f, .58);
        }
    }

    private boolean airborne(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players) {
        if (waveStarted >= 0) {
            int time = age - waveStarted;
            double edge = 1 + time * .67;
            Vec3 across = lateral(heading);
            wave(level, boss, players, landing, across, edge, 1.05, 8.7f);
            wave(level, boss, players, landing, across.scale(-1), edge, 1.05, 8.7f);
            return time >= 11;
        }
        if (flightStarted < 0) {
            if (!boss.onGround()) return false;
            return !jump(level, boss);
        }
        if (age - flightStarted > 2 && boss.onGround()) {
            landing = boss.position();
            impact(level, boss, landing, move == Move.OVERLEAP ? 1.6 : 1.0);
            if (move == Move.OVERLEAP) {
                struck.clear();
                waveStarted = age;
                return false;
            }
            for (ServerPlayer player : players) {
                if (player.position().distanceToSqr(landing) < 2.85 * 2.85 && Math.abs(player.getY() - landing.y) < 1.0) {
                    strike(level, boss, player, landing.add(0, .55, 0), 7.4f, .56);
                }
            }
            flightStarted = -1;
            struck.clear();
            return hops >= 3;
        }
        if (move == Move.OVERLEAP && age - flightStarted == 8) {
            hazards.rainBehind(level, boss, boss.position(), heading);
        }
        Vec3 offset = landing.subtract(boss.position());
        double speed = Math.min(move == Move.OVERLEAP ? .78 : .51, offset.horizontalDistance() * .35);
        Vec3 step = BossAttackGeometry.flatDirection(offset).scale(speed);
        var motion = boss.getDeltaMovement();
        boss.setDeltaMovement(step.x, motion.y, step.z);
        return false;
    }

    private boolean jump(ServerLevel level, SourceCrabulon boss) {
        landing = leapLanding(level, boss, move, hops);
        if (landing == null) return false;
        Vec3 motion = BossAttackGeometry.flatDirection(landing.subtract(boss.position())).scale(.48);
        boss.setDeltaMovement(motion.x, move == Move.OVERLEAP ? .70 : .57, motion.z);
        boss.hasImpulse = true;
        flightStarted = age;
        ++hops;
        return true;
    }

    private Vec3 leapLanding(ServerLevel level, SourceCrabulon boss, Move leap, int hop) {
        boolean overleap = leap == Move.OVERLEAP;
        Vec3 destination;
        if (overleap) {
            destination = aim.add(heading.scale(3.1));
        } else {
            Vec3 side = lateral(heading).scale(hop == 1 ? 2.25 : -1.85);
            Vec3 toward = BossAttackGeometry.flatDirection(aim.subtract(boss.position()));
            destination = boss.position().add(toward.scale(hop == 0 ? 4.7 : 3.25)).add(side);
        }
        Vec3 floor = CrabulonArena.nearbyFloor(level, boss, destination);
        return floor != null && CrabulonArena.clearLeap(level, boss, floor, overleap ? 3.25 : 2.05) ? floor : null;
    }

    private void stomp(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players) {
        if (age == 1) impact(level, boss, launch, 1.1);
        double edge = 1.1 + age * .40;
        Vec3 origin = launch.add(0, .38, 0);
        int samples = Math.max(12, Mth.ceil(edge * Math.PI / STOMP_HALF_WIDTH));
        for (int i = 0; i < samples; ++i) {
            Vec3 direction = BossAttackGeometry.flatDirection(i * (Math.PI * 2 / samples));
            Vec3 center = waveCenter(level, boss, origin, direction, edge);
            if (center != null) waveParticles(level, center, Math.max(2, 60 / samples), Math.max(1, 24 / samples));
        }

        // Sample terrain in each target's actual direction, independently of the particle spacing.
        for (ServerPlayer player : players) {
            if (struck.contains(player.getUUID())) continue;
            Vec3 offset = player.position().subtract(launch);
            if (Math.abs(offset.horizontalDistance() - edge) > STOMP_HALF_WIDTH) continue;
            Vec3 center = waveCenter(level, boss, origin, BossAttackGeometry.flatDirection(offset), edge);
            if (center == null) continue;
            double feet = player.getY() - center.y;
            if (feet <= .76 && feet >= -1.0
                    && BossAttackGeometry.visible(level, boss, origin, player.position().add(0, .48, 0))) {
                strike(level, boss, player, center, 9.0f, .56);
            }
        }
    }

    private static Vec3 waveCenter(ServerLevel level, SourceCrabulon boss, Vec3 origin, Vec3 direction, double distance) {
        Vec3 end = BossAttackGeometry.shorten(level, boss, origin, origin.add(direction.scale(distance)));
        if (end.distanceToSqr(origin) < (distance - .23) * (distance - .23)) return null;
        Vec3 floor = BossAttackGeometry.ground(level, boss, end.add(0, .62, 0), 2.0);
        if (floor == null) return null;
        // A visible endpoint can be on the far bank of a gap that stops the ground wave.
        int segments = Mth.ceil(distance * 2);
        for (int i = 0; i < segments; ++i) {
            Vec3 sample = origin.lerp(end, i / (double) segments);
            if (BossAttackGeometry.ground(level, boss, sample.add(0, .62, 0), 2.0) == null) return null;
        }
        return floor.add(0, .37, 0);
    }

    private static void waveParticles(ServerLevel level, Vec3 center, int spores, int dust) {
        level.sendParticles(ParticleTypes.MYCELIUM, center.x, center.y, center.z, spores, .26, .14, .26, .08);
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, dust, .18, .06, .18, .03);
    }

    private void wave(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players,
                      Vec3 start, Vec3 direction, double distance, double radius, float damage) {
        Vec3 origin = start.add(0, .38, 0);
        Vec3 center = waveCenter(level, boss, origin, direction, distance);
        if (center == null) return;
        waveParticles(level, center, 5, 2);
        for (ServerPlayer player : players) {
            Vec3 relative = player.position().subtract(center);
            if (relative.horizontalDistanceSqr() <= radius * radius && relative.y <= .76 && relative.y >= -1.0
                    && BossAttackGeometry.visible(level, boss, origin, player.position().add(0, .48, 0))) {
                strike(level, boss, player, center, damage, .56);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void ironStagePressure(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getSource().getEntity() instanceof SourceCrabulon) {
            event.setAmount(event.getAmount() * 1.32f);
        }
    }

    private boolean strike(ServerLevel level, SourceCrabulon boss, ServerPlayer player, Vec3 origin, float damage, double force) {
        if (struck.contains(player.getUUID()) || !BossAttackGeometry.visible(level, boss, origin, player.getBoundingBox().getCenter())
                || !player.hurt(level.damageSources().mobAttack(boss), damage)) return false;
        struck.add(player.getUUID());
        if (force > 0) {
            Vec3 inward = BossAttackGeometry.flatDirection(origin.subtract(player.position()));
            player.knockback(force, inward.x, inward.z);
        }
        return true;
    }

    private static void sliceParticles(ServerLevel level, SourceCrabulon boss, Vec3 from, Vec3 heading, double length) {
        for (double spread : new double[]{-.23, 0, .23}) {
            Vec3 endpoint = BossAttackGeometry.shorten(level, boss, from,
                from.add(heading.yRot((float)spread).scale(length)));
            sliceTrail(level, from, endpoint);
        }
    }

    private static void sliceTrail(ServerLevel level, Vec3 from, Vec3 end) {
        for (int i = 2; i <= 6; ++i) {
            Vec3 point = from.lerp(end, i / 6d);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, point.x, point.y, point.z, 4, .15, .19, .15, .06);
        }
    }

    private static void impact(ServerLevel level, SourceCrabulon boss, Vec3 at, double radius) {
        Vec3 origin = at.add(0, .22, 0);
        var floor = level.getBlockState(BlockPos.containing(at.add(0, -.17, 0)));
        if (!floor.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
            origin.x, origin.y, origin.z, 18, radius, .14, radius, .13);
        level.sendParticles(ParticleTypes.MYCELIUM, origin.x, origin.y, origin.z, 14, radius, .23, radius, .13);
        level.playSound(null, boss.blockPosition(), SoundEvents.MUD_BREAK, SoundSource.HOSTILE, 1.25f, .71f);
    }

    private void recover(SourceCrabulon boss) {
        boss.setDeltaMovement(boss.getDeltaMovement().multiply(.12, 1, .12));
        blockedApproaches = 0;
        enter(boss, Action.RECOVERY, move.recovery);
    }

    private void enter(SourceCrabulon boss, Action next, int duration) {
        action = next;
        age = 0;
        length = duration;
        boss.pose(move, next, duration);
    }

    private static Vec3 lateral(Vec3 heading) {
        return new Vec3(-heading.z, 0, heading.x);
    }

    private static Vec3 clawSide(Move move, Vec3 heading) {
        // The model's left claw is on the positive X side when it faces south.
        Vec3 right = lateral(heading);
        return move == Move.HOOK ? right : right.scale(-1);
    }

    private static void groundStep(SourceCrabulon boss, Vec3 step) {
        boss.setDeltaMovement(step.x, Math.min(-.07, boss.getDeltaMovement().y), step.z);
        boss.hasImpulse = true;
    }

    private static void faceFan(SourceCrabulon boss, CrabulonFanShot shot) {
        Vec3 direction = shot.direction();
        face(boss, direction);
        boss.setXRot((float)-Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
    }

    private static void face(SourceCrabulon boss, Vec3 direction) {
        if (direction.horizontalDistanceSqr() < .00001) return;
        float yaw = (float)(Mth.atan2(-direction.x, direction.z) * 180 / Math.PI);
        boss.setYRot(yaw);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;
    }
}
