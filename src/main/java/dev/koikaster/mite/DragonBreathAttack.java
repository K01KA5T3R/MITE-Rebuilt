package dev.koikaster.mite;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class DragonBreathAttack {
    static final int TRACK = 5;
    static final int LOCK = 6;
    static final int RECOVERY = 7;
    private static final String RITUAL_OWNER = "MiteRitualBreathOwner";
    static final int TRACK_TICKS = 8;
    static final int LOCK_TICKS = 21;
    private static final int RECOVERY_TICKS = 13;
    // The native DragonBreathParticle can remain for up to 100 client ticks.
    private static final int CLOUD_FADE_TICKS = 104;
    private static final int CLOUD_WAIT_TICKS = 3;
    private static final int CLOUD_GROWTH_TICKS = 18;
    private static final float INITIAL_RADIUS = 4.25f;
    private static final float RADIUS = 7.75f;
    private static final int STREAM_TICKS = 8;

    private UUID target;
    private Vec3 aim;
    private int elapsed;
    private AreaEffectCloud cloud;
    private Vec3 streamMouth;
    private List<Vec3> streamEnds = List.of();
    private long boundaryQuietUntil;

    boolean start(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players) {
        for (var player : players.stream().sorted(Comparator.comparingDouble(dragon::distanceToSqr)).toList()) {
            Vec3 landing = aimAt(level, dragon, player);
            if (landing == null) continue;
            target = player.getUUID();
            aim = landing;
            setStage(dragon, TRACK);
            level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4, .98f);
            return true;
        }
        return false;
    }

    boolean step(ServerLevel level, SourceEnderDragon dragon, List<ServerPlayer> players, DragonArenaPressure arena) {
        ++elapsed;
        var velocity = dragon.getDeltaMovement().scale(.77);
        var next = dragon.position().add(velocity);
        if (BossAttackGeometry.visible(level, dragon, dragon.position().add(0, 1, 0), next.add(0, 1, 0))) {
            DragonCombat.position(dragon, next, dragon.getYRot());
        } else dragon.setDeltaMovement(Vec3.ZERO);
        switch (dragon.ritualPosture()) {
            case TRACK -> {
                var victim = level.getPlayerByUUID(target);
                if (!(victim instanceof ServerPlayer player) || !players.contains(player)) return false;
                Vec3 landing = aimAt(level, dragon, player);
                if (landing == null || !arena.allows(landing, RADIUS + .75)) return false;
                aim = landing;
                face(dragon);
                if (elapsed % 3 == 0) markMouth(level, dragon);
                if (elapsed >= TRACK_TICKS) {
                    if (!escapable(level, players)) return false;
                    setStage(dragon, LOCK);
                    markLanding(level, dragon);
                }
            }
            case LOCK -> {
                face(dragon);
                if (elapsed >= LOCK_TICKS) {
                    if (!escapable(level, players) || !visible(level, dragon, aim)) return false;
                    createCloud(level, dragon);
                    startStreams(level, dragon);
                    setStage(dragon, RECOVERY);
                } else if (elapsed % 5 == 0) {
                    markLanding(level, dragon);
                }
            }
            case RECOVERY -> {
                if (elapsed < STREAM_TICKS) stream(level, dragon, elapsed);
                if (elapsed >= RECOVERY_TICKS) return false;
            }
            default -> { return false; }
        }
        return true;
    }

    private static Vec3 aimAt(ServerLevel level, SourceEnderDragon dragon, ServerPlayer player) {
        double distance = player.distanceToSqr(dragon);
        if (player.isInWaterOrBubble() || distance < 12 * 12 || distance > 46 * 46) return null;
        Vec3 feet = player.position();
        BlockHitResult floor = level.clip(new ClipContext(feet.add(0, .06, 0), feet.add(0, -3.25, 0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (floor.getType() == HitResult.Type.MISS) return null;
        Vec3 landing = floor.getLocation();
        return dragon.getY() - landing.y >= 4 && visible(level, dragon, landing) ? landing : null;
    }

    private static boolean visible(ServerLevel level, SourceEnderDragon dragon, Vec3 landing) {
        Vec3 body = dragon.position().add(0, 1.3, 0);
        return level.clip(new ClipContext(body, landing.add(0, .4, 0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, dragon)).getType() == HitResult.Type.MISS;
    }

    private boolean escapable(ServerLevel level, List<ServerPlayer> players) {
        for (var player : players) {
            Vec3 offset = player.position().subtract(aim).multiply(1, 0, 1);
            if (Math.abs(player.getY() - aim.y) > 1.5 || offset.lengthSqr() > (RADIUS + .31) * (RADIUS + .31)) continue;
            if (!canLeavePool(level, player, offset)) return false;
        }
        return true;
    }

    private static boolean canLeavePool(ServerLevel level, ServerPlayer player, Vec3 offset) {
        double initial = offset.lengthSqr() > .01 ? Math.atan2(offset.z, offset.x) : 0;
        double rim = RADIUS + .75;
        for (int i = 0; i < 8; i++) {
            double angle = initial + Math.PI * i / 4;
            var direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            double projection = direction.dot(offset);
            double length = -projection + Math.sqrt(projection * projection + rim * rim - offset.lengthSqr());
            if (DragonCombat.escape(level, player, direction, length, rim + .15)) return true;
        }
        return false;
    }

    private static void markMouth(ServerLevel level, SourceEnderDragon dragon) {
        BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON,
            dragon.sourceParts()[0].getBoundingBox().getCenter(), 1.6, 4);
    }

    private void markLanding(ServerLevel level, SourceEnderDragon dragon) {
        BossCombatVisual.cue(level, dragon, BossCombatVisual.Theme.DRAGON, aim.add(0, .35, 0), 1.6, 6);
    }

    private void startStreams(ServerLevel level, SourceEnderDragon dragon) {
        streamMouth = dragon.sourceParts()[0].getBoundingBox().getCenter().add(0, -.25, 0);
        streamEnds = streamLandings(streamMouth, aim).stream()
            .map(end -> BossAttackGeometry.shorten(level, dragon, streamMouth, end)).toList();
        stream(level, dragon, 0);
    }

    static List<Vec3> streamLandings(Vec3 mouth, Vec3 aim) {
        Vec3 forward = BossAttackGeometry.flatDirection(aim.subtract(mouth));
        if (forward.lengthSqr() < .001) forward = new Vec3(0, 0, 1);
        Vec3 side = new Vec3(-forward.z, 0, forward.x).scale(INITIAL_RADIUS * .80);
        Vec3 center = aim.add(0, .38, 0);
        return List.of(center.subtract(side), center, center.add(side));
    }

    private void stream(ServerLevel level, SourceEnderDragon dragon, int age) {
        for (Vec3 locked : streamEnds) {
            Vec3 end = BossAttackGeometry.shorten(level, dragon, streamMouth, locked);
            double leading = Math.min(1, (age + 1) / 5d);
            double trailing = Math.max(0, (age - 3) / 5d);
            Vec3 from = streamMouth.lerp(end, trailing);
            Vec3 to = streamMouth.lerp(end, leading);
            BossAttackGeometry.line(level, ParticleTypes.DRAGON_BREATH, from, to, .65);
        }
    }

    private void createCloud(ServerLevel level, SourceEnderDragon dragon) {
        clearCloud();
        var pool = new AreaEffectCloud(level, aim.x, aim.y, aim.z);
        pool.setOwner(dragon);
        pool.getPersistentData().putUUID(RITUAL_OWNER, dragon.getUUID());
        pool.setParticle(ParticleTypes.DRAGON_BREATH);
        pool.setRadius(INITIAL_RADIUS);
        pool.setDuration(72);
        pool.setWaitTime(CLOUD_WAIT_TICKS);
        pool.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 1));
        if (level.addFreshEntity(pool)) cloud = pool;
        level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 4, 1.05f);
    }

    void clearCloud() {
        if (cloud != null && !cloud.isRemoved()) cloud.discard();
        cloud = null;
        streamMouth = null;
        streamEnds = List.of();
    }

    static boolean ritualCloud(AreaEffectCloud cloud) {
        return cloud.getPersistentData().hasUUID(RITUAL_OWNER);
    }

    static void clearRitualClouds(ServerLevel level, UUID owner) {
        if (owner == null) return;
        var area = new AABB(-136, level.getMinBuildHeight(), -136, 136, level.getMaxBuildHeight(), 136);
        for (var pool : level.getEntitiesOfClass(AreaEffectCloud.class, area,
                cloud -> ritualCloud(cloud) && owner.equals(cloud.getPersistentData().getUUID(RITUAL_OWNER)))) pool.discard();
    }

    boolean hasActiveCloud() {
        return cloud != null && !cloud.isRemoved();
    }

    boolean blocksBoundary(long now) {
        if (hasActiveCloud()) {
            if (cloud.getRadius() < RADIUS) {
                float growth = Mth.clamp((cloud.tickCount - CLOUD_WAIT_TICKS) / (float)CLOUD_GROWTH_TICKS, 0, 1);
                cloud.setRadius(Mth.lerp(growth, INITIAL_RADIUS, RADIUS));
            }
            boundaryQuietUntil = now + CLOUD_FADE_TICKS;
        }
        return now < boundaryQuietUntil;
    }

    private void face(SourceEnderDragon dragon) {
        var delta = aim.subtract(dragon.position());
        float yaw = (float) Math.toDegrees(Math.atan2(delta.x, -delta.z));
        dragon.setYRot(Mth.approachDegrees(dragon.getYRot(), yaw, 10));
    }

    private void setStage(SourceEnderDragon dragon, int stage) {
        dragon.setRitualPosture(stage);
        elapsed = 0;
    }
}
