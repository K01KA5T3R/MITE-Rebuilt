package dev.koikaster.mite;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class BossCombatTuning {
    private static final float DAMAGE_INCREASE = 1.50f;
    private static final float RITUAL_PLAYER_DAMAGE_SCALE = .80f * DAMAGE_INCREASE;

    private BossCombatTuning() {}

    static float playerDamage(float raw) { return raw * RITUAL_PLAYER_DAMAGE_SCALE; }

    static int witherDuration(int ticks) {
        return extendDamageEffect(Math.round(ticks * .75f), 20);
    }

    static int queenPoisonDuration(int ticks) {
        return extendDamageEffect(ticks, 25);
    }

    private static int extendDamageEffect(int duration, int period) {
        // Native level-II Wither and level-I Poison each deal one point per period.
        // Round up the additional hit count so short effects receive at least 50% more.
        int oldHits = duration / period;
        return duration + (oldHits + 1) / 2 * period;
    }

    static float rescaledHealth(float health, float oldMaximum, float newMaximum) {
        if (oldMaximum <= 0 || newMaximum <= 0) return 0;
        return Mth.clamp(health / oldMaximum, 0, 1) * newMaximum;
    }

    static void applyHealth(LivingEntity boss, double expected, boolean fresh) {
        var attribute = boss.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        if (!fresh && Double.compare(attribute.getBaseValue(), expected) == 0) return;
        float previousHealth = boss.getHealth();
        float previousMaximum = boss.getMaxHealth();
        attribute.setBaseValue(expected);
        float maximum = boss.getMaxHealth();
        boss.setHealth(fresh ? maximum : rescaledHealth(previousHealth, previousMaximum, maximum));
    }

    static boolean knockbackImmune(Entity entity) {
        if (entity instanceof PartEntity<?> part) entity = part.getParent();
        return entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof ElderGuardian
                || entity instanceof SourceGiantZombieBoss giant && giant.ritual()
                || entity instanceof SourceKingSlime || entity instanceof SourceCrabulon || entity instanceof SourceQueenBee
                || entity instanceof SourceGiantSkeleton || entity instanceof SourceRedstoneGolem || MythrilKnightEncounter.member(entity);
    }

    @SubscribeEvent
    public static void protectBossKnockbackAttribute(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity boss) || !knockbackImmune(boss)) return;
        var resistance = boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null) resistance.setBaseValue(1);
    }

    @SubscribeEvent
    public static void preventBossKnockback(LivingKnockBackEvent event) {
        if (knockbackImmune(event.getEntity())) event.setCanceled(true);
    }

    public static void applyCollisionPush(Object target, double x, double y, double z, Object other) {
        Entity entity = (Entity)target;
        if (other instanceof net.minecraft.world.entity.player.Player && knockbackImmune(entity)) return;
        entity.push(x, y, z);
    }

    @SubscribeEvent
    public static void tuneEncounterDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var source = event.getSource();
        Entity owner = source.getEntity();
        if (owner instanceof Projectile projectile) owner = projectile.getOwner();
        if (owner == null && source.getDirectEntity() instanceof Projectile projectile) owner = projectile.getOwner();
        if (owner == null && source.getDirectEntity() instanceof AreaEffectCloud cloud) owner = cloud.getOwner();
        if (owner == null || owner.level() != player.serverLevel()) return;
        if (ritualOwner(player.serverLevel(), owner)) {
            event.setAmount(playerDamage(event.getAmount()));
        } else if (replicaOwner(owner)) {
            event.setAmount(event.getAmount() * DAMAGE_INCREASE);
        }
    }

    private static boolean ritualOwner(ServerLevel level, Entity owner) {
        if (owner instanceof SourceEnderDragon dragon) return DragonRitual.owns(level, dragon);
        if (owner instanceof SourceWitherBoss wither) return wither.ritual() != null;
        if (owner instanceof SourceGiantZombieBoss giant) return giant.ritual();
        return owner instanceof SourceElderGuardianBoss elder && elder.ritual();
    }

    private static boolean replicaOwner(Entity owner) {
        return owner instanceof SourceKingSlime || owner instanceof KingSlimeMinion
                || owner instanceof SourceCrabulon || owner instanceof CrabulonBrood
                || owner instanceof SourceQueenBee || owner instanceof QueenBeeGuard;
    }
}
