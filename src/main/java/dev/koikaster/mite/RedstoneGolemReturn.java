package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class RedstoneGolemReturn {
    private static final double OUTBOUND_SPEED = 1.12;
    private static final double INBOUND_SPEED = 1.30;
    private static final double ANCHOR_HEIGHT = .68;
    private static final double CONTACT = .49;
    private static final int TURN_TICKS = 4;

    private final Vec3 origin;
    private final Vec3 ground;
    private final Vec3 anchor;
    private Vec3 position;
    private boolean returning;
    private int pause;

    private RedstoneGolemReturn(Vec3 origin, Vec3 ground) {
        this.origin = origin;
        this.ground = ground;
        anchor = ground.add(0, ANCHOR_HEIGHT, 0);
        position = origin;
    }

    static RedstoneGolemReturn plan(ServerLevel level, SourceRedstoneGolem golem, ServerPlayer target, Vec3 mouth) {
        Vec3 feet = target.position();
        Vec3 toward = feet.subtract(golem.position());
        double distance = toward.horizontalDistance();
        if (distance < 4.5 || distance > 11.5 || Math.abs(toward.y) > 1.1) return null;
        var forward = BossAttackGeometry.flatDirection(toward);
        var side = new Vec3(-forward.z, 0, forward.x);
        if (!DragonCombat.escape(level, target, side, 1.7, 1.7)
                && !DragonCombat.escape(level, target, side.reverse(), 1.7, 1.7)) return null;
        var ground = RedstoneGolemPatterns.floor(level, golem, feet, .12);
        if (ground == null) return null;
        var origin = mouth.add(forward.scale(.55)).add(0, -.35, 0);
        var candidate = new RedstoneGolemReturn(origin, ground);
        return clear(level, golem, origin, candidate.anchor) ? candidate : null;
    }

    Vec3 direction() {
        return anchor.subtract(origin);
    }

    boolean tick(ServerLevel level, SourceRedstoneGolem golem, List<ServerPlayer> players) {
        if (!RedstoneGolemPatterns.sameGround(level, golem, ground)) return false;
        if (!returning && position.equals(anchor)) {
            BossAttackGeometry.point(level, ParticleTypes.END_ROD, anchor);
            if (++pause < TURN_TICKS) return true;
            returning = true;
            level.playSound(null, BlockPos.containing(anchor), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.HOSTILE, .8f, 1.5f);
        }
        Vec3 destination = returning ? origin : anchor;
        Vec3 delta = destination.subtract(position);
        double speed = returning ? INBOUND_SPEED : OUTBOUND_SPEED;
        Vec3 requested = delta.lengthSqr() <= speed * speed ? destination : position.add(delta.normalize().scale(speed));
        if (!validSegment(level, position, requested)) return false;
        var trace = level.clip(new ClipContext(position, requested, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, golem));
        Vec3 end = trace.getLocation();
        BossAttackGeometry.line(level, ParticleTypes.ELECTRIC_SPARK, position, end, returning ? .18 : .42);
        BossAttackGeometry.point(level, ParticleTypes.END_ROD, end);
        if (returning) {
            for (var player : players) {
                var box = player.getBoundingBox();
                Vec3 center = box.getCenter();
                var expanded = box.inflate(CONTACT);
                Vec3 hit = expanded.contains(position) ? position : expanded.clip(position, end).orElse(null);
                if (hit != null && BossAttackGeometry.visible(level, golem, hit, center)) golem.hurtPlayer(level, player, 11);
            }
        }
        if (trace.getType() != HitResult.Type.MISS) return false;
        position = requested;
        return !returning || !requested.equals(origin);
    }

    private static boolean validSegment(ServerLevel level, Vec3 from, Vec3 to) {
        var start = BlockPos.containing(from);
        var end = BlockPos.containing(to);
        return level.hasChunksAt(start, end) && level.getWorldBorder().isWithinBounds(start)
            && level.getWorldBorder().isWithinBounds(end);
    }

    private static boolean clear(ServerLevel level, SourceRedstoneGolem golem, Vec3 from, Vec3 to) {
        return validSegment(level, from, to)
            && level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, golem))
                .getType() == HitResult.Type.MISS;
    }
}
