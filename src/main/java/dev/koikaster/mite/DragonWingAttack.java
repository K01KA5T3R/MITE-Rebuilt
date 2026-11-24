package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Ground-level gust after the dragon has given melee players a recovery window. */
record DragonWingAttack(Vec3 center, Vec3 forward) {
    private static final double RADIUS = 10;
    private static final double HALF_ARC = 1.43;
    private static final double PUSH_CLEARANCE = 4.2;

    boolean hasTarget(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        return players.stream().anyMatch(player -> pushDirection(level, dragon, player).lengthSqr() > 0);
    }

    void warn(ServerLevel level, SourceEnderDragon dragon) {
        level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3, 1.35f);
        mark(level, dragon);
    }

    void mark(ServerLevel level, SourceEnderDragon dragon) {
        for (int part = 5; part <= 6; ++part) {
            Vec3 wing = dragon.sourceParts()[part].getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.POOF, wing.x, wing.y, wing.z, 10, .7, .25, .7, .08);
        }
    }

    void push(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 4, .72f);
        for (int i = 0; i <= 24; i++) {
            Vec3 edge = edge(i, .55);
            level.sendParticles(ParticleTypes.POOF, edge.x, edge.y, edge.z, 2, .14, .10, .14, .045);
        }
        for (var player : players) {
            Vec3 outward = pushDirection(level, dragon, player);
            if (outward.lengthSqr() == 0) continue;
            player.hurt(level.damageSources().mobAttack(dragon), 13);
            double strength = 1 - player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            if (strength <= 0) continue;
            player.setDeltaMovement(outward.x * .72 * strength, .22 * strength, outward.z * .72 * strength);
            player.hurtMarked = true;
        }
    }

    private Vec3 pushDirection(ServerLevel level, SourceEnderDragon dragon, ServerPlayer player) {
        Vec3 offset = player.position().subtract(center);
        if (offset.y < -.55 || offset.y > 1.35 || offset.horizontalDistanceSqr() > RADIUS * RADIUS) return Vec3.ZERO;
        Vec3 outward = offset.horizontalDistanceSqr() < .04 ? forward : BossAttackGeometry.flatDirection(offset);
        if (outward.dot(forward) < Math.cos(HALF_ARC)) return Vec3.ZERO;
        Vec3 origin = center.add(0, .75, 0);
        if (level.clip(new ClipContext(origin, player.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon)).getType() != HitResult.Type.MISS) return Vec3.ZERO;
        return DragonCombat.escape(level, player, outward, PUSH_CLEARANCE) ? outward : Vec3.ZERO;
    }

    private Vec3 edge(int point, double height) {
        double angle = Math.atan2(forward.z, forward.x) + HALF_ARC * (point / 12d - 1);
        return center.add(Math.cos(angle) * RADIUS, height, Math.sin(angle) * RADIUS);
    }
}
