package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** EntityBoneLord, including the source summoning budget and troop reclamation. */
public class SourceBoneLord extends SourceSkeleton {
 private int troopsSummoned;
 public SourceBoneLord(EntityType<? extends SourceBoneLord> type,Level level){super(type,level);}
 public boolean ancient(){return false;}
 public static AttributeSupplier.Builder attributes(boolean ancient){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,ancient?24:20).add(Attributes.FOLLOW_RANGE,40).add(Attributes.MOVEMENT_SPEED,ancient?.27f:.26f).add(Attributes.ATTACK_DAMAGE,ancient?8:5).add(Attributes.ARMOR,0);}
 @Override public int getExperienceReward(){return ancient()?30:15;}
 @Override public int getMaxSpawnClusterSize(){return 1;}
 int troopsSummoned(){return troopsSummoned;}
 @Override protected void addRandomWeapon(){
  int weight=4;
  if(!ancient()&&level() instanceof ServerLevel world){
   int day=SourceTournament.mode(world.getServer())==null?SourceMoonCycle.dayOfWorld(world.getGameTime()):1;
   weight=day>=20?4:day>=10?3:2;
  }
  int choice=random.nextInt(weight);String weapon=choice<2?"sword":choice==2?"battleaxe":"warhammer";
  setItemSlot(EquipmentSlot.MAINHAND,randomEquipment(Content.get((ancient()?"ancient_metal_":"rusted_iron_")+weapon),true));
 }
 @Override protected void addRandomEquipment(){
  addRandomWeapon();String material=ancient()?"ancient_metal_":"rusted_iron_";
  setItemSlot(EquipmentSlot.FEET,randomEquipment(Content.get(material+"boots"),true));setItemSlot(EquipmentSlot.LEGS,randomEquipment(Content.get(material+"leggings"),true));
  setItemSlot(EquipmentSlot.CHEST,randomEquipment(Content.get(material+"chestplate"),true));setItemSlot(EquipmentSlot.HEAD,randomEquipment(Content.get(material+"helmet"),true));
 }
 @Override public void aiStep(){
  super.aiStep();if(level().isClientSide)return;
  if(!isPersistenceRequired()&&getTarget() instanceof Player)setPersistenceRequired();
  if((tickCount+getId()*47)%20==0)commandTroops();
 }
 /** One source20-tick command, also used by the new owner's focused native tests. */
 void commandTroops(){
  LivingEntity target=getTarget();
  if(target!=null&&(target.isRemoved()||!target.isAlive()||distanceTo(target)>16||!OriginalVision.see(this,target)))target=null;
  if(target instanceof Player&&troopsSummoned<6&&random.nextInt(8)<7-troopsSummoned){
   troopsSummoned+=trySummonTroop(target);
   if(troopsSummoned<6&&random.nextBoolean())troopsSummoned+=trySummonTroop(target);
  }
  for(var skeleton:level().getEntitiesOfClass(AbstractSkeleton.class,getBoundingBox().inflate(16,8,16))){
   if(skeleton!=this&&!OriginalVision.from(skeleton,this,skeleton.getEyePosition(),Double.MAX_VALUE,true))continue;
   if(skeleton.getHealth()<skeleton.getMaxHealth())skeleton.heal(1);
   if(skeleton instanceof SourceBoneLord)continue;
   if(skeleton instanceof SourceSkeleton ordinary)ordinary.frenziedByBoneLord(target);
   else if(skeleton instanceof SourceLongdead longdead)longdead.frenziedByBoneLord(target);
   else if(skeleton instanceof SourceWitherSkeleton wither)wither.frenziedByBoneLord(target);
   else continue; // Unported native Stray owners are not fake source troops.
   boolean exact=ancient()?skeleton.getClass()==SourceLongdead.class:skeleton.getClass()==SourceSkeleton.class;
   if(troopsSummoned<=0||!exact||!((skeleton.getTarget()==null&&skeleton.getHealth()/skeleton.getMaxHealth()>=1&&random.nextFloat()<.05f)||OriginalDespawn.counter(skeleton)>=0))continue;
   int counter=OriginalDespawn.counter(skeleton);OriginalDespawn.set(skeleton,Integer.MAX_VALUE);
   // R195 leaves the MAX value when mayDespawn() itself refuses removal.
   if(!OriginalDespawn.mayDespawn(skeleton))continue;
   OriginalDespawn.tryDespawn(skeleton);if(skeleton.isRemoved())troopsSummoned--;else OriginalDespawn.set(skeleton,counter);
  }
 }
 private int trySummonTroop(LivingEntity target){
  int maxAttempts=48-troopsSummoned*8;var foot=blockPosition();
  Vec3 leg=position().add(0,getBbHeight()*.25f,0),head=position().add(0,getBbHeight()*.75f,0);
  for(int attempts=0;attempts<maxAttempts;attempts++){
   var skeleton=SourceTroopPlacement.createCloseTo(this,ancient());if(skeleton==null)continue;
   int type=ancient()?(level().dimension()==Level.NETHER?1:skeleton.getRandom().nextFloat()<.5f?2:0):((SourceSkeleton)skeleton).randomSkeletonType();
   boolean rejected=false;double summonerDistance=skeleton.distanceToSqr(this);
   for(var player:level().players()){
    if(player.isAlive()&&!player.isSpectator()&&!player.isCreative()&&skeleton.distanceToSqr(player)<16){rejected=true;break;}
    // Source's second exclusion includes every living player, including creative.
    if(player.isAlive()&&skeleton.distanceToSqr(player)<summonerDistance){rejected=true;break;}
   }
   if(rejected||type==0&&!OriginalVision.from(skeleton,target,skeleton.getEyePosition(),Double.MAX_VALUE,true))continue;
   var eye=skeleton.getEyePosition();boolean seesLord=eye.distanceToSqr(leg)<256&&SourceTroopPlacement.projectileLine(skeleton,leg)||eye.distanceToSqr(head)<256&&SourceTroopPlacement.projectileLine(skeleton,head);
   if(!seesLord&&!SourceTroopPlacement.pathEndsNear(skeleton,foot))continue;
   if(type==2&&!SourceTroopPlacement.pathEndsNear(skeleton,target.blockPosition()))continue;
   OriginalDespawn.refresh(skeleton,-9600);
   if(!level().addFreshEntity(skeleton))continue;
   if(skeleton instanceof SourceLongdead longdead)longdead.finalizeSummonedSpawn((ServerLevel)level(),type);
   else{var ordinary=(SourceSkeleton)skeleton;ordinary.forceSkeletonType(type);ordinary.finalizeSpawn((ServerLevel)level(),level().getCurrentDifficultyAt(skeleton.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);}
   skeleton.setTarget(target);SourceSummonedFx.send(skeleton);return 1;
  }
  return 0;
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(troopsSummoned>0)tag.putByte("num_troops_summoned",(byte)troopsSummoned);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);troopsSummoned=tag.getByte("num_troops_summoned");}
}
