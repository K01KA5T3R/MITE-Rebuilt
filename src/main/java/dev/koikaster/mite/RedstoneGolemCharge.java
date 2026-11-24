package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class RedstoneGolemCharge {
    private static final double GRAVITY = .045;
    private static final double MAX_SPEED = 1.35;
    private static final int MAX_FLIGHT = 26;
    private static final int PULSE_DELAY = 5;
    private static final double PULSE_RADIUS = 1.05;
    private static final double CONTACT = .17;

    private final Vec3 landing;
    private Vec3 position;
    private Vec3 velocity;
    private int age;
    private int discharge;

    private RedstoneGolemCharge(Vec3 landing) {
        this.landing = landing;
    }

    static RedstoneGolemCharge target(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer player, Vec3 mouth) {
        Vec3 delta = player.position().subtract(golem.position());
        double distance = delta.horizontalDistance();
        if (distance < 4 || distance > 14 || Math.abs(delta.y) > 2.25) return null;
        if (BossAttackGeometry.visible(level, golem, mouth, player.getBoundingBox().getCenter())) return null;
        Vec3 surface = BossAttackGeometry.ground(level, golem, player.position().add(0, .30, 0), .75);
        if (surface == null || !hasSupport(level, golem, surface)) return null;
        Vec3 forward = BossAttackGeometry.flatDirection(delta);
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        if (!DragonCombat.escape(level, player, side, 1.7, 1.85)
                && !DragonCombat.escape(level, player, side.reverse(), 1.7, 1.85)) return null;
        return initial(level, golem, mouth, surface) == null ? null : new RedstoneGolemCharge(surface);
    }

    Vec3 aim(Vec3 mouth) {
        return landing.add(0, 2.3, 0).subtract(mouth).normalize();
    }

    Vec3 release(ServerLevel level, SourceRedstoneGolem golem, Vec3 mouth) {
        velocity = initial(level, golem, mouth, landing);
        if (velocity == null) return null;
        position = mouth;
        return velocity;
    }

    boolean advance(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> fighters) {
        if (discharge != 0) return delay(level, golem, fighters);
        if (++age > MAX_FLIGHT) return false;
        Vec3 to = position.add(velocity);
        BlockHitResult hit = trace(level, golem, position, to);
        if (hit == null) return false;
        Vec3 end = hit.getLocation();
        ServerPlayer first = null;
        Vec3 impact = null;
        double nearest = position.distanceToSqr(end) + (hit.getType() == HitResult.Type.MISS ? 1e-9 : 0);
        for (var player : fighters) {
            var box = player.getBoundingBox().inflate(CONTACT);
            Vec3 touch = box.contains(position) ? position : box.clip(position, end).orElse(null);
            if (touch == null || !BossAttackGeometry.visible(level, golem, position, player.getBoundingBox().getCenter())) continue;
            double distance = position.distanceToSqr(touch);
            if (distance >= nearest) continue;
            first = player;
            impact = touch;
            nearest = distance;
        }
        if (first != null) {
            golem.hurtPlayer(level, first, 11);
            BossAttackGeometry.point(level, ParticleTypes.CRIT, impact);
            return false;
        }

        BossAttackGeometry.line(level, ParticleTypes.ELECTRIC_SPARK, position, end, .27);
        BossAttackGeometry.point(level, ParticleTypes.END_ROD, end);
        position = end;
        if (hit.getType() == HitResult.Type.BLOCK) {
            if (hit.getDirection() != Direction.UP || !hasSupport(level, golem, end)) return false;
            discharge = PULSE_DELAY + 1;
            return true;
        }
        velocity = velocity.add(0, -GRAVITY, 0);
        return true;
    }

    private boolean delay(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> fighters) {
        if (!hasSupport(level, golem, position)) return false;
        Vec3 center = position.add(0, .12, 0);
        if (--discharge > 1) {
            BossAttackGeometry.point(level, ParticleTypes.END_ROD, center);
            return true;
        }
        BossAttackGeometry.ring(level, ParticleTypes.ELECTRIC_SPARK, center, PULSE_RADIUS, 20);
        level.playSound(null, BlockPos.containing(center), SoundEvents.FIREWORK_ROCKET_BLAST,
            SoundSource.HOSTILE, .85f, 1.35f);
        for (var player : fighters) {
            var box = player.getBoundingBox();
            if (box.minY > center.y + .73 || box.maxY < position.y) continue;
            double x = Math.max(box.minX, Math.min(center.x, box.maxX));
            double z = Math.max(box.minZ, Math.min(center.z, box.maxZ));
            if (center.distanceToSqr(x, center.y, z) <= PULSE_RADIUS * PULSE_RADIUS
                    && BossAttackGeometry.visible(level, golem, center, player.position().add(0, .26, 0))) {
                golem.hurtPlayer(level, player, 11);
            }
        }
        return false;
    }

    private static Vec3 initial(ServerLevel level, SourceRedstoneGolem golem, Vec3 mouth, Vec3 landing) {
        if (!hasSupport(level, golem, landing)) return null;
        for (int ticks = 12; ticks <= MAX_FLIGHT; ticks += 2) {
            double gravityDrop = GRAVITY * ticks * (ticks - 1) * .5;
            Vec3 velocity = landing.subtract(mouth).add(0, gravityDrop - .09, 0).scale(1.0 / ticks);
            if (velocity.y < .26 || velocity.lengthSqr() > MAX_SPEED * MAX_SPEED) continue;
            if (landsNear(level, golem, mouth, velocity, landing, ticks)) return velocity;
        }
        return null;
    }

    private static boolean landsNear(ServerLevel level, SourceRedstoneGolem golem, Vec3 from, Vec3 velocity,
                                     Vec3 landing, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            Vec3 to = from.add(velocity);
            BlockHitResult hit = trace(level, golem, from, to);
            if (hit == null) return false;
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 at = hit.getLocation();
                return hit.getDirection() == Direction.UP && at.distanceToSqr(landing) <= .25 * .25
                    && hasSupport(level, golem, at);
            }
            from = to;
            velocity = velocity.add(0, -GRAVITY, 0);
        }
        return false;
    }

    private static BlockHitResult trace(ServerLevel level, SourceRedstoneGolem golem, Vec3 from, Vec3 to) {
        BlockPos start = BlockPos.containing(from);
        BlockPos end = BlockPos.containing(to);
        if (!level.hasChunksAt(start, end) || !level.getWorldBorder().isWithinBounds(end)) return null;
        var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, golem));
        if (hit.getType() == HitResult.Type.BLOCK && !level.getFluidState(hit.getBlockPos()).isEmpty()) return null;
        return hit;
    }

    private static boolean hasSupport(ServerLevel level, SourceRedstoneGolem golem, Vec3 at) {
        Vec3 ground = BossAttackGeometry.ground(level, golem, at.add(0, .10, 0), .19);
        return ground != null && Math.abs(ground.y - at.y) <= .045
            && level.getFluidState(BlockPos.containing(at.add(0, .04, 0))).isEmpty()
            && level.getFluidState(BlockPos.containing(at.add(0, -.04, 0))).isEmpty();
    }
}