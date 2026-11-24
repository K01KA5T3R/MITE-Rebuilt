package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.*;
public final class Mobs {
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MiteReforged.ID);
 public static final Map<String,RegistryObject<EntityType<Undead>>> UNDEAD=new LinkedHashMap<>();
 public static final Map<String,RegistryObject<EntityType<WoodSpider>>> SPIDERS=new LinkedHashMap<>();
 public static final Map<String,RegistryObject<EntityType<SourceGelatinousCube>>> SLIMES=new LinkedHashMap<>();
 public static final Map<String,RegistryObject<EntityType<DireWolf>>> WOLVES=new LinkedHashMap<>();
 public static final RegistryObject<EntityType<SourceVampireBat>> BAT=ENTITIES.register("vampire_bat",()->EntityType.Builder.of(SourceVampireBat::new,MobCategory.AMBIENT).sized(.5f,.9f).build(MiteReforged.ID+":vampire_bat"));
 static {
  for(String n:new String[]{"ghoul","wight","revenant","earth_elemental","fire_elemental","shadow","clay_golem"}){
   var type=ENTITIES.register(n,()->{var b=EntityType.Builder.<Undead>of(n.equals("revenant")?SourceRevenant::new:Undead::new,MobCategory.MONSTER).sized(.6f,1.8f);if(n.equals("fire_elemental"))b.fireImmune();return b.build(MiteReforged.ID+":"+n);});UNDEAD.put(n,type);egg(n,type,0x3b4243,0x91a89c);
  }
  for(String n:new String[]{"wood_spider","demon_spider","phase_spider","black_widow_spider"}){var t=ENTITIES.register(n,()->spiderType(n));SPIDERS.put(n,t);egg(n,t,n.equals("black_widow_spider")?0x171717:0x332522,n.equals("black_widow_spider")?11013646:0xbf5258);}
  for(String n:new String[]{"black_pudding","ochre_jelly","crimson_blob","gray_ooze"}){var t=ENTITIES.register(n,()->EntityType.Builder.of(SourceGelatinousCube::new,MobCategory.MONSTER).sized(.5f,.5f).build(MiteReforged.ID+":"+n));SLIMES.put(n,t);egg(n,t,0x302935,0xa87924);}
  for(String n:new String[]{"dire_wolf","hellhound"}){var t=ENTITIES.register(n,()->{var b=EntityType.Builder.of(DireWolf::new,n.equals("dire_wolf")?MobCategory.CREATURE:MobCategory.MONSTER).sized(.6f,.8f);if(n.equals("hellhound"))b.fireImmune();return b.build(MiteReforged.ID+":"+n);});WOLVES.put(n,t);egg(n,t,0x33363c,0xab5735);}
  egg("vampire_bat",BAT,0x302a32,0xb33941);
 }
 private static <T extends Mob>void egg(String n,RegistryObject<EntityType<T>> type,int a,int b){Content.item(n+"_spawn_egg",()->new ForgeSpawnEggItem(type,a,b,new net.minecraft.world.item.Item.Properties()));}
 private static SourceArachnidState.Species spiderSpecies(String name){return switch(name){case "wood_spider"->SourceArachnidState.Species.WOOD;case "demon_spider"->SourceArachnidState.Species.DEMON;case "phase_spider"->SourceArachnidState.Species.PHASE;case "black_widow_spider"->SourceArachnidState.Species.BLACK_WIDOW;default->throw new IllegalArgumentException(name);};}
 private static EntityType<WoodSpider> spiderType(String name){var species=spiderSpecies(name);var builder=EntityType.Builder.of(WoodSpider::new,MobCategory.MONSTER).sized(SourceArachnidProfiles.width(species),SourceArachnidProfiles.height(species));if(species==SourceArachnidState.Species.DEMON)builder.fireImmune();return builder.build(MiteReforged.ID+":"+name);}
 public static String name(Entity e){return ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).getPath();}
 public static void attributes(EntityAttributeCreationEvent e){
  UNDEAD.forEach((n,t)->e.put(t.get(),n.equals("revenant")?SourceRevenant.attributes():OriginalHumanoidProfiles.profile(n).attributes()));
  SPIDERS.forEach((n,t)->e.put(t.get(),SourceArachnidProfiles.attributes(spiderSpecies(n)).build()));
  SLIMES.forEach((n,t)->e.put(t.get(),SourceGelatinousCube.attributes().build()));
  WOLVES.forEach((n,t)->e.put(t.get(),n.equals("hellhound")?OriginalHellhoundProfile.attributes().build():Wolf.createAttributes().add(Attributes.MAX_HEALTH,16).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,16).add(Attributes.MOVEMENT_SPEED,.4f).build()));
  e.put(BAT.get(),SourceBat.attributes(1).build());
 }
 public static void placements(SpawnPlacementRegisterEvent e){
  UNDEAD.forEach((n,t)->e.register(t.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,pos,r)->SourceSpecialSpawns.allow(n,l,reason,pos,r),SpawnPlacementRegisterEvent.Operation.REPLACE));
  SPIDERS.forEach((n,t)->e.register(t.get(),n.equals("wood_spider")||n.equals("black_widow_spider")?SourceArachnidAI.WOOD_PLACEMENT:SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,pos,r)->SourceArachnidAI.allowSpawn(n,l,reason,pos,r),SpawnPlacementRegisterEvent.Operation.REPLACE));
  WOLVES.forEach((n,t)->e.register(t.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,pos,r)->n.equals("dire_wolf")?SourceSpecialSpawns.direWolf(l,reason,pos):SourceSpecialSpawns.allow(n,l,reason,pos,r),SpawnPlacementRegisterEvent.Operation.REPLACE));
  SLIMES.forEach((n,t)->e.register(t.get(),SourceGelatinousContent.PLACEMENT,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,pos,r)->SourceGelatinousContent.allow(n,l,reason,pos,r),SpawnPlacementRegisterEvent.Operation.REPLACE));
 }
 public static class Undead extends Monster implements OriginalDigging.Host {
  private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> EARTH_TYPE=net.minecraft.network.syncher.SynchedEntityData.defineId(Undead.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
  private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> TACTICAL_WIGHT =
   net.minecraft.network.syncher.SynchedEntityData.defineId(Undead.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
  private static final String TACTICAL_WIGHT_TAG = "MiteTacticalWight";
  private OriginalDigging digging;
  private OriginalHumanoidFood food;
  private OriginalMobEnvironment environment;
  private OriginalFireElementalLife fireLife;
  private OriginalEarthElementalLife earthLife;
  private OriginalShadowLife shadowLife;
  private LivingEntity originalRetarget;
  public Undead(EntityType<? extends Monster> t,Level l){super(t,l);xpReward=switch(name(this)){case "ghoul","wight"->10;default->15;};}
  public OriginalDigging originalDigging(){if(digging==null)digging=new OriginalDigging(this);return digging;}
  public OriginalHumanoidFood originalFood(){if(food==null)food=new OriginalHumanoidFood(this);return food;}
  public OriginalMobEnvironment originalEnvironment(){if(environment==null)environment=new OriginalMobEnvironment(this);return environment;}
  public boolean earthBody(){return name(this).equals("earth_elemental")||name(this).equals("clay_golem");}
  public OriginalEarthElementalLife originalEarth(){if(earthLife==null)earthLife=new OriginalEarthElementalLife(this,()->entityData.get(EARTH_TYPE),value->entityData.set(EARTH_TYPE,value),Network::environmentSmoke);return earthLife;}
  public OriginalFireElementalLife originalFire(){if(fireLife==null)fireLife=new OriginalFireElementalLife(this);return fireLife;}
  public OriginalShadowLife originalShadow(){if(shadowLife==null)shadowLife=new OriginalShadowLife(this);return shadowLife;}
  protected void defineSynchedData(){super.defineSynchedData();entityData.define(EARTH_TYPE,0);entityData.define(TACTICAL_WIGHT,false);}
  public boolean isTacticalWight(){return entityData.get(TACTICAL_WIGHT);}
  public boolean canBreatheUnderwater(){return getMobType()==MobType.UNDEAD||earthBody()||super.canBreatheUnderwater();}
  public boolean fireImmune(){return earthBody()||name(this).equals("shadow")||super.fireImmune();}
  public boolean isOnFire(){return name(this).equals("fire_elemental")||super.isOnFire();}
  public MobType getMobType(){return OriginalHumanoidProfiles.profile(name(this)).creatureType();}
  public void originalRetarget(LivingEntity target){originalRetarget=target;}public LivingEntity consumeOriginalRetarget(){var target=originalRetarget;originalRetarget=null;return target;}
  protected void registerGoals(){
   String species=name(this);var dig=originalDigging();var navigation=(net.minecraft.world.entity.ai.navigation.GroundPathNavigation)getNavigation();navigation.setCanOpenDoors(!species.equals("fire_elemental"));
   goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this));if(OriginalDigging.watcher(this)){goalSelector.addGoal(1,new OriginalWatchAnimalGoal(dig));goalSelector.addGoal(1,new OriginalBreakPortalGoal(this));}
   if(species.equals("wight"))goalSelector.addGoal(1,new TacticalWight2062(this));
   goalSelector.addGoal(2,new OriginalMeleeGoal(this,Player.class,1,false,dig));goalSelector.addGoal(3,new OriginalMeleeGoal(this,net.minecraft.world.entity.npc.Villager.class,1,true,dig));
   goalSelector.addGoal(4,new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this,1));goalSelector.addGoal(6,new OriginalWanderGoal(this,species.equals("ghoul")?.8:1));
   if(!species.equals("fire_elemental")){goalSelector.addGoal(7,new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(7,new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));}
   targetSelector.addGoal(1,new OriginalHurtByGoal(this,true));targetSelector.addGoal(2,new OriginalNearestTargetGoal(this,Player.class,0,true));
   if(OriginalDigging.watcher(this))targetSelector.addGoal(2,new OriginalNearestTargetGoal(this,net.minecraft.world.entity.npc.Villager.class,0,false));
   if(species.equals("fire_elemental")){goalSelector.addGoal(1,new OriginalLeaveWaterGoal(this,1));goalSelector.addGoal(2,new OriginalRainShelterGoal(this,1,true));}if(species.equals("revenant"))goalSelector.addGoal(3,new OriginalBurningTreeGoal(this,1));
   if(species.equals("shadow")){goalSelector.addGoal(2,new OriginalRestrictSunGoal(this));goalSelector.addGoal(3,new OriginalFleeSunGoal(this,1));goalSelector.addGoal(3,new OriginalSeekLitTorchGoal(this,1));}
   if(species.equals("ghoul")||species.equals("revenant")){goalSelector.addGoal(species.equals("ghoul")?1:2,new OriginalMoveToFoodGoal(this,originalFood(),1,true));goalSelector.addGoal(4,new OriginalMeleeGoal(this,net.minecraft.world.entity.animal.Animal.class,1,true,dig));targetSelector.addGoal(3,new OriginalNearestTargetGoal(this,net.minecraft.world.entity.animal.Animal.class,10,true));}
  }
  public void aiStep(){String species=name(this);if(species.equals("fire_elemental")){if(level().isClientSide)ClientFireElementalEffects.tick(this);else originalFire().beforeAi();}if(species.equals("shadow"))originalShadow().beforeAi();if(!level().isClientSide)originalEnvironment().sunlight();if(!level().isClientSide&&OriginalDigging.watcher(this))originalDigging().beforeAi();if(!level().isClientSide&&earthBody()&&(tickCount+getId()*47)%1000==0)heal(getMaxHealth()*.1f);super.aiStep();if(earthBody())originalEarth().afterAi();if(!level().isClientSide&&(species.equals("ghoul")||species.equals("revenant")))runUndeadPickup();}
  protected void runUndeadPickup(){originalFood().tick();}
  public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);if(OriginalDigging.watcher(this))originalDigging().state.save(tag);if(earthBody())originalEarth().save(tag);if(isTacticalWight())tag.putBoolean(TACTICAL_WIGHT_TAG,true);}
  public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(TACTICAL_WIGHT,name(this).equals("wight")&&tag.getBoolean(TACTICAL_WIGHT_TAG));if(OriginalDigging.watcher(this))originalDigging().state.load(tag);if(earthBody()){originalEarth().load(tag);if(!tag.contains("type")&&getPersistentData().contains("MiteEarthType")){int legacy=getPersistentData().getInt("MiteEarthType");originalEarth().type(legacy);if(!tag.contains("heat")&&(legacy&256)!=0)originalEarth().heat=1000;}getPersistentData().remove("MiteEarthType");}}
  public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,net.minecraft.nbt.CompoundTag tag){if(earthBody()){var foot=blockPosition();originalEarth().forBlock(level.getBlockState(foot.below()).getBlock(),level.getBlockState(foot).is(Blocks.LAVA)||name(this).equals("clay_golem")&&level.getBlockState(foot).is(Blocks.FIRE));}
   var group=super.finalizeSpawn(level,difficulty,reason,data,tag);
   if(TacticalWight2062.shouldPromote(this,level,reason))entityData.set(TACTICAL_WIGHT,true);
   return group;
  }
  public boolean doHurtTarget(Entity target){if(earthBody())swing(net.minecraft.world.InteractionHand.MAIN_HAND);return super.doHurtTarget(target);}
  public void thunderHit(ServerLevel level,LightningBolt bolt){super.thunderHit(level,bolt);if(earthBody())originalEarth().lightning();}
  public float getWalkTargetValue(BlockPos pos,LevelReader level){return earthBody()?0:super.getWalkTargetValue(pos,level);}
  public int getMaxSpawnClusterSize(){return earthBody()?1:super.getMaxSpawnClusterSize();}
  public int getExperienceReward(){return earthBody()||name(this).equals("fire_elemental")?15:name(this).equals("shadow")?10:super.getExperienceReward()+(isTacticalWight()?6:0);}
  protected net.minecraft.sounds.SoundEvent getAmbientSound(){return switch(name(this)){case "ghoul"->SourceCreatureSounds.GHOUL_SAY.get();case "wight"->SourceCreatureSounds.WIGHT_SAY.get();case "revenant"->net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT;case "shadow"->SourceCreatureSounds.SHADOW_SAY.get();default->super.getAmbientSound();};}
  protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return switch(name(this)){case "ghoul"->SourceCreatureSounds.GHOUL_HURT.get();case "wight"->SourceCreatureSounds.WIGHT_HURT.get();case "revenant"->net.minecraft.sounds.SoundEvents.ZOMBIE_HURT;case "fire_elemental"->null;case "earth_elemental","clay_golem"->net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT;case "shadow"->SourceCreatureSounds.SHADOW_HURT.get();default->super.getHurtSound(source);};}
  protected net.minecraft.sounds.SoundEvent getDeathSound(){return switch(name(this)){case "ghoul"->SourceCreatureSounds.GHOUL_DEATH.get();case "wight"->SourceCreatureSounds.WIGHT_DEATH.get();case "revenant"->net.minecraft.sounds.SoundEvents.ZOMBIE_DEATH;case "fire_elemental"->null;case "earth_elemental","clay_golem"->net.minecraft.sounds.SoundEvents.IRON_GOLEM_DEATH;case "shadow"->SourceCreatureSounds.SHADOW_DEATH.get();default->super.getDeathSound();};}
  protected float getSoundVolume(){return name(this).equals("shadow")?.2f:super.getSoundVolume();}
  protected void playStepSound(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){if(name(this).equals("fire_elemental")||name(this).equals("shadow"))return;if(Set.of("ghoul","wight","revenant").contains(name(this))){playSound(net.minecraft.sounds.SoundEvents.ZOMBIE_STEP,.15f,1);return;}super.playStepSound(pos,state);if(earthBody())playSound(net.minecraft.sounds.SoundEvents.IRON_GOLEM_STEP,.25f,1);}
  public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
   boolean snowball=!level().isClientSide&&source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Snowball&&!SourceSnowballTraits.proper(source);
   if(snowball&&name(this).equals("fire_elemental")){Network.environmentSteam(this);boolean affected=super.hurt(source,3);Network.environmentSteam(this);if(Math.random()<.5)clearFire();return affected;}
   if(snowball&&earthBody()&&originalEarth().magma())originalEarth().normal(true);
   if(SourceSpecificImmunities.immune(this,source))return false;
   if(snowball&&(earthBody()||name(this).equals("shadow")))return !(source.getEntity() instanceof Player)&&super.hurt(source,1);
   return super.hurt(source,amount);
  }
  protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean recent){if(earthBody()){var block=OriginalEarthElementalLife.blockForType(originalEarth().type());if(block!=null)spawnAtLocation(block==Blocks.STONE?net.minecraft.world.item.Items.COBBLESTONE:block.asItem());return;}super.dropCustomDeathLoot(source,looting,recent);
   if(isTacticalWight()&&recent&&source.getEntity() instanceof Player&&getRandom().nextInt(4)==0)
    spawnAtLocation(net.minecraft.world.item.Items.BREAD);
  }
  public void die(net.minecraft.world.damagesource.DamageSource source){if(OriginalDigging.watcher(this))originalDigging().cancel();super.die(source);}
 }
 public static class WoodSpider extends Spider implements OriginalPhaseSpiderLife.Owner {
  private final OriginalPhaseSpiderLife phaseLife;
  public WoodSpider(EntityType<? extends Spider> t,Level l){super(t,l);phaseLife=name(this).equals("phase_spider")?new OriginalPhaseSpiderLife(this):null;}
  public OriginalPhaseSpiderLife originalPhaseSpider(){return phaseLife;}
  public void aiStep(){super.aiStep();if(phaseLife!=null)phaseLife.afterAi();}
  public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){if(phaseLife!=null&&phaseLife.beforeHurt(SourcePhaseDamageAdapter.of(source)))return false;return super.hurt(source,amount);}
  public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);if(phaseLife!=null)phaseLife.save(tag);}
  public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);if(phaseLife!=null)phaseLife.load(tag);}
  protected net.minecraft.sounds.SoundEvent getAmbientSound(){return name(this).equals("demon_spider")?SourceCreatureSounds.DEMON_SPIDER_SAY.get():super.getAmbientSound();}
  protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return name(this).equals("demon_spider")?SourceCreatureSounds.DEMON_SPIDER_HURT.get():super.getHurtSound(source);}
  protected net.minecraft.sounds.SoundEvent getDeathSound(){return name(this).equals("demon_spider")?SourceCreatureSounds.DEMON_SPIDER_DEATH.get():super.getDeathSound();}
  protected float getSoundVolume(){return name(this).equals("demon_spider")?super.getSoundVolume():super.getSoundVolume()*.6f;}
  public float getVoicePitch(){return name(this).equals("demon_spider")?super.getVoicePitch():super.getVoicePitch()*1.2f;}
 }
 public static class DireWolf extends Wolf {
  private OriginalDireWolfLife life;
  public DireWolf(EntityType<? extends Wolf> t,Level l){super(t,l);xpReward=10;}
  public boolean sourceDire(){return name(this).equals("dire_wolf");}
  public boolean sourceHellhound(){return name(this).equals("hellhound");}
  public OriginalDireWolfLife originalWolf(){if(life==null)life=new OriginalDireWolfLife(this);return life;}
  protected void registerGoals(){if(sourceHellhound()){OriginalHellhoundProfile.install(this);return;}if(OriginalWolfOwners.install(this))return;super.registerGoals();if(sourceDire()){targetSelector.removeAllGoals(g->g.getClass()==net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal.class||g instanceof net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal<?>);targetSelector.addGoal(3,originalWolf().playerGoal());}}
  public void setTame(boolean tame){float before=getMaxHealth(),health=getHealth();super.setTame(sourceHellhound()?false:tame);if(sourceHellhound()){getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);setHealth(Math.min(20,health));}else if(sourceDire()){getAttribute(Attributes.MAX_HEALTH).setBaseValue(tame?24:16);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(tame?32:16);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5);setHealth(Math.min(getMaxHealth(),health+Math.max(0,getMaxHealth()-before)));}}
  public boolean isTame(){return !sourceHellhound()&&super.isTame();}
  public void setTarget(LivingEntity target){if(sourceDire()&&target!=null&&target==getOwner())return;super.setTarget(target);if(sourceDire()&&getTarget()!=null)originalWolf().target();}
  public void tick(){super.tick();if(sourceDire()&&!level().isClientSide)originalWolf().tick();}
  public net.minecraft.world.InteractionResult mobInteract(Player player,net.minecraft.world.InteractionHand hand){if(sourceHellhound())return net.minecraft.world.InteractionResult.PASS;if(sourceDire()){var result=originalWolf().bone(player,hand);if(result!=null)return result;}return super.mobInteract(player,hand);}
  public boolean isFood(net.minecraft.world.item.ItemStack stack){return !sourceHellhound()&&super.isFood(stack);}
  public boolean canMate(net.minecraft.world.entity.animal.Animal other){return !sourceHellhound()&&super.canMate(other);}
  public Wolf getBreedOffspring(ServerLevel level,AgeableMob other){return sourceHellhound()?null:super.getBreedOffspring(level,other);}
  public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);if(sourceDire())originalWolf().save(tag);}
  public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);if(sourceDire())originalWolf().load(tag);}
  public int getExperienceReward(){return sourceHellhound()?OriginalHellhoundProfile.EXPERIENCE:sourceDire()?10:super.getExperienceReward();}
  public float getWalkTargetValue(BlockPos pos,LevelReader level){return sourceHellhound()?OriginalHellhoundProfile.walkValue(this,pos):super.getWalkTargetValue(pos,level);}
  protected net.minecraft.sounds.SoundEvent getAmbientSound(){if(sourceHellhound())return SourceCreatureSounds.HELLHOUND_SAY.get();if(!sourceDire())return super.getAmbientSound();var state=originalWolf();if(!isBaby()&&!isTame()&&!isInLove()&&!SourceSpecialSpawns.blueUp(level())&&(state.nearby()!=null||random.nextFloat()<.1f)||state.hostile||getTarget()!=null&&!isTame())return net.minecraft.sounds.SoundEvents.WOLF_GROWL;return isTame()&&getHealth()/getMaxHealth()<.5f?net.minecraft.sounds.SoundEvents.WOLF_WHINE:net.minecraft.sounds.SoundEvents.WOLF_PANT;}
  protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return sourceHellhound()?SourceCreatureSounds.HELLHOUND_HURT.get():super.getHurtSound(source);}
  protected net.minecraft.sounds.SoundEvent getDeathSound(){return sourceHellhound()?SourceCreatureSounds.HELLHOUND_DEATH.get():super.getDeathSound();}
  protected float getSoundVolume(){return sourceHellhound()?isBaby()?.2f:.4f:sourceDire()?(isBaby()?.2f:.4f)*1.5f:super.getSoundVolume();}
  public float getVoicePitch(){return sourceDire()?super.getVoicePitch()*.8f:super.getVoicePitch();}
  public boolean doHurtTarget(Entity target){boolean affected=super.doHurtTarget(target);if(sourceHellhound())OriginalHellhoundStrike.result(this,target,affected,SourceCreatureSounds.HELLHOUND_BREATH.get());else if(sourceDire()&&affected){if(!target.isAlive())setTarget(null);else originalWolf().target();}return affected;}
  protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean recent){if(!sourceHellhound())super.dropCustomDeathLoot(source,looting,recent);}
 }

 static void breakObstacles(Mob mob){
  if(!Settings.GRIEFING||mob.getTarget()==null||!ForgeEventFactory.getMobGriefingEvent(mob.level(),mob))return;
  var l=mob.level();var pos=mob.blockPosition();
  for(BlockPos q:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,2,1))){var s=l.getBlockState(q);if(s.getBlock() instanceof TorchBlock||s.getBlock() instanceof WallTorchBlock){l.destroyBlock(q,true,mob);return;}}
  if(!mob.hasLineOfSight(mob.getTarget())){
   var dir=Direction.getNearest((float)(mob.getTarget().getX()-mob.getX()),0f,(float)(mob.getTarget().getZ()-mob.getZ()));
   BlockPos q=pos.relative(dir);if(l.getBlockState(q).isAir())q=q.above();var s=l.getBlockState(q);float hard=s.getDestroySpeed(l,q);
   if(!s.isAir()&&hard>=0&&hard<=2.5f&&l.getBlockEntity(q)==null&&s.getFluidState().isEmpty())l.destroyBlock(q,true,mob);
  }
 }
 public static void onHurt(LivingHurtEvent e){
  var source=e.getSource().getEntity();if(source==null)return;String n=name(source);
 }
 private Mobs(){}
}
