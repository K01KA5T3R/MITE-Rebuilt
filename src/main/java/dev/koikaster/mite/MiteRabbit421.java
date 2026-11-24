package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;

public final class MiteRabbit421 extends Rabbit implements SelectiveBreeding420 {
    private OriginalWellness419 wellness;
    private Tag retainedWellness;

    public MiteRabbit421(EntityType<? extends Rabbit> type, Level level) {
        super(type, level);
    }

    @Override
    public OriginalWellness419 sourceWellness() {
        if (wellness == null) wellness = ModernBreeding420.initialize(this);
        return wellness;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (ModernBreeding420.usesSelectiveBreeding(this)) ModernBreeding420.afterAi(this);
    }

    @Override
    public void setInLove(Player player) {
        if (!ModernBreeding420.usesSelectiveBreeding(this) || ModernBreeding420.canLove(this)) super.setInLove(player);
    }

    @Override
    public void setAge(int age) {
        super.setAge(ModernBreeding420.usesSelectiveBreeding(this) ? ModernBreeding420.growthTarget(this, age) : age);
    }

    @Override
    public void setBaby(boolean baby) {
        if (ModernBreeding420.usesSelectiveBreeding(this)) setAge(baby ? -64000 : 0);
        else super.setBaby(baby);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
            SpawnGroupData group, CompoundTag tag) {
        var result = super.finalizeSpawn(level, difficulty, reason, group, tag);
        if (ModernBreeding420.usesSelectiveBreeding(this) && isBaby()) setAge(-64000);
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ModernBreeding420.usesSelectiveBreeding(this)) ModernBreeding420.save(this, tag);
        else if (retainedWellness != null) tag.put(ModernBreeding420.DATA, retainedWellness.copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (ModernBreeding420.usesSelectiveBreeding(this)) {
            retainedWellness = null;
            ModernBreeding420.load(this, tag);
        } else {
            var saved = tag.get(ModernBreeding420.DATA);
            retainedWellness = saved == null ? null : saved.copy();
        }
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal other) {
        if (ModernBreeding420.usesSelectiveBreeding(this)) ModernBreeding420.breed(this, level, other);
        else super.spawnChildFromBreeding(level, other);
    }

    @Override
    public void finalizeSpawnChildFromBreeding(ServerLevel level, Animal other, AgeableMob child) {
        if (ModernBreeding420.usesSelectiveBreeding(this)) ModernBreeding420.finishBirth(this, level, other, child);
        else super.finalizeSpawnChildFromBreeding(level, other, child);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (ModernBreeding420.usesSelectiveBreeding(this) && isFood(stack)) return ModernBreeding420.feed(this, player, stack);
        return super.mobInteract(player, hand);
    }
}

