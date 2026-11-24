package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SourceGiantSkeleton extends Skeleton {
    private static final double RADIUS = 34;
    private static final double HEIGHT = 16;
    private static final int INTERMISSION = 36;
    private static final int RETREAT_GRACE = 200;
    private static final int WAVE_RETRY = 15;
    private static final int WAVE_RECOVERY_LIMIT = 20 * 30;
    private static final String LEGACY_ABSENT_SINCE = "MiteGiantSkeletonAbsentSince";
    private static final String ABSENT_TICKS = "MiteGiantSkeletonAbsentTicks";
    private static final String UNSEALED = "MiteGiantSkeletonUnsealed";
    private static final EntityDataAccessor<Boolean> SEALED = SynchedEntityData.defineId(
        SourceGiantSkeleton.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> BOW_POSE = SynchedEntityData.defineId(
        SourceGiantSkeleton.class, EntityDataSerializers.BYTE);
    private static final byte NO_BOW_POSE = 0;
    static final byte LOW_BOW_POSE = 1;
    static final byte LOB_BOW_POSE = 2;
    static final String ENCOUNTER_ARROW = "MiteGiantSkeletonEncounterArrow";
    private final ServerBossEvent bar=new ServerBossEvent(Component.translatable("entity.mite_reforged.underworld_giant_skeleton"),
        BossEvent.BossBarColor.WHITE,BossEvent.BossBarOverlay.PROGRESS);
    private final BossMusicAudience music=new BossMusicAudience(BossMusicTrack.UNDERWORLD_GIANT_SKELETON);
    private final Set<UUID> participants=new HashSet<>();
    private final GiantSkeletonWaveMembers army = new GiantSkeletonWaveMembers();
    private final GiantSkeletonTactics tactics = new GiantSkeletonTactics();
    private final GiantSkeletonLateTactics lateTactics = new GiantSkeletonLateTactics();
    private final Set<UUID> introViewers=new HashSet<>();
    private final BossEncounterPresence presence = new BossEncounterPresence(4);
    private final GiantSkeletonBow bow = new GiantSkeletonBow();
    private long introStarted=-1;
    private int absentTicks = -1;
    private long legacyAbsentSince = -1;
    private int wave;
    private int gap=INTERMISSION;
    private long nextReinforcement;
    private boolean paid;
    private int recoveryDelay;
    private long recoveryFailedSince = -1;

    SourceGiantSkeleton(EntityType<? extends Skeleton> type,Level level) { super(type,level); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SEALED, true);
        entityData.define(BOW_POSE, NO_BOW_POSE);
    }

    void setBowPose(GiantSkeletonBow.Shot shot) {
        byte pose = NO_BOW_POSE;
        if (shot == GiantSkeletonBow.Shot.LOW) pose = LOW_BOW_POSE;
        else if (shot == GiantSkeletonBow.Shot.LOB) pose = LOB_BOW_POSE;
        entityData.set(BOW_POSE, pose);
    }

    byte bowPose() {
        return entityData.get(BOW_POSE);
    }

    boolean sealed() {
        return entityData.get(SEALED);
    }

    static AttributeSupplier.Builder attributes() {
        return Skeleton.createAttributes().add(Attributes.MAX_HEALTH,60)
            .add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,RADIUS);
    }

    static boolean summon(ServerLevel level,ServerPlayer owner,Vec3 site) {
        if(!WorldContentMode.rebuiltBosses(level) || level.getDifficulty()==Difficulty.PEACEFUL || Incapacitation2123.incapacitated(owner)
                || !level.getEntitiesOfClass(SourceGiantSkeleton.class,new AABB(site,site).inflate(68,32,68)).isEmpty())return false;
        var boss=GiantSkeletonContent.BOSS.get().create(level);
        if(boss==null)return false;
        boss.moveTo(site.x,site.y,site.z,owner.getYRot()+180,0);
        if(!level.noCollision(boss))return false;
        boss.setPersistenceRequired();
        boss.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        boss.setDropChance(EquipmentSlot.MAINHAND,0);
        if(!level.addFreshEntity(boss))return false;
        boss.presence.hold(level, boss);
        return true;
    }

    @Override protected void registerGoals() {}

    @Override public void reassessWeaponGoal() {}

    @Override protected boolean isSunBurnTick() { return false; }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        return size.height * .875f;
    }

    private boolean finale() { return !sealed(); }

    private void unseal(ServerLevel level) {
        entityData.set(SEALED, false);
        bow.readyAfter(level.getGameTime(), 4);
        bar.setProgress(getHealth() / getMaxHealth());
        level.playSound(null, blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.2f, .62f);
        var rubble = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        level.sendParticles(rubble, getX(), getY() + 1.1, getZ(), 46, .76, .67, .76, .18);
        level.sendParticles(rubble, getX(), getY() + 2.65, getZ(), 34, .53, .75, .53, .15);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && sealed()) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        }
        super.tick();
        if(!(level() instanceof ServerLevel level) || !isAlive() || isNoAi())return;
        long now = level.getGameTime();
        if(level.dimension()!=WorldEvents.UNDERWORLD || retreatExpired(now)) { discard(); return; }
        if (sealed() && wave == GiantSkeletonWavePlan.LAST_WAVE && army.isEmpty()) unseal(level);
        if(introStarted<0)introStarted=now;
        presence.hold(level, this);
        var players=level.getPlayers(this::fighter);
        if(players.isEmpty()) {
            if (absentTicks < 0) absentTicks = 0;
            clearWaveRecovery();
            getNavigation().stop();
            release(level);
            return;
        }
        absentTicks = -1;
        music.sync(level, getUUID(), players, tickCount % 40 == 0);
        if(tickCount%10==0)updateAudience(level,players);
        if(!finale()) {
            tickWave(level, players, now);
            return;
        }
        lateTactics.clear(level, this);
        bow.tick(level, this, players);
    }

    private boolean retreatExpired(long dimensionTime) {
        if (legacyAbsentSince >= 0) {
            // Convert the old absolute dimension clock only once, after the world is ticking.
            long elapsed = dimensionTime >= legacyAbsentSince ? dimensionTime - legacyAbsentSince : 0;
            absentTicks = (int) Math.min(elapsed, RETREAT_GRACE);
            legacyAbsentSince = -1;
        } else if (absentTicks >= 0 && absentTicks < RETREAT_GRACE) {
            absentTicks++;
        }
        return absentTicks >= RETREAT_GRACE;
    }

    private void updateAudience(ServerLevel level,List<ServerPlayer> players) {
        for(var player:List.copyOf(bar.getPlayers()))if(!players.contains(player))bar.removePlayer(player);
        for(var player:players) {
            bar.addPlayer(player);
            if(level.getGameTime()-introStarted<45 && introViewers.add(player.getUUID())) {
                GroundSeaBossIntroPacket.send(player,getUUID(),introStarted,GroundSeaBossIntroPacket.Kind.GIANT_SKELETON);
            }
        }
        int remaining = GiantSkeletonWavePlan.LAST_WAVE - wave + (army.isEmpty() ? 0 : 1);
        bar.setProgress(finale() ? getHealth() / getMaxHealth() : (float)remaining / GiantSkeletonWavePlan.LAST_WAVE);
    }

    private void tickWave(ServerLevel level, List<ServerPlayer> players, long now) {
        if (army.isEmpty()) {
            if (tactics.active()) BossArenaPortals.release(level, getUUID());
            tactics.clear();
            lateTactics.clear(level, this);
            if (--gap <= 0) startWave(level, players, now);
            return;
        }

        var missing = new ArrayList<UUID>();
        var defeated = new ArrayList<UUID>();
        var lead = nearest(players, this);
        boolean planOpening = tactics.awaitingOpening(wave, now, army.entries().size(), !army.reserves().isEmpty());
        boolean openingWasActive = tactics.active();
        boolean planLate = lateTactics.shouldCheck(wave, tactics.waveAge(now), now, openingWasActive);
        var available = planOpening || planLate ? new ArrayList<PathfinderMob>() : null;
        for (var entry : army.entries().entrySet()) {
            var id = entry.getKey();
            var slot = entry.getValue();
            var servant = waveMember(level, id);
            if (servant != null && !servant.isAlive() && servant.getPose() == Pose.DYING) {
                defeated.add(id);
                continue;
            }
            if (servant == null || !servant.isAlive()) {
                if (slot.missing(level, chunkPosition(), now)) missing.add(id);
                continue;
            }

            GiantSkeletonContent.updateHome(servant, this);
            if (servant.distanceToSqr(this) > 31 * 31) {
                var site = GiantSkeletonArena.waveSites(level, this, position()).stream()
                    .filter(at -> players.stream().noneMatch(player -> player.distanceToSqr(at) < 9))
                    .filter(at -> level.noCollision(servant, servant.getBoundingBox().move(at.subtract(servant.position()))))
                    .findFirst().orElse(null);
                if (site != null) servant.teleportTo(site.x, site.y, site.z);
            }
            slot.observe(servant);
            if (available != null && servant instanceof PathfinderMob mob) available.add(mob);
            if (!players.contains(servant.getTarget())) {
                servant.setTarget(waveTarget(players, lead, servant, slot.role()));
            }
        }
        defeated.forEach(this::defeated);
        tactics.tick(this, level, lead, planOpening ? available : List.of(), wave, now);
        if (tactics.active()) holdAttackPortals(level);
        else if (openingWasActive) BossArenaPortals.release(level, getUUID());
        lateTactics.tick(this, level, wave, now);
        if (planLate) lateTactics.select(this, level, lead, available, wave, now);
        if (now >= nextReinforcement && !army.reserves().isEmpty()) {
            nextReinforcement = now + WAVE_RETRY;
            if (deployReserves(level, players, waveSites(level, players), wave)) clearWaveRecovery();
            else if (army.entries().isEmpty()) waveRecoveryFailed(now);
        }
        if (missing.isEmpty()) {
            if (!army.entries().isEmpty() || army.reserves().isEmpty()) clearWaveRecovery();
            return;
        }
        if (recoveryDelay > 0) {
            --recoveryDelay;
            return;
        }
        replaceMissing(level, players, missing, now);
    }

    private Mob waveMember(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof Mob servant && GiantSkeletonContent.belongsTo(servant, this) ? servant : null;
    }

    void waveArrowSpawned(net.minecraft.world.entity.projectile.AbstractArrow arrow) {
        if (sealed() && level() instanceof ServerLevel level) lateTactics.observeArrow(arrow, level.getGameTime());
    }

    boolean includesWaveMember(UUID id) {
        return army.contains(id);
    }

    void trackWaveMember(UUID id, ChunkPos chunk) {
        army.locate(id, chunk);
    }

    private ArrayList<Vec3> waveSites(ServerLevel level, List<ServerPlayer> players) {
        var sites = new ArrayList<>(GiantSkeletonArena.waveSites(level, this, position()));
        sites.removeIf(site -> players.stream().anyMatch(player -> player.distanceToSqr(site) < 9)
            || !level.getEntitiesOfClass(Mob.class, new AABB(site, site).inflate(1.15, 1.4, 1.15),
                Mob::isAlive).isEmpty());
        return sites;
    }

    private void startWave(ServerLevel level, List<ServerPlayer> players, long now) {
        var sites = waveSites(level, players);
        int next = wave + 1;
        if (sites.size() < 3) {
            gap = WAVE_RETRY;
            waveRecoveryFailed(now);
            return;
        }
        int count = GiantSkeletonWavePlan.units(next, players.size());
        army.reserves().addAll(GiantSkeletonWavePlan.roster(next, count, getRandom()));
        wave = next;
        nextReinforcement = now + WAVE_RETRY;
        if (deployReserves(level, players, sites, wave)) clearWaveRecovery();
        else waveRecoveryFailed(now);
        level.playSound(null, blockPosition(), SoundEvents.SKELETON_HORSE_AMBIENT, SoundSource.HOSTILE, 2, .66f);
    }

    private boolean deployReserves(ServerLevel level, List<ServerPlayer> players, ArrayList<Vec3> sites, int waveNumber) {
        if (sites.isEmpty()) return false;
        var lead = nearest(players, this);
        Vec3 firstSupport = army.entries().entrySet().stream()
            .filter(entry -> entry.getValue().role() == GiantSkeletonWavePlan.Role.SUPPORT)
            .map(entry -> waveMember(level, entry.getKey())).filter(java.util.Objects::nonNull)
            .filter(Mob::isAlive).map(Mob::position).findFirst().orElse(null);
        boolean deployed = false;
        var reserves = army.reserves().iterator();
        while (reserves.hasNext() && !sites.isEmpty()) {
            var recruit = reserves.next();
            while (!sites.isEmpty()) {
                var site = chooseSite(level, sites, lead, recruit, firstSupport);
                var servant = spawnServant(level, players, lead, recruit, site, waveNumber);
                if (servant == null) continue;
                army.add(servant);
                reserves.remove();
                if (firstSupport == null && recruit.role() == GiantSkeletonWavePlan.Role.SUPPORT) firstSupport = site;
                sites.removeIf(other -> other.distanceToSqr(site) < 2.25);
                deployed = true;
                break;
            }
        }
        return deployed;
    }

    private void replaceMissing(ServerLevel level, List<ServerPlayer> players, List<UUID> missing, long now) {
        var sites = waveSites(level, players);
        var lead = nearest(players, this);
        Vec3 existingSupport = army.entries().entrySet().stream()
            .filter(entry -> entry.getValue().role() == GiantSkeletonWavePlan.Role.SUPPORT)
            .map(entry -> waveMember(level, entry.getKey())).filter(java.util.Objects::nonNull)
            .filter(Mob::isAlive).map(Mob::position).findFirst().orElse(null);
        boolean anyReplaced = false;
        for (var id : missing) {
            var slot = army.entries().get(id);
            if (slot == null || sites.isEmpty()) continue;
            var type = slot.type();
            if (type == null) type = GiantSkeletonWavePlan.draw(wave, getRandom());
            var recruit = new GiantSkeletonWavePlan.Recruit(type, slot.role());
            var site = chooseSite(level, sites, lead, recruit, existingSupport);
            var replacement = spawnServant(level, players, lead, recruit, site, wave);
            if (replacement == null) continue;
            army.replace(id, replacement);
            anyReplaced = true;
        }
        if (anyReplaced) clearWaveRecovery();
        else waveRecoveryFailed(now);
        recoveryDelay = WAVE_RETRY;
    }

    private Vec3 chooseSite(ServerLevel level, ArrayList<Vec3> sites, ServerPlayer target,
                            GiantSkeletonWavePlan.Recruit recruit, Vec3 existingSupport) {
        if (recruit.role() == GiantSkeletonWavePlan.Role.FILL) return sites.remove(getRandom().nextInt(sites.size()));
        Vec3 front = BossAttackGeometry.flatDirection(target.position().subtract(position()));
        Vec3 side = new Vec3(-front.z, 0, front.x);
        double flank = getRandom().nextBoolean() ? 1 : -1;
        var chosen = sites.stream().min(Comparator.comparingDouble(site -> {
            double distance = site.distanceTo(target.position());
            return switch (recruit.role()) {
                case SUPPORT -> Math.abs(12 - distance) - .6 * (site.y - position().y)
                    + (BossAttackGeometry.visible(level, this, site.add(0, 1.65, 0), target.getEyePosition()) ? 0 : 8)
                    + (existingSupport == null ? 0 : Math.max(0, 7 - site.distanceTo(existingSupport)) * 1.3);
                case FLANK -> Math.abs(9 - distance) - flank * site.subtract(position()).dot(side) * .65;
                default -> distance + Math.abs(site.y - target.getY()) * 2;
            };
        })).orElseThrow();
        sites.remove(chosen);
        return chosen;
    }

    private Mob spawnServant(ServerLevel level, List<ServerPlayer> players, ServerPlayer lead, GiantSkeletonWavePlan.Recruit recruit,
                             Vec3 site, int waveNumber) {
        var servant = recruit.type().create(level);
        if (servant == null) return null;
        servant.moveTo(site.x, site.y, site.z, getRandom().nextFloat() * 360, 0);
        if (!level.noCollision(servant)) return null;
        servant.setPersistenceRequired();
        GiantSkeletonContent.assign(servant, this);
        servant.getPersistentData().putString(GiantSkeletonWavePlan.ROLE_TAG, recruit.role().name());
        if (servant instanceof AbstractSkeleton skeleton && !servant.getMainHandItem().is(Items.BOW)) {
            servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(
                servant.getType() == EntityType.WITHER_SKELETON ? Items.IRON_SWORD : Items.BOW));
            skeleton.reassessWeaponGoal();
        }
        equipServant(servant, recruit, waveNumber);
        servant.setTarget(waveTarget(players, lead, servant, recruit.role()));
        return level.addFreshEntity(servant) ? servant : null;
    }

    private static ServerPlayer waveTarget(List<ServerPlayer> players, ServerPlayer lead, Mob servant,
                                           GiantSkeletonWavePlan.Role role) {
        if ((role == GiantSkeletonWavePlan.Role.FRONT || role == GiantSkeletonWavePlan.Role.SUPPORT
                || role == GiantSkeletonWavePlan.Role.CAPTAIN) && servant.hasLineOfSight(lead)) return lead;
        return nearest(players, servant);
    }

    private static void equipServant(Mob servant, GiantSkeletonWavePlan.Recruit recruit, int wave) {
        switch (recruit.role()) {
            case FRONT -> {
                if (wave >= 5) servant.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                if (wave >= 6 && !recruit.ranged()) servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
            case FLANK -> {
                servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                if (wave >= 8) servant.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            }
            case SUPPORT -> {
                if (wave >= 7) servant.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
            }
            case CAPTAIN -> {
                servant.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Content.get("ancient_metal_chestplate")));
                servant.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Content.get("ancient_metal_helmet")));
                servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Content.get("ancient_metal_sword")));
            }
            default -> { }
        }
        servant.setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) servant.setDropChance(slot, 0);
    }

    private static ServerPlayer nearest(List<ServerPlayer> players, Mob servant) {
        return players.stream().min(Comparator.comparingDouble(servant::distanceToSqr)).orElseThrow();
    }

    private void waveRecoveryFailed(long now) {
        if (recoveryFailedSince < 0 || recoveryFailedSince > now) recoveryFailedSince = now;
        if (now - recoveryFailedSince >= WAVE_RECOVERY_LIMIT) discard();
    }

    private void clearWaveRecovery() {
        recoveryFailedSince = -1;
        recoveryDelay = 0;
    }

    void defeated(UUID id) {
        if (!army.defeated(id) || !army.isEmpty()) return;
        gap = INTERMISSION;
        clearWaveRecovery();
    }

    void credit(ServerPlayer player) {
        if(fighter(player))participants.add(player.getUUID());
    }

    void holdAttackPortals(ServerLevel level) {
        BossArenaPortals.holdFollowing(level, this, RADIUS, HEIGHT);
    }

    boolean fighter(ServerPlayer player) {
        return player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&!Incapacitation2123.incapacitated(player)
            && BossArenaPortals.withinCylinder(position(),player.position(),RADIUS*RADIUS,HEIGHT);
    }

    @Override public boolean hurt(DamageSource source,float amount) {
        if(!finale())return false;
        float previousHealth = getHealth();
        boolean damaged=super.hurt(source,amount);
        if (damaged && getHealth() < previousHealth && source.getEntity() instanceof ServerPlayer player) credit(player);
        return damaged;
    }

    @Override public boolean isPushable() {
        return !sealed() && super.isPushable();
    }

    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent) {
        if(!finale() || paid || !(level() instanceof ServerLevel level))return;
        paid=true;
        if(source.getEntity() instanceof ServerPlayer player && fighter(player))participants.add(player.getUUID());
        BossRewardDelivery.settle(level, getUUID(), participants, this::fighter,
            (owner, offline) -> victoryRewards(level, owner, offline));
    }

    boolean canRecordRewardDeparture(ServerPlayer player) {
        return !paid && participants.contains(player.getUUID()) && fighter(player);
    }

    private List<ItemStack> victoryRewards(ServerLevel level, UUID owner, boolean offline) {
        var bones = new ItemStack(GiantSkeletonContent.BONE.get(), GiantSkeletonFirsts.bonesFor(level, owner, offline));
        var random = getRandom();
        if (random.nextFloat() >= .45f) return List.of(bones);
        var loot = Content.ALL_ITEMS.entrySet().stream().filter(entry -> entry.getKey().startsWith("ancient_metal_"))
            .map(entry -> new ItemStack(entry.getValue().get())).filter(ItemStack::isDamageableItem).toList();
        if (loot.isEmpty()) return List.of(bones);
        var reward = loot.get(random.nextInt(loot.size()));
        reward.setDamageValue(reward.getMaxDamage() - Math.max(1, (int)(reward.getMaxDamage() * (.15 + random.nextDouble() * .25))));
        return List.of(bones, reward);
    }

    private void release(ServerLevel level) {
        tactics.clear();
        lateTactics.clear(level, this);
        bow.cancel(level, this);
        music.stop(getUUID());
        bar.removeAllPlayers();
        BossArenaPortals.release(level,getUUID());
    }

    @Override public boolean canChangeDimensions() { return false; }

    @Override public void die(DamageSource source) {
        super.die(source);
        if (!isAlive() && level() instanceof ServerLevel level) {
            release(level);
            presence.release(level, getUUID());
        }
    }

    @Override public void remove(RemovalReason reason) {
        if(level() instanceof ServerLevel level) {
            release(level);
            presence.release(level, getUUID());
            if (reason.shouldDestroy()) {
                BossRewardDelivery.close(level, getUUID());
                for (var id : army.entries().keySet()) {
                    var servant = waveMember(level, id);
                    if (servant != null) servant.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (legacyAbsentSince >= 0) {
            tag.putLong(LEGACY_ABSENT_SINCE, legacyAbsentSince);
            tag.remove(ABSENT_TICKS);
        } else {
            tag.remove(LEGACY_ABSENT_SINCE);
            if (absentTicks >= 0) tag.putInt(ABSENT_TICKS, absentTicks);
            else tag.remove(ABSENT_TICKS);
        }
        tag.putInt("MiteGiantSkeletonWave",wave);
        tag.putBoolean(UNSEALED, finale());
        tag.putInt("MiteGiantSkeletonGap",gap);
        tag.putBoolean("MiteGiantSkeletonPaid",paid);
        tag.putLong("MiteGiantSkeletonIntro",introStarted);
        army.save(tag);
        writeIds(tag,"MiteGiantSkeletonPlayers",participants);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boolean hasElapsed = tag.contains(ABSENT_TICKS, Tag.TAG_INT);
        absentTicks = hasElapsed ? net.minecraft.util.Mth.clamp(tag.getInt(ABSENT_TICKS), -1, RETREAT_GRACE) : -1;
        legacyAbsentSince = !hasElapsed && tag.contains(LEGACY_ABSENT_SINCE, Tag.TAG_LONG)
            ? Math.max(-1L, tag.getLong(LEGACY_ABSENT_SINCE)) : -1;
        wave=net.minecraft.util.Mth.clamp(tag.getInt("MiteGiantSkeletonWave"),0,GiantSkeletonWavePlan.LAST_WAVE);
        gap=net.minecraft.util.Mth.clamp(tag.getInt("MiteGiantSkeletonGap"),0,INTERMISSION);
        paid=tag.getBoolean("MiteGiantSkeletonPaid");
        introStarted=tag.contains("MiteGiantSkeletonIntro")?tag.getLong("MiteGiantSkeletonIntro"):-1;
        army.load(wave > 0 ? tag : new CompoundTag());
        boolean cleared = wave == GiantSkeletonWavePlan.LAST_WAVE && army.isEmpty();
        entityData.set(SEALED, !(cleared && (!tag.contains(UNSEALED) || tag.getBoolean(UNSEALED))));
        readIds(tag,"MiteGiantSkeletonPlayers",participants);
    }

    private static void writeIds(CompoundTag tag,String key,Set<UUID> values) {
        var entries=new net.minecraft.nbt.ListTag();
        values.stream().sorted().forEach(value->{
            var row=new CompoundTag();
            row.putUUID("Id",value);
            entries.add(row);
        });
        tag.put(key,entries);
    }

    private static void readIds(CompoundTag tag,String key,Set<UUID> out) {
        out.clear();
        var entries=tag.getList(key,Tag.TAG_COMPOUND);
        for(int i=0;i<entries.size();i++)if(entries.getCompound(i).hasUUID("Id"))out.add(entries.getCompound(i).getUUID("Id"));
    }
}
