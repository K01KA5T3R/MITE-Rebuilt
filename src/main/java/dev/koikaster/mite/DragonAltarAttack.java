package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A charge aimed at the exit altar, with a timed, physical head weak point. */
final class DragonAltarAttack {
    static final int POSITIONING = 8;
    static final int CHARGE = 9;
    static final int INTERRUPTED = 10;
    static final int RELEASED = 11;
    private static final int CHARGE_TICKS = 31;
    private static final int SETTLE_TICKS = 7;
    private static final double RADIUS = 5.3;
    private static final SimpleParticleType EDGE = ParticleTypes.ELECTRIC_SPARK;
    private static final Vec3 DEFAULT_DIRECTION = new Vec3(0, 0, 1);

    private Vec3 center;
    private Vec3 focus;
    private Vec3 hover;
    private Vec3 facing;
    private List<BlockPos> ridges = List.of();
    private int ticks;
    private int settledTicks;

    boolean active() {
        return hover != null;
    }

    boolean start(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        BlockPos altar = DragonRitual.altar(level);
        if (altar == null) return false;
        focus = Vec3.atBottomCenterOf(altar.above()).add(0, .2, 0);
        center = focus.add(0, -3.2, 0);
        if (players.stream().noneMatch(player -> player.distanceToSqr(center) < 14 * 14)
                || !hasEscapeRoutes(level, players)) return false;

        Vec3 side = horizontal(dragon.position().subtract(center));
        if (side.lengthSqr() < .5) side = DEFAULT_DIRECTION;
        for (int i = 0; i < 4; i++) {
            var candidate = focus.add(side.scale(11)).add(0, -.6, 0);
            if (clearHover(level, dragon, candidate, side.scale(-1))) {
                hover = candidate;
                facing = side.scale(-1);
                settledTicks = 0;
                enter(dragon, POSITIONING);
                return true;
            }
            side = new Vec3(-side.z, 0, side.x);
        }
        return false;
    }

    boolean step(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        ++ticks;
        return switch (dragon.ritualPosture()) {
            case POSITIONING -> approach(level, dragon, players);
            case CHARGE -> charge(level, dragon, players);
            case INTERRUPTED, RELEASED -> recover(level, dragon);
            default -> false;
        };
    }

    void interrupt(ServerLevel level, SourceEnderDragon dragon) {
        if (!active() || dragon.ritualPosture() != CHARGE) return;
        enter(dragon, INTERRUPTED);
        Vec3 head = head(dragon);
        level.sendParticles(ParticleTypes.END_ROD, head.x, head.y, head.z, 28, .45, .35, .45, .22);
        level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_HURT, SoundSource.HOSTILE, 4, .65f);
    }

    void reset() {
        hover = null;
        ridges = List.of();
    }

    private boolean approach(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        Vec3 delta = hover.subtract(dragon.position());
        if (delta.lengthSqr() <= 1) {
            if (!clearHover(level, dragon, hover, facing)) return false;
            DragonCombat.position(dragon, hover, DragonCombat.yaw(facing));
            // Allow the native model's position history to catch up before lighting the weak point.
            if (++settledTicks < SETTLE_TICKS) return true;
            if (!hasEscapeRoutes(level, players)) return false;
            ridges = DragonRitualTerrain.plan(level, dragon, DragonRitual.altar(level));
            enter(dragon, CHARGE);
            level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4, 1.15f);
            return true;
        }
        if (ticks > 74) return false;
        settledTicks = 0;
        Vec3 direction = delta.normalize();
        Vec3 next = dragon.position().add(direction.scale(Math.min(1.25, delta.length())));
        if (!clear(level, dragon, next, 1.5, 2.5)) return false;
        DragonCombat.position(dragon, next, DragonCombat.yaw(direction));
        return true;
    }

    private boolean charge(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        dragon.setDeltaMovement(Vec3.ZERO);
        if (ticks >= CHARGE_TICKS) {
            if (!hasEscapeRoutes(level, players)) {
                enter(dragon, INTERRUPTED);
                return true;
            }
            enter(dragon, RELEASED);
            level.playSound(null, BlockPos.containing(center), SoundEvents.ENDER_DRAGON_SHOOT,
                SoundSource.HOSTILE, 4, .65f);
            for (var player : players) {
                if (threatens(player)) player.hurt(level.damageSources().mobAttack(dragon), 17);
            }
            var available = DragonRitualTerrain.available(level, dragon, ridges);
            if (!available.isEmpty() && hasEscapeRoutes(level, players, available)) {
                DragonRitualTerrain.place(level, dragon.getUUID(), available);
            }
            pulse(level);
        } else if (ticks % 4 == 1) {
            ring(level, EDGE, RADIUS, .16);
            BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON, head(dragon), 1.45, 5);
            BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON, focus, 1.25, 5);
            for (BlockPos at : ridges) point(level, ParticleTypes.END_ROD, Vec3.atBottomCenterOf(at).add(0, .18, 0));
        }
        return true;
    }

    private boolean recover(ServerLevel level, SourceEnderDragon dragon) {
        if (ticks <= 9) {
            Vec3 next = dragon.position().add(facing.scale(-.14)).add(0, .12, 0);
            if (clear(level, dragon, next, 1.5, 2.5)) DragonCombat.position(dragon, next, DragonCombat.yaw(facing));
        } else {
            dragon.setDeltaMovement(Vec3.ZERO);
        }
        if (dragon.ritualPosture() == RELEASED && ticks <= 9 && ticks % 3 == 0) pulse(level);
        if (ticks < (dragon.ritualPosture() == INTERRUPTED ? 32 : 22)) return true;
        dragon.setDeltaMovement(new Vec3(0, .55, 0));
        dragon.sourceBrain().state.force = true;
        return false;
    }

    private boolean threatens(ServerPlayer player) {
        var feet = player.position();
        return feet.y >= center.y - .4 && feet.y <= center.y + 2.1
            && feet.subtract(center).horizontalDistanceSqr() <= RADIUS * RADIUS;
    }

    private boolean hasEscapeRoutes(ServerLevel level, List<ServerPlayer> players) {
        return hasEscapeRoutes(level, players, List.of());
    }

    private boolean hasEscapeRoutes(ServerLevel level, List<ServerPlayer> players, List<BlockPos> proposed) {
        for (var player : players) {
            if (!threatens(player)) continue;
            var offset = player.position().subtract(center);
            double distance = Math.sqrt(offset.horizontalDistanceSqr());
            Vec3 outward = distance < .01 ? DEFAULT_DIRECTION : horizontal(offset);
            Vec3 diagonal = new Vec3(-outward.z, 0, outward.x).scale(.72);
            if (!escape(level, player, outward, RADIUS + .5 - distance, proposed)
                    && !escape(level, player, outward.add(diagonal).normalize(), RADIUS + .5 - distance * .65, proposed)
                    && !escape(level, player, outward.subtract(diagonal).normalize(), RADIUS + .5 - distance * .65, proposed)) return false;
        }
        return true;
    }

    private static boolean escape(ServerLevel level, ServerPlayer player, Vec3 direction, double distance,
                                  List<BlockPos> proposed) {
        if (!DragonCombat.escape(level, player, direction, distance, RADIUS + .5)) return false;
        if (proposed.isEmpty()) return true;
        Vec3 travel = direction.scale(distance);
        AABB sweep = player.getBoundingBox().deflate(.015).expandTowards(travel);
        for (BlockPos at : proposed) {
            if (DragonRitualTerrain.bounds(at).intersects(sweep)) return false;
        }
        return true;
    }

    private static boolean clearHover(ServerLevel level, SourceEnderDragon dragon, Vec3 candidate, Vec3 forward) {
        return clear(level, dragon, candidate, 2, 2.5) && clear(level, dragon, candidate.add(forward.scale(5.5)), 1, 2);
    }

    private static boolean clear(ServerLevel level, SourceEnderDragon dragon, Vec3 point, double halfWidth, double height) {
        return level.hasChunkAt(BlockPos.containing(point))
            && !level.getBlockCollisions(dragon, new AABB(point.x - halfWidth, point.y - .05, point.z - halfWidth,
                point.x + halfWidth, point.y + height, point.z + halfWidth)).iterator().hasNext();
    }

    private void pulse(ServerLevel level) {
        ring(level, ParticleTypes.DRAGON_BREATH, RADIUS, .3);
        ring(level, EDGE, RADIUS * .6, .65);
    }

    private void ring(ServerLevel level, ParticleOptions particle, double radius, double height) {
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2 * i / 28;
            point(level, particle, center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius));
        }
    }

    private static Vec3 horizontal(Vec3 vector) {
        return vector.multiply(1, 0, 1).normalize();
    }

    private static Vec3 head(SourceEnderDragon dragon) {
        return dragon.sourceParts()[0].position().add(0, .8, 0);
    }

    private static void point(ServerLevel level, ParticleOptions particle, Vec3 at) {
        level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    private void enter(SourceEnderDragon dragon, int posture) {
        ticks = 0;
        dragon.setRitualPosture(posture);
    }
}
