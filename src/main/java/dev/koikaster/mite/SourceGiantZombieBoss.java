package dev.koikaster.mite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

public final class SourceGiantZombieBoss extends Giant {
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(SourceGiantZombieBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_START = SynchedEntityData.defineId(SourceGiantZombieBoss.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> RITUAL = SynchedEntityData.defineId(SourceGiantZombieBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION_ELAPSED = SynchedEntityData.defineId(SourceGiantZombieBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ATTACK_YAW_FROM = SynchedEntityData.defineId(SourceGiantZombieBoss.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTACK_YAW_TO = SynchedEntityData.defineId(SourceGiantZombieBoss.class, EntityDataSerializers.FLOAT);
    static final int EMERGE = 13;
    static final int FALLEN = 14;
    private static final int INTRO = 53;
    private static final int ARENA_RADIUS = 52;
    private static final int ARENA_HEIGHT = 24;
    private static final double MAX_HEALTH = 370;

    private final GiantZombieLeg[] parts;
    private final GiantZombieCombat combat = new GiantZombieCombat();
    private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.GIANT_ZOMBIE);
    private final ServerBossEvent bar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> titles = new HashSet<>();
    private BlockPos anchor;
    private long appearedAt;
    private long lastPresent;
    private int clientActionSampleTick;

    public SourceGiantZombieBoss(EntityType<? extends Giant> type, Level level) {
        super(type, level);
        parts = new GiantZombieLeg[]{new GiantZombieLeg(this, 0), new GiantZombieLeg(this, 1),
            new GiantZombieLeg(this, GiantZombieLeg.TORSO), new GiantZombieLeg(this, GiantZombieLeg.RIGHT_ARM),
            new GiantZombieLeg(this, GiantZombieLeg.LEFT_ARM)};
        setId(getId());
    }

    static boolean summon(ServerLevel level, Vec3 at, float yaw) {
        if (!WorldContentMode.rebuiltBosses(level) || level.getDifficulty() == Difficulty.PEACEFUL) return false;
        var search = new AABB(at, at).inflate(ARENA_RADIUS * 2, ARENA_HEIGHT * 2, ARENA_RADIUS * 2);
        if (!level.getEntitiesOfClass(SourceGiantZombieBoss.class, search, boss -> boss.anchor != null).isEmpty()) return false;
        if (!(EntityType.GIANT.create(level) instanceof SourceGiantZombieBoss giant)) return false;
        giant.moveTo(at.x, at.y, at.z, yaw, 0);
        if (!level.noCollision(giant)) return false;
        giant.anchor = BlockPos.containing(at);
        giant.appearedAt = level.getGameTime();
        giant.lastPresent = giant.appearedAt;
        giant.setPersistenceRequired();
        giant.setMaxUpStep(1.45f);
        giant.getAttribute(Attributes.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        giant.setHealth(giant.getMaxHealth());
        giant.entityData.set(RITUAL, true);
        giant.action(EMERGE);
        if (!level.addFreshEntity(giant)) return false;
        level.playSound(null, giant.blockPosition(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 3, .42f);
        return true;
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTION, 0);
        entityData.define(ACTION_START, 0L);
        entityData.define(RITUAL, false);
        entityData.define(ACTION_ELAPSED, 0);
        entityData.define(ATTACK_YAW_FROM, 0f);
        entityData.define(ATTACK_YAW_TO, 0f);
    }

    int action() { return entityData.get(ACTION); }
    long actionAge() {
        if (level().isClientSide)
            return Math.max(0, entityData.get(ACTION_ELAPSED)) + Math.max(0, Math.min(2, tickCount - clientActionSampleTick));
        return Math.max(0, level().getGameTime() - entityData.get(ACTION_START));
    }

    void lockAttackFacing(Vec3 facing) {
        entityData.set(ATTACK_YAW_FROM, getYRot());
        entityData.set(ATTACK_YAW_TO, (float)Math.toDegrees(Math.atan2(-facing.x, facing.z)));
    }

    void action(int action) {
        int previous = action();
        if (action != previous) {
            if (action == FALLEN) lockAttackFacing(Vec3.directionFromRotation(0, yBodyRot));
            entityData.set(ACTION_START, level().getGameTime());
            entityData.set(ACTION_ELAPSED, 0);
            entityData.set(ACTION, action);
            if (action == FALLEN || previous == FALLEN) alignParts();
        }
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (ACTION.equals(accessor) || ACTION_ELAPSED.equals(accessor)) clientActionSampleTick = tickCount;
        if (ACTION.equals(accessor)) refreshDimensions();
    }

    @Override public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> values) {
        super.onSyncedDataUpdated(values);
        if (!level().isClientSide || parts == null) return;
        boolean fallen = action() == FALLEN;
        // The bulk callback has the new pose, elapsed ticks and facing from the same packet.
        for (var value : values) {
            int id = value.id();
            if (id == ACTION.getId() || fallen && (id == ACTION_ELAPSED.getId() || id == ATTACK_YAW_TO.getId())) {
                alignParts();
                return;
            }
        }
    }

    @Override public EntityDimensions getDimensions(Pose pose) {
        if (entityData != null && entityData.hasItem(ACTION) && action() == FALLEN) return EntityDimensions.scalable(4.2f, 4.6f);
        return super.getDimensions(pose);
    }

    @Override public AABB getBoundingBoxForCulling() {
        AABB bounds = super.getBoundingBoxForCulling();
        if (parts != null && action() == FALLEN) {
            for (var part : parts) bounds = bounds.minmax(part.getBoundingBox());
        }
        return bounds;
    }

    @Override public boolean isMultipartEntity() { return true; }
    @Override public PartEntity<?>[] getParts() { return parts; }

    @Override public void setId(int id) {
        super.setId(id);
        if (parts != null) for (int i = 0; i < parts.length; ++i) parts[i].setId(id + i + 1);
    }

    @Override public void tick() {
        super.tick();
        // Relative pose age avoids client world-clock corrections skipping the wind-up.
        // Two-tick sampling bounds traffic and extrapolation; combat still uses server time.
        if (!level().isClientSide && action() != 0 && tickCount % 2 == 0)
            entityData.set(ACTION_ELAPSED, (int)Math.min(4096, actionAge()));
        var attack = GiantZombieCombat.Move.fromAction(action());
        if (action() == FALLEN) {
            float facing = entityData.get(ATTACK_YAW_TO);
            setYRot(facing);
            yBodyRot = facing;
            setYHeadRot(facing);
        } else if (attack != null) {
            if (level().isClientSide) {
                float from = entityData.get(ATTACK_YAW_FROM);
                float delta = net.minecraft.util.Mth.wrapDegrees(entityData.get(ATTACK_YAW_TO) - from);
                float reach = Math.max(10, 180f / attack.warning) * (actionAge() + 1);
                setYRot(from + net.minecraft.util.Mth.clamp(delta, -reach, reach));
            }
            // Vanilla body/head following must not redirect a locked attack.
            yBodyRot = getYRot();
            setYHeadRot(getYRot());
        }
        alignParts();
    }

    private void alignParts() {
        if (parts == null) return;
        boolean fallen = action() == FALLEN;
        Vec3 forward = Vec3.directionFromRotation(0, fallen ? entityData.get(ATTACK_YAW_TO) : yBodyRot);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 feet = position();
        if (fallen) {
            float fall = GiantZombieCombat.fallenProgress(actionAge());
            float pitch = GiantZombieCombat.fallenLegPitch(fall);
            double drop = GiantZombieCombat.fallenDrop(fall);
            Vec3 hip = feet.add(0, 4.506 - drop, 0);
            parts[0].alignKneeling(hip.add(right.scale(.7125)), forward, pitch, getY());
            parts[1].alignKneeling(hip.subtract(right.scale(.7125)), forward, pitch, getY());
            parts[2].alignBounds(GiantZombieCombat.fallenUpperBounds(feet, forward, drop));
            parts[3].alignBounds(GiantZombieCombat.fallenArmBounds(feet, forward, drop, 1, fall));
            parts[4].alignBounds(GiantZombieCombat.fallenArmBounds(feet, forward, drop, -1, fall));
            return;
        }
        var attack = GiantZombieCombat.Move.fromAction(action());
        if (attack == GiantZombieCombat.Move.KNEEL) {
            float bend = GiantZombieCombat.kneelBend(actionAge() - attack.warning);
            double drop = GiantZombieCombat.kneelDrop(bend);
            Vec3 hip = feet.add(0, 4.506 - drop, 0);
            parts[0].alignKneeling(hip.add(right.scale(.7125)), forward, bend, getY());
            parts[1].alignKneeling(hip.subtract(right.scale(.7125)), forward, -bend, getY());
            parts[2].align(feet.add(0, 4.45 - drop, 0));
        } else {
            int lifted = attack == GiantZombieCombat.Move.STOMP ? 0 : attack == GiantZombieCombat.Move.FISSURE ? 1 : -1;
            float pitch = lifted < 0 ? 0 : GiantZombieCombat.plantedLegPitch(attack, actionAge());
            for (int leg = 0; leg < 2; ++leg) {
                double side = leg == 0 ? 1 : -1;
                if (leg == lifted && pitch != 0) {
                    Vec3 hip = feet.add(right.scale(.7125 * side)).add(0, 4.506, 0);
                    parts[leg].alignLifted(hip, forward, pitch, feet.y);
                } else {
                    parts[leg].align(feet.add(right.scale(.82 * side)));
                }
            }
            parts[2].align(feet.add(0, 4.45, 0));
        }
        parts[3].align(feet);
        parts[4].align(feet);
    }

    @Override public boolean isPickable() {
        return (entityData == null || !entityData.hasItem(RITUAL) || !entityData.get(RITUAL)) && super.isPickable();
    }
    @Override public boolean canBeHitByProjectile() { return isPickable() && super.canBeHitByProjectile(); }
    boolean canHitPart(boolean onlyFallen) {
        return entityData.get(RITUAL) && isAlive() && action() != EMERGE && (!onlyFallen || action() == FALLEN);
    }

    boolean hurtLeg(int side, DamageSource damage, float amount) {
        if (anchor == null || !canHitPart(false) || action() == FALLEN || !(damage.getEntity() instanceof ServerPlayer)) return false;
        float before = getHealth();
        boolean accepted = super.hurt(damage, amount * combat.legDamageMultiplier());
        if (accepted && isAlive() && action() != FALLEN && getHealth() < before) combat.hurtLeg(this, side, before - getHealth());
        return accepted;
    }

    @Override public boolean hurt(DamageSource damage, float amount) {
        if (anchor == null) return super.hurt(damage, amount);
        if (action() == EMERGE) return false;
        return super.hurt(damage, action() == FALLEN ? amount : amount * .38f);
    }

    @Override protected void customServerAiStep() {
        if (anchor == null || !(level() instanceof ServerLevel server)) {
            super.customServerAiStep();
            return;
        }
        if (!isAlive()) return;
        if (SourceMoonCycle.daytime(server.getDayTime())) {
            dawn(server);
            return;
        }
        var audience = server.getPlayers(player -> player.isAlive() && inArena(player.position()));
        var players = audience.stream().filter(player -> !player.isCreative() && !player.isSpectator()
            && !Incapacitation2123.incapacitated(player)).toList();
        music.sync(server, getUUID(), audience);
        int elapsed = (int)Math.max(0, server.getGameTime() - appearedAt);
        if (elapsed < INTRO) {
            setDeltaMovement(Vec3.ZERO);
            if (elapsed % 4 == 0) dust(server, position(), 32, 2.1);
            if (elapsed == 13 || elapsed == 34) {
                server.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.8f, .58f);
            }
            return;
        }
        if (action() == EMERGE) action(0);
        bar.setProgress(getHealth() / getMaxHealth());
        for (var player : audience) bar.addPlayer(player);
        for (var player : List.copyOf(bar.getPlayers())) if (!audience.contains(player)) bar.removePlayer(player);
        if (elapsed < INTRO + 56) {
            for (var player : audience) if (player.connection.isAcceptingMessages() && titles.add(player.getUUID())) {
                GroundSeaBossIntroPacket.send(player, getUUID(), appearedAt + INTRO, false);
            }
        }
        if (players.isEmpty()) {
            combat.suspend(this);
            lock(server, false);
            if (server.getGameTime() - lastPresent >= 140) discard();
        } else {
            lastPresent = server.getGameTime();
            if (!combat.step(server, this, players)) {
                discard();
                return;
            }
            lock(server, combat.attacking());
        }
        int time = SourceMoonCycle.adjustedTime(server.getDayTime());
        if (time >= 4760 && time <= 5000 && time % 28 == 0) {
            dust(server, position(), 27, 3.8);
            server.playSound(null, blockPosition(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 3.6f, .34f);
        }
    }

    boolean ritual() { return anchor != null; }

    boolean inArena(Vec3 at) {
        return anchor != null && BossArenaPortals.withinCylinder(position(), at, ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    private void lock(ServerLevel server, boolean active) {
        if (active) BossArenaPortals.holdFollowing(server, this, ARENA_RADIUS, ARENA_HEIGHT);
        else BossArenaPortals.release(server, getUUID());
    }

    void dust(ServerLevel server, Vec3 at, int count, double width) {
        var floor = server.getBlockState(BlockPos.containing(at.add(0, -.18, 0)));
        if (floor.isAir()) return;
        if (!BuiltInRegistries.BLOCK.getKey(floor.getBlock()).getNamespace().equals("minecraft"))
            floor = Blocks.COBBLESTONE.defaultBlockState();
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), at.x, at.y + .15, at.z,
            count, width, .15, width, .07);
    }

    private void dawn(ServerLevel server) {
        lock(server, true);
        server.playSound(null, blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 5, .34f);
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 5, getZ(), 145, 6, 2.5, 6, .18);
        for (var player : server.getPlayers(p -> p.isAlive() && !p.isSpectator() && inArena(p.position()))) {
            Incapacitation2123.finishDeath(player, server.damageSources().mobAttack(this));
        }
        discard();
    }

    @Override public void die(DamageSource source) {
        super.die(source);
        if (!isAlive()) endEncounter();
    }

    @Override public void onRemovedFromWorld() {
        endEncounter();
        super.onRemovedFromWorld();
    }

    private void endEncounter() {
        if (anchor == null || !(level() instanceof ServerLevel server)) return;
        combat.suspend(this);
        lock(server, false);
        music.stop(getUUID());
        bar.removeAllPlayers();
    }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recent) {
        super.dropCustomDeathLoot(source, looting, recent);
        if (anchor == null || !recent || !(level() instanceof ServerLevel server)) return;
        spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, 3 + random.nextInt(4)));
        spawnAtLocation(new ItemStack(Items.BONE, 2 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(Content.BANDAGE.get(), 2));
        BossTrophies.first(server, this, BossMusicTrack.GIANT_ZOMBIE, GiantRitualContent.TROPHY);
    }

    @Override public int getExperienceReward() { return anchor == null ? super.getExperienceReward() : 85; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (anchor == null) return;
        tag.putLong("MiteGiantAnchor", anchor.asLong());
        tag.putLong("MiteGiantAppeared", appearedAt);
        tag.putLong("MiteGiantLastPresent", lastPresent);
        combat.save(tag);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("MiteGiantAnchor")) return;
        BossCombatTuning.applyHealth(this, MAX_HEALTH, false);
        anchor = BlockPos.of(tag.getLong("MiteGiantAnchor"));
        appearedAt = tag.getLong("MiteGiantAppeared");
        lastPresent = tag.contains("MiteGiantLastPresent") ? tag.getLong("MiteGiantLastPresent") : level().getGameTime();
        entityData.set(RITUAL, true);
        setMaxUpStep(1.45f);
        if (level().getGameTime() - appearedAt < INTRO) {
            entityData.set(ACTION_START, appearedAt);
            entityData.set(ACTION, EMERGE);
        }
        combat.load(tag);
    }
}
