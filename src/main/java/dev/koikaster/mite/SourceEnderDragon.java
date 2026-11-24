package dev.koikaster.mite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.*;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

/** sk's single flight/combat owner, with seven actual Forge parts. Native
 * EnderDragon retains only construction, renderer transport and parent hurt. */
public final class SourceEnderDragon extends EnderDragon {
 private static final SimpleParticleType PROJECTILE_GUARD = ParticleTypes.ENCHANT;
 private static final int RITUAL_CRYSTAL_HEAL_INTERVAL = 40;
 private static final EntityDataAccessor<Integer> RITUAL_CRYSTAL = SynchedEntityData.defineId(SourceEnderDragon.class, EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> RITUAL_POSTURE = SynchedEntityData.defineId(SourceEnderDragon.class, EntityDataSerializers.INT);
 private static final EntityDataAccessor<Long> RITUAL_MOVE_START = SynchedEntityData.defineId(SourceEnderDragon.class, EntityDataSerializers.LONG);
 private SourceDragonPart[] sourceParts;private SourceDragonBrain source;private final SourceDragonBreath breath=new SourceDragonBreath();private SourceDragonKinematicsAdapter kinematics;
 private final DragonCombat combat = new DragonCombat();
 public SourceEnderDragon(EntityType<? extends EnderDragon> type,Level level){super(type,level);sourceParts=new SourceDragonPart[]{new SourceDragonPart(this,"head",6,6),new SourceDragonPart(this,"body",8,8),new SourceDragonPart(this,"tail",4,4),new SourceDragonPart(this,"tail",4,4),new SourceDragonPart(this,"tail",4,4),new SourceDragonPart(this,"wing",4,4),new SourceDragonPart(this,"wing",4,4)};source=new SourceDragonBrain(this);kinematics=new SourceDragonKinematicsAdapter(this);setId(getId());getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);}
 @Override protected void defineSynchedData() {
  super.defineSynchedData();
  entityData.define(RITUAL_CRYSTAL, -1);
  entityData.define(RITUAL_POSTURE, DragonCombat.ORBIT);
  entityData.define(RITUAL_MOVE_START, 0L);
 }
 int ritualPosture() { return entityData.get(RITUAL_POSTURE); }
 void setRitualPosture(int posture) {
  if (ritualPosture() != posture) setRitualMove(posture, level().getGameTime());
 }
 void setRitualMove(int posture, long started) {
  entityData.set(RITUAL_MOVE_START, started);
  entityData.set(RITUAL_POSTURE, posture);
 }
 long ritualMoveAge() { return Math.max(0, level().getGameTime() - entityData.get(RITUAL_MOVE_START)); }
 public SourceDragonBrain sourceBrain(){return source;}public SourceDragonPart[] sourceParts(){return sourceParts;}
 @Override public EnderDragonPart[] getSubEntities(){return sourceParts==null?super.getSubEntities():sourceParts;}
 @Override public PartEntity<?>[] getParts(){return sourceParts==null?super.getParts():sourceParts;}
 @Override public void setId(int value){super.setId(value);if(sourceParts!=null)for(int i=0;i<sourceParts.length;i++)sourceParts[i].setId(value+i+1);}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(16,8);}
 @Override public void tick(){if(source!=null&&!level().isClientSide)source.harm.beforeTick();super.tick();}
 @Override public void aiStep(){
  if(source==null)return;
  boolean ritual=false;
  if(level() instanceof ServerLevel world){ritual=DragonRitual.owns(world,this);if(SourceEndEntry.consume(world)&&!ritual&&getHealth()>0&&getHealth()<getMaxHealth())setHealth(getHealth()+getMaxHealth()*.5f);if(getDragonFight()==null&&world.getDragonFight()!=null&&getUUID().equals(world.getDragonFight().getDragonUUID()))setDragonFight(world.getDragonFight());}
  int posture = ritualPosture();
  if(level().isClientSide && posture != DragonCombat.WING_RAISE && posture != DragonCombat.WING_SWEEP){float now=SourceDragonTrig.cos(flapTime*(float)Math.PI*2f),last=SourceDragonTrig.cos(oFlapTime*(float)Math.PI*2f);if(last<=-.3f&&now>=-.3f)level().playLocalSound(getX(),getY(),getZ(),SoundEvents.ENDER_DRAGON_FLAP,getSoundSource(),5,.8f+random.nextFloat()*.3f,false);}
  oFlapTime=flapTime;
  if(getHealth()<=0){source.particle(false,getX()+(double)((random.nextFloat()-.5f)*8),getY()+2+(double)((random.nextFloat()-.5f)*4),getZ()+(double)((random.nextFloat()-.5f)*8));return;}
  if (ritual && !isNoAi()) combat.observeHealth(this);
  if (level().isClientSide && level().dimension() == Level.END) {
   int id = entityData.get(RITUAL_CRYSTAL);
   nearestCrystal = id >= 0 && level().getEntity(id) instanceof EndCrystal crystal ? crystal : null;
  } else if (!ritual || !isNoAi()) {
   if (ritual) SourceDragonRules.crystals(source,RITUAL_CRYSTAL_HEAL_INTERVAL,DragonRitual.healthScale(this));
   else SourceDragonRules.crystals(source);
   if (ritual) entityData.set(RITUAL_CRYSTAL, nearestCrystal == null ? -1 : nearestCrystal.getId());
  }
  var motion=getDeltaMovement();flapTime=SourceDragonKinematics.flap(flapTime,motion.x,motion.y,motion.z,inWall);
  if (posture == DragonCombat.APPROACH || posture == DragonCombat.WARNING || posture == DragonCombat.DIVE) {
   float folded = Math.round(oFlapTime - .75f) + .75f;
   flapTime = Mth.lerp(posture == DragonCombat.APPROACH ? .11f : .34f, oFlapTime, folded);
  } else if (posture == DragonCombat.WING_RAISE) {
   float raised = Math.round(oFlapTime - .25f) + .25f;
   flapTime = Mth.lerp(.28f, oFlapTime, raised);
  } else if (posture == DragonCombat.WING_SWEEP) {
   float lowered = Math.round(oFlapTime - .74f) + .75f;
   flapTime = Mth.lerp(.62f, oFlapTime, lowered);
  } else {
   var move = DragonRepertoire.Move.fromPosture(posture);
   if (move != null) {
    long age = ritualMoveAge();
    float pose = switch (move) {
     case TAIL -> age < move.warning ? .48f : .74f;
     case SPIRES, CROSSBEAM -> age < move.warning ? .20f : .52f;
     case VORTEX -> .45f + (float)Math.sin(age * .24) * .18f;
     case RIPPLE -> age < move.warning ? .24f : age - move.warning < 7 ? .76f : .45f;
     case PECK -> age < move.warning ? .59f : .79f;
     case BURST -> age < move.warning ? .14f : .58f;
     case EMBERS -> age < move.warning ? .33f : .60f + (float)Math.sin((age - move.warning) * .5) * .13f;
    };
    float target = Math.round(oFlapTime - pose) + pose;
    flapTime = Mth.lerp(age < move.warning ? .20f : .37f, oFlapTime, target);
   }
  }
  boolean ritualMotion=ritual||posture!=DragonCombat.ORBIT;
  if(!ritualMotion)recordPositionHistory();
  if(level().isClientSide){if(lerpSteps>0){double x=getX()+(lerpX-getX())/(double)lerpSteps,y=getY()+(lerpY-getY())/(double)lerpSteps,z=getZ()+(lerpZ-getZ())/(double)lerpSteps;setYRot((float)((double)getYRot()+wrap(lerpYRot-(double)getYRot())/(double)lerpSteps));setXRot((float)((double)getXRot()+(lerpXRot-(double)getXRot())/(double)lerpSteps));--lerpSteps;setPos(x,y,z);setRot(getYRot(),getXRot());}}
  else if (!isNoAi() && (!ritual || !combat.step((ServerLevel) level(), this))) flight();
  if(ritualMotion)recordPositionHistory();
  if(ritual&&level() instanceof ServerLevel world)DragonRitual.attackLock(world,getUUID(),!isNoAi()&&combat.attacking(this,world.getGameTime()));
  yBodyRot=getYRot();layout(!ritual);
  if (ritual && !isNoAi() && tickCount%4==0 && combat.guardingProjectiles(this) && level() instanceof ServerLevel world) {
   showProjectileGuard(world);
  }
  if(!level().isClientSide){if(!isNoAi()&&!ritual){inWall=source.walls(sourceParts[0].getBoundingBox())|source.walls(sourceParts[1].getBoundingBox());breath.tick(this);}if(getDragonFight()!=null)getDragonFight().updateDragon(this);}
 }
 private void recordPositionHistory(){
  setYRot(wrap(getYRot()));
  if(posPointer<0)for(var history:positions){history[0]=getYRot();history[1]=getY();}
  if(++posPointer==positions.length)posPointer=0;
  positions[posPointer][0]=getYRot();positions[posPointer][1]=getY();
 }
 private void flight(){SourceDragonKinematics.flight(kinematics);}
 void moveRelativeSource(float amount){float x=0f,z=-1f;x*=amount;z*=amount;float angle=getYRot()*(float)Math.PI/180f,sin=SourceDragonTrig.sin(angle),cos=SourceDragonTrig.cos(angle);setDeltaMovement(getDeltaMovement().add((double)(x*cos-z*sin),0,(double)(z*cos+x*sin)));}
 private void layout(boolean originalContact){kinematics.layout(originalContact);head.setPos(sourceParts[0].position());}
 static float wrap(float value){value%=360f;if(value>=180f)value-=360f;if(value< -180f)value+=360f;return value;}static double wrap(double value){value%=360;if(value>=180)value-=360;if(value< -180)value+=360;return value;}
 boolean sourceParentHurt(DamageSource source,float amount){return super.reallyHurt(source,amount);}
 private void showProjectileGuard(ServerLevel level) {
  Vec3 front=Vec3.directionFromRotation(0,getYRot()).scale(-1);
  Vec3 side=new Vec3(-front.z,0,front.x);
  Vec3 center=sourceParts[0].getBoundingBox().getCenter().add(front.scale(.92));
  for (int i=0;i<8;i++) {
   double angle=i*Math.PI/4;
   BossAttackGeometry.point(level,PROJECTILE_GUARD,center.add(side.scale(1.55*Math.cos(angle))).add(0,1.3*Math.sin(angle),0));
  }
 }
 @Override public boolean hurt(EnderDragonPart part, DamageSource damage, float amount) {
  if (source == null) return false;
  boolean weakPoint = ritualPosture() == DragonAltarAttack.CHARGE && (part == sourceParts[0] || part == head);
  boolean ritual = level() instanceof ServerLevel server && DragonRitual.owns(server,this);
  boolean blocked = ritual && damage.is(DamageTypeTags.IS_PROJECTILE) && combat.guardingProjectiles(this);
  float damageScale = ritual ? DragonRitual.healthScale(this) : 1f;
  if (blocked) damageScale *= .25f;
  float before = getHealth();
  boolean accepted = source.hurt(part, damage, amount, damageScale);
  if (blocked && getHealth() < before && level() instanceof ServerLevel server) {
   Vec3 at = part.getBoundingBox().getCenter();
   server.sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,6,.3,.3,.3,.05);
  }
  if (accepted && getHealth() < before && level() instanceof ServerLevel world
      && damage.getEntity() instanceof ServerPlayer attacker && attacker.isAlive() && attacker.level() == world) {
   DragonRitual.contribute(world, this, attacker);
   if (weakPoint && getHealth() > 0) combat.interruptAltar(world, this);
  }
  return accepted;
 }
 @Override public boolean hurt(DamageSource source,float amount){return Boolean.TRUE.equals(SourceDragonRules.hurtDirect());}
 @Override protected void tickDeath() {
  if (source == null) return;
  if (source.state.deathTicks == 0) {
   combat.clearHazards();
   if (level() instanceof ServerLevel world && DragonRitual.owns(world, this)) {
    DragonRitual.clearAttackEffects(world, getUUID());
   }
  }
  if (getDragonFight() != null) getDragonFight().updateDragon(this);
  SourceDragonRules.death(source);
  dragonDeathTime = source.state.deathTicks;
 }
 @Override public void onRemovedFromWorld(){
  if (level() instanceof ServerLevel world) {
   combat.clearHazards();
   BossArenaPortals.release(world, getUUID());
  }
  super.onRemovedFromWorld();
 }
 @Override public void kill(){setHealth(0);}
 @Override public void onCrystalDestroyed(EndCrystal crystal,net.minecraft.core.BlockPos position,DamageSource source){
  if (level() instanceof ServerLevel world && source.getEntity() instanceof ServerPlayer player
      && DragonRitualCrystals.isTowerCrystal(world, crystal)) DragonRitual.contribute(world, this, player);
 }
 @Override protected float getSoundVolume(){return 5;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(source!=null){source.harm.save(tag);combat.save(tag);}}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);if(source!=null){source.state.deathTicks=Math.min(dragonDeathTime,199);source.harm.load(tag);combat.load(tag);}}
 @Override public boolean addEffect(MobEffectInstance effect,Entity source){return ((SourceDragonEffectParent)(Object)this).mite$dragonMobEffect(effect,source);}
 // The original dragon owns its XP drops through the death animation.
 @Override protected void dropExperience(){}
 @Override public int getExperienceReward(){return 100;} // sk: inherited of5 *20.
}
