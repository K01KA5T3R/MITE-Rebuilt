package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

final class GiantSkeletonBow {
    private static final float BASE_DAMAGE = 5.1f;
    private static final int DRAW_TICKS = 19;
    private static final int LOCK_TICKS = 5;
    private static final int UPPER_DRAW_TICKS = 13;
    private static final int MIN_LAYER_GAP_TICKS = 11;
    private static final int MAX_ARROW_FLIGHT_TICKS = 80;
    private static final double MAX_DISTANCE_SQUARED = 24 * 24;

    enum Shot {
        DIRECT, LOW, LOB, LAYERS
    }

    private enum LayersPhase { LOWER, GAP, UPPER }

    private record Aim(Vec3 facing, GiantArrowTrajectory.Flight flight) {}

    private final BossAttackOrder<Shot> order = new BossAttackOrder<>(Shot.class);
    private final List<Arrow> shots = new ArrayList<>();
    private final List<Arrow> flying = new ArrayList<>();
    private UUID targetId;
    private Shot selected;
    private Aim aim;
    private long fireAt;
    private long nextDraw;
    private long nextAimCheck;
    private GiantSkeletonBowLayers layers;
    private LayersPhase layerPhase;
    private long raiseUpperAt;

    void readyAfter(long now, int ticks) {
        nextDraw = now + ticks;
    }

    void tick(ServerLevel level, SourceGiantSkeleton boss, List<ServerPlayer> fighters) {
        updateFlyingArrows(level, boss);
        long now = level.getGameTime();
        if (now < nextDraw) return;
        if (layerPhase != null) {
            tickLayers(level, boss, fighters, now);
            return;
        }
        if (selected == null) {
            begin(level, boss, fighters, now);
            return;
        }
        var target = level.getPlayerByUUID(targetId);
        if (!(target instanceof ServerPlayer player) || !fighters.contains(player)
                || boss.distanceToSqr(player) > MAX_DISTANCE_SQUARED) {
            finish(level, boss, now + 7);
            return;
        }
        if (!boss.isUsingItem()) {
            finish(level, boss, now + 7);
            return;
        }
        long lockAt = fireAt - LOCK_TICKS;
        if (now <= lockAt && (now == lockAt || now >= nextAimCheck)) {
            nextAimCheck = now + 3;
            Aim updated = solution(level, boss, player, selected);
            if (updated != null) aim = updated;
            else if (now == lockAt) {
                finish(level, boss, now + 7);
                return;
            }
        }
        face(boss, aim.facing());
        if (now < fireAt) return;
        if (selected == Shot.DIRECT || GiantArrowTrajectory.stillClear(level, boss, aim.flight(), selected == Shot.LOW)) {
            fire(level, boss);
        }
        finish(level, boss, now + 24);
    }

    void cancel(ServerLevel level, SourceGiantSkeleton boss) {
        if (selected != null) finish(level, boss, level.getGameTime() + 7);
        shots.forEach(Arrow::discard);
        shots.clear();
        flying.clear();
        BossArenaPortals.release(level, boss.getUUID());
    }

    private void updateFlyingArrows(ServerLevel level, SourceGiantSkeleton boss) {
        flying.removeIf(arrow -> {
            if (arrow.isRemoved() || arrow.shakeTime > 0) return true;
            if (arrow.tickCount < MAX_ARROW_FLIGHT_TICKS) return false;
            arrow.discard();
            return true;
        });
        shots.removeIf(Arrow::isRemoved);
        refreshPortals(level, boss);
    }

    private void refreshPortals(ServerLevel level, SourceGiantSkeleton boss) {
        if (selected != null || !flying.isEmpty()) boss.holdAttackPortals(level);
        else BossArenaPortals.release(level, boss.getUUID());
    }

    private void begin(ServerLevel level, SourceGiantSkeleton boss, List<ServerPlayer> fighters, long now) {
        List<ServerPlayer> targets = fighters.stream().filter(player -> boss.distanceToSqr(player) < MAX_DISTANCE_SQUARED)
            .sorted(Comparator.comparingDouble(boss::distanceToSqr)).toList();
        for (Shot candidate : order.candidates(boss.getRandom())) {
            if (candidate == Shot.LAYERS) {
                layers = GiantSkeletonBowLayers.find(level, boss, targets);
                if (layers == null) continue;
                selected = Shot.LAYERS;
                layerPhase = LayersPhase.LOWER;
                order.used(candidate);
                startLayerDraw(boss, layers.lower(), true, now + DRAW_TICKS);
                boss.holdAttackPortals(level);
                return;
            }
            for (ServerPlayer player : targets) {
                Aim opening = solution(level, boss, player, candidate);
                if (opening == null) continue;
                selected = candidate;
                targetId = player.getUUID();
                aim = opening;
                fireAt = now + DRAW_TICKS;
                nextAimCheck = now + 3;
                order.used(candidate);
                boss.getNavigation().stop();
                boss.setAggressive(true);
                boss.startUsingItem(InteractionHand.MAIN_HAND);
                boss.setBowPose(candidate);
                face(boss, opening.facing());
                boss.holdAttackPortals(level);
                return;
            }
        }
        readyAfter(now, 4);
        fighters.stream().min(Comparator.comparingDouble(boss::distanceToSqr))
            .ifPresent(player -> boss.getNavigation().moveTo(player, .85));
    }

    private void tickLayers(ServerLevel level, SourceGiantSkeleton boss, List<ServerPlayer> fighters, long now) {
        boolean lower = layerPhase == LayersPhase.LOWER;
        var lane = lower ? layers.lower() : layers.upper();
        if (fighters.stream().noneMatch(player -> lane.player().equals(player.getUUID()))) {
            finish(level, boss, now + 7);
            return;
        }
        if (layerPhase == LayersPhase.GAP && now < raiseUpperAt) return;
        if (layerPhase == LayersPhase.GAP) {
            if (lane.flight(level, boss, fighters, false) == null) {
                finish(level, boss, now + 20);
                return;
            }
            layerPhase = LayersPhase.UPPER;
            startLayerDraw(boss, lane, false, now + UPPER_DRAW_TICKS);
            return;
        }
        if (!boss.isUsingItem()) {
            finish(level, boss, now + 7);
            return;
        }

        face(boss, lane.feet().add(0, lower ? .6 : 1.05, 0).subtract(boss.getEyePosition()));
        if (now < fireAt) return;
        var flight = lane.flight(level, boss, fighters, lower);
        if (flight == null) {
            finish(level, boss, now + 20);
            return;
        }
        var arrow = newArrow(level, boss);
        pointArrow(arrow, flight);
        if (!launch(level, boss, arrow) || !lower) {
            finish(level, boss, now + 24);
            return;
        }
        layerPhase = LayersPhase.GAP;
        raiseUpperAt = now + Math.max(MIN_LAYER_GAP_TICKS, flight.ticks() + 4);
        boss.stopUsingItem();
        boss.setAggressive(false);
        boss.setBowPose(null);
    }

    private void startLayerDraw(SourceGiantSkeleton boss, GiantSkeletonBowLayers.Lane lane, boolean lower, long fire) {
        fireAt = fire;
        boss.getNavigation().stop();
        boss.setAggressive(true);
        boss.startUsingItem(InteractionHand.MAIN_HAND);
        boss.setBowPose(lower ? Shot.LOW : Shot.DIRECT);
        face(boss, lane.feet().add(0, lower ? .6 : 1.05, 0).subtract(boss.getEyePosition()));
    }

    private static Aim solution(ServerLevel level, SourceGiantSkeleton boss, ServerPlayer player, Shot kind) {
        Vec3 normal = boss.getEyePosition().add(0, -.1, 0);
        Vec3 middle = player.getBoundingBox().getCenter();
        Vec3 direction = middle.subtract(normal);
        if (kind == Shot.DIRECT) return boss.hasLineOfSight(player) ? new Aim(direction, null) : null;

        double distance = direction.horizontalDistance();
        Vec3 forward = BossAttackGeometry.flatDirection(direction);
        GiantArrowTrajectory.Flight flight;
        if (kind == Shot.LOB) {
            if (distance < 8 || distance > 18 || player.getY() - boss.getY() > 3 || boss.hasLineOfSight(player)) return null;
            flight = GiantArrowTrajectory.lob(level, boss, normal, middle);
        } else {
            if (distance < 4 || distance > 11 || !boss.onGround() || !player.onGround()
                    || Math.abs(player.getY() - boss.getY()) > .45) return null;
            Vec3 lowOrigin = boss.position().add(forward.scale(.65)).add(0, .59, 0);
            Vec3 lowTarget = player.position().add(0, .48, 0);
            flight = GiantArrowTrajectory.low(level, boss, lowOrigin, lowTarget);
        }
        if (flight == null) return null;
        // The low arrow starts below the bowman's eyes and the giant must lower its weapon accordingly.
        Vec3 facing = kind == Shot.LOW ? flight.landing().subtract(normal) : flight.initial();
        return new Aim(facing, flight);
    }

    private void fire(ServerLevel level, SourceGiantSkeleton boss) {
        var arrow = newArrow(level, boss);
        if (selected == Shot.DIRECT) {
            Vec3 direction = aim.facing();
            Vec3 launch = direction.add(0, direction.horizontalDistance() * .11, 0);
            face(boss, launch);
            arrow.shoot(launch.x, launch.y, launch.z, 1.85f, 1);
        } else {
            pointArrow(arrow, aim.flight());
        }
        launch(level, boss, arrow);
    }

    private static Arrow newArrow(ServerLevel level, SourceGiantSkeleton boss) {
        var arrow = new Arrow(level, boss);
        arrow.getPersistentData().putBoolean(SourceGiantSkeleton.ENCOUNTER_ARROW, true);
        arrow.setBaseDamage(BASE_DAMAGE);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }

    private boolean launch(ServerLevel level, SourceGiantSkeleton boss, Arrow arrow) {
        if (!level.addFreshEntity(arrow)) return false;
        shots.add(arrow);
        flying.add(arrow);
        level.playSound(null, boss.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.75f, .64f);
        return true;
    }

    private static void pointArrow(Arrow arrow, GiantArrowTrajectory.Flight flight) {
        Vec3 origin = flight.origin();
        Vec3 velocity = flight.initial();
        arrow.setPos(origin.x, origin.y, origin.z);
        arrow.setDeltaMovement(velocity);
        float yaw = (float)Math.toDegrees(Math.atan2(velocity.x, velocity.z));
        float pitch = (float)Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance()));
        arrow.setYRot(yaw);
        arrow.setXRot(pitch);
        arrow.yRotO = yaw;
        arrow.xRotO = pitch;
    }

    private static void face(SourceGiantSkeleton boss, Vec3 direction) {
        float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float)-Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()));
        boss.setYRot(yaw);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;
        boss.setXRot(pitch);
        Vec3 eye = boss.getEyePosition();
        boss.getLookControl().setLookAt(eye.x + direction.x, eye.y + direction.y, eye.z + direction.z, 35, 35);
    }

    private void finish(ServerLevel level, SourceGiantSkeleton boss, long cooldownUntil) {
        selected = null;
        layers = null;
        layerPhase = null;
        targetId = null;
        aim = null;
        nextDraw = cooldownUntil;
        boss.stopUsingItem();
        boss.setAggressive(false);
        boss.setBowPose(null);
        refreshPortals(level, boss);
    }
}
