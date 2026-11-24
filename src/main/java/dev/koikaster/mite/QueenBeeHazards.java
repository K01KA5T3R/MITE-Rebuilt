package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class QueenBeeHazards {
    private final List<QueenBeeShot> stingers = new ArrayList<>();
    private final List<QueenBeeShot> globules = new ArrayList<>();
    private final List<QueenBeeGuard> guards = new ArrayList<>();
    private final List<HoneyPatch> honey = new ArrayList<>();

    boolean dangerous() { return !stingers.isEmpty() || !globules.isEmpty() || !guards.isEmpty() || !honey.isEmpty(); }
    boolean honeySpace() { return honey.size() + globules.size() < 3; }
    boolean guardSpace() { return guards.isEmpty(); }

    void tick(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
        stingers.removeIf(QueenBeeShot::isRemoved);
        globules.removeIf(QueenBeeShot::isRemoved);
        guards.removeIf(guard -> !guard.isAlive());
        honey.removeIf(patch -> patch.tick(level, bee, players));
    }

    void clearStingers() {
        stingers.forEach(QueenBeeShot::discard);
        stingers.clear();
    }

    void clear() {
        clearStingers();
        globules.forEach(QueenBeeShot::discard);
        globules.clear();
        guards.forEach(QueenBeeGuard::discard);
        guards.clear();
        honey.clear();
    }

    void fan(ServerLevel level, SourceQueenBee bee, Vec3 origin, Vec3 aim, int layer, boolean poison) {
        Vec3 forward = aim.subtract(origin).normalize();
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 3; ++i) {
                float angle = side * (.20f + .112f * i) + layer * .048f;
                stinger(level, bee, origin, origin.add(BossAttackGeometry.spread(forward, angle)), .96 + layer * .06, poison, 7.5f);
            }
        }
    }

    void stinger(ServerLevel level, SourceQueenBee bee, Vec3 origin, Vec3 aim, double speed, boolean poison, float damage) {
        if (stingers.size() >= 27 || origin.distanceToSqr(aim) < .001) return;
        Vec3 velocity = aim.subtract(origin).normalize().scale(speed);
        spawn(level, bee, origin, velocity, false, poison, damage);
    }

    void rain(ServerLevel level, SourceQueenBee bee, Vec3 floor, Vec3 forward, Vec3 side, int row, boolean poison) {
        for (int i = -1; i <= 1; ++i) {
            Vec3 point = floor.add(side.scale(i * 2.0 + row * .68)).add(forward.scale(row * 2.18));
            Vec3 ground = QueenBeeArena.floor(level, bee, point.add(0, 1.8, 0), 4);
            if (ground == null) continue;
            Vec3 source = ground.add(0, 4.4 + .55 * (i + 1), 0);
            if (!BossAttackGeometry.visible(level, bee, bee.position().add(0, .45, 0), source)
                    || !BossAttackGeometry.visible(level, bee, source, ground.add(0, .15, 0))) continue;
            stinger(level, bee, source, ground.add(0, .25, 0), .66, poison, 8.3f);
            level.sendParticles(ParticleTypes.CRIT, source.x, source.y, source.z, 7, .33, .30, .33, .04);
        }
    }

    void cross(ServerLevel level, SourceQueenBee bee, Vec3 origin, Vec3 aim, Vec3 side, int layer, boolean poison) {
        for (int direction : new int[]{-1, 1}) {
            Vec3 source = origin.add(side.scale(2.8 * direction)).add(0, direction == layer ? .8 : -.5, 0);
            if (!BossAttackGeometry.visible(level, bee, origin, source)) continue;
            Vec3 target = aim.add(side.scale(direction * -.90));
            stinger(level, bee, source, target, 1.04, poison, 8.5f);
            Vec3 outer = aim.add(side.scale(direction * -2.15));
            stinger(level, bee, source, outer, .90, poison, 7.8f);
        }
    }

    void lobHoney(ServerLevel level, SourceQueenBee bee, Vec3 origin, Vec3 point) {
        if (!honeySpace()) return;
        Vec3 floor = QueenBeeArena.floor(level, bee, point.add(0, 1.8, 0), 4);
        if (floor == null) return;
        double ticks = 12;
        Vec3 velocity = floor.add(0, -.09, 0).subtract(origin).scale(1 / ticks)
            .add(0, QueenBeeShot.HONEY_GRAVITY * ticks * .5, 0);
        spawn(level, bee, origin, velocity, true, false, 0);
    }

    private void spawn(ServerLevel level, SourceQueenBee bee, Vec3 origin, Vec3 velocity, boolean sticky, boolean poison, float damage) {
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(origin))) return;
        QueenBeeShot shot = QueenBeeContent.SHOT.get().create(level);
        if (shot == null) return;
        shot.init(bee, origin, velocity, sticky, poison, damage);
        if (level.addFreshEntity(shot)) (sticky ? globules : stingers).add(shot);
    }

    void honeyAt(ServerLevel level, SourceQueenBee bee, Vec3 point, double radius) {
        if (honey.size() >= 3) return;
        Vec3 floor = QueenBeeArena.floor(level, bee, point.add(0, .35, 0), 1.6);
        if (floor == null || floor.distanceToSqr(point) > 1.3) return;
        honey.add(new HoneyPatch(floor.add(0, .09, 0), radius));
        level.playSound(null, net.minecraft.core.BlockPos.containing(point), SoundEvents.HONEY_BLOCK_FALL, SoundSource.HOSTILE, .95f, .80f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HONEY_BLOCK.defaultBlockState()),
            floor.x, floor.y + .22, floor.z, 15, radius * .43, .16, radius * .43, .045);
    }

    void guards(ServerLevel level, SourceQueenBee bee, Vec3 origin, Vec3 forward, Vec3 side, UUID target) {
        if (!guardSpace()) return;
        int count = bee.partySize() == 1 ? 2 : bee.partySize() < 4 ? 3 : 4;
        for (int i = 0; i < count; ++i) {
            Vec3 spot = origin.add(forward.scale(1.55)).add(side.scale((i - (count - 1) * .5) * 1.30))
                .add(0, i % 2 == 0 ? -.28 : .29, 0);
            if (!BossAttackGeometry.visible(level, bee, origin, spot)) continue;
            QueenBeeGuard guard = QueenBeeContent.GUARD.get().create(level);
            if (guard == null) continue;
            guard.init(bee, spot, target);
            if (level.noCollision(guard) && level.addFreshEntity(guard)) guards.add(guard);
        }
        level.playSound(null, bee.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.4f, 1.48f);
    }

    private static final class HoneyPatch {
        private final Vec3 center;
        private final double radius;
        private int age;

        private HoneyPatch(Vec3 center, double radius) {
            this.center = center;
            this.radius = radius;
        }

        private boolean tick(ServerLevel level, SourceQueenBee bee, List<ServerPlayer> players) {
            if (++age > 64) return true;
            if (age % 5 == 1) {
                for (int i = 0; i < 9; ++i) {
                    Vec3 point = center.add(BossAttackGeometry.flatDirection(i * 2 * Math.PI / 9).scale(radius * .86));
                    if (BossAttackGeometry.visible(level, bee, center, point)) {
                        level.sendParticles(ParticleTypes.LANDING_HONEY, point.x, point.y, point.z, 3, .10, .04, .10, .005);
                    }
                }
            }
            if (age % 3 == 1) {
                for (ServerPlayer player : players) {
                    Vec3 offset = player.position().subtract(center);
                    if (offset.horizontalDistanceSqr() <= radius * radius && offset.y >= -.60 && offset.y < .64
                            && BossAttackGeometry.visible(level, bee, center, player.position().add(0, .4, 0))) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 7, 1, true, false, false), bee);
                    }
                }
            }
            return false;
        }
    }
}
