package dev.koikaster.mite;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import static java.util.EnumSet.of;

/** Short pathfinding maneuvers for the rare Wight variant. Normal target and melee goals take over afterward. */
final class TacticalWight2062 extends Goal {
    private static final int SPAWN_CHANCE = 16;
    private static final int RETRY_DELAY = 20;
    private static final int MANEUVER_COOLDOWN = 160;
    private static final int MANEUVER_LIMIT = 38;

    private final Mobs.Undead wight;
    private Path plannedPath;
    private Player target;
    private long retryAt;
    private long finishAt;
    private boolean retreat;
    private boolean retreated;

    TacticalWight2062(Mobs.Undead wight) {
        this.wight = wight;
        setFlags(of(Flag.MOVE, Flag.LOOK));
    }

    static boolean shouldPromote(Mobs.Undead wight, ServerLevelAccessor access, MobSpawnType reason) {
        if (reason != MobSpawnType.NATURAL || !(wight.level() instanceof ServerLevel level)
            || !WorldContentMode.rebuilt(level) || level.getDifficulty() == Difficulty.PEACEFUL
            || !Mobs.name(wight).equals("wight")) return false;
        BlockPos pos = wight.blockPosition();
        if (!eligibleEnvironment(level, pos)) return false;
        // A separate UUID roll leaves the existing spawn and equipment RNG sequence intact.
        var id = wight.getUUID();
        long mixed = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 27);
        if (Math.floorMod(mixed, SPAWN_CHANCE) != 0) return false;
        return level.getEntitiesOfClass(Mobs.Undead.class, wight.getBoundingBox().inflate(48, 24, 48),
            entity -> entity != wight && entity.isAlive() && entity.isTacticalWight()).isEmpty();
    }

    static boolean eligibleEnvironment(ServerLevel level, BlockPos pos) {
        if (level.dimension().equals(WorldEvents.UNDERWORLD)) return true;
        return level.dimension().equals(Level.OVERWORLD) && pos.getY() <= 48
            && level.getBrightness(LightLayer.SKY, pos) == 0;
    }

    @Override
    public boolean canUse() {
        if (!WorldContentMode.rebuilt(wight.level()) || !wight.isTacticalWight()
                || wight.isInWaterOrBubble() || wight.getTarget() == null) return false;
        long now = wight.level().getGameTime();
        if (now < retryAt) return false;
        retryAt = now + RETRY_DELAY;
        if (!(wight.getTarget() instanceof Player player) || !eligibleTarget(player)) return false;
        double range = wight.distanceToSqr(player);
        if (range > 144 || !wight.getSensing().hasLineOfSight(player)) return false;
        target = player;

        retreat = !retreated && range >= 2.25 && wight.getHealth() <= wight.getMaxHealth() * .3f;
        if (retreat) {
            Vec3 away = horizontalUnit(wight.position().subtract(player.position()));
            plannedPath = pathAt(wight.position().add(away.scale(4)));
            if (plannedPath == null) plannedPath = pathAt(wight.position().add(away.yRot(.6f).scale(4)));
            if (plannedPath == null) plannedPath = pathAt(wight.position().add(away.yRot(-.6f).scale(4)));
        } else if (range >= 9) {
            Monster partner = wight.level().getEntitiesOfClass(Monster.class, wight.getBoundingBox().inflate(10),
                mob -> mob != wight && mob.isAlive() && mob.getMobType() == MobType.UNDEAD
                    && mob.getTarget() == player && mob.distanceToSqr(player) <= 144)
                .stream().min(Comparator.comparingDouble(wight::distanceToSqr)).orElse(null);
            if (partner == null) return false;
            Vec3 line = horizontalUnit(partner.position().subtract(player.position()));
            Vec3 left = line.yRot(1.57f).scale(3.2);
            Vec3 right = left.scale(-1);
            // Take the nearer reachable side while the partner keeps the front.
            if (wight.distanceToSqr(player.position().add(left)) > wight.distanceToSqr(player.position().add(right))) {
                Vec3 swap = left;
                left = right;
                right = swap;
            }
            plannedPath = pathAt(player.position().add(left));
            if (plannedPath == null) plannedPath = pathAt(player.position().add(right));
        }
        return plannedPath != null;
    }

    private static boolean eligibleTarget(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private static Vec3 horizontalUnit(Vec3 vector) {
        return new Vec3(vector.x, 0, vector.z).normalize();
    }

    private Path pathAt(Vec3 position) {
        if (wight.distanceToSqr(position) < 2.25) return null;
        BlockPos destination = BlockPos.containing(position);
        for (int offset : new int[]{0, -1, 1}) {
            BlockPos foot = destination.offset(0, offset, 0);
            if (!wight.level().hasChunkAt(foot) || !wight.level().getFluidState(foot).isEmpty()) continue;
            Path path = wight.getNavigation().createPath(foot, 0);
            if (path != null && path.canReach() && path.getNodeCount() <= 22) return path;
        }
        return null;
    }

    @Override
    public void start() {
        long now = wight.level().getGameTime();
        retryAt = now + MANEUVER_COOLDOWN;
        if (wight.getNavigation().moveTo(plannedPath, 1.05)) {
            finishAt = now + MANEUVER_LIMIT;
            if (retreat) retreated = true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && wight.getTarget() == target && eligibleTarget(target)
            && wight.level().getGameTime() < finishAt && !wight.getNavigation().isDone();
    }

    @Override
    public void tick() {
        wight.getLookControl().setLookAt(target, 30, 30);
    }

    @Override
    public void stop() {
        wight.getNavigation().stop();
        target = null;
        plannedPath = null;
    }
}
