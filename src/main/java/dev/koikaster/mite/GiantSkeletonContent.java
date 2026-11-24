package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid=MiteReforged.ID)
final class GiantSkeletonContent {
    private static final String MASTER = "MiteGiantSkeletonMaster";
    private static final String WAVE_ARROW_MASTER = "MiteGiantSkeletonWaveArrowMaster";
    private static final String HOME_CHUNK = "MiteGiantSkeletonHomeChunk";
    static final RegistryObject<EntityType<SourceGiantSkeleton>> BOSS = Mobs.ENTITIES.register("underworld_giant_skeleton",
        () -> EntityType.Builder.of(SourceGiantSkeleton::new,MobCategory.MONSTER).sized(1.8f,5.97f)
            .clientTrackingRange(12).updateInterval(2).build(MiteReforged.ID+":underworld_giant_skeleton"));
    static final RegistryObject<Item> BONE = Content.item("large_bone",() -> new Item(new Item.Properties().stacksTo(16)));
    static final RegistryObject<Item> PICKAXE = Content.item("large_bone_pickaxe",LargeBonePickaxe::new);
    static final RegistryObject<Item> SIGIL = Content.item("undead_sigil",Sigil::new);

    private GiantSkeletonContent() {}

    static void register(IEventBus bus) { bus.addListener(GiantSkeletonContent::attributes); }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(BOSS.get(),SourceGiantSkeleton.attributes().build());
    }

    static void assign(Mob servant,SourceGiantSkeleton boss) {
        var data=servant.getPersistentData();
        data.putUUID(MASTER,boss.getUUID());
        updateHome(servant, boss);
    }

    static void updateHome(Mob servant, SourceGiantSkeleton boss) {
        var data = servant.getPersistentData();
        long home = boss.chunkPosition().toLong();
        if (data.getLong(HOME_CHUNK) != home || !data.contains(HOME_CHUNK)) data.putLong(HOME_CHUNK, home);
    }

    static boolean isServant(Mob servant) {
        return servant.getPersistentData().hasUUID(MASTER);
    }

    static boolean belongsTo(Mob servant, SourceGiantSkeleton boss) {
        var data = servant.getPersistentData();
        return data.hasUUID(MASTER) && boss.getUUID().equals(data.getUUID(MASTER));
    }

    @SubscribeEvent
    public static void preventPickaxeGrindstone(GrindstoneEvent.OnPlaceItem event) {
        if(LargeBonePickaxe.matches(event.getTopItem()) || LargeBonePickaxe.matches(event.getBottomItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventServantTravel(EntityTravelToDimensionEvent event) {
        var data = event.getEntity().getPersistentData();
        if(data.hasUUID(MASTER) || data.getBoolean(SourceGiantSkeleton.ENCOUNTER_ARROW))event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void observeWaveArrow(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide || event.isCanceled()
                || !(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof Mob archer)) return;
        var boss = activeWaveBoss(archer);
        if (boss == null) return;

        arrow.getPersistentData().putUUID(WAVE_ARROW_MASTER, boss.getUUID());
        if (archer instanceof SourceSkeleton) boss.waveArrowSpawned(arrow);
    }

    @SubscribeEvent
    public static void removeSavedEncounterArrow(EntityJoinLevelEvent event) {
        if(event.loadedFromDisk() && !event.getLevel().isClientSide && event.getEntity() instanceof Arrow arrow
                && arrow.getPersistentData().getBoolean(SourceGiantSkeleton.ENCOUNTER_ARROW))event.setCanceled(true);
    }

    @SubscribeEvent
    public static void keepWaveMember(LivingConversionEvent.Pre event) {
        if(event.getEntity().getPersistentData().hasUUID(MASTER))event.setCanceled(true);
    }

    @SubscribeEvent
    public static void trackMemberChunk(EntityEvent.EnteringSection event) {
        if (!(event.getEntity() instanceof Mob servant) || !(servant.level() instanceof ServerLevel)) return;
        var boss = owner(servant);
        if (boss != null) boss.trackWaveMember(servant.getUUID(), event.getNewPos().chunk());
    }

    @SubscribeEvent
    public static void rejectReplacedMember(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() || !(event.getEntity() instanceof Mob servant)
                || !(event.getLevel() instanceof ServerLevel)) return;
        var boss = owner(servant);
        if (boss != null && !boss.includesWaveMember(servant.getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restrictServantTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob servant) || !(servant.level() instanceof ServerLevel)
                || !servant.isAddedToWorld() || !servant.getPersistentData().hasUUID(MASTER)
                || !(event.getNewTarget() instanceof ServerPlayer candidate)) return;
        var boss = activeWaveBoss(servant);
        if (waveFighter(boss, candidate)) return;

        // Canceling the event would retain even an invalid previous target.
        var previous = servant.getTarget();
        if (previous instanceof ServerPlayer player && !waveFighter(boss, player)) {
            previous = null;
            servant.getNavigation().stop();
        }
        event.setNewTarget(previous);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void skipOutOfArenaImpact(ProjectileImpactEvent event) {
        var result = event.getImpactResult();
        if (result == ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE) return;
        if (event.getProjectile() instanceof AbstractArrow arrow && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ServerPlayer player && rejectsWaveArrow(arrow, player)) {
            event.setImpactResult(result == ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT
                ? ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE
                : ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        }
    }

    @SubscribeEvent
    public static void preventOutOfArenaAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var source = event.getSource();
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            if (rejectsWaveArrow(arrow, player)) event.setCanceled(true);
        } else if (source.getEntity() instanceof Mob servant && rejectsServantAttack(servant, player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cleanOrphan(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob servant) || !(servant.level() instanceof ServerLevel level)) return;
        var data = servant.getPersistentData();
        if (!data.hasUUID(MASTER)) return;
        var boss = owner(servant);
        if (servant.getTarget() instanceof ServerPlayer player && !waveFighter(activeWaveBoss(servant, boss), player)) {
            servant.setTarget(null);
            servant.getNavigation().stop();
        }
        if (boss != null) {
            if (!boss.includesWaveMember(servant.getUUID()) && servant.isAlive()) servant.discard();
            return;
        }
        if (servant.tickCount < 80 || servant.tickCount % 40 != 0 || !data.contains(HOME_CHUNK)) return;
        long home = data.getLong(HOME_CHUNK);
        var at = new net.minecraft.world.level.ChunkPos(home);
        var homePosition = new BlockPos(at.getMiddleBlockX(), servant.getBlockY(), at.getMiddleBlockZ());
        if (level.areEntitiesLoaded(home) && level.isPositionEntityTicking(homePosition)) servant.discard();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (event.getAmount() <= 0 || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var boss = owner(event.getEntity());
        if (boss != null && boss.includesWaveMember(event.getEntity().getUUID())) boss.credit(player);
    }

    @SubscribeEvent
    public static void memberDied(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Mob servant) || servant.getRemovalReason() != Entity.RemovalReason.KILLED
                || servant.getPose() != Pose.DYING) return;
        var boss = owner(servant);
        if (boss != null) boss.defeated(servant.getUUID());
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if(event.getEntity().getPersistentData().hasUUID(MASTER))event.getDrops().clear();
    }

    private static boolean rejectsWaveArrow(AbstractArrow arrow, ServerPlayer player) {
        if (!(arrow.level() instanceof ServerLevel level)) return false;
        var data = arrow.getPersistentData();
        if (data.hasUUID(WAVE_ARROW_MASTER)) {
            // A registered shot keeps its encounter even if its original archer dies or unloads.
            return !waveFighter(findBoss(level, data.getUUID(WAVE_ARROW_MASTER)), player);
        }
        // Pre-upgrade arrows still identify their encounter while their original archer is resolvable.
        return arrow.getOwner() instanceof Mob servant && rejectsServantAttack(servant, player);
    }

    private static boolean rejectsServantAttack(Mob servant, ServerPlayer player) {
        return servant.getPersistentData().hasUUID(MASTER) && !waveFighter(activeWaveBoss(servant), player);
    }

    private static boolean waveFighter(SourceGiantSkeleton boss, ServerPlayer player) {
        return boss != null && boss.isAlive() && boss.sealed() && player.level() == boss.level() && boss.fighter(player);
    }

    private static SourceGiantSkeleton activeWaveBoss(Mob servant) {
        return activeWaveBoss(servant, owner(servant));
    }

    private static SourceGiantSkeleton activeWaveBoss(Mob servant, SourceGiantSkeleton boss) {
        return boss != null && boss.isAlive() && boss.sealed() && boss.includesWaveMember(servant.getUUID()) ? boss : null;
    }

    private static SourceGiantSkeleton owner(LivingEntity member) {
        var tag = member.getPersistentData();
        if (!(member.level() instanceof ServerLevel level) || !tag.hasUUID(MASTER)) return null;
        return findBoss(level, tag.getUUID(MASTER));
    }

    private static SourceGiantSkeleton findBoss(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof SourceGiantSkeleton boss ? boss : null;
    }

    private static final class Sigil extends Item {
        Sigil() { super(new Properties().stacksTo(16)); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
            var stack=player.getItemInHand(hand);
            if(level.dimension()!=WorldEvents.UNDERWORLD || player.isSpectator())return InteractionResultHolder.pass(stack);
            if(level instanceof ServerLevel server && player instanceof ServerPlayer owner) {
                var site=GiantSkeletonArena.summonSite(server,owner);
                if(site==null || !SourceGiantSkeleton.summon(server,owner,site))return InteractionResultHolder.fail(stack);
                if(!player.getAbilities().instabuild)stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
    }
}
