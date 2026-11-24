package dev.koikaster.mite;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** EntityRevenant and its ordinary tw/og equipment and death paths. */
public final class SourceRevenant extends Mobs.Undead implements SourcePigmanGear.Host, SourceZombieLoot.Host {
    private static final String PICKED_HELD = "picked_up_a_held_item";

    private final SourcePigmanGear gear = new SourcePigmanGear(this);
    private boolean pickedUpHeld;

    public SourceRevenant(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    static AttributeSupplier attributes() {
        return OriginalHumanoidProfiles.profile("revenant").builder()
            .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0).build();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData data, CompoundTag tag) {
        if (GiantSkeletonContent.isServant(this)) return super.finalizeSpawn(level, difficulty, reason, data, tag);

        float leaderChance = SourceZombieBody.localSpawnFactor(level, blockPosition()) * .05f;
        var random = getRandom();
        // tw.az draws once before EntityRevenant.az replaces the same base.
        random.nextDouble();
        getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(random.nextDouble() * (double) .1f);
        SourceSpawnInitialization.apply(this, reason, random.nextGaussian() * .05);
        setCanPickUpLoot(true);
        if (data == null) {
            var worldRandom = level.getRandom();
            data = new SourceZombieBody.Group(worldRandom.nextFloat() < .05f, worldRandom.nextFloat() < .05f);
        }

        var world = level.getLevel();
        boolean datedWeapons = SourceTournament.mode(world.getServer()) == null;
        int day = datedWeapons ? SourceMoonCycle.dayOfWorld(world.getGameTime()) : 1;
        equip(EquipmentSlot.MAINHAND, weaponId(random, day));
        equip(EquipmentSlot.FEET, "rusted_iron_boots");
        equip(EquipmentSlot.LEGS, "rusted_iron_leggings");
        equip(EquipmentSlot.CHEST, "rusted_iron_chestplate");
        equip(EquipmentSlot.HEAD, "rusted_iron_helmet");
        modify(Attributes.KNOCKBACK_RESISTANCE, "Random spawn bonus", random.nextDouble() * (double) .05f,
            AttributeModifier.Operation.ADDITION);
        if (random.nextFloat() < leaderChance) {
            modify(Attributes.SPAWN_REINFORCEMENTS_CHANCE, "Leader zombie bonus", random.nextDouble() * .25 + .5,
                AttributeModifier.Operation.ADDITION);
            modify(Attributes.MAX_HEALTH, "Leader zombie bonus", random.nextDouble() * 3 + 1,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
        return data;
    }

    private void modify(Attribute attribute, String name, double value, AttributeModifier.Operation operation) {
        getAttribute(attribute).addPermanentModifier(new AttributeModifier(name, value, operation));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (GiantSkeletonContent.isServant(this)) return super.doHurtTarget(target);
        return SourceMobStrikeNative.attack(this, target);
    }

    void onPlayerMeleeAttack(Player attacker) {
        if (level().isClientSide || GiantSkeletonContent.isServant(this)
                || SourceKnockbackNative.protectsHand(attacker)) return;
        float chance = getTarget() == attacker && !SourceKnockbackNative.weakStrike(attacker) ? .125f : 0;
        if (getRandom().nextFloat() < chance) {
            var type = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ClassicDefense.HAND_DAMAGE);
            attacker.hurt(new DamageSource(type, this), 1);
        }
    }

    static String weaponId(RandomSource random, int day) {
        int weight = day >= 20 ? 4 : day >= 10 ? 3 : 2;
        return switch (random.nextInt(weight)) {
            case 2 -> "rusted_iron_battleaxe";
            case 3 -> "rusted_iron_warhammer";
            default -> "rusted_iron_sword";
        };
    }

    private void equip(EquipmentSlot slot, String id) {
        setItemSlot(slot, SourceZombieBody.randomEquipment(getRandom(), Content.get(id), true));
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return GiantSkeletonContent.isServant(this) && super.wantsToPickUp(stack);
    }

    @Override
    protected void runUndeadPickup() {
        if (!GiantSkeletonContent.isServant(this)) gear.pickup();
        super.runUndeadPickup();
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public int getExperienceReward() {
        return GiantSkeletonContent.isServant(this) ? super.getExperienceReward() : 15;
    }

    @Override
    protected void dropExperience() {
        if (GiantSkeletonContent.isServant(this)) super.dropExperience();
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved() && deathTime == 19 && !GiantSkeletonContent.isServant(this)) {
            super.dropExperience();
        }
        super.tickDeath();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recent) {
        if (GiantSkeletonContent.isServant(this)) {
            super.dropCustomDeathLoot(source, looting, recent);
            return;
        }
        if (SourceFallLoot1194.normalPart(this)) {
            SourceZombieLoot.drop(this, recent, looting, massiveFall());
        }
        gear.death(recent, looting);
        dropModernOffhand(looting, recent);
    }

    /** An old or custom save can already carry a modern offhand stack. */
    private void dropModernOffhand(int looting, boolean recent) {
        var stack = getOffhandItem();
        float chance = getEquipmentDropChance(EquipmentSlot.OFFHAND);
        boolean guaranteed = chance > 1;
        if (stack.isEmpty() || EnchantmentHelper.hasVanishingCurse(stack) || (!recent && !guaranteed)
                || Math.max(getRandom().nextFloat() - looting * .01f, 0) >= chance) return;

        if (!guaranteed && stack.isDamageableItem()) {
            int maximum = stack.getMaxDamage();
            stack.setDamageValue(maximum - getRandom().nextInt(1 + getRandom().nextInt(Math.max(maximum - 3, 1))));
        }
        spawnAtLocation(stack);
        setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (pickedUpHeld) tag.putBoolean(PICKED_HELD, true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pickedUpHeld = tag.getBoolean(PICKED_HELD);
    }

    @Override public Mob actor() { return this; }
    @Override public float dropChance(int slot) { return getEquipmentDropChance(SourcePigmanGear.slot(slot)); }
    @Override public void dropChance(int slot, float value) { setDropChance(SourcePigmanGear.slot(slot), value); }
    @Override public boolean massiveFall() { return SourceFallLoot1194.massive(this); }
    @Override public boolean pickedUpHeld() { return pickedUpHeld; }
    @Override public void pickedUpHeld(boolean value) { pickedUpHeld = value; }

    @Override public boolean villager() { return false; }
    @Override public boolean revenant() { return true; }
    @Override public float nextFloat() { return getRandom().nextFloat(); }
    @Override public int nextInt(int bound) { return getRandom().nextInt(bound); }
    @Override public void drop(SourceZombieLoot.Item item) { SourceZombieBody.dropZombieLoot(this, item); }
}
