package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class MythrilKnightEncounter {
    private static final String ID = "MiteMythrilKnightEncounter";
    private static final String PARTNER = "MiteMythrilKnightPartner";
    private static final String MOUNT = "MiteMythrilKnightMount";
    private static final String DEFEATED = "MiteMythrilKnightOtherDefeated";
    private static final String CONTRIBUTORS = "MiteMythrilKnightContributors";
    private static final String PAID = "MiteMythrilKnightPaid";
    private static final BossEncounterPresence.RetreatKeys ABSENCE = new BossEncounterPresence.RetreatKeys(
        "MiteMythrilKnightAbsentTicks", "MiteMythrilKnightAbsentSince");
    private static final String SUMMONED = "MiteMythrilKnightSummoned";
    private static final BossEncounterPresence.RetreatKeys MISSING = new BossEncounterPresence.RetreatKeys(
        "MiteMythrilKnightMissingTicks", "MiteMythrilKnightMissingSince");
    private static final int RETREAT_GRACE = 160;
    private static final String BOLT_OWNER = "MiteMythrilKnightBolt";
    private static final UUID MYTHRIL_ARMOR = UUID.fromString("562171a1-73cd-4f45-903a-e5232996ccbf");
    private static final double MOUNT_HEALTH = 200;
    private static final double RIDER_HEALTH = 100;
    private static final java.util.Map<ServerLevel, java.util.Map<UUID, Audience>> AUDIENCES = new WeakHashMap<>();

    private static final class Audience {
        private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.MYTHRIL_KNIGHT);
        private final Set<UUID> introduced = new HashSet<>();
        private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.mite_reforged.mythril_knight"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

        void update(ServerLevel level, UUID id, LivingEntity actor, List<ServerPlayer> fighters) {
            music.sync(level, id, fighters);
            for (var player : List.copyOf(bar.getPlayers())) if (!fighters.contains(player)) bar.removePlayer(player);
            for (var player : fighters) bar.addPlayer(player);
            var data = actor.getPersistentData();
            long started = data.getLong(SUMMONED);
            if (level.getGameTime() - started < 60) {
                for (var player : fighters) {
                    if (player.connection.isAcceptingMessages() && introduced.add(player.getUUID())) {
                        GroundSeaBossIntroPacket.send(player, id, started, GroundSeaBossIntroPacket.Kind.MYTHRIL_KNIGHT);
                    }
                }
            }
            double current = actor.getHealth();
            if (level.getEntity(data.getUUID(PARTNER)) instanceof LivingEntity partner && partner.isAlive()) {
                current += partner.getHealth();
            }
            bar.setProgress((float)(current / (MOUNT_HEALTH + RIDER_HEALTH)));
        }

        void stop(UUID id) {
            music.stop(id);
            bar.removeAllPlayers();
        }
    }

    private MythrilKnightEncounter() {}

    static boolean member(Entity actor) {
        return actor instanceof MythrilRavager || actor instanceof MythrilPillager;
    }

    static boolean summon(ServerLevel level, ServerPlayer player, Vec3 site) {
        if (!WorldContentMode.rebuiltBosses(level) || level.getDifficulty() == Difficulty.PEACEFUL) return false;
        var local = new net.minecraft.world.phys.AABB(site, site).inflate(64, 32, 64);
        if (!level.getEntitiesOfClass(LivingEntity.class, local, MythrilKnightEncounter::member).isEmpty()) return false;
        Ravager mount = MythrilKnightContent.MOUNT.get().create(level);
        Pillager rider = MythrilKnightContent.RIDER.get().create(level);
        if (mount == null || rider == null) return false;
        float facing = player.getYRot() + 180;
        mount.moveTo(site.x, site.y, site.z, facing, 0);
        rider.moveTo(site.x, site.y + mount.getBbHeight(), site.z, facing, 0);
        UUID encounter = mount.getUUID();
        prepare(mount, rider.getUUID(), encounter, true);
        prepare(rider, mount.getUUID(), encounter, false);
        mount.setCanJoinRaid(false);
        rider.setCanJoinRaid(false);
        mount.setCustomName(Component.translatable("entity.mite_reforged.mythril_ravager"));
        rider.setCustomName(Component.translatable("entity.mite_reforged.mythril_knight"));
        mount.setTarget(player);
        rider.setTarget(player);
        var defense = mount.getAttribute(Attributes.ARMOR);
        if (defense != null) defense.addPermanentModifier(new AttributeModifier(MYTHRIL_ARMOR,
            "Mythril barding", 10, AttributeModifier.Operation.ADDITION));
        rider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        rider.setDropChance(EquipmentSlot.MAINHAND, 0);
        armor(rider, EquipmentSlot.HEAD, "helmet");
        armor(rider, EquipmentSlot.CHEST, "chestplate");
        armor(rider, EquipmentSlot.LEGS, "leggings");
        armor(rider, EquipmentSlot.FEET, "boots");
        if (!level.addFreshEntity(mount) || !level.addFreshEntity(rider) || !rider.startRiding(mount, true)) {
            rider.discard();
            mount.discard();
            return false;
        }
        return true;
    }

    private static void prepare(LivingEntity actor, UUID partner, UUID encounter, boolean mount) {
        var tag = actor.getPersistentData();
        tag.putUUID(ID, encounter);
        tag.putUUID(PARTNER, partner);
        tag.putBoolean(MOUNT, mount);
        tag.putLong(SUMMONED, actor.level().getGameTime());
        applyHealth(actor, true);
        ((net.minecraft.world.entity.Mob) actor).setPersistenceRequired();
    }

    private static void applyHealth(LivingEntity actor, boolean fresh) {
        BossCombatTuning.applyHealth(actor, actor instanceof MythrilRavager ? MOUNT_HEALTH : RIDER_HEALTH, fresh);
    }

    private static void armor(Pillager rider, EquipmentSlot slot, String name) {
        rider.setItemSlot(slot, new ItemStack(Content.get("mithril_" + name)));
        rider.setDropChance(slot, 0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        var actor = event.getEntity();
        if (!member(actor) || !actor.getPersistentData().hasUUID(ID) || event.getAmount() <= 0 || event.isCanceled()
                || !(actor.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !isFighter(level, lead(level, actor), player)) return;
        var contributor = player.getUUID();
        var credited = contributors(actor);
        if (credited.add(contributor)) saveContributors(actor, credited);
        var partner = partner(level, actor);
        if (partner != null) {
            credited.addAll(contributors(partner));
            saveContributors(partner, credited);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        var actor = event.getEntity();
        if (!member(actor) || !actor.getPersistentData().hasUUID(ID) || event.isCanceled() || !(actor.level() instanceof ServerLevel level)) return;
        var data = actor.getPersistentData();
        BossArenaPortals.release(level, actor.getUUID());
        presence(actor).release(level, actor.getUUID());
        var partner = partner(level, actor);
        combat((Mob)actor).cancel(level, (Mob)actor, mountedRider(actor, partner));
        if (partner != null && partner.isAlive()) {
            var credited = contributors(partner);
            credited.addAll(contributors(actor));
            saveContributors(partner, credited);
            partner.getPersistentData().putBoolean(DEFEATED, true);
            return;
        }
        if (!data.getBoolean(DEFEATED) || data.getBoolean(PAID)) return;
        stopAudience(level, data.getUUID(ID));
        data.putBoolean(PAID, true);
        BossRewardDelivery.settle(level, data.getUUID(ID), contributors(actor),
            player -> isFighter(level, actor, player),
            (owner, offline) -> actor.getRandom().nextFloat() < .30f ? List.of(reward(actor)) : List.of());
    }

    @SubscribeEvent
    public static void suppressEquipmentDrops(LivingDropsEvent event) {
        if (member(event.getEntity())) event.getDrops().clear();
    }

    @SubscribeEvent
    public static void maintainFight(LivingEvent.LivingTickEvent event) {
        var actor = event.getEntity();
        if (!member(actor) || !actor.getPersistentData().hasUUID(ID) || !actor.isAlive() || !(actor.level() instanceof ServerLevel level)
                || !(actor instanceof Mob mob)) return;
        var data = actor.getPersistentData();
        long now = level.getGameTime();
        var partner = partner(level, actor);
        applyHealth(actor, false);
        if (partner != null && partner.isAlive()) applyHealth(partner, false);
        UUID id = data.getUUID(ID);
        var presence = presence(actor);
        boolean pairedFight = !data.getBoolean(DEFEATED);
        if (pairedFight && partner != null && partner.isAlive() && !data.getBoolean(MOUNT)) {
            presence.hold(level, actor);
            if (actor.getVehicle() != partner && actor.distanceToSqr(partner) <= 9) actor.startRiding(partner, true);
            if (actor.getVehicle() != partner) mob.getNavigation().moveTo(partner, 1.12);
            return;
        }

        // The mount owns the shared timer while paired. A surviving member can take it over.
        var retreat = presence.retreat();
        retreat.load(data, ABSENCE, RETREAT_GRACE);
        if (pairedFight && actor instanceof MythrilRavager && partner instanceof MythrilPillager rider && rider.isAlive()) {
            resumeFromRider(level, data, retreat, rider);
        }
        if (retreat.expired(level, RETREAT_GRACE)) {
            stopAudience(level, id);
            if (member(partner)) partner.discard();
            actor.discard();
            return;
        }
        presence.hold(level, actor);
        if (pairedFight && partner == null) {
            suspendActor(level, mob);
            stopAudience(level, id);
            retreat.save(data, ABSENCE);
            var missing = presence.missingPartner();
            missing.load(data, MISSING, RETREAT_GRACE);
            if (missing.expired(level, RETREAT_GRACE)) {
                actor.discard();
            } else {
                missing.observe(false);
                missing.save(data, MISSING);
            }
            return;
        }
        MISSING.clear(data);

        var fighters = level.getPlayers(player -> isFighter(level, actor, player));
        var target = fighters.stream().min(Comparator.comparingDouble(actor::distanceToSqr)).orElse(null);
        retreat.observe(target != null);
        retreat.save(data, ABSENCE);
        if (partner != null) {
            retreat.save(partner.getPersistentData(), ABSENCE);
            presence(partner).retreat().inheritSample(retreat);
        }
        if (target == null) {
            suspendActor(level, mob);
            if (partner instanceof Mob other) suspendActor(level, other);
            stopAudience(level, id);
            return;
        }
        if (now % 10 == 0) AUDIENCES.computeIfAbsent(level, ignored -> new java.util.HashMap<>())
            .computeIfAbsent(id, ignored -> new Audience()).update(level, id, actor, fighters);
        var controller = combat(mob);
        var accepted = chooseFighter(level, mob, controller, target, fighters);
        var mountedRider = mountedRider(actor, partner);
        if (accepted == null) {
            mob.getNavigation().stop();
            controller.cancel(level, mob, mountedRider);
            return;
        }
        if (mountedRider != null && mountedRider.getTarget() != null) mountedRider.setTarget(null);
        if (actor instanceof MythrilPillager rider && rider.isPassenger() && !rider.getVehicle().isAlive()) rider.stopRiding();
        controller.tick(level, mob, mountedRider, accepted, fighters);
    }

    private static ServerPlayer chooseFighter(ServerLevel level, Mob actor, MythrilKnightCombat controller,
                                             ServerPlayer nearest, List<ServerPlayer> fighters) {
        var before = actor.getTarget();
        var current = before instanceof ServerPlayer player && fighters.contains(player) ? player : null;
        var preferred = current == null ? nearest : current;
        if (fighters.size() > 1 && (current == null || !controller.attacking() && !actor.hasLineOfSight(current))) {
            preferred = fighters.stream().filter(actor::hasLineOfSight)
                .min(Comparator.comparingDouble(actor::distanceToSqr)).orElse(preferred);
        }
        if (before != preferred) actor.setTarget(preferred);
        return actor.getTarget() instanceof ServerPlayer accepted && fighters.contains(accepted)
            && isFighter(level, actor, accepted) ? accepted : null;
    }

    private static void resumeFromRider(ServerLevel level, CompoundTag mountData, BossEncounterPresence.RetreatTimer retreat,
                                        MythrilPillager rider) {
        var riderData = rider.getPersistentData();
        if (!MISSING.presentIn(riderData)) return;
        var riderTimer = presence(rider).retreat();
        riderTimer.load(riderData, ABSENCE, RETREAT_GRACE);
        if (!MISSING.presentIn(mountData) || riderTimer.elapsedAt(level, RETREAT_GRACE) > retreat.elapsedAt(level, RETREAT_GRACE)) {
            retreat.load(riderData, ABSENCE, RETREAT_GRACE);
            retreat.inheritSample(riderTimer);
        }
        MISSING.clear(riderData);
    }

    private static MythrilPillager mountedRider(LivingEntity actor, LivingEntity partner) {
        return actor instanceof MythrilRavager && partner instanceof MythrilPillager rider
            && rider.isAlive() && rider.getVehicle() == actor ? rider : null;
    }

    static UUID rewardDeparture(ServerLevel level, LivingEntity actor, ServerPlayer player) {
        var data = actor.getPersistentData();
        if (!data.hasUUID(ID) || data.getBoolean(PAID)) return null;
        var partner = partner(level, actor);
        boolean partnerActive = partner != null && partner.isAlive();
        if ((!data.getBoolean(DEFEATED) && !partnerActive) || !isFighter(level, lead(level, actor), player)) return null;

        UUID playerId = player.getUUID();
        if (contributors(actor).contains(playerId) || partnerActive && contributors(partner).contains(playerId)) {
            return data.getUUID(ID);
        }
        return null;
    }

    static boolean isFighter(ServerLevel level, LivingEntity actor, Entity target) {
        return target instanceof ServerPlayer player && player.serverLevel() == level && player.isAlive()
            && !player.isCreative() && !player.isSpectator() && !Incapacitation2123.incapacitated(player)
            && MythrilKnightArena.includes(actor.position(), player.position());
    }

    private static LivingEntity lead(ServerLevel level, LivingEntity actor) {
        var partner = partner(level, actor);
        return actor instanceof MythrilPillager && partner instanceof MythrilRavager && partner.isAlive() ? partner : actor;
    }

    private static LivingEntity partner(ServerLevel level, LivingEntity actor) {
        var data = actor.getPersistentData();
        if (!data.hasUUID(PARTNER) || !data.hasUUID(ID)
                || !(level.getEntity(data.getUUID(PARTNER)) instanceof LivingEntity other) || !member(other)) return null;
        var paired = other.getPersistentData();
        return paired.hasUUID(ID) && data.getUUID(ID).equals(paired.getUUID(ID))
            && paired.hasUUID(PARTNER) && actor.getUUID().equals(paired.getUUID(PARTNER)) ? other : null;
    }

    private static BossEncounterPresence presence(LivingEntity member) {
        return member instanceof MythrilRavager mount ? mount.encounterPresence : ((MythrilPillager) member).encounterPresence;
    }

    private static void suspendActor(ServerLevel level, Mob actor) {
        actor.setTarget(null);
        actor.getNavigation().stop();
        combat(actor).cancel(level, actor, mountedRider(actor, partner(level, actor)));
    }

    private static MythrilKnightCombat combat(Mob actor) {
        return actor instanceof MythrilRavager mount ? mount.combat : ((MythrilPillager)actor).combat;
    }

    @SubscribeEvent
    public static void memberRemoved(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob actor)
                || !member(actor)) return;
        var data = actor.getPersistentData();
        presence(actor).release(level, actor.getUUID());
        combat(actor).cancel(level, actor, mountedRider(actor, partner(level, actor)));
        if (!data.hasUUID(ID)) return;
        if (actor.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
            BossRewardDelivery.close(level, data.getUUID(ID));
        }
        var partner = partner(level, actor);
        if (partner != null && partner.isAlive()
                && partner.getPersistentData().getBoolean(DEFEATED)) return;
        stopAudience(level, data.getUUID(ID));
    }

    static void markBolt(net.minecraft.world.entity.projectile.Projectile bolt, MythrilPillager rider) {
        if (rider.getPersistentData().hasUUID(ID)) {
            bolt.getPersistentData().putUUID(BOLT_OWNER, rider.getPersistentData().getUUID(ID));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interceptBolt(ProjectileImpactEvent event) {
        var bolt = event.getProjectile();
        if (!(bolt.level() instanceof ServerLevel level) || !bolt.getPersistentData().hasUUID(BOLT_OWNER)
                || !(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)) return;
        UUID encounter = bolt.getPersistentData().getUUID(BOLT_OWNER);
        var result = event.getImpactResult();
        if (result != ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE
                && !boltMayHit(level, encounter, bolt.getOwner(), hit.getEntity())) {
            event.setImpactResult(result == ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT
                ? ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE
                : ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        }
    }

    private static boolean boltMayHit(ServerLevel level, UUID encounter, Entity shooter, Entity victim) {
        if (!(shooter instanceof MythrilPillager rider) || !rider.isAlive()
                || rider.level() != level || !WorldContentMode.rebuiltBosses(level)) return false;

        var data = rider.getPersistentData();
        if (!data.hasUUID(ID) || !encounter.equals(data.getUUID(ID))) return false;
        if (data.getBoolean(DEFEATED)) return isFighter(level, rider, victim);

        // A living rider waits for an unloaded partner. Only a real death gives it command of the fight.
        if (!(partner(level, rider) instanceof MythrilRavager mount) || !mount.isAlive()
                || MISSING.presentIn(data) || MISSING.presentIn(mount.getPersistentData())) return false;
        return isFighter(level, mount, victim);
    }

    @SubscribeEvent
    public static void preventBoltTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity().getPersistentData().hasUUID(BOLT_OWNER)) event.setCanceled(true);
    }

    static void face(Mob actor, Vec3 direction) {
        float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        actor.setYRot(yaw);
        actor.setYHeadRot(yaw);
        actor.setXRot((float)-Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        actor.yBodyRot = yaw;
    }

    private static void stopAudience(ServerLevel level, UUID id) {
        var active = AUDIENCES.get(level);
        if (active == null) return;
        var audience = active.remove(id);
        if (audience != null) audience.stop(id);
        if (active.isEmpty()) AUDIENCES.remove(level);
    }

    @SubscribeEvent
    public static void levelUnloaded(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var active = AUDIENCES.remove(level);
        if (active != null) active.forEach((id, audience) -> audience.stop(id));
    }

    private static ItemStack reward(LivingEntity actor) {
        List<ItemStack> choices = Content.ALL_ITEMS.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("mithril_"))
            .map(entry -> new ItemStack(entry.getValue().get()))
            .filter(ItemStack::isDamageableItem).toList();
        if (choices.isEmpty()) return ItemStack.EMPTY;
        var reward = choices.get(actor.getRandom().nextInt(choices.size()));
        int remaining = Math.max(1, (int)(reward.getMaxDamage() * (.30 + actor.getRandom().nextDouble() * .15)));
        reward.setDamageValue(reward.getMaxDamage() - remaining);
        return reward;
    }

    private static Set<UUID> contributors(LivingEntity actor) {
        Set<UUID> ids = new HashSet<>();
        var rows = actor.getPersistentData().getList(CONTRIBUTORS, Tag.TAG_COMPOUND);
        for (int i=0;i<rows.size();++i) {
            var row=rows.getCompound(i);
            if(row.hasUUID("Player")) ids.add(row.getUUID("Player"));
        }
        return ids;
    }

    private static void saveContributors(LivingEntity actor, Set<UUID> ids) {
        var list = new net.minecraft.nbt.ListTag();
        ids.stream().sorted().forEach(id -> {
            var row=new CompoundTag();
            row.putUUID("Player",id);
            list.add(row);
        });
        actor.getPersistentData().put(CONTRIBUTORS, list);
    }
}
