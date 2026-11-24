package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SourceCrabulon extends Monster {
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(SourceCrabulon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(SourceCrabulon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_LENGTH = SynchedEntityData.defineId(SourceCrabulon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_TIME = SynchedEntityData.defineId(SourceCrabulon.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> ENTRANCE_AGE = SynchedEntityData.defineId(SourceCrabulon.class, EntityDataSerializers.INT);
    static final float HEALTH = 876;
    private static final float LEGACY_HEALTH = 680;
    static final double ARENA_RADIUS = 30;
    static final double ARENA_HEIGHT = 10;
    private static final int INTRO_TICKS = 34;
    private static final int ENCOUNTER_TICKET_DISTANCE = 2;
    private static final TicketType<UUID> ENCOUNTER_TICKET =
        TicketType.create("mite_crabulon_encounter", Comparator.<UUID>naturalOrder(), 40);

    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final CrabulonCombat combat = new CrabulonCombat();
    private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.CRABULON);
    private final Map<UUID, ServerPlayer> introViewers = new HashMap<>();
    private int initialParty = 1;
    private int missingPlayers;
    private int introLeft = INTRO_TICKS;
    private int clientEntranceSampleTick;
    private UUID lastSelectedTarget;
    private ChunkPos encounterChunk;

    SourceCrabulon(EntityType<? extends SourceCrabulon> type, Level level) {
        super(type, level);
        xpReward = 25;
    }

    static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.MOVEMENT_SPEED, .27)
            .add(Attributes.KNOCKBACK_RESISTANCE, .9).add(Attributes.FOLLOW_RANGE, ARENA_RADIUS);
    }

    static boolean summon(ServerLevel level, ServerPlayer player) {
        if (!WorldContentMode.calamityBosses(level) || level.getDifficulty() == Difficulty.PEACEFUL || Incapacitation2123.incapacitated(player)) return false;
        Vec3 site = CrabulonArena.summonSite(level, player);
        if (site == null) return false;
        var area = new AABB(site, site).inflate(ARENA_RADIUS * 2, ARENA_HEIGHT * 2, ARENA_RADIUS * 2);
        if (!level.getEntitiesOfClass(SourceCrabulon.class, area, SourceCrabulon::isAlive).isEmpty()) return false;
        SourceCrabulon boss = CrabulonContent.CRABULON.get().create(level);
        if (boss == null) return false;
        boss.initialParty = Mth.clamp(level.getPlayers(viewer -> eligible(viewer, site)).size(), 1, 8);
        boss.configureHealth();
        boss.setHealth(boss.getMaxHealth());
        boss.setPersistenceRequired();
        boss.moveTo(site.x, site.y, site.z, player.getYRot() + 180, 0);
        boss.pose(null, CrabulonCombat.Action.INTRO, INTRO_TICKS);
        boss.entityData.set(ENTRANCE_AGE, 0);
        if (!level.noCollision(boss) || !level.addFreshEntity(boss)) return false;
        boss.updateAudience(level);
        level.playSound(null, boss.blockPosition(), SoundEvents.MUD_BREAK, SoundSource.HOSTILE, 1.8f, .55f);
        level.sendParticles(ParticleTypes.MYCELIUM, site.x, site.y + .55, site.z, 38, 1.05, .35, 1.05, .09);
        return true;
    }

    private void configureHealth() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(partyHealth(HEALTH));
    }

    private double partyHealth(float base) {
        double bonus = initialParty <= 1 ? 0 : .30 + .03 * (initialParty - 2);
        return base * (1 + bonus);
    }

    int partySize() {
        return initialParty;
    }

    @Override
    protected void registerGoals() {
        // CrabulonCombat owns movement and contact timing.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK, 0);
        entityData.define(ACTION, CrabulonCombat.Action.INTRO.ordinal());
        entityData.define(ACTION_LENGTH, INTRO_TICKS);
        entityData.define(ACTION_TIME, 0L);
        entityData.define(ENTRANCE_AGE, -1);
    }

    void pose(CrabulonCombat.Move attack, CrabulonCombat.Action action, int length) {
        entityData.set(ATTACK, attack == null ? 0 : attack.ordinal() + 1);
        entityData.set(ACTION, action.ordinal());
        entityData.set(ACTION_LENGTH, length);
        entityData.set(ACTION_TIME, level().getGameTime());
    }

    CrabulonCombat.Move attackPose() {
        int id = entityData.get(ATTACK);
        return id == 0 ? null : CrabulonCombat.Move.values()[id - 1];
    }

    CrabulonCombat.Action actionPose() {
        return CrabulonCombat.Action.values()[entityData.get(ACTION)];
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (ENTRANCE_AGE.equals(accessor)) clientEntranceSampleTick = tickCount;
    }

    float actionProgress(float partial) {
        if (level().isClientSide && actionPose() == CrabulonCombat.Action.INTRO && entranceAge() >= 0) {
            return Mth.clamp((entranceAge() + partial) / INTRO_TICKS, 0, 1);
        }
        return Mth.clamp((level().getGameTime() - entityData.get(ACTION_TIME) + partial)
            / Math.max(1, entityData.get(ACTION_LENGTH)), 0, 1);
    }

    int entranceAge() {
        int age = entityData.get(ENTRANCE_AGE);
        if (age < 0 || !level().isClientSide) return age;
        return age + Mth.clamp(tickCount - clientEntranceSampleTick, 0, 3);
    }

    boolean withinArena(Vec3 at) {
        return BossArenaPortals.withinCylinder(position(), at, ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    private static boolean eligible(ServerPlayer player, Vec3 center) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !Incapacitation2123.incapacitated(player)
            && BossArenaPortals.withinCylinder(center, player.position(), ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    boolean canTarget(ServerPlayer player) {
        return eligible(player, position());
    }

    @Override
    public void tick() {
        AABB skitterStart = !level().isClientSide && combat.skitterStepPending() ? getBoundingBox() : null;
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive() || isNoAi()) return;
        if (level.dimension() != WorldEvents.UNDERWORLD) {
            discard();
            return;
        }
        // Keep the encounter ticking through a last-player dimension exit.
        holdEncounterChunk(level);
        if (tickCount % 10 == 0) updateAudience(level);
        var players = level.getPlayers(this::canTarget);
        if (players.isEmpty()) {
            combat.suspend(level, this);
            if (++missingPlayers >= 140) discard();
            return;
        }
        missingPlayers = 0;
        bar.setProgress(getHealth() / getMaxHealth());
        if (introLeft > 0) {
            --introLeft;
            if (introLeft == 0) {
                pose(null, CrabulonCombat.Action.REST, 0);
                entityData.set(ENTRANCE_AGE, -1);
                introViewers.clear();
            } else if (entranceAge() >= 0 && introLeft % 2 == 0) {
                entityData.set(ENTRANCE_AGE, INTRO_TICKS - introLeft);
            }
            return;
        }
        if (skitterStart != null) combat.finishSkitterStep(level, this, players, skitterStart);
        ServerPlayer target;
        boolean selecting = combat.wantsTarget();
        if (selecting) {
            target = chooseTarget(level, players);
        } else {
            UUID locked = combat.target();
            target = players.stream().filter(player -> player.getUUID().equals(locked)).findFirst().orElse(null);
        }
        combat.tick(level, this, target, players);
        if (selecting && target != null && !combat.wantsTarget()) {
            lastSelectedTarget = target.getUUID();
        }
        if (combat.dangerous()) BossArenaPortals.holdFollowing(level, this, ARENA_RADIUS, ARENA_HEIGHT);
        else BossArenaPortals.release(level, getUUID());
    }

    private void holdEncounterChunk(ServerLevel level) {
        ChunkPos current = chunkPosition();
        if (current.equals(encounterChunk) && tickCount % 10 != 0) return;
        var chunks = level.getChunkSource();
        chunks.addRegionTicket(ENCOUNTER_TICKET, current, ENCOUNTER_TICKET_DISTANCE, getUUID());
        if (encounterChunk != null && !current.equals(encounterChunk)) {
            chunks.removeRegionTicket(ENCOUNTER_TICKET, encounterChunk, ENCOUNTER_TICKET_DISTANCE, getUUID());
        }
        encounterChunk = current;
    }

    private void releaseEncounterChunk(ServerLevel level) {
        if (encounterChunk == null) return;
        level.getChunkSource().removeRegionTicket(ENCOUNTER_TICKET, encounterChunk, ENCOUNTER_TICKET_DISTANCE, getUUID());
        encounterChunk = null;
    }

    private ServerPlayer chooseTarget(ServerLevel level, List<ServerPlayer> players) {
        var reachable = players.stream().filter(player -> canReachTarget(level, player))
            .sorted(Comparator.comparing(ServerPlayer::getUUID)).toList();
        if (reachable.isEmpty()) return players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElseThrow();
        if (lastSelectedTarget != null) {
            for (var player : reachable) if (player.getUUID().compareTo(lastSelectedTarget) > 0) return player;
        }
        return reachable.get(0);
    }

    private boolean canReachTarget(ServerLevel level, ServerPlayer player) {
        double distance = distanceToSqr(player);
        if (distance > 15 * 15) return false;
        if (Math.abs(player.getY() - getY()) <= CrabulonCombat.VISIBLE_ATTACK_HEIGHT
                && BossAttackGeometry.visible(level, this, getEyePosition(), player.getEyePosition())) return true;
        return CrabulonCombat.fanInRange(distance)
            && CrabulonFanShot.plan(level, this, player.getBoundingBox().getCenter()) != null;
    }

    private void updateAudience(ServerLevel level) {
        var viewers = level.getPlayers(player -> player.isAlive() && withinArena(player.position()));
        for (var previous : List.copyOf(bar.getPlayers())) if (!viewers.contains(previous)) bar.removePlayer(previous);
        for (var viewer : viewers) bar.addPlayer(viewer);
        music.sync(level, getUUID(), viewers);
        int age = entranceAge();
        if (age >= 0 && actionPose() == CrabulonCombat.Action.INTRO) {
            for (var viewer : viewers) {
                if (viewer.connection.isAcceptingMessages() && introViewers.put(viewer.getUUID(), viewer) != viewer) {
                    GroundSeaBossIntroPacket.send(viewer, getUUID(), age, GroundSeaBossIntroPacket.Kind.CRABULON);
                }
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    void broodLanded(ServerLevel level, Vec3 floor) {
        combat.broodLanded(level, this, floor);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ServerPlayer player && combat.isOpenBelly(this, player)) amount *= 1.35f;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) {
            releaseEncounterChunk(level);
            combat.suspend(level, this);
            bar.removeAllPlayers();
            music.stop(getUUID());
        }
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && isDeadOrDying()) {
            releaseEncounterChunk(level);
            combat.suspend(level, this);
            bar.removeAllPlayers();
            music.stop(getUUID());
        }
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean credited) {
        super.dropCustomDeathLoot(source, looting, credited);
        if (credited && level() instanceof ServerLevel level) CrabulonContent.dropRewards(level, this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CrabulonParty", initialParty);
        tag.putFloat("EncounterMaxHealth", getMaxHealth());
        tag.putInt("CrabulonIntro", introLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        initialParty = Mth.clamp(tag.getInt("CrabulonParty"), 1, 8);
        introLeft = Mth.clamp(tag.getInt("CrabulonIntro"), 0, INTRO_TICKS);
        float oldMax = tag.contains("EncounterMaxHealth") ? tag.getFloat("EncounterMaxHealth")
            : (float)partyHealth(LEGACY_HEALTH);
        float ratio = tag.contains("Health") && oldMax > 0 ? Mth.clamp(tag.getFloat("Health") / oldMax, 0, 1) : 1;
        configureHealth();
        setHealth(getMaxHealth() * ratio);
        pose(null, introLeft > 0 ? CrabulonCombat.Action.INTRO : CrabulonCombat.Action.REST, introLeft);
        entityData.set(ENTRANCE_AGE, introLeft > 0 ? INTRO_TICKS - introLeft : -1);
    }
}
