package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.Vec3;

/** The dragon's additional attacks. Committed positions are shared by cues and hit checks. */
final class DragonRepertoire {
    enum Move {
        TAIL(14, 13, 10, 8), SPIRES(15, 19, 23, 5), CROSSBEAM(16, 22, 21, 7),
        VORTEX(17, 17, 28, 6), RIPPLE(18, 13, 29, 5), PECK(19, 11, 10, 10), BURST(20, 21, 24, 6),
        EMBERS(21, 12, DragonEmberVolley.STRIKE_TICKS, 9);

        final int posture;
        final int warning;
        final int strike;
        final int recovery;
        private static final Move[] BY_POSTURE = values();

        Move(int posture, int warning, int strike, int recovery) {
            this.posture = posture;
            this.warning = warning;
            this.strike = strike;
            this.recovery = recovery;
        }

        static Move fromPosture(int posture) {
            int index = posture - 14;
            return index < 0 || index >= BY_POSTURE.length ? null : BY_POSTURE[index];
        }
    }

    private record Line(Vec3 from, Vec3 to) {}

    private static final SimpleParticleType WARN = ParticleTypes.REVERSE_PORTAL;
    private static final SimpleParticleType WARM = ParticleTypes.FLAME;
    private static final SimpleParticleType HIT = ParticleTypes.DRAGON_BREATH;
    private static final double RIPPLE_SPEED = .86;
    private static final double VORTEX_RADIUS = 10;
    private static final double VORTEX_CORE = 4;
    private static final double CROSS_WIDTH = 1.45;
    private static final double SPIRE_WIDTH = 1.55;
    private static final int BURST_LANES = 7;
    private static final double PECK_PULLBACK = 1.3;
    private static final double PECK_MAX_LUNGE = 5.5;
    private static final double PECK_BODY_HEIGHT = .86;
    private static final double PECK_STRIKE_HEIGHT = 1.1;
    private static final int PECK_MISS_RECOVERY = 42;
    private static final int EMBER_MISS_RECOVERY = 40;
    private static final double TAIL_LENGTH = 10.3;

    private final Set<UUID> struck = new HashSet<>();
    private final DragonEmberVolley embers = new DragonEmberVolley();
    private Move move;
    private Vec3 center;
    private Vec3 forward;
    private Vec3 origin;
    private List<Vec3> spires = List.of();
    private List<Line> rays = List.of();
    private BossAttackFocus focus;
    private int age;
    private double peckAdvance;
    private boolean landedHit;

    boolean active() { return move != null; }

    boolean attacking() { return active() && age < move.warning + move.strike; }

    boolean start(Move next, ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> targets,
                  List<ServerPlayer> players, DragonArenaPressure arena) {
        for (var target : targets) {
            Vec3 direction = BossAttackGeometry.flatDirection(Vec3.directionFromRotation(0, dragon.getYRot())).scale(-1);
            Vec3 foot = BossAttackGeometry.ground(level, dragon, target.position().add(0, .55, 0), 4);
            Vec3 anchor = dragon.position();
            double distance = target.distanceTo(dragon);
            if (next == Move.TAIL || next == Move.PECK) {
                var floor = BossAttackGeometry.ground(level, dragon, anchor.add(0, .3, 0), 4);
                if (floor == null || anchor.y - floor.y > 2.6 || distance > 15) continue;
                Vec3 towardTarget = BossAttackGeometry.flatDirection(target.position().subtract(anchor));
                if (next == Move.TAIL && direction.dot(towardTarget) > -.2
                        || next == Move.PECK && direction.dot(towardTarget) < .6) continue;
                Vec3 heading = next == Move.PECK ? towardTarget : direction;
                if (players.stream().anyMatch(player -> player.position().distanceToSqr(anchor) < 13 * 13
                        && !sideExit(level, player, heading))) continue;
                if (next == Move.PECK) {
                    double range = target.position().subtract(anchor).horizontalDistance();
                    if (range < 4.2 || range > 11.7 || foot == null || Math.abs(foot.y - floor.y) > .75) continue;
                    double extension = peckExtension(range);
                    Vec3 base = peckBase(floor, heading);
                    Vec3 tip = base.add(heading.scale(extension + PECK_PULLBACK));
                    Vec3 lowBody = floor.add(heading.scale(extension)).add(0, PECK_BODY_HEIGHT, 0);
                    if (!arena.allows(tip, 3.3) || !BossAttackGeometry.visible(level, dragon, base, tip)
                            || !BossAttackGeometry.visible(level, dragon, anchor.add(0, 1, 0), lowBody.add(0, 1, 0))) continue;
                    peckAdvance = extension;
                }
                center = floor;
                forward = heading;
            } else {
                if (foot == null || distance > 47 || !arena.allows(foot, margin(next))) continue;
                if (next == Move.RIPPLE && !canJumpRing(level, players, foot)) continue;
                if (next == Move.VORTEX && !escapableCenter(level, players, foot, VORTEX_CORE)) continue;
                center = foot;
                forward = direction;
                switch (next) {
                    case SPIRES -> {
                        spires = spireSites(level, dragon, foot, players);
                        if (spires.size() < 3) continue;
                    }
                    case CROSSBEAM -> {
                        rays = crossingLines(level, dragon, foot);
                        if (rays.size() != 2 || !lineExits(level, players, rays)) continue;
                    }
                    case BURST -> {
                        var launch = foot.add(0, 7, 0);
                        if (!BossAttackGeometry.visible(level, dragon, launch, foot.add(0, 1, 0))) continue;
                        rays = burstLines(level, dragon, foot, launch);
                        if (rays.size() < 3 || !lineExits(level, players, rays)) continue;
                        var orb = new BossAttackFocus(level, launch);
                        orb.setOwner(dragon);
                        if (!level.addFreshEntity(orb)) continue;
                        focus = orb;
                    }
                    case EMBERS -> {
                        if (!embers.prepare(level, dragon, foot, players, arena)) continue;
                    }
                    default -> { }
                }
            }
            origin = anchor;
            move = next;
            age = 0;
            landedHit = false;
            struck.clear();
            dragon.setRitualMove(next.posture, level.getGameTime());
            level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE,
                3.4f, switch (move) { case TAIL, RIPPLE -> .77f; case PECK -> 1.2f; default -> .96f; });
            mark(level, dragon);
            return true;
        }
        return false;
    }

    boolean step(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, DragonArenaPressure arena) {
        boolean connected = move == Move.EMBERS ? embers.landedHit() : landedHit;
        if (++age >= move.warning + move.strike + recoveryTicks(move, connected)) return false;
        if (move == Move.BURST && age <= move.warning && (focus == null || focus.isRemoved())) {
            if (age < move.warning) age = move.warning + move.strike;
            else return false;
        }
        int tick = age - move.warning;
        pose(dragon, tick);
        if (tick < 0) {
            if (age % 5 == 0) mark(level, dragon);
            return true;
        }
        if (tick >= move.strike) return true;
        if (tick == 0 && move == Move.EMBERS && !embers.lockLaunch(level, dragon, players, arena)) return false;
        if (tick == 0) {
            if (focus != null) {
                focus.discard();
                focus = null;
            }
            level.playSound(null, dragon.blockPosition(), move == Move.TAIL || move == Move.RIPPLE
                ? SoundEvents.ENDER_DRAGON_FLAP : SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 3.5f, .94f);
        }
        switch (move) {
            case TAIL -> tail(level, dragon, players, tick);
            case SPIRES -> strikeSpires(level, dragon, players, tick);
            case CROSSBEAM -> cross(level, dragon, players, tick);
            case VORTEX -> vortex(level, dragon, players, tick);
            case RIPPLE -> ripple(level, dragon, players, tick);
            case PECK -> peck(level, dragon, players, tick);
            case BURST -> burst(level, dragon, players, tick);
            case EMBERS -> embers.step(level, dragon, players, tick);
        }
        return true;
    }

    void reset() {
        if (focus != null) focus.discard();
        focus = null;
        move = null;
        spires = List.of();
        rays = List.of();
        struck.clear();
        embers.reset();
    }

    private void pose(SourceEnderDragon dragon, int tick) {
        if (move == Move.PECK) {
            double along = peckOffset(tick, peckAdvance, landedHit);
            double height = Mth.lerp(smooth(age / 5d), origin.y, center.y + PECK_BODY_HEIGHT);
            Vec3 next = new Vec3(origin.x, height, origin.z).add(forward.scale(along));
            float yaw = Mth.approachDegrees(dragon.getYRot(), DragonCombat.yaw(forward), 11);
            if (BossAttackGeometry.visible((ServerLevel)dragon.level(), dragon, dragon.position().add(0, 1, 0), next.add(0, 1, 0))) {
                DragonCombat.position(dragon, next, yaw);
            } else DragonCombat.position(dragon, dragon.position(), yaw);
            dragon.yRotA *= .65f;
            return;
        }
        if (move == Move.TAIL) {
            double curl = tick < 0 ? smooth(age / (double)move.warning) : Math.cos(Math.PI * Math.min(1, tick / 9d));
            float yaw = DragonCombat.yaw(forward) + (float)(curl * 34);
            DragonCombat.position(dragon, origin, Mth.approachDegrees(dragon.getYRot(), yaw, 12));
            return;
        }
        if (move == Move.EMBERS) {
            if (tick <= DragonEmberVolley.LAST_RELEASE) {
                float yaw = Mth.approachDegrees(dragon.getYRot(), DragonCombat.yaw(embers.heading()), 18);
                DragonCombat.position(dragon, origin, yaw);
                dragon.yRotA *= .65f;
                return;
            }
            if (tick >= move.strike && !embers.landedHit()) {
                DragonCombat.position(dragon, dragon.position(), dragon.getYRot());
                return;
            }
        }
        var aim = center.subtract(dragon.position());
        Vec3 side = BossAttackGeometry.flatDirection(aim);
        var desired = move == Move.VORTEX ? new Vec3(-side.z, 0, side.x).scale(.38) : side.scale(.12);
        if (tick >= move.strike) desired = desired.add(0, .21, 0);
        var velocity = dragon.getDeltaMovement().lerp(desired, .14);
        float yaw = Mth.approachDegrees(dragon.getYRot(), DragonCombat.yaw(side), 8);
        Vec3 next = dragon.position().add(velocity);
        if (BossAttackGeometry.visible((ServerLevel)dragon.level(), dragon, dragon.position().add(0, 1, 0), next.add(0, 1, 0))) {
            DragonCombat.position(dragon, next, yaw);
        }
    }

    private void mark(ServerLevel level, SourceEnderDragon dragon) {
        int ticks = Math.max(1, Math.min(5, move.warning - age));
        switch (move) {
            case TAIL -> BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON,
                dragon.sourceParts()[4].getBoundingBox().getCenter(), 1.35, ticks);
            case SPIRES -> {
                for (Vec3 at : spires) BossAttackGeometry.ring(level, WARM, at.add(0, .12, 0), SPIRE_WIDTH, 10);
            }
            case CROSSBEAM -> {
                for (Line ray : rays) BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON, ray.from, 1.35, ticks);
            }
            case BURST -> {
                if (focus != null) BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON, focus.position(), 1.45, ticks);
            }
            case VORTEX -> BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON, center.add(0, .55, 0), 1.6, ticks);
            case RIPPLE -> BossAttackGeometry.ring(level, WARN, center.add(0, .16, 0), 2.1, 20);
            case PECK -> BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON,
                dragon.sourceParts()[0].getBoundingBox().getCenter(), 1.15, ticks);
            case EMBERS -> embers.warn(level, dragon, ticks);
        }
    }

    private Vec3 tailTip(double progress) {
        double yaw = Math.atan2(-forward.z, -forward.x) + Mth.lerp(progress, -.94, .94);
        return center.add(BossAttackGeometry.flatDirection(yaw).scale(TAIL_LENGTH)).add(0, .9, 0);
    }

    private void tail(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        Vec3 tip = tailTip((tick + 1d) / move.strike);
        Vec3 start = center.add(0, .9, 0).lerp(tip, .35);
        strikeLine(level, dragon, players, start, tip, 1.05, 15);
    }

    private void strikeSpires(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        int index = tick / 5;
        int flight = tick % 5;
        if (index >= spires.size()) return;
        Vec3 floor = spires.get(index).add(0, .18, 0);
        Vec3 top = floor.add(0, 11, 0);
        Vec3 from = top.lerp(floor, flight / 5d);
        Vec3 to = top.lerp(floor, (flight + 1) / 5d);
        if (BossAttackGeometry.visible(level, dragon, top, from)) strikeLine(level, dragon, players, from, to, SPIRE_WIDTH, 16);
    }

    private void cross(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        if (tick < 6 || tick >= 12 && tick < 18) {
            Line ray = rays.get(tick < 6 ? 0 : 1);
            strikeLine(level, dragon, players, ray.from, ray.to, CROSS_WIDTH, 17);
        }
    }

    private void vortex(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        Vec3 eye = center.add(0, .85, 0);
        for (var player : players) {
            var delta = eye.subtract(player.position());
            double distance = delta.horizontalDistance();
            if (distance > VORTEX_RADIUS || Math.abs(delta.y) > 3
                    || !BossAttackGeometry.visible(level, dragon, eye, player.getEyePosition())) continue;
            if (tick < 20 && tick % 3 == 0 && distance > .4) {
                Vec3 pull = BossAttackGeometry.flatDirection(delta).scale(.055 + tick * .0015);
                player.setDeltaMovement(player.getDeltaMovement().add(pull));
                player.hurtMarked = true;
            } else if (tick == 23 && distance < VORTEX_CORE) {
                hurt(level, dragon, player, 16);
            }
        }
        if (tick % 3 == 0 && tick < 20) {
            BossAttackGeometry.ring(level, WARN, eye, VORTEX_RADIUS * (1 - tick / 23d), 24);
        } else if (tick == 23) BossAttackGeometry.ring(level, HIT, eye, VORTEX_CORE, 20);
    }

    private void ripple(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        ringStrike(level, dragon, players, tick, 2.1);
        if (tick >= 10) ringStrike(level, dragon, players, tick - 10, 2.1);
    }

    private void ringStrike(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick, double start) {
        if (tick > 17) return;
        double radius = start + tick * RIPPLE_SPEED;
        var at = center.add(0, .28, 0);
        for (int i = 0; i < 28; ++i) {
            var edge = at.add(BossAttackGeometry.flatDirection(i * Math.PI / 14).scale(radius));
            if (BossAttackGeometry.visible(level, dragon, at, edge)) BossAttackGeometry.point(level, HIT, edge);
        }
        for (var player : players) {
            var feet = player.position();
            if (feet.y < center.y - .4 || feet.y > center.y + .73
                    || Math.abs(feet.subtract(center).horizontalDistance() - radius) > .97) continue;
            if (BossAttackGeometry.visible(level, dragon, at, feet.add(0, .25, 0))) hurt(level, dragon, player, 14);
        }
    }

    static int recoveryTicks(Move move, boolean landedHit) {
        if (landedHit) return move.recovery;
        return switch (move) {
            case PECK -> PECK_MISS_RECOVERY;
            case EMBERS -> EMBER_MISS_RECOVERY;
            default -> move.recovery;
        };
    }

    static double peckExtension(double range) {
        return Mth.clamp(range - DragonCombat.HEAD_OFFSET, .25, PECK_MAX_LUNGE);
    }

    static double peckOffset(int strikeTick, double extension, boolean landedHit) {
        Move peck = Move.PECK;
        if (strikeTick < 0) return -PECK_PULLBACK * smooth((strikeTick + peck.warning) / (double)peck.warning);
        if (strikeTick < peck.strike) return Mth.lerp(smooth(strikeTick / 6d), -PECK_PULLBACK, extension);
        return landedHit ? Mth.lerp(smooth((strikeTick - peck.strike) / (double)peck.recovery), extension, 0) : extension;
    }

    static Vec3 peckBase(Vec3 floor, Vec3 forward) {
        return floor.add(forward.scale(DragonCombat.HEAD_OFFSET - PECK_PULLBACK)).add(0, PECK_STRIKE_HEIGHT, 0);
    }

    private void peck(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        Vec3 base = peckBase(center, forward);
        if (!BossAttackGeometry.visible(level, dragon, dragon.position().add(0, .65, 0), base)) return;
        double previous = peckOffset(Math.max(0, tick - 1), peckAdvance, false) + PECK_PULLBACK;
        double current = peckOffset(tick, peckAdvance, false) + PECK_PULLBACK;
        Vec3 tip = BossAttackGeometry.shorten(level, dragon, base, base.add(forward.scale(current)));
        if (tip.distanceToSqr(base) + .0001 < previous * previous) return;
        strikeLine(level, dragon, players, base.add(forward.scale(previous)), tip, .95, 16);
    }

    private void burst(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        int count = rays.size();
        for (int i = 0; i < count; ++i) {
            int first = i * move.strike / count;
            int limit = (i + 1) * move.strike / count;
            if (tick < first || tick >= limit) continue;
            Line ray = rays.get(i);
            Vec3 end = BossAttackGeometry.shorten(level, dragon, ray.from, ray.to);
            double progress = (tick - first) / (double)(limit - first);
            double next = (tick + 1 - first) / (double)(limit - first);
            strikeLine(level, dragon, players, ray.from.lerp(end, progress), ray.from.lerp(end, next), .9, 15);
            return;
        }
    }

    private void strikeLine(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players,
                            Vec3 from, Vec3 toward, double width, float damage) {
        Vec3 end = BossAttackGeometry.shorten(level, dragon, from, toward);
        BossAttackGeometry.line(level, HIT, from, end, .85);
        for (var player : players) {
            if (BossAttackGeometry.hits(player.getBoundingBox(), from, end, width)
                    && BossAttackGeometry.visible(level, dragon, from, player.getBoundingBox().getCenter())) {
                hurt(level, dragon, player, damage);
            }
        }
    }

    private void hurt(ServerLevel level, SourceEnderDragon dragon, ServerPlayer player, float damage) {
        UUID target = player.getUUID();
        if (struck.contains(target) || !player.hurt(level.damageSources().mobAttack(dragon), damage)) return;
        struck.add(target);
        landedHit = true;
    }

    private static boolean sideExit(ServerLevel level, ServerPlayer player, Vec3 axis) {
        Vec3 lateral = new Vec3(-axis.z, 0, axis.x);
        return DragonCombat.escape(level, player, lateral, 2.6)
            || DragonCombat.escape(level, player, lateral.scale(-1), 2.6);
    }

    private static boolean canJumpRing(ServerLevel level, List<ServerPlayer> players, Vec3 center) {
        return players.stream().filter(player -> player.position().distanceToSqr(center) < 18 * 18)
            .allMatch(player -> !level.getBlockCollisions(player, player.getBoundingBox().move(0, .9, 0)).iterator().hasNext());
    }

    private static boolean escapableCenter(ServerLevel level, List<ServerPlayer> players, Vec3 center, double radius) {
        for (var player : players) {
            Vec3 offset = player.position().subtract(center);
            double distance = offset.horizontalDistance();
            if (Math.abs(offset.y) > 2.8 || distance >= radius + .3) continue;
            Vec3 direction = distance < .1 ? new Vec3(1, 0, 0) : BossAttackGeometry.flatDirection(offset);
            boolean possible = false;
            for (int i = 0; i < 4; ++i) {
                if (DragonCombat.escape(level, player, direction, radius + .5 - Math.max(0, direction.dot(offset)))) {
                    possible = true;
                    break;
                }
                direction = new Vec3(-direction.z, 0, direction.x);
            }
            if (!possible) return false;
        }
        return true;
    }

    private static boolean lineExits(ServerLevel level, List<ServerPlayer> players, List<Line> lines) {
        for (var player : players) {
            for (Line ray : lines) {
                if (BossAttackGeometry.hits(player.getBoundingBox(), ray.from, ray.to, CROSS_WIDTH)
                        && !sideExit(level, player, BossAttackGeometry.flatDirection(ray.to.subtract(ray.from)))) return false;
            }
        }
        return true;
    }

    private static List<Vec3> spireSites(ServerLevel level, SourceEnderDragon dragon, Vec3 around, List<ServerPlayer> players) {
        List<Vec3> sites = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Vec3 offset = i == 0 ? Vec3.ZERO : BossAttackGeometry.flatDirection((i - 1) * Math.PI * 2 / 3).scale(6.1);
            Vec3 at = BossAttackGeometry.ground(level, dragon, around.add(offset).add(0, 2.5, 0), 6);
            if (at != null && BossAttackGeometry.visible(level, dragon, at.add(0, .2, 0), at.add(0, 11, 0))
                    && escapableCenter(level, players, at, SPIRE_WIDTH)) sites.add(at);
        }
        return sites;
    }

    private static List<Line> crossingLines(ServerLevel level, SourceEnderDragon dragon, Vec3 foot) {
        List<Line> result = new ArrayList<>();
        var spikes = SpikeFeature.getSpikesForLevel(level).stream().sorted(java.util.Comparator.comparingDouble(spike ->
            foot.distanceToSqr(spike.getCenterX() + .5, foot.y, spike.getCenterZ() + .5))).toList();
        Vec3 center = foot.add(0, 1.15, 0);
        Vec3 previousDirection = null;
        for (var spike : spikes) {
            Vec3 root = new Vec3(spike.getCenterX() + .5, center.y, spike.getCenterZ() + .5);
            Vec3 axis = BossAttackGeometry.flatDirection(center.subtract(root));
            if (axis.lengthSqr() < .5 || previousDirection != null && Math.abs(previousDirection.dot(axis)) > .83) continue;
            Vec3 start = root.add(axis.scale(spike.getRadius() + .7));
            if (start.distanceToSqr(center) > 32 * 32 || !level.hasChunkAt(BlockPos.containing(start))) continue;
            Vec3 end = BossAttackGeometry.shorten(level, dragon, start, center.add(axis.scale(8.5)));
            if (end.distanceToSqr(start) < 12 * 12 || end.subtract(center).dot(axis) < 0) continue;
            result.add(new Line(start, end));
            previousDirection = axis;
            if (result.size() == 2) break;
        }
        return result;
    }

    private static List<Line> burstLines(ServerLevel level, SourceEnderDragon dragon, Vec3 floor, Vec3 launch) {
        List<Line> result = new ArrayList<>();
        double rotation = dragon.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < BURST_LANES; ++i) {
            Vec3 spread = BossAttackGeometry.flatDirection(rotation + i * Math.PI * 2 / BURST_LANES);
            Vec3 end = floor.add(spread.scale(i == 0 ? 0 : 9.4)).add(0, .24, 0);
            Vec3 fixed = BossAttackGeometry.shorten(level, dragon, launch, end);
            if (fixed.distanceToSqr(launch) >= 9) result.add(new Line(launch, fixed));
        }
        return result;
    }

    private static int margin(Move move) {
        return switch (move) { case RIPPLE -> 18; case VORTEX, BURST -> 10; case SPIRES -> 8; default -> 4; };
    }

    static double smooth(double progress) {
        double t = Mth.clamp(progress, 0, 1);
        return t * t * (3 - 2 * t);
    }

}
