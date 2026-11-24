package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server timeline for attacks owned by the summoning ritual. */
final class DragonCombat {
    static final int ORBIT = 0;
    static final int APPROACH = 1;
    static final int WARNING = 2;
    static final int DIVE = 3;
    static final int RECOVERY = 4;
    static final int WING_RAISE = 12;
    static final int WING_SWEEP = 13;
    private static final int WARNING_TICKS = 25;
    static final int DESCENT_TICKS = 8;
    private static final int SWEEP_TICKS = 23;
    private static final int RECOVERY_TICKS = 38;
    private static final double HALF_WIDTH = 4.2;
    static final double HEAD_OFFSET = 5.5;
    private static final double DIVE_OVERRUN = 1.0;
    private static final double RECOVERY_DRAG = .5;

    private final Set<UUID> struck = new HashSet<>();
    private final DragonBreathAttack breath = new DragonBreathAttack();
    private final DragonAltarAttack altar = new DragonAltarAttack();
    private final DragonArenaPressure arena = new DragonArenaPressure();
    private final DragonRepertoire repertoire = new DragonRepertoire();
    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);

    private enum Move { DIVE, BREATH, WINGS, ALTAR, TAIL, SPIRES, CROSSBEAM, VORTEX, RIPPLE, PECK, BURST, EMBERS }

    private int phase = 1;
    private int stage;
    private int elapsed;
    private int cooldown = 10;
    private int laneTicks;
    private Lane lane;
    private Vec3 diveStart;
    private DragonWingAttack wing;
    private boolean breathing;
    private int altarCooldown;

    void observeHealth(SourceEnderDragon dragon) {
        float health = dragon.getHealth() / dragon.getMaxHealth();
        phase = Math.max(phase, health <= .35f ? 3 : health <= .70f ? 2 : 1);
    }

    boolean step(ServerLevel level, SourceEnderDragon dragon) {
        var players = DragonRitual.combatants(level);
        if (players.isEmpty()) {
            if (stage != ORBIT || breathing || altar.active() || repertoire.active()) finish(dragon, 20);
            breath.clearCloud();
            arena.suspend();
            dragon.setDeltaMovement(Vec3.ZERO);
            return true;
        }

        boolean cloudFading = breath.blocksBoundary(level.getGameTime());
        boolean idle = stage == ORBIT && !breathing && !altar.active() && !repertoire.active();
        if (phase == 3 && arena.tick(level, players, idle, cloudFading, breath.hasActiveCloud())) return false;
        if (altarCooldown > 0) --altarCooldown;
        if (altar.active()) {
            if (!altar.step(level, dragon, players)) finish(dragon, 16);
            return true;
        }
        if (breathing) {
            if (!breath.step(level, dragon, players, arena)) finish(dragon, 12);
            return true;
        }
        if (repertoire.active()) {
            if (!repertoire.step(level, dragon, players, arena)) finish(dragon, phase == 3 ? 8 : 10);
            return true;
        }

        if (stage == ORBIT) {
            if (cooldown-- > 0) return false;
            var targets = players.stream().filter(player -> arena.allows(player.position(), 0))
                .sorted(Comparator.comparingDouble(dragon::distanceToSqr)).toList();
            for (Move move : order.candidates(dragon.getRandom())) {
                if (!start(move, level, dragon, targets, players)) continue;
                order.used(move);
                return true;
            }
            cooldown = 4;
            return false;
        }

        switch (stage) {
            case APPROACH -> approach(level, dragon, players);
            case WARNING -> approach(level, dragon, players);
            case DIVE -> dive(level, dragon, players);
            case RECOVERY -> recover(level, dragon);
            case WING_RAISE -> raiseWings(level, dragon);
            case WING_SWEEP -> sweepWings(level, dragon, players);
            default -> throw new IllegalStateException("Unknown dragon combat stage " + stage);
        }
        return true;
    }

    private boolean start(Move move, ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> targets,
                          List<ServerPlayer> players) {
        return switch (move) {
            case DIVE -> !breath.hasActiveCloud() && startDive(level, dragon, targets, players);
            case BREATH -> phase >= 2 && (phase != 3 || !arena.boundaryDue()) && !breath.hasActiveCloud()
                && (breathing = breath.start(level, dragon, targets));
            case WINGS -> startWings(level, dragon, players);
            case ALTAR -> {
                if (phase < 2 || breath.hasActiveCloud() || altarCooldown != 0 || !altar.start(level, dragon, players)) yield false;
                altarCooldown = 150;
                yield true;
            }
            case TAIL -> repertoire.start(DragonRepertoire.Move.TAIL, level, dragon, targets, players, arena);
            case SPIRES -> !breath.hasActiveCloud()
                && repertoire.start(DragonRepertoire.Move.SPIRES, level, dragon, targets, players, arena);
            case CROSSBEAM -> phase >= 2 && !breath.hasActiveCloud()
                && repertoire.start(DragonRepertoire.Move.CROSSBEAM, level, dragon, targets, players, arena);
            case VORTEX -> phase == 3 && !breath.hasActiveCloud()
                && repertoire.start(DragonRepertoire.Move.VORTEX, level, dragon, targets, players, arena);
            case RIPPLE -> phase >= 2 && !breath.hasActiveCloud()
                && repertoire.start(DragonRepertoire.Move.RIPPLE, level, dragon, targets, players, arena);
            case PECK -> !breath.hasActiveCloud()
                && repertoire.start(DragonRepertoire.Move.PECK, level, dragon, targets, players, arena);
            case BURST -> phase >= 2 && !breath.hasActiveCloud()
                && repertoire.start(DragonRepertoire.Move.BURST, level, dragon, targets, players, arena);
            case EMBERS -> !breath.hasActiveCloud()
                && repertoire.start(DragonRepertoire.Move.EMBERS, level, dragon, targets, players, arena);
        };
    }

    private boolean startDive(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> targets,
                              List<ServerPlayer> players) {
        lane = selectLane(level, dragon, targets, players);
        if (lane == null) return false;
        laneTicks = 0;
        enter(dragon, APPROACH);
        level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5, .72f);
        markDive(level, dragon);
        return true;
    }

    void clearHazards() {
        breath.clearCloud();
        altar.reset();
        arena.suspend();
        repertoire.reset();
    }

    boolean attacking(SourceEnderDragon dragon, long now) {
        if (arena.attacking(now) || breath.hasActiveCloud() || repertoire.attacking()) return true;
        return switch (dragon.ritualPosture()) {
            case APPROACH, WARNING, DIVE, WING_RAISE, WING_SWEEP, DragonBreathAttack.TRACK,
                DragonBreathAttack.LOCK, DragonAltarAttack.POSITIONING, DragonAltarAttack.CHARGE -> true;
            default -> false;
        };
    }

    boolean guardingProjectiles(SourceEnderDragon dragon) {
        if (repertoire.active()) return repertoire.attacking();
        return switch (dragon.ritualPosture()) {
            case APPROACH, WARNING, DIVE, WING_RAISE, WING_SWEEP, DragonBreathAttack.TRACK,
                DragonBreathAttack.LOCK, DragonAltarAttack.POSITIONING -> true;
            default -> false;
        };
    }

    void interruptAltar(ServerLevel level, SourceEnderDragon dragon) {
        altar.interrupt(level, dragon);
    }

    private void approach(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        Vec3 entry = lane.bodyAt(lane.entry - 5, 8);
        Vec3 delta = entry.subtract(dragon.position());
        ++laneTicks;
        if (laneTicks > 43) {
            finish(dragon, 4);
        } else if (laneTicks >= WARNING_TICKS && delta.lengthSqr() <= 4) {
            if (!clearLane(level, dragon, lane) || !hasEscapeRoutes(level, lane, players)) {
                finish(dragon, 4);
                return;
            }
            diveStart = dragon.position();
            enter(dragon, DIVE);
        } else {
            if (delta.lengthSqr() <= 36 && stage == APPROACH) {
                enter(dragon, WARNING);
                level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4, .93f);
            }
            var speed = delta.normalize().scale(Math.min(1.95, delta.length() * .29));
            var velocity = dragon.getDeltaMovement().lerp(speed, .32);
            float facing = Mth.approachDegrees(dragon.getYRot(), delta.lengthSqr() > 64 ? yaw(delta) : lane.yaw(), 12);
            Vec3 next = dragon.position().add(velocity);
            if (BossAttackGeometry.visible(level, dragon, dragon.position().add(0, 1, 0), next.add(0, 1, 0))) {
                position(dragon, next, facing);
            } else {
                position(dragon, dragon.position().add(0, .4, 0), facing);
            }
            if (stage == WARNING && laneTicks % 5 == 0) markDive(level, dragon);
        }
    }

    private void dive(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        ++elapsed;
        if (elapsed <= DESCENT_TICKS) {
            double progress = elapsed / (double) DESCENT_TICKS;
            Vec3 landing = lane.bodyAt(lane.entry, 1);
            Vec3 turn = lane.bodyAt(lane.entry - 6, 3.4);
            Vec3 next = diveStart.lerp(turn, progress).lerp(turn.lerp(landing, progress), progress);
            if (!BossAttackGeometry.visible(level, dragon, dragon.position().add(0, 1.5, 0), next.add(0, 1.5, 0))) {
                finish(dragon, 8);
                return;
            }
            position(dragon, next, Mth.approachDegrees(dragon.getYRot(), lane.yaw(), 12));
            return;
        }

        int sweep = elapsed - DESCENT_TICKS;
        double previous = Mth.lerp((sweep - 1) / (double) SWEEP_TICKS, lane.entry, lane.exit);
        double current = Mth.lerp(sweep / (double) SWEEP_TICKS, lane.entry, lane.exit);
        if (!clearLaneSection(level, dragon, lane, lane.along(dragon.position()), current)) {
            dragon.setDeltaMovement(Vec3.ZERO);
            enter(dragon, RECOVERY);
            return;
        }
        position(dragon, lane.bodyAt(current, 1), lane.yaw());
        for (ServerPlayer player : players) {
            if (struck.contains(player.getUUID()) || !lane.contains(player)) continue;
            double along = lane.along(player.position());
            if (along + .31 < previous || along - .31 > current) continue;
            Vec3 origin = lane.point(Mth.clamp(along, previous, current), 0, 1.05);
            if (level.clip(new ClipContext(origin, player.getEyePosition(), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, dragon)).getType() != HitResult.Type.MISS) continue;
            if (player.hurt(level.damageSources().mobAttack(dragon), 17)) struck.add(player.getUUID());
        }
        if (sweep >= SWEEP_TICKS) enter(dragon, RECOVERY);
    }

    private void recover(ServerLevel level, SourceEnderDragon dragon) {
        Vec3 from = dragon.position();
        Vec3 drift = dragon.getDeltaMovement().scale(RECOVERY_DRAG);
        if (drift.lengthSqr() > 1.0E-6) {
            Vec3 next = from.add(drift);
            if (clearLaneSection(level, dragon, lane, lane.along(from), lane.along(next) + HEAD_OFFSET)
                    && BossAttackGeometry.visible(level, dragon, from.add(0, 1, 0), next.add(0, 1, 0))) {
                position(dragon, next, lane.yaw());
            } else dragon.setDeltaMovement(Vec3.ZERO);
        } else dragon.setDeltaMovement(Vec3.ZERO);
        if (++elapsed < RECOVERY_TICKS) return;
        finish(dragon, 2);
    }

    private boolean startWings(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        var floor = BossAttackGeometry.ground(level, dragon, dragon.position().add(0, .5, 0), 4);
        if (floor == null || dragon.getY() - floor.y > 2.6) return false;
        Vec3 forward = Vec3.directionFromRotation(0, dragon.getYRot()).scale(-1);
        var sweep = new DragonWingAttack(floor.add(forward.scale(2.5)), forward);
        if (!sweep.hasTarget(level, dragon, players)) return false;
        wing = sweep;
        enter(dragon, WING_RAISE);
        wing.warn(level, dragon);
        return true;
    }

    private void raiseWings(ServerLevel level, SourceEnderDragon dragon) {
        if (++elapsed >= 14) {
            enter(dragon, WING_SWEEP);
        } else if (elapsed % 5 == 0) {
            wing.mark(level, dragon);
        }
    }

    private void sweepWings(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        ++elapsed;
        if (elapsed == 3) wing.push(level, dragon, players);
        if (elapsed >= 9) takeOff(dragon);
    }

    private void takeOff(SourceEnderDragon dragon) {
        dragon.setDeltaMovement(wing.forward().scale(.33).add(0, .49, 0));
        dragon.sourceBrain().state.force = true;
        finish(dragon, switch (phase) { case 3 -> 3; case 2 -> 5; default -> 7; });
    }

    private static void markDive(ServerLevel level, SourceEnderDragon dragon) {
        BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON,
            dragon.sourceParts()[0].getBoundingBox().getCenter(), 1.3, 5);
    }

    private static Lane selectLane(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> candidates,
                                   List<ServerPlayer> players) {
        var targets = candidates.stream().filter(ServerPlayer::onGround).filter(p -> !p.isInWaterOrBubble())
            .filter(p -> p.distanceToSqr(dragon) < 58 * 58).sorted(Comparator.comparingDouble(dragon::distanceToSqr)).toList();
        for (var target : targets) {
            Vec3 center = target.position();
            Vec3 toward = center.subtract(dragon.position()).multiply(1, 0, 1).normalize();
            if (toward.lengthSqr() < .5) toward = new Vec3(0, 0, 1);
            for (int turn : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                double angle = Math.PI * turn / 4;
                Vec3 forward = new Vec3(toward.x * Math.cos(angle) - toward.z * Math.sin(angle), 0,
                    toward.x * Math.sin(angle) + toward.z * Math.cos(angle));
                var choice = new Lane(center, forward, new Vec3(-forward.z, 0, forward.x), DIVE_OVERRUN - 34, DIVE_OVERRUN);
                if (availableLane(level, dragon, choice, players)) return choice;
                var compact = new Lane(center, forward, choice.side, DIVE_OVERRUN - 21, DIVE_OVERRUN);
                if (availableLane(level, dragon, compact, players)) return compact;
            }
        }
        return null;
    }

    private static boolean availableLane(ServerLevel level, SourceEnderDragon dragon, Lane lane, List<ServerPlayer> players) {
        return dragon.distanceToSqr(lane.bodyAt(lane.entry - 5, 8)) < 65 * 65
            && clearLane(level, dragon, lane) && hasEscapeRoutes(level, lane, players);
    }

    private static boolean clearLane(ServerLevel level, SourceEnderDragon dragon, Lane lane) {
        double start = lane.entry - HEAD_OFFSET;
        double end = lane.exit + (lane.exit - lane.entry) / SWEEP_TICKS * RECOVERY_DRAG / (1 - RECOVERY_DRAG);
        return clearLaneSection(level, dragon, lane, start, end);
    }

    private static boolean clearLaneSection(ServerLevel level, SourceEnderDragon dragon, Lane lane, double start, double end) {
        int samples = Math.max(1, Mth.ceil((end - start) / .75));
        for (int i = 0; i <= samples; ++i) {
            Vec3 center = lane.point(Mth.lerp((double)i / samples, start, end), 0, .08);
            if (!level.hasChunkAt(BlockPos.containing(center))) return false;
            var column = new AABB(center.x - .75, center.y, center.z - .75, center.x + .75, center.y + 3, center.z + .75);
            if (level.getBlockCollisions(dragon, column).iterator().hasNext()) return false;
        }
        return true;
    }

    private static boolean hasEscapeRoutes(ServerLevel level, Lane lane, List<ServerPlayer> players) {
        for (var player : players) {
            if (!lane.contains(player)) continue;
            double lateral = lane.side(player.position());
            double clearance = HALF_WIDTH + .8;
            if (!escape(level, player, lane.side, clearance - lateral, clearance)
                    && !escape(level, player, lane.side.scale(-1), clearance + lateral, clearance)) return false;
        }
        return true;
    }

    static boolean escape(ServerLevel level, ServerPlayer player, Vec3 direction, double distance) {
        return escape(level, player, direction, distance, 4.6);
    }

    static boolean escape(ServerLevel level, ServerPlayer player, Vec3 direction, double distance, double limit) {
        if (distance > limit) return false;
        AABB box = player.getBoundingBox();
        for (int i = 1, steps = Mth.ceil(distance * 2); i <= steps; i++) {
            var offset = direction.scale(distance * i / steps);
            var cell = box.move(offset);
            if (!level.hasChunkAt(BlockPos.containing(cell.getCenter()))
                    || level.getBlockCollisions(player, cell.deflate(.015)).iterator().hasNext()
                    || !level.getBlockCollisions(player, new AABB(cell.minX, cell.minY - .65, cell.minZ,
                        cell.maxX, cell.minY + .01, cell.maxZ)).iterator().hasNext()) return false;
        }
        return true;
    }

    static void position(SourceEnderDragon dragon, Vec3 next, float yaw) {
        dragon.setDeltaMovement(next.subtract(dragon.position()));
        dragon.setYRot(yaw);
        dragon.setPos(next.x, next.y, next.z);
    }

    static float yaw(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(direction.x, -direction.z));
    }

    private void enter(SourceEnderDragon dragon, int next) {
        stage = next;
        elapsed = 0;
        dragon.setRitualPosture(next);
    }

    private void finish(SourceEnderDragon dragon, int wait) {
        enter(dragon, ORBIT);
        cooldown = wait;
        breathing = false;
        altar.reset();
        repertoire.reset();
        lane = null;
        diveStart = null;
        wing = null;
        struck.clear();
    }

    void save(CompoundTag tag) {
        if (phase > 1) tag.putInt("MiteRitualCombatPhase", phase);
        arena.save(tag);
    }

    void load(CompoundTag tag) {
        phase = Mth.clamp(tag.getInt("MiteRitualCombatPhase"), 1, 3);
        arena.load(tag);
    }

    private record Lane(Vec3 center, Vec3 forward, Vec3 side, double entry, double exit) {
        double along(Vec3 position) { return position.subtract(center).dot(forward); }
        double side(Vec3 position) { return position.subtract(center).dot(side); }
        float yaw() { return DragonCombat.yaw(forward); }
        Vec3 point(double along, double lateral, double height) {
            return center.add(forward.scale(along)).add(side.scale(lateral)).add(0, height, 0);
        }
        Vec3 bodyAt(double along, double height) { return point(along - HEAD_OFFSET, 0, height); }
        boolean contains(ServerPlayer player) {
            Vec3 feet = player.position();
            double along = along(feet);
            return along >= entry - .31 && along <= exit + .31 && Math.abs(side(feet)) <= HALF_WIDTH + .31
                && player.getBoundingBox().maxY > center.y && feet.y <= center.y + 1.55;
        }
    }
}
