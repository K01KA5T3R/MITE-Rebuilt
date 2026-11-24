package dev.koikaster.mite;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SourceQueenBee extends Monster {
    private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(SourceQueenBee.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(SourceQueenBee.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_LENGTH = SynchedEntityData.defineId(SourceQueenBee.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_TIME = SynchedEntityData.defineId(SourceQueenBee.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> INTRO_TIME = SynchedEntityData.defineId(SourceQueenBee.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> LATE = SynchedEntityData.defineId(SourceQueenBee.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BANK = SynchedEntityData.defineId(SourceQueenBee.class, EntityDataSerializers.FLOAT);
    static final float HEALTH = 550;
    private static final int INTRO_TICKS = 42;
    private static final double ARENA_RADIUS = 34;
    private static final double ARENA_HEIGHT = 34;
    private static final int TICKET_DISTANCE = 2;
    private static final TicketType<UUID> ENCOUNTER_TICKET =
        TicketType.create("mite_queen_bee_encounter", Comparator.<UUID>naturalOrder(), 40);

    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private final QueenBeeCombat combat = new QueenBeeCombat();
    private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.QUEEN_BEE);
    private QueenBeeArena.Entrance entrance;
    private int partySize = 1;
    private int introLeft;
    private int missingPlayers;
    private UUID lastTarget;
    private ChunkPos encounterChunk;

    SourceQueenBee(EntityType<? extends SourceQueenBee> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 25;
    }

    static AttributeSupplier.Builder attributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.FOLLOW_RANGE, ARENA_RADIUS)
            .add(Attributes.MOVEMENT_SPEED, .30).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    static boolean summon(ServerLevel level, ServerPlayer player) {
        if (!WorldContentMode.calamityBosses(level) || level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
                || Incapacitation2123.incapacitated(player)) return false;
        QueenBeeArena.Entrance entrance = QueenBeeArena.entrance(level, player);
        if (entrance == null) return false;
        var area = new AABB(entrance.center(), entrance.center()).inflate(ARENA_RADIUS * 2, ARENA_HEIGHT * 2, ARENA_RADIUS * 2);
        if (!level.getEntitiesOfClass(SourceQueenBee.class, area, SourceQueenBee::isAlive).isEmpty()) return false;
        SourceQueenBee bee = QueenBeeContent.QUEEN.get().create(level);
        if (bee == null) return false;
        bee.entrance = entrance;
        bee.partySize = Mth.clamp(level.getPlayers(viewer -> eligible(viewer, entrance.center())).size(), 1, 8);
        bee.configureHealth();
        bee.setHealth(bee.getMaxHealth());
        bee.setPersistenceRequired();
        bee.introLeft = INTRO_TICKS;
        Vec3 start = QueenBeeArena.introPoint(entrance, 0);
        bee.moveTo(start.x, start.y, start.z, player.getYRot() + 180, 0);
        bee.entityData.set(INTRO_TIME, level.getGameTime());
        bee.pose(null, QueenBeeCombat.Action.INTRO, INTRO_TICKS);
        if (!level.noCollision(bee) || !level.addFreshEntity(bee)) return false;
        bee.updateAudience(level);
        level.playSound(null, bee.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 2.2f, .65f);
        return true;
    }

    private void configureHealth() {
        double bonus = partySize <= 1 ? 0 : .30 + .07 * (partySize - 2);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH * (1 + bonus));
    }

    @Override
    protected void registerGoals() {
        // The encounter uses collision-aware flight instead of ground navigation.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MOVE, 0);
        entityData.define(ACTION, QueenBeeCombat.Action.REST.ordinal());
        entityData.define(ACTION_LENGTH, 1);
        entityData.define(ACTION_TIME, 0L);
        entityData.define(INTRO_TIME, -1L);
        entityData.define(LATE, false);
        entityData.define(BANK, 0f);
    }

    void pose(QueenBeeCombat.Move move, QueenBeeCombat.Action action, int length) {
        entityData.set(MOVE, move == null ? 0 : move.ordinal() + 1);
        entityData.set(ACTION, action.ordinal());
        entityData.set(ACTION_LENGTH, Math.max(1, length));
        entityData.set(ACTION_TIME, level().getGameTime());
    }

    QueenBeeCombat.Move attackPose() {
        int move = entityData.get(MOVE);
        return move == 0 ? null : QueenBeeCombat.Move.values()[move - 1];
    }

    QueenBeeCombat.Action actionPose() { return QueenBeeCombat.Action.values()[entityData.get(ACTION)]; }

    float actionProgress(float partial) {
        return Mth.clamp((level().getGameTime() - entityData.get(ACTION_TIME) + partial) / entityData.get(ACTION_LENGTH), 0, 1);
    }

    long introStarted() { return entityData.get(INTRO_TIME); }
    boolean late() { return entityData.get(LATE); }
    float bank() { return entityData.get(BANK); }
    int partySize() { return partySize; }

    boolean withinArena(Vec3 point) {
        return BossArenaPortals.withinCylinder(position(), point, ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    boolean canTarget(ServerPlayer player) { return eligible(player, position()); }

    private static boolean eligible(ServerPlayer player, Vec3 center) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !Incapacitation2123.incapacitated(player)
            && BossArenaPortals.withinCylinder(center, player.position(), ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive() || isNoAi()) return;
        if (level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        holdChunk(level);
        if (tickCount % 8 == 0) updateAudience(level);
        List<ServerPlayer> players = level.getPlayers(this::canTarget);
        if (players.isEmpty()) {
            combat.suspend(level, this);
            bar.removeAllPlayers();
            if (++missingPlayers >= 140) discard();
            return;
        }
        missingPlayers = 0;
        bar.setProgress(getHealth() / getMaxHealth());
        if (introLeft > 0 && entrance != null) {
            intro(level);
            return;
        }
        if (!late() && getHealth() <= getMaxHealth() * .50f) {
            entityData.set(LATE, true);
            level.playSound(null, blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 1.8f, .78f);
            level.sendParticles(ParticleTypes.FALLING_HONEY, getX(), getY() + .4, getZ(), 20, .68, .24, .68, .06);
        }
        ServerPlayer target = combat.wantsTarget() ? chooseTarget(level, players) : null;
        if (combat.tick(level, this, target, players) && target != null) lastTarget = target.getUUID();
        if (combat.dangerous()) BossArenaPortals.holdFollowing(level, this, ARENA_RADIUS, 24);
        else BossArenaPortals.release(level, getUUID());
    }

    private void intro(ServerLevel level) {
        Vec3 point = QueenBeeArena.introPoint(entrance, (INTRO_TICKS - introLeft + 1) / (double)INTRO_TICKS);
        if (QueenBeeArena.clearPath(level, this, position(), point)) flyToward(point, 1.10, .5f);
        if (introLeft % 5 == 0) level.sendParticles(ParticleTypes.FALLING_HONEY, getX(), getY() + .5, getZ(), 5, .65, .18, .65, .02);
        if (--introLeft == 0) {
            pose(null, QueenBeeCombat.Action.REST, 1);
            entrance = null;
        }
    }

    Vec3 flyToward(Vec3 goal, double speed, float bank) {
        Vec3 before = position();
        Vec3 diff = goal.subtract(before);
        Vec3 step = diff.length() <= speed ? diff : diff.normalize().scale(speed);
        Vec3 after = before;
        if (step.lengthSqr() > .0001 && level().hasChunkAt(net.minecraft.core.BlockPos.containing(before.add(step)))) {
            move(MoverType.SELF, step);
            after = position();
            Vec3 actual = after.subtract(before);
            if (actual.horizontalDistanceSqr() > .001) face(actual);
        }
        entityData.set(BANK, bank);
        return after;
    }

    void face(Vec3 direction) {
        if (direction.horizontalDistanceSqr() < .001) return;
        float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float heading = getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -42, 42);
        setYRot(heading);
        setYHeadRot(heading);
        setYBodyRot(heading);
    }

    private ServerPlayer chooseTarget(ServerLevel level, List<ServerPlayer> players) {
        var nearby = players.stream().filter(player -> distanceToSqr(player) < 21 * 21
            && BossAttackGeometry.visible(level, this, getEyePosition(), player.getEyePosition()))
            .sorted(Comparator.comparing(ServerPlayer::getUUID)).toList();
        if (nearby.isEmpty()) return players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElseThrow();
        if (lastTarget != null) {
            for (ServerPlayer player : nearby) if (player.getUUID().compareTo(lastTarget) > 0) return player;
        }
        return nearby.get(0);
    }

    private void updateAudience(ServerLevel level) {
        var viewers = level.getPlayers(player -> player.isAlive() && withinArena(player.position()));
        for (var previous : List.copyOf(bar.getPlayers())) if (!viewers.contains(previous)) bar.removePlayer(previous);
        for (var viewer : viewers) bar.addPlayer(viewer);
        music.sync(level, getUUID(), viewers);
    }

    private void holdChunk(ServerLevel level) {
        ChunkPos current = chunkPosition();
        if (current.equals(encounterChunk) && tickCount % 10 != 0) return;
        var chunks = level.getChunkSource();
        chunks.addRegionTicket(ENCOUNTER_TICKET, current, TICKET_DISTANCE, getUUID());
        if (encounterChunk != null && !current.equals(encounterChunk)) {
            chunks.removeRegionTicket(ENCOUNTER_TICKET, encounterChunk, TICKET_DISTANCE, getUUID());
        }
        encounterChunk = current;
    }

    private void stopEncounter(ServerLevel level) {
        combat.suspend(level, this);
        bar.removeAllPlayers();
        music.stop(getUUID());
        if (encounterChunk == null) return;
        level.getChunkSource().removeRegionTicket(ENCOUNTER_TICKET, encounterChunk, TICKET_DISTANCE, getUUID());
        encounterChunk = null;
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (introLeft > 0) return false;
        if (source.getEntity() instanceof ServerPlayer) {
            amount *= combat.lowRecovery() ? 1.20f : source.getDirectEntity() == source.getEntity() ? .90f : .72f;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) stopEncounter(level);
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && isDeadOrDying()) stopEncounter(level);
    }

    @Override
    protected ResourceLocation getDefaultLootTable() { return BuiltInLootTables.EMPTY; }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean credited) {
        super.dropCustomDeathLoot(source, looting, credited);
        if (credited && level() instanceof ServerLevel level) QueenBeeContent.dropRewards(level, this);
    }

    void stickyImpact(ServerLevel level, Vec3 point) { combat.stickyImpact(level, this, point); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("QueenParty", partySize);
        if (introLeft > 0 && entrance != null) {
            tag.putInt("QueenIntro", introLeft);
            tag.putDouble("QueenEntranceX", entrance.center().x);
            tag.putDouble("QueenEntranceY", entrance.center().y);
            tag.putDouble("QueenEntranceZ", entrance.center().z);
            tag.putDouble("QueenEntranceTop", entrance.crownHeight());
            tag.putDouble("QueenEntranceAngle", entrance.angle());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        partySize = Mth.clamp(tag.getInt("QueenParty"), 1, 8);
        configureHealth();
        if (tag.contains("Health")) setHealth(tag.getFloat("Health"));
        introLeft = Mth.clamp(tag.getInt("QueenIntro"), 0, INTRO_TICKS);
        if (introLeft > 0) {
            Vec3 center = new Vec3(tag.getDouble("QueenEntranceX"), tag.getDouble("QueenEntranceY"), tag.getDouble("QueenEntranceZ"));
            entrance = new QueenBeeArena.Entrance(center, tag.getDouble("QueenEntranceTop"), tag.getDouble("QueenEntranceAngle"));
            pose(null, QueenBeeCombat.Action.INTRO, introLeft);
        }
    }
}
