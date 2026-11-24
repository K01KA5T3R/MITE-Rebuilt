package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class SourceKingSlime extends Slime {
    private static final EntityDataAccessor<Integer> POSTURE = SynchedEntityData.defineId(SourceKingSlime.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> INTRO_STARTED = SynchedEntityData.defineId(SourceKingSlime.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> WINDUP_SQUISH = SynchedEntityData.defineId(SourceKingSlime.class, EntityDataSerializers.FLOAT);
    private static final float BASE_HEALTH = 400;
    private static final float LEGACY_HEALTH = 288;
    private static final int MAX_SIZE = 11;
    private static final double ARENA_RADIUS = 34;
    private static final double ARENA_HEIGHT = 13;
    private static final int RETREAT_TICKS = 140;
    private static final int INTRO_TICKS = 30;
    private static final int ENCOUNTER_TICKET_DISTANCE = 2;
    private static final int FOLLOWER_RETRY_TICKS = 10;
    private static final TicketType<UUID> ENCOUNTER_TICKET =
        TicketType.create("mite_king_slime_encounter", Comparator.<UUID>naturalOrder(), 40);

    private final KingSlimeCombat combat = new KingSlimeCombat();
    private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.KING_SLIME);
    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private final List<KingSlimeMinion> minions = new ArrayList<>();
    private final List<KingSlimeMinion.Saved> pendingMinions = new ArrayList<>();
    private boolean followersResumed;
    private int nextFollowerRetry;
    private List<KingSlimeMinion.Saved> stoppingMinions;
    private int minionWaves;
    private KingSlimeJewel jewel;
    private boolean jewelBroken;
    private UUID committedTarget;
    private UUID lastAttackTarget;
    private int startingParty = 1;
    private int retreatTicks;
    private int intro = INTRO_TICKS;
    private ChunkPos encounterChunk;

    SourceKingSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, BASE_HEALTH).add(Attributes.MOVEMENT_SPEED, .28)
            .add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.FOLLOW_RANGE, ARENA_RADIUS);
    }

    static boolean summon(ServerLevel level, ServerPlayer player) {
        if (!WorldContentMode.calamityBosses(level) || level.getDifficulty() == Difficulty.PEACEFUL || Incapacitation2123.incapacitated(player)) return false;
        Vec3 site = KingSlimeArena.summonSite(level, player);
        if (site == null) return false;
        var nearby = new AABB(site, site).inflate(ARENA_RADIUS * 2, ARENA_HEIGHT * 2, ARENA_RADIUS * 2);
        if (!level.getEntitiesOfClass(SourceKingSlime.class, nearby, SourceKingSlime::isAlive).isEmpty()) return false;
        SourceKingSlime boss = KingSlimeContent.KING.get().create(level);
        if (boss == null) return false;
        boss.startingParty = Math.max(1, Math.min(8, participants(level, site).size()));
        boss.resize(MAX_SIZE);
        boss.setHealth(boss.getMaxHealth());
        boss.moveTo(site.x, site.y, site.z, player.getYRot() + 180, 0);
        boss.setPersistenceRequired();
        boss.entityData.set(INTRO_STARTED, level.getGameTime());
        if (!level.noCollision(boss) || !level.addFreshEntity(boss)) return false;
        boss.updateAudience(level);
        level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 1.8f, .56f);
        level.sendParticles(ParticleTypes.ITEM_SLIME, site.x, site.y + .45, site.z, 32, 1.2, .3, 1.2, .11);
        return true;
    }

    private static List<ServerPlayer> participants(ServerLevel level, Vec3 center) {
        return level.getPlayers(player -> player.isAlive() && !player.isCreative() && !player.isSpectator()
            && !Incapacitation2123.incapacitated(player)
            && BossArenaPortals.withinCylinder(center, player.position(), ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT));
    }

    @Override
    protected void registerGoals() {
        // The encounter owns all movement and damage; the native slime still owns physics and animation.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(POSTURE, KingSlimeCombat.Posture.INTRO.ordinal());
        entityData.define(INTRO_STARTED, -1L);
        entityData.define(WINDUP_SQUISH, -.36f);
    }

    @Override
    protected boolean isDealsDamage() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        if (level().isClientSide && entityData.get(POSTURE) == KingSlimeCombat.Posture.WINDUP.ordinal()) {
            targetSquish = entityData.get(WINDUP_SQUISH);
        }
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive() || isNoAi()) return;
        // Keep the retreat ticking after the last player leaves the tracked chunks.
        holdEncounterChunk(level);
        var players = participants(level, position());
        if (tickCount % 10 == 0) updateAudience(level);
        if (players.isEmpty()) {
            combat.suspend(level, this);
            committedTarget = null;
            removeFollowers();
            if (followersResumed) pendingMinions.clear();
            if (++retreatTicks >= RETREAT_TICKS) discard();
            return;
        }
        retreatTicks = 0;
        bar.setProgress(getHealth() / getMaxHealth());
        if (intro > 0) {
            --intro;
            return;
        }
        restoreMinions(level);
        int desired = sizeForHealth(healthRatio());
        if (getSize() != desired) resize(desired);
        boolean choosing = combat.canChooseTarget();
        ServerPlayer target = choosing ? chooseTarget(level, players) : currentTarget(players);
        if (target == null) {
            if (combat.targetLostDuringWindup()) {
                combat.suspend(level, this);
                committedTarget = null;
                return;
            }
            target = nearest(players);
        }
        if (choosing) committedTarget = target.getUUID();
        combat.tick(level, this, target, players);
        if (choosing && combat.hasAttackWindup()) lastAttackTarget = target.getUUID();
        if (healthRatio() <= .67f && !jewelBroken) {
            if (jewel == null || jewel.isRemoved()) jewel = KingSlimeJewel.attach(level, this);
            if (jewel != null) {
                var alternate = jewelTarget(level, players, target);
                jewel.followAndFire(level, this, alternate);
            }
        }
        if (tickCount % 4 == 0) callMinions(level);
        if (combat.active()) BossArenaPortals.holdFollowing(level, this, ARENA_RADIUS, ARENA_HEIGHT);
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
        if (players.size() == 1) return players.get(0);
        var candidates = players.stream().filter(player -> inAttackRange(level, player))
            .sorted(Comparator.comparing(ServerPlayer::getUUID)).toList();
        if (candidates.isEmpty()) return nearest(players);
        if (lastAttackTarget != null) {
            for (var player : candidates) {
                if (player.getUUID().compareTo(lastAttackTarget) > 0) return player;
            }
        }
        return candidates.get(0);
    }

    private boolean inAttackRange(ServerLevel level, ServerPlayer player) {
        Vec3 offset = player.position().subtract(position());
        return Math.abs(offset.y) <= 2.6 && offset.horizontalDistanceSqr() <= 11.5 * 11.5
            && BossAttackGeometry.visible(level, this, getEyePosition(), player.getEyePosition());
    }

    private ServerPlayer currentTarget(List<ServerPlayer> players) {
        if (committedTarget == null) return null;
        return players.stream().filter(player -> player.getUUID().equals(committedTarget)).findFirst().orElse(null);
    }

    private ServerPlayer nearest(List<ServerPlayer> players) {
        return players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElseThrow();
    }

    private ServerPlayer jewelTarget(ServerLevel level, List<ServerPlayer> players, ServerPlayer mainTarget) {
        return players.stream().filter(player -> player != mainTarget && inAttackRange(level, player))
            .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(mainTarget);
    }

    private void updateAudience(ServerLevel level) {
        var nearby = level.getPlayers(player -> player.isAlive()
            && BossArenaPortals.withinCylinder(position(), player.position(), ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT));
        for (var viewer : List.copyOf(bar.getPlayers())) {
            if (!nearby.contains(viewer)) bar.removePlayer(viewer);
        }
        for (var viewer : nearby) bar.addPlayer(viewer);
        music.sync(level, getUUID(), nearby);
    }

    @SubscribeEvent
    public static void preserveFollowersOnStop(ServerStoppingEvent event) {
        for (var level : event.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof SourceKingSlime king && king.isAlive()) {
                    king.stoppingMinions = king.livingMinions();
                }
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    long introStarted() {
        return entityData.get(INTRO_STARTED);
    }

    boolean withinIntroRange(Vec3 position) {
        return BossArenaPortals.withinCylinder(position(), position, ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    void windupSquish(float squish) {
        entityData.set(WINDUP_SQUISH, squish);
    }

    void posture(KingSlimeCombat.Posture posture) {
        entityData.set(POSTURE, posture.ordinal());
    }

    float healthRatio() {
        return getHealth() / getMaxHealth();
    }

    private static int sizeForHealth(float ratio) {
        return ratio > .67f ? MAX_SIZE : ratio > .34f ? 9 : 7;
    }

    private void resize(int size) {
        float hp = getHealth();
        setSize(size, false);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH * (1 + .30 * (startingParty - 1)));
        setHealth(hp);
        xpReward = 18;
    }

    boolean jewelAttackWindow() {
        return isAlive() && combat.permitsJewelShots();
    }

    boolean permitsJewelFlight() {
        return isAlive() && retreatTicks == 0 && !combat.missedSlamRecovery();
    }

    boolean canTarget(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !Incapacitation2123.incapacitated(player)
            && BossArenaPortals.withinCylinder(position(), player.position(), ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    void breakJewel(KingSlimeJewel broken) {
        if (jewel == broken) {
            jewelBroken = true;
            jewel = null;
        }
    }

    private int minionLimit() {
        return startingParty == 1 ? 2 : startingParty < 4 ? 3 : 4;
    }

    private void restoreMinions(ServerLevel level) {
        followersResumed = true;
        if (pendingMinions.isEmpty() || tickCount < nextFollowerRetry) return;
        nextFollowerRetry = tickCount + FOLLOWER_RETRY_TICKS;
        for (var iterator = pendingMinions.iterator(); iterator.hasNext();) {
            var saved = iterator.next();
            var existing = level.getEntity(saved.uuid());
            if (existing != null) {
                if (existing instanceof KingSlimeMinion follower && follower.belongsTo(this) && follower.isAlive()) {
                    if (!minions.contains(follower)) minions.add(follower);
                    iterator.remove();
                }
                continue;
            }
            var position = saved.position();
            KingSlimeMinion minion = null;
            if (position.distanceToSqr(position()) <= KingSlimeMinion.OWNER_RANGE * KingSlimeMinion.OWNER_RANGE) {
                minion = KingSlimeMinion.restore(level, this, saved, position);
            }
            if (minion == null) {
                for (int i = 0; i < 8; ++i) {
                    var near = position().add(BossAttackGeometry.flatDirection(i * Math.PI / 4).scale(4.2));
                    if (!level.isPositionEntityTicking(BlockPos.containing(near))) continue;
                    var floor = BossAttackGeometry.ground(level, this, near.add(0, 1.4, 0), 3.4);
                    if (floor == null) continue;
                    minion = KingSlimeMinion.restore(level, this, saved, floor);
                    if (minion != null) break;
                }
            }
            if (minion != null) {
                minions.add(minion);
                iterator.remove();
            }
        }
    }

    private void callMinions(ServerLevel level) {
        minions.removeIf(minion -> !minion.isAlive());
        int wave = (minionWaves & 1) == 0 ? 1 : 2;
        if ((minionWaves & wave) != 0 || healthRatio() > (wave == 1 ? .67f : .34f)) return;
        int limit = minionLimit() - pendingMinions.size();
        if (minions.size() >= limit) return;
        int before = minions.size();
        Vec3 origin = position();
        double floorProbe = onGround() ? 3.4 : 12;
        for (int i = 0; i < 8 && minions.size() < limit; ++i) {
            Vec3 at = origin.add(BossAttackGeometry.flatDirection((i + getId() % 8) * Math.PI / 4).scale(4.2));
            Vec3 floor = BossAttackGeometry.ground(level, this, at.add(0, 1.4, 0), floorProbe);
            if (floor == null) continue;
            var minion = KingSlimeMinion.spawn(level, this, floor);
            if (minion != null) minions.add(minion);
        }
        if (minions.size() > before) minionWaves |= wave;
    }

    private void removeFollowers() {
        if (jewel != null) {
            jewel.discard();
            jewel = null;
        }
        minions.forEach(KingSlimeMinion::discard);
        minions.clear();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StartingParty", startingParty);
        tag.putFloat("EncounterMaxHealth", getMaxHealth());
        tag.putInt("Intro", intro);
        tag.putInt("Retreat", retreatTicks);
        tag.putBoolean("JewelBroken", jewelBroken);
        tag.putInt("MinionWaves", minionWaves);
        var survivors = new ListTag();
        var savedMinions = stoppingMinions == null ? livingMinions() : stoppingMinions;
        for (var saved : savedMinions) survivors.add(saved.write());
        if (!survivors.isEmpty()) tag.put("LivingMinions", survivors);
    }

    private List<KingSlimeMinion.Saved> livingMinions() {
        var survivors = new ArrayList<KingSlimeMinion.Saved>();
        var seen = new HashSet<UUID>();
        for (var minion : minions) {
            if (minion.isAlive() && minion.belongsTo(this) && minion.level() == level()
                    && minion.distanceToSqr(this) <= KingSlimeMinion.OWNER_RANGE * KingSlimeMinion.OWNER_RANGE) {
                if (seen.add(minion.getUUID())) survivors.add(minion.saveFollower());
                if (survivors.size() == minionLimit()) return survivors;
            }
        }
        for (var pending : pendingMinions) {
            if (seen.add(pending.uuid())) survivors.add(pending);
            if (survivors.size() == minionLimit()) break;
        }
        return survivors;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        startingParty = Mth.clamp(tag.getInt("StartingParty"), 1, 8);
        intro = Mth.clamp(tag.getInt("Intro"), 0, INTRO_TICKS);
        retreatTicks = Mth.clamp(tag.getInt("Retreat"), 0, RETREAT_TICKS);
        jewelBroken = tag.getBoolean("JewelBroken");
        minionWaves = tag.getInt("MinionWaves") & 3;
        pendingMinions.clear();
        followersResumed = false;
        nextFollowerRetry = 0;
        if (minionWaves != 0) {
            var ids = new HashSet<UUID>();
            var survivors = tag.getList("LivingMinions", Tag.TAG_COMPOUND);
            for (int i = 0; i < survivors.size() && pendingMinions.size() < minionLimit(); ++i) {
                var saved = KingSlimeMinion.Saved.read(survivors.getCompound(i));
                if (saved != null && !saved.uuid().equals(getUUID()) && ids.add(saved.uuid())) pendingMinions.add(saved);
            }
        }
        float oldMax = tag.contains("EncounterMaxHealth") ? tag.getFloat("EncounterMaxHealth")
            : (float)(LEGACY_HEALTH * (1 + .30 * (startingParty - 1)));
        float ratio = tag.contains("Health") && oldMax > 0 ? Mth.clamp(tag.getFloat("Health") / oldMax, 0, 1) : 1;
        resize(sizeForHealth(ratio));
        setHealth(getMaxHealth() * ratio);
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) {
            releaseEncounterChunk(level);
            BossArenaPortals.release(level, getUUID());
            removeFollowers();
            bar.removeAllPlayers();
            music.stop(getUUID());
        }
        // The standard Slime removal would split this distinct boss type into other bosses.
        ((SourceMagmaNativeParent)(Object)this).mite$magmaParentRemove(reason);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean credited) {
        super.dropCustomDeathLoot(source, looting, credited);
        if (credited && level() instanceof ServerLevel server) KingSlimeRewards.drop(server, this);
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            if (!isAlive()) releaseEncounterChunk(level);
            BossArenaPortals.release(level, getUUID());
            removeFollowers();
            bar.removeAllPlayers();
            music.stop(getUUID());
        }
    }
}
