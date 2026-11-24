package dev.koikaster.mite;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class WitherGroundWave {
    private static final int RAYS = 12;
    static final float DAMAGE = 15;
    private static final int SEARCH_TICKS = 10;
    private static final int TOTAL_TICKS = 27;
    private static final double SPEED = .74;
    private static final SimpleParticleType WAVE = ParticleTypes.SOUL_FIRE_FLAME;

    private final Vec3 impact;
    private final Vec3[] directions = new Vec3[RAYS];
    private final Vec3[] tips = new Vec3[RAYS];
    private final boolean[] ended = new boolean[RAYS];
    private final Set<UUID> hit = new HashSet<>();
    private int age;

    WitherGroundWave(Vec3 impact, float angle) {
        this.impact = impact;
        for (int i = 0; i < RAYS; ++i) directions[i] = BossAttackGeometry.flatDirection(angle + i * Math.PI * 2 / RAYS).scale(SPEED);
    }

    boolean step(ServerLevel level, SourceWitherBoss boss, List<ServerPlayer> players) {
        if (++age > TOTAL_TICKS) return false;
        boolean active = false;
        var contacts = new ArrayList<Vec3>(RAYS);
        for (int i = 0; i < RAYS; ++i) {
            if (ended[i]) continue;
            Vec3 previous = tips[i];
            Vec3 next = previous == null ? impact.add(directions[i].scale(age)) : previous.add(directions[i]);
            Vec3 support = surface(level, boss, next.add(0, .75, 0), previous == null ? 2.15 : 1.55);
            if (support == null || !BossAttackGeometry.visible(level, boss,
                    (previous == null ? impact : previous).add(0, .73, 0), support.add(0, .73, 0))) {
                ended[i] = previous != null || age == SEARCH_TICKS;
                active |= !ended[i];
                continue;
            }
            active = true;
            tips[i] = support;
            contacts.add(support);
            BossAttackGeometry.point(level, WAVE, support.add(0, .2, 0));
            AABB contact = new AABB(support.x - .77, support.y - .14, support.z - .77,
                support.x + .77, support.y + .74, support.z + .77);
            for (var player : players) {
                UUID target = player.getUUID();
                if (player.getBoundingBox().intersects(contact) && !hit.contains(target)
                        && player.hurt(level.damageSources().mobAttack(boss), DAMAGE)) {
                    hit.add(target);
                }
            }
        }
        BossCombatVisual.ground(level, boss, contacts);
        return active;
    }

    static Vec3 surface(ServerLevel level, SourceWitherBoss boss, Vec3 from, double depth) {
        var hit = level.clip(new ClipContext(from, from.add(0, -depth, 0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, boss));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP
                || !level.getFluidState(hit.getBlockPos()).isEmpty()) return null;
        return hit.getLocation();
    }
}
