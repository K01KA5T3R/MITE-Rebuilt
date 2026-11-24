package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** sd owner. Native identity/renderer/sync transport remain IronGolem; native
 * anger, village goals, damage formula, ingot repair, cracks and loot do not. */
public final class SourceIronGolem extends IronGolem implements SourceIronGolemRules.Host,OriginalParentHarmState.Flags {
 private final SourceIronGolemRules.State state=new SourceIronGolemRules.State();
 private SourceVillageDoors.Village village;private boolean massiveFall,decided,fleeing;
 private final OriginalParentHarmState harm;
 public SourceIronGolem(EntityType<? extends IronGolem> type,Level level){
  super(type,level);getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.25);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);setHealth(getMaxHealth());setMaxUpStep(.5f);setPathfindingMalus(BlockPathTypes.WATER,8);xpReward=20;
  harm=new OriginalParentHarmState(this,this);SourceIronGolemGoals.install(this);refreshDimensions();
 }
 private SourceIronGolemParent parent(){return (SourceIronGolemParent)(Object)this;}
 @Override protected void registerGoals(){}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(1.4f,2.9f);}
 @Override public void tick(){if(!level().isClientSide&&harm!=null)harm.beforeTick();super.tick();}
 @Override public void aiStep(){if(state==null){parent().mite$golemParentLiving();return;}SourceIronGolemRules.living(this);}
 @Override protected void customServerAiStep(){SourceIronGolemRules.ai(this);}
 @Override public boolean doHurtTarget(Entity target){return SourceIronGolemRules.attack(this,new Node(target)).accepted();}
 @Override protected void doPush(Entity target){SourceIronGolemRules.push(this,new Node(target));}
 @Override public void handleEntityEvent(byte event){SourceIronGolemRules.event(this,event);}
 @Override public void offerFlower(boolean value){SourceIronGolemRules.flower(this,value);}
 @Override public int getAttackAnimationTick(){return state.attackTicks;}
 @Override public int getOfferFlowerTick(){return state.flowerTicks;}
 @Override public boolean canAttackType(EntityType<?> type){if(isPlayerCreated()&&type==EntityType.PLAYER)return false;return parent().mite$golemParentTargetType(type);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){return parent().mite$golemParentInteract(player,hand);}
 @Override public Crackiness getCrackiness(){return Crackiness.NONE;}
 @Override public boolean canSpawnSprintParticle(){return false;}
 @Override public void setSecondsOnFire(int seconds){}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.is(DamageTypeTags.IS_FIRE)&&!source.is(DamageTypes.LAVA)||ClassicDefense.name(source).equals("pepsin"))return false;
  // rv's fall handler is empty. A direct damage-source call is not that
  // handler and must not manufacture of's massive-fall state.
  try(var frame=SourceParentHurtFrame.begin(this,outcome->{if(harm!=null&&!level().isClientSide)harm.afterDamage(source,outcome.negativelyAffected());})){boolean accepted=parent().mite$golemParentHurt(source,amount);frame.finish();return accepted;}
 }
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){if(!massiveFall||random.nextFloat()<.1f)SourceIronGolemRules.drops(this,recent,massiveFall,looting);}
 @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect){var p=effect.getEffect();return SourceConstructRules.potion(false,p==net.minecraft.world.effect.MobEffects.REGENERATION,p==net.minecraft.world.effect.MobEffects.POISON,p==net.minecraft.world.effect.MobEffects.WITHER)&&super.canBeAffected(effect);}
 @Override protected int increaseAirSupply(int air){return SourceConstructRules.recoveredAir();}
 @Override public int getExperienceReward(){return SourceIronGolemRules.experience(5);}
 @Override public void die(DamageSource source){if(level() instanceof ServerLevel l)SourceVillageLife.golemDied(l,village,isPlayerCreated(),lastHurtByPlayer);super.die(source);}
 @Override public void addAdditionalSaveData(CompoundTag tag){parent().mite$golemParentSave(tag);tag.putBoolean("PlayerCreated",isPlayerCreated());tag.putBoolean("R195IronGolemOwner",true);harm.save(tag);tag.remove("AngerTime");tag.remove("AngryAt");}
 @Override public void readAdditionalSaveData(CompoundTag tag){parent().mite$golemParentLoad(tag);setPlayerCreated(tag.getBoolean("PlayerCreated"));harm.load(tag);setRemainingPersistentAngerTime(0);setPersistentAngerTarget(null);if(!tag.getBoolean("R195IronGolemOwner")){getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.25);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);}}
 public SourceVillageDoors.Village sourceVillage(){return village;}
 @Override public SourceIronGolemRules.State state(){return state;}
 @Override public int nextInt(int bound){return random.nextInt(bound);}@Override public float nextFloat(){return random.nextFloat();}
 @Override public double x(){return getX();}@Override public double y(){return getY();}@Override public double z(){return getZ();}
 @Override public double motionX(){return getDeltaMovement().x;}@Override public double motionZ(){return getDeltaMovement().z;}@Override public double yOffset(){return 0;}@Override public float width(){return getBbWidth();}@Override public double boxMinY(){return getBoundingBox().minY;}
 @Override public SourceIronGolemRules.Village villageAt(int x,int y,int z,int extra){if(!(level() instanceof ServerLevel l)){village=null;return null;}village=SourceVillageDoors.get(l).closest(new BlockPos(x,y,z),extra);return village==null?null:new SourceIronGolemRules.Village(new SourceIronGolemRules.Point(village.center.getX(),village.center.getY(),village.center.getZ()),village.radius);}
 @Override public void village(SourceIronGolemRules.Village value){if(value==null)village=null;}
 @Override public void restrict(SourceIronGolemRules.Point p,int radius){restrictTo(new BlockPos(p.x(),p.y(),p.z()),radius);}
 @Override public void parentAi(){parent().mite$golemParentAi();}@Override public void parentLiving(){if(SourceConstructRules.ironHeal(!level().isClientSide,tickCount+getId()*47))heal(getMaxHealth()*.1f);parent().mite$golemParentLiving();}
 @Override public void parentPush(SourceIronGolemRules.Target target){parent().mite$golemParentPush(((Node)target).actor());}@Override public void parentEvent(int event){parent().mite$golemParentEvent((byte)event);}
 @Override public void setTarget(SourceIronGolemRules.Target target){setTarget((LivingEntity)((Node)target).actor());}
 @Override public int block(int x,int y,int z){var state=level().getBlockState(new BlockPos(x,y,z));return state.isAir()?0:Block.getId(state);}@Override public int metadata(int x,int y,int z){return 0;}
 @Override public void particle(int block,int meta,double x,double y,double z,double dx,double dy,double dz){level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK,Block.stateById(block)),x,y,z,dx,dy,dz);}
 @Override public void event(int event){level().broadcastEntityEvent(this,(byte)event);}
 @Override public SourceIronGolemRules.Hit damage(SourceIronGolemRules.Target target,float amount){
  var actor=((Node)target).actor();var damage=damageSources().mobAttack(this);
  if(actor instanceof LivingEntity living)try(var scope=SourceMeleeResultScope.begin(living,damage);var frame=SourceParentHurtFrame.beginObserver(living,outcome->{})){boolean accepted=living.hurt(damage,amount);return scope.result(accepted,frame.finish());}
  boolean admitted=!actor.isRemoved()&&!actor.isInvulnerableTo(damage)&&amount>0;boolean accepted=actor.hurt(damage,amount);return new SourceIronGolemRules.Hit(admitted,false,accepted);
 }
 @Override public void throwSound(){playSound(SoundEvents.IRON_GOLEM_ATTACK,1,1);}
 @Override public void drop(boolean rose){spawnAtLocation(rose?Items.POPPY:Items.IRON_NUGGET);}
 private record Node(Entity actor) implements SourceIronGolemRules.Target {public boolean monster(){return actor instanceof Enemy;}public void addVertical(double amount){actor.setDeltaMovement(actor.getDeltaMovement().add(0,amount,0));}}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
 // Keep the host symbol independent of the inherited Mob name during reobfuscation.
 @Override public void miteClearVillageRestriction(){super.clearRestriction();}
}
