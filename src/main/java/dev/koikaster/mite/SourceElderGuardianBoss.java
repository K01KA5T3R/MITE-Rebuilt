package dev.koikaster.mite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SourceElderGuardianBoss extends ElderGuardian {
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(SourceElderGuardianBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_START = SynchedEntityData.defineId(SourceElderGuardianBoss.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> RITUAL = SynchedEntityData.defineId(SourceElderGuardianBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETRACTED = SynchedEntityData.defineId(SourceElderGuardianBoss.class, EntityDataSerializers.BOOLEAN);
    static final int INTRO_ACTION = 13;
    private static final int INTRO_TICKS = 34;
    private static final double MAX_HEALTH = 310;
    private static final String FATIGUE_OWNER = MiteReforged.ID + ":elder_fatigue_owner";
    private static final String EXTERNAL_FATIGUE = MiteReforged.ID + ":elder_external_fatigue";
    private static final int FATIGUE_DURATION = 6000;

    private final ElderGuardianCombat combat = new ElderGuardianCombat();
    private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.ELDER_GUARDIAN);
    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> titles = new HashSet<>();
    private final Map<ServerPlayer, MobEffectInstance> fatigue = new HashMap<>();
    private AABB arena;
    private long appearedAt;
    private long fatigueAt;

    public SourceElderGuardianBoss(EntityType<? extends ElderGuardian> type, Level level) { super(type, level); }

    static boolean summon(ServerLevel level, Vec3 summoner, float yaw, AABB arena) {
        if (!WorldContentMode.rebuiltBosses(level) || level.getDifficulty() == Difficulty.PEACEFUL || !level.getEntitiesOfClass(SourceElderGuardianBoss.class,
                arena, boss -> boss.ritual() && boss.isAlive()).isEmpty()) return false;
        if (!(EntityType.ELDER_GUARDIAN.create(level) instanceof SourceElderGuardianBoss boss)) return false;
        Vec3 at = findWater(level, boss, summoner, arena, yaw);
        if (at == null) return false;
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.arena = arena;
        boss.appearedAt = level.getGameTime();
        boss.fatigueAt = boss.appearedAt + 1200;
        boss.setPersistenceRequired();
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        boss.setHealth(boss.getMaxHealth());
        boss.entityData.set(RITUAL, true);
        boss.action(INTRO_ACTION);
        boss.retract(true);
        if (!level.addFreshEntity(boss)) return false;
        level.playSound(null, boss.blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE, 3.7f, .61f);
        return true;
    }

    private static Vec3 findWater(ServerLevel level, SourceElderGuardianBoss boss, Vec3 player, AABB arena, float yaw) {
        Vec3 heading = Vec3.directionFromRotation(0, yaw);
        for (double distance : new double[]{5.2, 3.45, 7.5}) {
            for (int i = 0; i < 8; ++i) {
                Vec3 direction = heading.yRot(i * (float)Math.PI / 4);
                for (int elevation : new int[]{0, -2, 2}) {
                    Vec3 point = player.add(direction.scale(distance)).add(0, elevation, 0);
                    boss.setPos(point.x, point.y, point.z);
                    if (arena.contains(point) && level.noCollision(boss) && water(level, point.add(0, 1, 0))
                            && water(level, point.add(0, .18, 0))
                            && BossAttackGeometry.visible(level, boss, point.add(0, 1, 0), player.add(0, 1.1, 0))) return point;
                }
            }
        }
        return null;
    }

    @Override protected void registerGoals() {}

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTION, 0);
        entityData.define(ACTION_START, 0L);
        entityData.define(RITUAL, false);
        entityData.define(RETRACTED, false);
    }

    boolean ritual() { return arena != null; }
    int action() { return entityData.get(ACTION); }
    long actionAge() { return Math.max(0, level().getGameTime() - entityData.get(ACTION_START)); }

    void action(int next) {
        if (next == action()) return;
        entityData.set(ACTION_START, level().getGameTime());
        entityData.set(ACTION, next);
    }

    void retract(boolean value) { entityData.set(RETRACTED, value); }
    boolean retracted() { return entityData.get(RETRACTED); }

    @Override public boolean isMoving() {
        return entityData != null && entityData.hasItem(RITUAL) && entityData.get(RITUAL) ? retracted() : super.isMoving();
    }

    boolean contains(Vec3 at) { return arena != null && arena.contains(at); }

    static boolean water(ServerLevel level, Vec3 at) {
        var pos = BlockPos.containing(at);
        return level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    @Override protected void customServerAiStep() {
        if (arena == null || !(level() instanceof ServerLevel server) || !isAlive()) return;
        var audience = server.getPlayers(player -> player.isAlive() && contains(player.position()));
        var swimmers = audience.stream().filter(player -> !player.isSpectator() && !Incapacitation2123.incapacitated(player)
            && player.isEyeInFluid(FluidTags.WATER)).toList();
        if (swimmers.isEmpty()) {
            discard();
            return;
        }
        music.sync(server, getUUID(), audience);
        bar.setProgress(getHealth() / getMaxHealth());
        for (var player : audience) bar.addPlayer(player);
        for (var player : List.copyOf(bar.getPlayers())) if (!audience.contains(player)) bar.removePlayer(player);
        long now = server.getGameTime();
        long elapsed = now - appearedAt;
        if (elapsed < INTRO_TICKS) {
            setDeltaMovement(getDeltaMovement().scale(.55));
            if (elapsed >= 19 && elapsed % 3 == 0) {
                Vec3 eyes = getEyePosition();
                server.sendParticles(ParticleTypes.GLOW, eyes.x, eyes.y, eyes.z, 10, .18, .18, .18, .045);
            }
            if (elapsed == 21) server.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6f, .83f);
            return;
        }
        if (action() == INTRO_ACTION) {
            action(0);
            retract(false);
        }
        if (elapsed < INTRO_TICKS + 56) {
            for (var player : audience) if (player.connection.isAcceptingMessages() && titles.add(player.getUUID())) {
                GroundSeaBossIntroPacket.send(player, getUUID(), appearedAt + INTRO_TICKS, true);
            }
        }
        var targets = swimmers.stream().filter(player -> !player.isCreative()).toList();
        combat.step(server, this, targets);
        if (combat.attacking()) BossArenaPortals.hold(server, getUUID(), arena);
        else BossArenaPortals.release(server, getUUID());
        fatigue.keySet().removeIf(player -> {
            if (audience.contains(player)) return false;
            removeFatigue(player, fatigue.get(player));
            return true;
        });
        if (now >= fatigueAt) {
            fatigueAt = now + 1200;
            for (var player : audience) if (!player.isCreative() && !player.isSpectator()) applyFatigue(player);
        }
    }

    private void applyFatigue(ServerPlayer player) {
        var previous = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (previous != null && previous != fatigue.get(player)) return;
        var effect = new MobEffectInstance(MobEffects.DIG_SLOWDOWN, FATIGUE_DURATION, 2);
        var outside = previous == null ? null : readExternalFatigue(player.getPersistentData());
        if (outside != null) {
            effect.update(outside);
            player.forceAddEffect(effect, this);
        } else {
            if (previous != null) removeFatigue(player, previous);
            player.addEffect(effect, this);
        }
        if (player.getEffect(MobEffects.DIG_SLOWDOWN) != effect) return;
        fatigue.put(player, effect);
        player.getPersistentData().putUUID(FATIGUE_OWNER, getUUID());
        if (player.connection.isAcceptingMessages()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, isSilent() ? 0 : 1));
        }
    }

    private void removeFatigue(ServerPlayer player, MobEffectInstance effect) {
        var data = player.getPersistentData();
        if (!data.hasUUID(FATIGUE_OWNER) || !data.getUUID(FATIGUE_OWNER).equals(getUUID())) return;
        if (player.getEffect(MobEffects.DIG_SLOWDOWN) == effect) endFatigue(player, effect);
        else clearFatigueData(data);
    }

    static void rememberExternalFatigue(ServerPlayer player, MobEffectInstance previous, MobEffectInstance incoming) {
        var data = player.getPersistentData();
        if (incoming.getEffect() != MobEffects.DIG_SLOWDOWN || !data.hasUUID(FATIGUE_OWNER)) return;
        if (previous == null) {
            clearFatigueData(data);
            return;
        }
        if (!(player.serverLevel().getEntity(data.getUUID(FATIGUE_OWNER)) instanceof SourceElderGuardianBoss elder)
                || elder.fatigue.get(player) != previous) return;
        var external = MobEffectInstance.load(incoming.save(new CompoundTag()));
        if (external == null) return;
        var saved = readExternalFatigue(data);
        if (saved != null) {
            saved.update(external);
            external = saved;
        }
        data.put(EXTERNAL_FATIGUE, external.save(new CompoundTag()));
    }

    static void tickFatigue(ServerPlayer player, boolean advanceDuration) {
        var data = player.getPersistentData();
        if (!data.hasUUID(FATIGUE_OWNER)) return;
        if (player.serverLevel().getEntity(data.getUUID(FATIGUE_OWNER)) instanceof SourceElderGuardianBoss elder) {
            var effect = elder.fatigue.get(player);
            if (effect != null && player.getEffect(MobEffects.DIG_SLOWDOWN) != effect) {
                elder.fatigue.remove(player);
                clearFatigueData(data);
                return;
            }
        }
        if (advanceDuration && data.contains(EXTERNAL_FATIGUE, Tag.TAG_COMPOUND)) {
            var saved = tickExternalFatigue(data.getCompound(EXTERNAL_FATIGUE));
            if (saved == null) data.remove(EXTERNAL_FATIGUE);
            else data.put(EXTERNAL_FATIGUE, saved);
        }
    }

    static CompoundTag tickExternalFatigue(CompoundTag effect) {
        // Native hidden effects count down while the stronger effect is visible.
        if (effect.contains("HiddenEffect", Tag.TAG_COMPOUND)) {
            var hidden = tickExternalFatigue(effect.getCompound("HiddenEffect"));
            if (hidden == null) effect.remove("HiddenEffect");
            else effect.put("HiddenEffect", hidden);
        }
        int duration = effect.getInt("Duration");
        if (duration == -1) return effect;
        if (duration <= 1) return effect.contains("HiddenEffect", Tag.TAG_COMPOUND) ? effect.getCompound("HiddenEffect") : null;
        effect.putInt("Duration", duration - 1);
        return effect;
    }

    static void restoreFatigueOwner(ServerPlayer player) {
        var data = player.getPersistentData();
        if (!data.hasUUID(FATIGUE_OWNER)) return;
        var effect = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (effect != null && (ownEffect(effect) || readExternalFatigue(data) != null)
                && player.serverLevel().getEntity(data.getUUID(FATIGUE_OWNER)) instanceof SourceElderGuardianBoss elder
                && elder.isAlive() && elder.contains(player.position())) {
            elder.fatigue.put(player, effect);
            return;
        }
        endFatigue(player, effect);
    }

    private static void endFatigue(ServerPlayer player, MobEffectInstance effect) {
        var data = player.getPersistentData();
        var external = readExternalFatigue(data);
        clearFatigueData(data);
        if (effect == null) return;
        if (external != null) player.forceAddEffect(external, null);
        if ((external != null || ownEffect(effect)) && player.getEffect(MobEffects.DIG_SLOWDOWN) == effect) {
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
    }

    private static MobEffectInstance readExternalFatigue(CompoundTag data) {
        if (!data.contains(EXTERNAL_FATIGUE, Tag.TAG_COMPOUND)) return null;
        var effect = MobEffectInstance.load(data.getCompound(EXTERNAL_FATIGUE));
        return effect != null && effect.getEffect() == MobEffects.DIG_SLOWDOWN ? effect : null;
    }

    private static void clearFatigueData(CompoundTag data) {
        data.remove(FATIGUE_OWNER);
        data.remove(EXTERNAL_FATIGUE);
    }

    private static boolean ownEffect(MobEffectInstance effect) {
        return effect != null && effect.getAmplifier() == 2 && effect.getDuration() > 0
            && effect.getDuration() <= FATIGUE_DURATION;
    }

    @Override public boolean hurt(DamageSource damage, float amount) {
        return arena == null || action() != INTRO_ACTION ? super.hurt(damage, amount) : false;
    }

    @Override public void die(DamageSource damage) {
        super.die(damage);
        if (!isAlive()) endEncounter();
    }

    @Override public void onRemovedFromWorld() {
        endEncounter();
        super.onRemovedFromWorld();
    }

    private void endEncounter() {
        if (arena == null || !(level() instanceof ServerLevel server)) return;
        combat.suspend(this);
        BossArenaPortals.release(server, getUUID());
        music.stop(getUUID());
        bar.removeAllPlayers();
        fatigue.forEach(this::removeFatigue);
        fatigue.clear();
    }

    @Override protected void dropCustomDeathLoot(DamageSource damage, int looting, boolean recent) {
        super.dropCustomDeathLoot(damage, looting, recent);
        if (arena == null || !recent || !(level() instanceof ServerLevel server)) return;
        spawnAtLocation(Items.WET_SPONGE);
        spawnAtLocation(new ItemStack(Items.PRISMARINE_SHARD, 3 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(Items.PRISMARINE_CRYSTALS, 2));
        BossTrophies.first(server, this, BossMusicTrack.ELDER_GUARDIAN, ElderRitualContent.TROPHY);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arena == null) return;
        tag.putIntArray("MiteMonumentArena", new int[]{(int)arena.minX, (int)arena.minY, (int)arena.minZ,
            (int)arena.maxX, (int)arena.maxY, (int)arena.maxZ});
        tag.putLong("MiteMonumentAppeared", appearedAt);
        tag.putLong("MiteMonumentFatigue", fatigueAt);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        var box = tag.getIntArray("MiteMonumentArena");
        if (box.length != 6) return;
        for (int i = 0; i < 3; ++i) if ((long)box[i + 3] - box[i] <= 0 || (long)box[i + 3] - box[i] > 128) return;
        BossCombatTuning.applyHealth(this, MAX_HEALTH, false);
        arena = new AABB(box[0], box[1], box[2], box[3], box[4], box[5]);
        appearedAt = tag.getLong("MiteMonumentAppeared");
        fatigueAt = tag.getLong("MiteMonumentFatigue");
        entityData.set(RITUAL, true);
        if (level().getGameTime() - appearedAt < INTRO_TICKS) {
            entityData.set(ACTION_START, appearedAt);
            entityData.set(ACTION, INTRO_ACTION);
            retract(true);
        }
    }
}
