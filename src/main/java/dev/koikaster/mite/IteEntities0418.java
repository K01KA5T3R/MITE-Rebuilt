package dev.koikaster.mite;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MITE-ITE 2.0.30 new entity types: Zombie Lord, Zombie Pigman Lord,
 * Annihilation Skeleton, Wandering Witch, Ancient Dragon, LargeFireballNB, RideMarker.
 */
public final class IteEntities0418 {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MiteReforged.ID);

    public static final Set<String> ALL_ENTITY_IDS = Set.of(
        "ite_zombie_lord",
        "ite_zombie_pigman_lord",
        "annihilation_skeleton",
        "wandering_witch",
        "ancient_dragon",
        "large_fireball_nb",
        "ride_marker"
    );

    public static final RegistryObject<EntityType<ZombieLordEntity>> ZOMBIE_LORD = ENTITIES.register(
        "ite_zombie_lord",
        () -> EntityType.Builder.<ZombieLordEntity>of(ZombieLordEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.8F)
            .build(MiteReforged.ID + ":ite_zombie_lord")
    );
    public static final RegistryObject<EntityType<ZombiePigmanLordEntity>> ZOMBIE_PIGMAN_LORD = ENTITIES.register(
        "ite_zombie_pigman_lord",
        () -> EntityType.Builder.<ZombiePigmanLordEntity>of(ZombiePigmanLordEntity::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.6F, 1.8F)
            .build(MiteReforged.ID + ":ite_zombie_pigman_lord")
    );
    public static final RegistryObject<EntityType<AnnihilationSkeletonEntity>> ANNIHILATION_SKELETON = ENTITIES.register(
        "annihilation_skeleton",
        () -> EntityType.Builder.<AnnihilationSkeletonEntity>of(AnnihilationSkeletonEntity::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(0.6F, 1.8F)
            .build(MiteReforged.ID + ":annihilation_skeleton")
    );
    public static final RegistryObject<EntityType<WanderingWitchEntity>> WANDERING_WITCH = ENTITIES.register(
        "wandering_witch",
        () -> EntityType.Builder.<WanderingWitchEntity>of(WanderingWitchEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .build(MiteReforged.ID + ":wandering_witch")
    );
    public static final RegistryObject<EntityType<AncientDragonEntity>> ANCIENT_DRAGON = ENTITIES.register(
        "ancient_dragon",
        () -> EntityType.Builder.<AncientDragonEntity>of(AncientDragonEntity::new, MobCategory.MONSTER)
            .fireImmune()
            .sized(6.0F, 10.0F)
            .build(MiteReforged.ID + ":ancient_dragon")
    );
    public static final RegistryObject<EntityType<LargeFireballNbEntity>> LARGE_FIREBALL_NB = ENTITIES.register(
        "large_fireball_nb",
        () -> EntityType.Builder.<LargeFireballNbEntity>of(LargeFireballNbEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(MiteReforged.ID + ":large_fireball_nb")
    );
    public static final RegistryObject<EntityType<RideMarkerEntity>> RIDE_MARKER = ENTITIES.register(
        "ride_marker",
        () -> EntityType.Builder.<RideMarkerEntity>of(RideMarkerEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .noSave()
            .build(MiteReforged.ID + ":ride_marker")
    );

    static {
        egg("ite_zombie_lord", ZOMBIE_LORD, 0x2B4A32, 0x7A2CE0);
        egg("ite_zombie_pigman_lord", ZOMBIE_PIGMAN_LORD, 0x8F433E, 0x9F48EE);
        egg("annihilation_skeleton", ANNIHILATION_SKELETON, 0x1D1A29, 0xB832F5);
        egg("wandering_witch", WANDERING_WITCH, 0x2E203C, 0x8CE37A);
        egg("ancient_dragon", ANCIENT_DRAGON, 0x1A1822, 0xC43B3B);
    }

    private static <T extends Mob> void egg(String id, RegistryObject<EntityType<T>> type, int bg, int spot) {
        Content.item(id + "_spawn_egg", () -> new ForgeSpawnEggItem(type, bg, spot, new Item.Properties()));
    }

    public static void init() {}

    public static boolean isIteEntity(String path) {
        return path != null && ALL_ENTITY_IDS.contains(path);
    }

    public static boolean canBeTargeted(LivingEntity target, LivingEntity attacker) {
        if (target instanceof WanderingWitchEntity) return false;
        return true;
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ZOMBIE_LORD.get(), ZombieLordEntity.attributes().build());
        event.put(ZOMBIE_PIGMAN_LORD.get(), ZombieLordEntity.attributes().build());
        event.put(ANNIHILATION_SKELETON.get(), AnnihilationSkeletonEntity.attributes().build());
        event.put(WANDERING_WITCH.get(), WanderingWitchEntity.attributes().build());
        event.put(ANCIENT_DRAGON.get(), AncientDragonEntity.attributes().build());
    }

    public static void placements(SpawnPlacementRegisterEvent event) {
        event.register(
            ANNIHILATION_SKELETON.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, rand) -> level.getFluidState(pos).isEmpty() && level.getDifficulty() != Difficulty.PEACEFUL,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }

    /** Full Vibranium-clad Zombie Lord (50 + day/14 HP, 12 + day/48 ATK, drops 3 diamonds, 60t summoned FX). */
    public static class ZombieLordEntity extends Monster {
        public ZombieLordEntity(EntityType<? extends ZombieLordEntity> type, Level level) {
            super(type, level);
            this.xpReward = 24;
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
            this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
            this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        public boolean canBeDisarmed() { return false; }

        @Override
        public boolean isOnFire() { return false; }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return source.is(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
        }

        @Override
        public int getMaxSpawnClusterSize() { return 1; }

        @Override
        public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
            SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
            equipVibraniumLoadout(level.getLevel());
            return result;
        }

        public void equipVibraniumLoadout(Level level) {
            int day = IteMobScaling0418.getCappedIteDay(level);
            ItemStack sword = new ItemStack(Content.get("vibranium_sword"));
            ItemStack helm = new ItemStack(Content.get("vibranium_helmet"));
            ItemStack chest = new ItemStack(Content.get("vibranium_chestplate"));
            ItemStack legs = new ItemStack(Content.get("vibranium_leggings"));
            ItemStack boots = new ItemStack(Content.get("vibranium_boots"));
            if (day > 64) {
                for (ItemStack st : List.of(sword, helm, chest, legs, boots)) {
                    IteMobScaling0418.addRandomEnchantments(day, st, this.getRandom(), true);
                }
            }
            setItemSlot(EquipmentSlot.MAINHAND, sword);
            setItemSlot(EquipmentSlot.HEAD, helm);
            setItemSlot(EquipmentSlot.CHEST, chest);
            setItemSlot(EquipmentSlot.LEGS, legs);
            setItemSlot(EquipmentSlot.FEET, boots);
            for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0.0F);
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 0, false, false));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 0, false, false));
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level() instanceof ServerLevel server && this.tickCount % 60 == 0) {
                server.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.0D, getZ(), 6, 0.35D, 0.6D, 0.35D, 0.02D);
            }
        }

        @Override
        protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recent) {
            super.dropCustomDeathLoot(source, looting, recent);
            if (recent) {
                spawnAtLocation(new ItemStack(Items.DIAMOND, 3));
            }
        }
    }

    public static class ZombiePigmanLordEntity extends ZombieLordEntity {
        public ZombiePigmanLordEntity(EntityType<? extends ZombiePigmanLordEntity> type, Level level) {
            super(type, level);
        }

        @Override
        protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT; }
        @Override
        protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIFIED_PIGLIN_HURT; }
        @Override
        protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIFIED_PIGLIN_DEATH; }
    }

    /** High-knockback Vibranium-dagger skeleton immune to fall/fire/potions with combat-despawn timer. */
    public static class AnnihilationSkeletonEntity extends Skeleton {
        public static final int DEFAULT_DESPAWN_TICKS = 1200;
        private boolean attackedByPlayer;
        private int despawnCount;

        public AnnihilationSkeletonEntity(EntityType<? extends AnnihilationSkeletonEntity> type, Level level) {
            super(type, level);
            this.xpReward = 25;
            this.attackedByPlayer = false;
            this.despawnCount = 0;
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
        }

        @Override
        public void reassessWeaponGoal() {}

        @Override
        protected boolean isSunBurnTick() { return false; }

        @Override
        public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

        @Override
        public boolean canBeAffected(MobEffectInstance effect) { return false; }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(source);
        }

        public void markAttackedByPlayer() { this.attackedByPlayer = true; }
        public boolean isAttackedByPlayer() { return this.attackedByPlayer; }
        public int despawnTicks() { return this.despawnCount; }
        public int despawnThreshold() { return DEFAULT_DESPAWN_TICKS; }

        @Override
        public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
            SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
            ItemStack dagger = new ItemStack(Content.get("vibranium_dagger"));
            dagger.enchant(Enchantments.KNOCKBACK, 5);
            setItemSlot(EquipmentSlot.MAINHAND, dagger);
            setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            return out;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (source.getEntity() instanceof Player) {
                this.attackedByPlayer = true;
            }
            return super.hurt(source, amount);
        }

        @Override
        public void setTarget(LivingEntity target) {
            if (target instanceof Player) {
                this.attackedByPlayer = true;
            }
            super.setTarget(target);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level() instanceof ServerLevel server) {
                if (this.tickCount % 20 == 0) {
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 2, 0.25D, 0.4D, 0.25D, 0.01D);
                }
                if (this.attackedByPlayer) {
                    this.despawnCount++;
                    if (this.despawnCount >= despawnThreshold()) {
                        server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 12, 0.4D, 0.5D, 0.4D, 0.04D);
                        this.discard();
                    }
                }
            }
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putBoolean("AttackedByPlayer", this.attackedByPlayer);
            tag.putInt("DespawnCount", this.despawnCount);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            this.attackedByPlayer = tag.getBoolean("AttackedByPlayer");
            this.despawnCount = tag.getInt("DespawnCount");
        }
    }

    /** Wandering Witch with AvoidPlayer, cap-8 Vampire Bat summoning, and special delayed Curse. */
    public static class WanderingWitchEntity extends Witch {
        public static final int MAX_BATS_COUNT = 8;
        private int summonCooldown = 60;

        public WanderingWitchEntity(EntityType<? extends WanderingWitchEntity> type, Level level) {
            super(type, level);
            this.xpReward = 22;
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            this.goalSelector.removeAllGoals(g -> true);
            this.targetSelector.removeAllGoals(g -> true);
            this.goalSelector.addGoal(1, new FloatGoal(this));
            this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 3.0F, 1.1D, 1.4D));
            this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0D, 42, 10.0F));
            this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
            this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
            this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        public int aliveOwnedBatsCount() {
            return this.level().getEntitiesOfClass(
                SourceVampireBat.class,
                this.getBoundingBox().inflate(32.0D),
                bat -> bat.isAlive() && this.getUUID().toString().equals(bat.getPersistentData().getString("MiteIteOwnerWitch"))
            ).size();
        }

        public void trySummonVampireBat(ServerLevel server) {
            if (aliveOwnedBatsCount() >= MAX_BATS_COUNT) return;
            SourceVampireBat bat = Mobs.BAT.get().create(server);
            if (bat == null) return;
            RandomSource r = this.getRandom();
            bat.moveTo(getX() + (r.nextDouble() - 0.5D) * 2.0D, getY() + 1.2D, getZ() + (r.nextDouble() - 0.5D) * 2.0D, r.nextFloat() * 360.0F, 0.0F);
            bat.getPersistentData().putString("MiteIteOwnerWitch", this.getUUID().toString());
            server.addFreshEntity(bat);
        }

        public void cursePlayerSpecial(ServerPlayer target) {
            if (target == null || !(this.level() instanceof ServerLevel sl)) return;
            if (OriginalCurseEffects.id(target) != 0) return;
            OriginalCurses.get(sl).tryCurse(target, this);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (this.level() instanceof ServerLevel server && this.getTarget() instanceof ServerPlayer player) {
                if (--this.summonCooldown <= 0) {
                    this.summonCooldown = 80;
                    trySummonVampireBat(server);
                    cursePlayerSpecial(player);
                }
            }
        }
    }

    /** Ancient Dragon entity (6.0F x 10.0F, 600 HP, 15 ATK, 0.23 speed) firing non-griefing LargeFireballNB. */
    public static class AncientDragonEntity extends Monster {
        public AncientDragonEntity(EntityType<? extends AncientDragonEntity> type, Level level) {
            super(type, level);
            this.xpReward = 200;
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(0, new FloatGoal(this));
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
            this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return source.is(DamageTypeTags.IS_FIRE) || super.isInvulnerableTo(source);
        }
    }

    public static class LargeFireballNbEntity extends LargeFireball {
        public LargeFireballNbEntity(EntityType<? extends LargeFireballNbEntity> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onHit(HitResult result) {
            if (!this.level().isClientSide) {
                if (result instanceof EntityHitResult ehr) {
                    ehr.getEntity().hurt(this.damageSources().fireball(this, this.getOwner()), 10.0F);
                }
                this.level().explode(this, getX(), getY(), getZ(), 2.0F, false, Level.ExplosionInteraction.NONE);
                this.discard();
            }
        }

        @Override
        public Packet<ClientGamePacketListener> getAddEntityPacket() {
            return NetworkHooks.getEntitySpawningPacket(this);
        }
    }

    public static class RideMarkerEntity extends Entity {
        public RideMarkerEntity(EntityType<?> type, Level level) {
            super(type, level);
            this.noPhysics = true;
        }

        @Override
        protected void defineSynchedData() {}

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {}

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {}

        @Override
        public Packet<ClientGamePacketListener> getAddEntityPacket() {
            return NetworkHooks.getEntitySpawningPacket(this);
        }
    }

    private IteEntities0418() {}
}
