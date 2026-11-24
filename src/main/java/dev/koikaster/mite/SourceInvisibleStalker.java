package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** EntityInvisibleStalker
 * source AI/profile/torch action; no Shadow sunlight, healing or undead type. */
public final class SourceInvisibleStalker extends Monster {
 public SourceInvisibleStalker(EntityType<? extends Monster> type,Level level){super(type,level);((GroundPathNavigation)getNavigation()).setCanOpenDoors(true);}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.FOLLOW_RANGE,40).add(Attributes.MOVEMENT_SPEED,(double).23f).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.ARMOR,0);}
 @Override protected void registerGoals(){
  goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new OriginalMeleeGoal(this,Player.class,1,false,null));goalSelector.addGoal(3,new OriginalMeleeGoal(this,Villager.class,1,true,null));goalSelector.addGoal(4,new MoveTowardsRestrictionGoal(this,1));goalSelector.addGoal(6,new OriginalWanderGoal(this,1));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(7,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new OriginalHurtByGoal(this,true));targetSelector.addGoal(2,new OriginalNearestTargetGoal(this,Player.class,0,true));goalSelector.addGoal(4,new OriginalSeekLitTorchGoal(this,1));
 }
 @Override public void aiStep(){OriginalTorchInteraction.disable(this);super.aiStep();}
 @Override public MobType getMobType(){return MobType.UNDEFINED;}
 @Override public int getExperienceReward(){return 10;}
 @Override protected SoundEvent getAmbientSound(){return SourceCreatureSounds.STALKER_SAY.get();}
 @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return SourceCreatureSounds.STALKER_HURT.get();}
 @Override protected SoundEvent getDeathSound(){return SourceCreatureSounds.STALKER_DEATH.get();}
 @Override protected float getSoundVolume(){return .2f;}
 @Override protected void playStepSound(BlockPos pos,BlockState state){}
 @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus",random.nextGaussian()*.05,AttributeModifier.Operation.MULTIPLY_BASE));return group;}
}
