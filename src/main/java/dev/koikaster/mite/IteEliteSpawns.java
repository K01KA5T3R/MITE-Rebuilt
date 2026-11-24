package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Optional variants of the two existing, naturally spawned source mobs. */
final class IteEliteSpawns {
    interface Host {
        boolean iteElite();
        void iteElite(boolean elite);
    }

    private static final String SAVED_ELITE = "MiteIteElite";
    private static final UUID HEALTH = UUID.fromString("2ef4aef3-e967-4155-95a0-ad9a1b89492f");
    private static final UUID ATTACK = UUID.fromString("147fbf66-d55f-4f44-ba36-609960442344");
    private static final int RARITY = 48;
    static final int EXPERIENCE = 24;

    private IteEliteSpawns() {}

    static boolean isElite(Entity entity) {
        return entity instanceof Host host && host.iteElite();
    }

    static int firstEligibleDay(ServerLevel level, Mob actor) {
        if (actor.isBaby() || actor.isSpawnCancelled() || level.getDifficulty() == Difficulty.PEACEFUL) return 0;
        if (actor instanceof SourceZombie zombie && actor.getType() == EntityType.ZOMBIE) {
            if (level.dimension() != Level.OVERWORLD || SourceMoonCycle.daytime(level.getDayTime())
                    || zombie.sourceVillager() || zombie.sourceConverting() || zombie.isUnderWaterConverting()) return 0;
        } else if (!(actor instanceof SourcePigman) || actor.getType() != EntityType.ZOMBIFIED_PIGLIN
                || level.dimension() != Level.NETHER) {
            return 0;
        }
        return IteBloodMoonProgress.firstEligibleDay(level);
    }

    static boolean tryMark(ServerLevel level, Mob actor, int firstDay) {
        // The body's existing UUID makes this roll independent of the natural spawner's RNG.
        if (firstDay <= 0 || !winsRoll(actor.getUUID())) return false;
        var neighborhood = actor.getBoundingBox().inflate(48.0, 24.0, 48.0);
        if (!level.getEntitiesOfClass(Mob.class, neighborhood,
                other -> other.getType() == actor.getType() && other.isAlive() && isElite(other)).isEmpty()) {
            return false;
        }
        var lastDay = IteBloodMoonProgress.GROWTH_LIMIT_DAY;
        long primaryTime = Math.max(0L, Math.min(level.getServer().overworld().getDayTime(), 24000L * lastDay));
        double growth = growthFraction(firstDay, SourceMoonCycle.dayOfWorld(primaryTime));
        boolean pigman = actor instanceof SourcePigman;
        raiseTo(actor.getAttribute(Attributes.MAX_HEALTH), HEALTH, (pigman ? 40 : 36) + growth * (pigman ? 20 : 22));
        raiseTo(actor.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK, (pigman ? 9 : 6.5) + growth * (pigman ? 1.5 : 3));
        actor.setHealth(actor.getMaxHealth());
        actor.setCanPickUpLoot(false);
        ((Host) actor).iteElite(true);
        return true;
    }

    static boolean winsRoll(UUID id) {
        return Math.floorMod(id.hashCode(), RARITY) == 0;
    }

    static double growthFraction(int firstDay, int currentDay) {
        int end = IteBloodMoonProgress.GROWTH_LIMIT_DAY;
        if (firstDay >= end) return currentDay >= end ? 1.0 : 0.0;
        return (double) (Math.max(firstDay, Math.min(end, currentDay)) - firstDay) / (end - firstDay);
    }

    private static void raiseTo(AttributeInstance attribute, UUID id, double target) {
        double original = attribute.getValue();
        double modifier = Math.max(original, target) / original - 1.0;
        attribute.addPermanentModifier(new AttributeModifier(id, "ITE lord", modifier, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    static void save(Host host, CompoundTag tag) {
        if (host.iteElite()) tag.putBoolean(SAVED_ELITE, true);
    }

    static void restore(Mob actor, Host host, CompoundTag tag) {
        var health = actor.getAttribute(Attributes.MAX_HEALTH);
        var attack = actor.getAttribute(Attributes.ATTACK_DAMAGE);
        boolean hasHealth = health.getModifier(HEALTH) != null;
        boolean hasAttack = attack.getModifier(ATTACK) != null;
        if (!tag.getBoolean(SAVED_ELITE) && !hasHealth && !hasAttack) return;
        boolean valid = tag.getBoolean(SAVED_ELITE) && hasHealth && hasAttack && WorldContentMode.ite(actor.level());
        host.iteElite(valid);
        if (valid) {
            actor.setCanPickUpLoot(false);
        } else {
            if (hasHealth) health.removeModifier(HEALTH);
            if (hasAttack) attack.removeModifier(ATTACK);
            if (actor.getHealth() > actor.getMaxHealth()) actor.setHealth(actor.getMaxHealth());
        }
    }

    static void dropConsumables(Mob actor, boolean creditedToPlayer) {
        if (!creditedToPlayer) return;
        Item reward = actor instanceof SourcePigman ? Items.COOKED_PORKCHOP : Items.TORCH;
        actor.spawnAtLocation(new ItemStack(reward, 1 + actor.getRandom().nextInt(2)));
    }
}
