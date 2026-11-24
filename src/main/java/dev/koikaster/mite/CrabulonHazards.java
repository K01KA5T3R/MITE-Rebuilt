package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

final class CrabulonHazards {
    private static final int MAX_SPORES = 28;
    private static final double[] FAN_FIRST = {-.15, .15, -.30, .30, -.45, .45};
    private static final double[] FAN_SECOND = {-.075, .075, -.225, .225};

    private final List<Cloud> clouds = new ArrayList<>();
    private final List<CrabulonSpore> spores = new ArrayList<>();
    private final List<CrabulonBrood> broods = new ArrayList<>();

    boolean dangerous() {
        return !clouds.isEmpty() || !spores.isEmpty() || !broods.isEmpty();
    }

    private boolean hasCloudSpace() { return clouds.size() < 3; }

    void tick(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players) {
        clouds.removeIf(cloud -> cloud.tick(level, boss, players));
        spores.removeIf(CrabulonSpore::isRemoved);
        broods.removeIf(brood -> !brood.isAlive());
    }

    void clear() {
        clouds.clear();
        spores.forEach(CrabulonSpore::discard);
        spores.clear();
        broods.forEach(CrabulonBrood::discard);
        broods.clear();
    }

    void volley(ServerLevel level, SourceCrabulon boss, Vec3 aim, Vec3 direction, boolean followUp) {
        Vec3 origin = boss.position().add(0, boss.getBbHeight() * .78, 0);
        Vec3 side = new Vec3(-direction.z, 0, direction.x);
        if (followUp) {
            for (double lane : new double[]{-.6, .6, -1.7, 1.7}) {
                Vec3 point = aim.add(side.scale(lane * 1.85)).add(direction.scale(.75 + Math.abs(lane) * .45));
                lob(level, boss, origin, point, 14.5 + Math.abs(lane) * 1.6);
            }
        } else {
            lob(level, boss, origin, aim, 13.5);
            int lanes = boss.getHealth() < boss.getMaxHealth() * .36f ? 3 : 2;
            for (int lane = 1; lane <= lanes; ++lane) {
                Vec3 row = aim.add(direction.scale(lane * .42));
                Vec3 offset = side.scale(lane * 1.85);
                lob(level, boss, origin, row.add(offset), 13.5 + lane * 1.7);
                lob(level, boss, origin, row.subtract(offset), 13.5 + lane * 1.7);
            }
        }
        spraySound(level, boss, followUp ? .89f : 1.04f);
    }

    boolean fan(ServerLevel level, SourceCrabulon boss, CrabulonFanShot shot, boolean followUp) {
        if (!followUp && !spawn(level, boss, shot.origin(), shot.direction().scale(CrabulonFanShot.SPEED), true)) return false;
        boolean spawned = !followUp;
        for (double angle : followUp ? FAN_SECOND : FAN_FIRST) {
            Vec3 direction = BossAttackGeometry.spread(shot.direction(), angle);
            spawned |= spawn(level, boss, shot.origin(), direction.scale(CrabulonFanShot.SPEED), true);
        }
        if (spawned) spraySound(level, boss, followUp ? 1.15f : 1.37f);
        return spawned;
    }

    boolean canCloud(ServerLevel level, SourceCrabulon boss, Vec3 aim) {
        return hasCloudSpace() && cloudFloor(level, boss, aim) != null;
    }

    private static Vec3 cloudFloor(ServerLevel level, SourceCrabulon boss, Vec3 aim) {
        Vec3 floor = BossAttackGeometry.ground(level, boss, aim.add(0, 1.2, 0), 3);
        if (floor == null || !BossAttackGeometry.visible(level, boss, boss.getEyePosition(), floor.add(0, .72, 0))) return null;
        return floor;
    }

    boolean cloud(ServerLevel level, SourceCrabulon boss, Vec3 aim, double radius, int duration) {
        Vec3 floor = cloudFloor(level, boss, aim);
        if (floor == null || !cloudAt(level, boss, floor, radius, duration)) return false;
        spraySound(level, boss, .76f);
        return true;
    }

    boolean cloudAt(ServerLevel level, SourceCrabulon boss, Vec3 floor, double radius, int duration) {
        if (!hasCloudSpace()) return false;
        clouds.add(new Cloud(floor.add(0, .19, 0), radius, duration));
        level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, floor.x, floor.y + .8, floor.z,
            19, radius * .46, .12, radius * .46, .05);
        return true;
    }

    boolean canBrood(ServerLevel level, SourceCrabulon boss, Vec3 aim, Vec3 heading) {
        return !broodPositions(level, boss, aim, heading).isEmpty();
    }

    private List<Vec3> broodPositions(ServerLevel level, SourceCrabulon boss, Vec3 aim, Vec3 heading) {
        if (!broods.isEmpty()) return List.of();
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        int count = boss.partySize() == 1 ? 2 : boss.partySize() < 4 ? 3 : 4;
        var positions = new ArrayList<Vec3>(count);
        var dimensions = CrabulonContent.BROOD.get().getDimensions();
        for (int i = 0; i < count; ++i) {
            Vec3 position = aim.add(side.scale((i - (count - 1) * .5) * 2.35)).add(heading.scale(i % 2 == 0 ? 1 : -1.2));
            Vec3 floor = BossAttackGeometry.ground(level, boss, position.add(0, 1.45, 0), 4);
            if (floor == null || !BossAttackGeometry.visible(level, boss, boss.getEyePosition(), floor.add(0, 1, 0))) continue;
            Vec3 top = BossAttackGeometry.shorten(level, boss, floor.add(0, .35, 0), floor.add(0, 6, 0));
            if (top.y - floor.y < 2.2) continue;
            Vec3 spawn = top.add(0, -.64, 0);
            if (level.noCollision(dimensions.makeBoundingBox(spawn))) positions.add(spawn);
        }
        return positions;
    }

    boolean brood(ServerLevel level, SourceCrabulon boss, Vec3 aim, Vec3 heading) {
        int previous = broods.size();
        for (Vec3 spawn : broodPositions(level, boss, aim, heading)) {
            CrabulonBrood seed = CrabulonContent.BROOD.get().create(level);
            if (seed == null) continue;
            seed.init(boss, spawn);
            if (level.noCollision(seed) && level.addFreshEntity(seed)) broods.add(seed);
        }
        if (broods.size() == previous) return false;
        spraySound(level, boss, .85f);
        return true;
    }

    void rainBehind(ServerLevel level, SourceCrabulon boss, Vec3 above, Vec3 heading) {
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        Vec3 origin = above.add(0, .53, 0);
        for (int i = -1; i <= 1; ++i) {
            Vec3 motion = heading.scale(-.24 - Math.abs(i) * .1).add(side.scale(i * .21)).add(0, -.17, 0);
            spawn(level, boss, origin, motion, false);
        }
    }

    private void lob(ServerLevel level, SourceCrabulon boss, Vec3 origin, Vec3 aim, double time) {
        Vec3 target = aim.add(0, .28, 0);
        Vec3 ceiling = BossAttackGeometry.shorten(level, boss, origin, origin.add(0, 3, 0));
        if (ceiling.y - origin.y < 1.15) time = Math.min(time, 11);
        Vec3 motion = target.subtract(origin).scale(1 / time).add(0, CrabulonSpore.LOB_GRAVITY * time / 2, 0);
        spawn(level, boss, origin, motion, false);
    }

    private boolean spawn(ServerLevel level, SourceCrabulon boss, Vec3 origin, Vec3 velocity, boolean fan) {
        if (spores.size() >= MAX_SPORES) return false;
        Vec3 start = origin.add((fan ? velocity.normalize() : BossAttackGeometry.flatDirection(velocity)).scale(CrabulonFanShot.OFFSET));
        if (!BossAttackGeometry.visible(level, boss, origin, start)) return false;
        CrabulonSpore projectile = CrabulonContent.SPORE.get().create(level);
        if (projectile == null) return false;
        projectile.init(boss, start, velocity, fan);
        if (!level.addFreshEntity(projectile)) return false;
        spores.add(projectile);
        return true;
    }

    private static void spraySound(ServerLevel level, SourceCrabulon boss, float pitch) {
        level.playSound(null, boss.blockPosition(), SoundEvents.SPORE_BLOSSOM_PLACE,
            SoundSource.HOSTILE, 1.6f, pitch);
    }

    private static final class Cloud {
        private final Vec3 center;
        private final double radius;
        private final int duration;
        private int age;

        private Cloud(Vec3 center, double radius, int duration) {
            this.center = center;
            this.radius = radius;
            this.duration = duration;
        }

        private boolean tick(ServerLevel level, SourceCrabulon boss, List<ServerPlayer> players) {
            if (++age > duration) return true;
            if (age % 3 == 0) {
                for (int i = 0; i < 11; ++i) {
                    Vec3 particle = center.add(BossAttackGeometry.flatDirection(i * Math.PI * 2 / 11).scale(radius * .87));
                    if (!BossAttackGeometry.visible(level, boss, center, particle)) continue;
                    level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, particle.x, particle.y + .32, particle.z,
                        3, .13, .27, .13, .02);
                }
            }
            if (age >= 9 && (age - 9) % 10 == 0) {
                for (ServerPlayer player : players) {
                    Vec3 offset = player.position().subtract(center);
                    if (Math.abs(offset.y) <= 1.45 && offset.horizontalDistanceSqr() <= radius * radius
                            && BossAttackGeometry.visible(level, boss, center, player.position().add(0, .42, 0))) {
                        player.hurt(level.damageSources().mobAttack(boss), 5.6f);
                    }
                }
            }
            return false;
        }
    }
}
