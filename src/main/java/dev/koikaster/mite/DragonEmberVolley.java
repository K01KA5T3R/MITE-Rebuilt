package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class DragonEmberVolley {
    private static final int RELEASE_INTERVAL = 5;
    private static final int MAX_FLIGHT_TICKS = 22;
    private static final int RELEASE_WINDOW = RELEASE_INTERVAL * 2;
    static final int LAST_RELEASE = RELEASE_WINDOW;
    static final int STRIKE_TICKS = LAST_RELEASE + MAX_FLIGHT_TICKS;
    private static final double EMBER_RADIUS = .55;
    private static final double LANDING_RADIUS = 2.15;
    private static final double LANDING_MARGIN = LANDING_RADIUS + .6;
    private static final double LANDING_HEIGHT = .32;
    private static final float CONTACT_DAMAGE = 16.5f;
    private static final float LANDING_DAMAGE = 15.5f;

    record Flight(Vec3 launch, Vec3 landing, int released, int duration, double lift) {
        static Flight create(Vec3 launch, Vec3 landing, int released) {
            double horizontal = launch.subtract(landing).horizontalDistance();
            int duration = Mth.clamp(Mth.ceil(launch.distanceTo(landing) / 1.6), 12, MAX_FLIGHT_TICKS);
            return new Flight(launch, landing, released, duration, Mth.clamp(horizontal / 9, 1.6, 4.4));
        }

        Vec3 point(int tick) {
            double progress = Mth.clamp((tick - released) / (double)duration, 0, 1);
            return launch.lerp(landing, progress).add(0, 4 * lift * progress * (1 - progress), 0);
        }

        boolean arrives(int tick) { return tick >= released + duration; }

        boolean threatens(AABB box) {
            Vec3 feet = new Vec3((box.minX + box.maxX) * .5, box.minY, (box.minZ + box.maxZ) * .5);
            if (inImpact(landing.add(0, -LANDING_HEIGHT, 0), feet)) return true;
            Vec3 from = launch;
            for (int tick = 1; tick <= duration; ++tick) {
                Vec3 to = point(released + tick);
                if (BossAttackGeometry.hits(box, from, to, EMBER_RADIUS)) return true;
                from = to;
            }
            return false;
        }
    }

    private final List<Flight> active = new ArrayList<>(5);
    private final Set<UUID> hit = new HashSet<>();
    private List<Vec3> landings = List.of();
    private Vec3 axis;
    private Vec3 launch;
    private int volleys;
    private int nextShot;

    boolean prepare(ServerLevel level, SourceEnderDragon dragon, Vec3 floor,
                    List<ServerPlayer> players, DragonArenaPressure arena) {
        var origin = dragon.position();
        if (origin.distanceToSqr(floor) > 42 * 42) return false;
        Vec3 facing = Vec3.directionFromRotation(0, dragon.getYRot()).scale(-1);
        Vec3 heading = volleyHeading(origin, floor, facing);
        Vec3 mouth = mouth(dragon, heading);
        int count = projectileCount(volleys);
        var shots = new ArrayList<Flight>(count);
        for (int index = 0; index < count; ++index) {
            double offset = switch (index) {
                case 0 -> 0;
                case 1 -> 4.5;
                case 2 -> -4.5;
                case 3 -> volleys % 2 == 0 ? 2.25 : -2.25;
                default -> volleys % 2 == 0 ? -2.25 : 2.25;
            };
            Vec3 site = BossAttackGeometry.ground(level, dragon, floor.add(heading.scale(offset)).add(0, 2, 0), 5);
            if (site != null && arena.allows(site, LANDING_MARGIN)) {
                var shot = Flight.create(mouth, site.add(0, LANDING_HEIGHT, 0), releaseTick(shots.size(), count));
                if (openArc(level, dragon, shot)) {
                    shots.add(shot);
                    continue;
                }
            }
            if (offset == 0) return false;
        }
        if (shots.size() < 2 || !hasEscape(level, players, arena, heading, shots)) return false;
        landings = shots.stream().map(Flight::landing).toList();
        axis = heading;
        return true;
    }

    static int projectileCount(int priorVolleys) { return priorVolleys % 5 == 4 ? 5 : 4; }

    static int releaseTick(int index, int count) {
        return index * RELEASE_WINDOW / Math.max(1, count - 1);
    }

    static Vec3 volleyHeading(Vec3 origin, Vec3 floor, Vec3 facing) {
        Vec3 flatOffset = floor.subtract(origin).multiply(1, 0, 1);
        return flatOffset.lengthSqr() < .25 ? facing : flatOffset.normalize();
    }

    Vec3 heading() { return axis; }

    boolean landedHit() { return !hit.isEmpty(); }

    boolean lockLaunch(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, DragonArenaPressure arena) {
        Vec3 mouth = mouth(dragon, axis);
        var shots = new ArrayList<Flight>(landings.size());
        for (int i = 0; i < landings.size(); ++i) {
            var shot = Flight.create(mouth, landings.get(i), releaseTick(i, landings.size()));
            if (!openArc(level, dragon, shot)) return false;
            shots.add(shot);
        }
        if (!hasEscape(level, players, arena, axis, shots)) return false;
        launch = mouth;
        volleys = (volleys + 1) % 10;
        nextShot = 0;
        return true;
    }

    void warn(ServerLevel level, SourceEnderDragon dragon, int ticks) {
        BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON, mouth(dragon, axis), 1.8, ticks);
    }

    void step(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, int tick) {
        if (nextShot < landings.size() && tick == releaseTick(nextShot, landings.size())) {
            active.add(Flight.create(launch, landings.get(nextShot++), tick));
            level.sendParticles(ParticleTypes.DRAGON_BREATH, launch.x, launch.y, launch.z, 8, .28, .22, .28, .09);
        }
        active.removeIf(flight -> !advance(level, dragon, players, flight, tick));
    }

    void reset() {
        active.clear();
        landings = List.of();
        launch = null;
        hit.clear();
    }

    private boolean advance(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players,
                            Flight flight, int tick) {
        Vec3 from = flight.point(tick);
        Vec3 next = flight.point(tick + 1);
        if (!level.hasChunkAt(BlockPos.containing(next))) return false;
        var block = level.clip(new ClipContext(from, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        Vec3 end = block.getLocation();
        BossCombatVisual.dragonEmber(level, dragon, from, end, EMBER_RADIUS);
        for (var player : players) {
            var box = player.getBoundingBox();
            if (BossAttackGeometry.hits(box, from, end, EMBER_RADIUS)
                    && BossAttackGeometry.visible(level, dragon, from, box.getCenter())) hurt(level, dragon, player, CONTACT_DAMAGE);
        }
        if (block.getType() != HitResult.Type.MISS) {
            wallFlare(level, dragon, block);
            return false;
        }
        if (!flight.arrives(tick + 1)) return true;
        Vec3 ground = BossAttackGeometry.ground(level, dragon, end, .95);
        if (ground != null) {
            flare(level, dragon, end, LANDING_RADIUS);
            for (var player : players) {
                if (inImpact(ground, player.position())
                        && BossAttackGeometry.visible(level, dragon, end, player.getBoundingBox().getCenter())) {
                    hurt(level, dragon, player, LANDING_DAMAGE);
                }
            }
        } else flare(level, dragon, end, .9);
        return false;
    }

    static boolean inImpact(Vec3 floor, Vec3 feet) {
        var offset = feet.subtract(floor);
        return Math.abs(offset.y) < 1.35 && offset.horizontalDistanceSqr() <= LANDING_RADIUS * LANDING_RADIUS;
    }

    private void hurt(ServerLevel level, SourceEnderDragon dragon, ServerPlayer player, float damage) {
        if (!hit.contains(player.getUUID()) && player.hurt(level.damageSources().mobAttack(dragon), damage)) {
            hit.add(player.getUUID());
        }
    }

    private static void flare(ServerLevel level, SourceEnderDragon dragon, Vec3 at, double radius) {
        BossCombatVisual.area(level, dragon, BossCombatVisual.Theme.DRAGON, BossCombatVisual.Shape.CLOUD,
            at, .32, radius, 7);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 11, .25, .18, .25, .12);
    }

    private static void wallFlare(ServerLevel level, SourceEnderDragon dragon, BlockHitResult hit) {
        Vec3 face = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.12));
        BossCombatVisual.area(level, dragon, BossCombatVisual.Theme.DRAGON, BossCombatVisual.Shape.CLOUD,
            face, .32, .9, 7);
        level.sendParticles(ParticleTypes.END_ROD, face.x, face.y, face.z, 1, 0, 0, 0, 0);
    }

    private static boolean openArc(ServerLevel level, SourceEnderDragon dragon, Flight flight) {
        for (int tick = 0; tick < flight.duration(); ++tick) {
            Vec3 next = flight.point(tick + 1);
            if (!level.hasChunkAt(BlockPos.containing(next))
                    || !BossAttackGeometry.visible(level, dragon, flight.point(tick), next)) return false;
        }
        return true;
    }

    private static boolean hasEscape(ServerLevel level, List<ServerPlayer> players, DragonArenaPressure arena,
                                     Vec3 heading, List<Flight> shots) {
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        for (var player : players) {
            AABB body = player.getBoundingBox();
            if (shots.stream().noneMatch(shot -> shot.threatens(body))) continue;
            if (!canExit(level, player, arena, side, shots) && !canExit(level, player, arena, side.scale(-1), shots)
                    && !canExit(level, player, arena, heading, shots) && !canExit(level, player, arena, heading.scale(-1), shots)) return false;
        }
        return true;
    }

    private static boolean canExit(ServerLevel level, ServerPlayer player, DragonArenaPressure arena,
                                   Vec3 direction, List<Flight> shots) {
        AABB body = player.getBoundingBox();
        for (double distance = 2; distance <= 4; distance += .5) {
            Vec3 offset = direction.scale(distance);
            if (arena.allows(player.position().add(offset), .45)
                    && shots.stream().noneMatch(shot -> shot.threatens(body.move(offset)))
                    && DragonCombat.escape(level, player, direction, distance)) return true;
        }
        return false;
    }

    private static Vec3 mouth(SourceEnderDragon dragon, Vec3 heading) {
        return dragon.sourceParts()[0].getBoundingBox().getCenter().add(heading.scale(1.1)).add(0, -.25, 0);
    }
}
