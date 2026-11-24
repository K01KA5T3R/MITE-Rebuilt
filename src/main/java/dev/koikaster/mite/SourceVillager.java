package dev.koikaster.mite;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

/** Retained ub/nk owner. Native Villager identity transports age and common
 * inventory/NBT; source goals and clocks replace modern Brain and gossip.
 * V058-V060 continue to remove village generation, economy and breeding. */
public final class SourceVillager extends Villager implements SourceVillagerRules.Host,OriginalParentHarmState.Flags {
 private final SourceVillagerRules.State state=new SourceVillagerRules.State();
 private final OriginalParentHarmState harm;
 private SourceVillageDoors.Village village;
 private boolean decided,fleeing;private int deferRevenge;
 public SourceVillager(EntityType<? extends Villager> type,Level level){
  super(type,level);getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.5);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);setHealth(getMaxHealth());setMaxUpStep(.5f);setPathfindingMalus(BlockPathTypes.WATER,8);xpReward=0;
  harm=new OriginalParentHarmState(this,this);SourceVillagerGoals.install(this);refreshDimensions();
 }
 private SourceVillagerParent parent(){return (SourceVillagerParent)(Object)this;}
 @Override protected void registerGoals(){}
 @Override protected Brain<?> makeBrain(Dynamic<?> input){return brainProvider().makeBrain(input);}
 @Override public void refreshBrain(ServerLevel level){var old=getBrain();old.stopAll(level,this);brain=old.copyWithoutBehaviors();}
 @Override protected void ageBoundaryReached(){}
 @Override public void tick(){if(harm!=null&&!level().isClientSide)harm.beforeTick();parent().mite$villagerParentTick();}
 @Override public void aiStep(){SourceVillagerRules.living(this);}
 @Override protected void customServerAiStep(){SourceVillagerRules.ai(this);}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.6f,1.8f).scale(isBaby()?.5f:1);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height*.85f;}
 @Override public void setAge(int requested){
  int previous=getAge(),next=requested;boolean liveBoundary=previous<0&&requested>=0&&isAddedToWorld();
  // Loading a template/entity may run off the server thread. Terrain and push-out
  // checks belong only to a living NPC becoming adult, not spawn/NBT age setup.
  if(liveBoundary){var pos=blockPosition();next=SourceVillagerRules.growingAge(previous,requested,SourceVillagerGrowth.cauldron(level().getBlockState(pos)),OriginalDigging.passable(level(),pos.above(),true));}
  super.setAge(next);if(liveBoundary&&next>=0)SourceVillagerGrowth.escape(this);
 }
 @Override public void setBaby(boolean child){setAge(child?-64000:0);}
 @Override public void ageUp(int seconds,boolean forced){int next=getAge()+seconds*5;setAge(Math.min(next,0));}
 @Override public void ageUp(int seconds){ageUp(seconds,false);}
 @Override public Villager getBreedOffspring(ServerLevel level,AgeableMob other){return null;}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){SourceSpawnInitialization.apply(this,reason,random.nextGaussian()*.05);return data;}
 @Override public InteractionResult mobInteract(Player player,InteractionHand hand){return parent().mite$villagerParentInteract(player,hand);}
 @Override protected void updateTrades(){}
 @Override public MerchantOffers getOffers(){if(offers==null)offers=new MerchantOffers();return offers;}
 @Override public void handleEntityEvent(byte event){SourceVillagerRules.event(this,event);}
 @Override public SoundEvent getAmbientSound(){return SoundEvents.VILLAGER_AMBIENT;}
 @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.VILLAGER_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.VILLAGER_DEATH;}
 @Override public int getExperienceReward(){return SourceVillagerRules.experience();}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override public float getWalkTargetValue(BlockPos pos,LevelReader world){return OriginalWeather.rain(level(),pos.above())?SourceVillagerRules.pathWeight(true,0):SourceVillagerRules.pathWeight(false,super.getWalkTargetValue(pos,world));}
 @Override public void setLastHurtByMob(LivingEntity actor){parent().mite$villagerParentRevenge(actor);if(deferRevenge==0){SourceVillageLife.revenge(this,actor);if(actor instanceof Player&&isAlive()&&village!=null)level().broadcastEntityEvent(this,(byte)13);}}
 @Override public boolean hurt(DamageSource source,float amount){
  deferRevenge++;try(var frame=SourceParentHurtFrame.begin(this,outcome->{if(harm!=null&&!level().isClientSide){int deferred=deferRevenge;deferRevenge=0;try{harm.afterDamage(source,outcome.negativelyAffected());}finally{deferRevenge=deferred;}}})){boolean accepted=parent().mite$villagerParentHurt(source,amount);frame.finish();return accepted;}finally{deferRevenge--;}
 }
 @Override public void die(DamageSource source){SourceVillageLife.villagerDied(this,source);parent().mite$villagerParentDie(source);}
 @Override public void thunderHit(ServerLevel level,LightningBolt bolt){parent().mite$villagerParentThunder(level,bolt);}
 @Override public void onReputationEventFrom(ReputationEventType event,Entity actor){}
 @Override public int getPlayerReputation(Player player){return village==null?0:SourceVillageLife.reputation(village,player.getGameProfile().getName());}
 @Override public void addAdditionalSaveData(CompoundTag tag){parent().mite$villagerParentSave(tag);harm.save(tag);tag.putBoolean("R195VillagerOwner",true);tag.remove("ForcedAge");tag.remove("Offers");}
 @Override public void readAdditionalSaveData(CompoundTag tag){parent().mite$villagerParentLoad(tag);harm.load(tag);forcedAge=forcedAgeTimer=0;offers=new MerchantOffers();if(!tag.getBoolean("R195VillagerOwner")){getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.5);getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);}}
 @Override public void makeStuckInBlock(BlockState state,Vec3 factor){super.makeStuckInBlock(state,factor);if(state.is(Blocks.COBWEB)){getPersistentData().putInt("MiteSourceWebTick",tickCount);EnchantmentEffects.refreshMovement(this);}}
 @Override public boolean hasLineOfSight(Entity target){return OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);}
 public SourceVillageDoors.Village sourceVillage(){return village;}
 public void sourceCured(){state.generatedVillageBonus=true;}
 @Override public SourceVillagerRules.State state(){return state;}
 @Override public int nextInt(int bound){return random.nextInt(bound);}@Override public float nextFloat(){return random.nextFloat();}@Override public double nextGaussian(){return random.nextGaussian();}
 @Override public double x(){return getX();}@Override public double y(){return getY();}@Override public double z(){return getZ();}@Override public float width(){return getBbWidth();}@Override public float height(){return getBbHeight();}
 @Override public void enqueueVillage(int x,int y,int z){if(level() instanceof ServerLevel l)SourceVillageDoors.get(l).enqueue(new BlockPos(x,y,z));}
 @Override public SourceVillagerRules.Village villageAt(int x,int y,int z,int range){if(!(level() instanceof ServerLevel l))return null;village=SourceVillageDoors.get(l).closest(new BlockPos(x,y,z),range);return village==null?null:new SourceVillagerRules.Village(new SourceVillagerRules.Point(village.center.getX(),village.center.getY(),village.center.getZ()),village.radius);}
 @Override public void bind(SourceVillagerRules.Village value){if(value==null)village=null;SourceVillageLife.bind(this,village);}
 @Override public void boostKnownPlayers(int amount){if(level() instanceof ServerLevel l&&village!=null)for(var name:java.util.List.copyOf(village.life.reputation.keySet()))SourceVillageLife.change(l,village,name,amount);}
 @Override public void parentAi(){parent().mite$villagerParentAi();}@Override public void parentLiving(){if(!level().isClientSide&&(tickCount+getId()*47)%1000==0&&!isRemoved()&&getHealth()>0&&getHealth()<getMaxHealth())heal(getMaxHealth()*.1f);((SourceVillagerAgeParent)(Object)this).mite$villagerAgeParentLiving();}
 @Override public boolean client(){return level().isClientSide;}@Override public int age(){return getAge();}@Override public void age(int value){setAge(value);}@Override public void resize(){refreshDimensions();}
 @Override public void particle(int kind,double x,double y,double z,double dx,double dy,double dz){level().addParticle(kind==12?ParticleTypes.HEART:kind==13?ParticleTypes.ANGRY_VILLAGER:ParticleTypes.HAPPY_VILLAGER,x,y,z,dx,dy,dz);}
 @Override public void parentEvent(int event){parent().mite$villagerParentEvent((byte)event);}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
}
