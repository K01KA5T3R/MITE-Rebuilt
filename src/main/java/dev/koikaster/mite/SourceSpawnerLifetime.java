package dev.koikaster.mite;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** R195 ask, api and og: a fixed block stops after fifteen attributed deaths. */
public final class SourceSpawnerLifetime {
    private static final String KILLS = "MiteR195SpawnerKills";
    private static final String ORIGIN = "MiteR195FixedSpawnerOrigin";
    private static final int MAX_KILLS = 15;
    private static final int CREDIT_TICKS = 100;
    private static final Map<Mob, Integer> recentCredit = new WeakHashMap<>();

    public static boolean exhausted(BaseSpawner spawner, Level level, BlockPos pos) {
        var fixed = fixedSpawner(spawner, level, pos);
        return fixed != null && killCount(fixed) >= MAX_KILLS;
    }

    public static boolean admit(ServerLevel level, Entity entity, BaseSpawner spawner, BlockPos pos) {
        boolean accepted = level.tryAddFreshEntityWithPassengers(entity);
        // Forge can reject insertion after the native duplicate-UUID check succeeds.
        if (accepted && entity instanceof Mob mob && level.getEntity(mob.getUUID()) == mob) {
            var data = mob.getPersistentData();
            data.remove(ORIGIN);
            var fixed = fixedSpawner(spawner, level, pos);
            if (fixed != null) {
                var origin = new CompoundTag();
                origin.putString("Dimension", level.dimension().location().toString());
                origin.putLong("Position", pos.asLong());
                data.put(ORIGIN, origin);
            }
        }
        return accepted;
    }

    public static void recordHurt(LivingEntity target, DamageSource source) {
        if (!(target instanceof Mob mob) || !(mob.level() instanceof ServerLevel)
                || !mob.getPersistentData().contains(ORIGIN, Tag.TAG_COMPOUND)) return;

        Entity attacker = source.getEntity();
        if (attacker instanceof Player) {
            boolean snowball = source.getDirectEntity() instanceof Snowball || SourceSnowballTraits.proper(source);
            if (snowball && !SourceSnowballTraits.playerDamage(mob)) return;
        } else if (!(attacker instanceof Wolf wolf) || !wolf.isTame()) {
            return;
        }

        recentCredit.put(mob, CREDIT_TICKS);
    }

    public static void tickCredit(LivingEntity target) {
        if (!(target instanceof Mob mob) || !(mob.level() instanceof ServerLevel)) return;
        recentCredit.computeIfPresent(mob, (key, ticks) -> ticks > 1 ? ticks - 1 : null);
    }

    public static void committedDeath(LivingEntity target) {
        if (!(target instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;

        Integer credit = recentCredit.remove(mob);
        var data = mob.getPersistentData();
        if (!data.contains(ORIGIN, Tag.TAG_COMPOUND)) return;

        var origin = data.getCompound(ORIGIN);
        data.remove(ORIGIN);
        if (credit == null || !origin.contains("Position", Tag.TAG_LONG)
                || !level.dimension().location().toString().equals(origin.getString("Dimension"))) return;

        var pos = BlockPos.of(origin.getLong("Position"));
        if (!Level.isInSpawnableBounds(pos) || level.isOutsideBuildHeight(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity fixed)
                || fixed.isRemoved() || !fixed.getBlockState().is(Blocks.SPAWNER)) return;

        int killed = killCount(fixed);
        if (killed >= MAX_KILLS) return;
        int next = killed + 1;
        fixed.getPersistentData().putInt(KILLS, next);
        fixed.setChanged();
        if (next == MAX_KILLS) {
            var state = fixed.getBlockState();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static SpawnerBlockEntity fixedSpawner(BaseSpawner spawner, Level level, BlockPos pos) {
        if (spawner.getSpawnerBlockEntity() instanceof SpawnerBlockEntity block && block.getLevel() == level
                && !block.isRemoved() && block.getBlockPos().equals(pos) && block.getBlockState().is(Blocks.SPAWNER)) {
            return block;
        }
        return null;
    }

    private static int killCount(SpawnerBlockEntity fixed) {
        return Math.max(0, fixed.getPersistentData().getInt(KILLS));
    }

    private SourceSpawnerLifetime() {}
}
