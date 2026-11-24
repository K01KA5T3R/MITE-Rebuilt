package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
/** R195 tf plus Infernal leaf. Native parent bridges skip post-1.6.4 Creeper behavior. */
public class SourceCreeper extends Creeper implements OriginalParentHarmState.Flags {
 private int previousSwell,swell,fuse=30,cactusTicks;private float radius;private boolean exploded,decided,fleeing;
 private final OriginalParentHarmState harm;
 public SourceCreeper(EntityType<? extends Creeper> type,Level world){
  super(type,world);radius=infernal()?2:1;getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.25);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
  harm=new OriginalParentHarmState(this,this);
  goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new SourceCreeperSwell(this));goalSelector.addGoal(3,new AvoidEntityGoal<>(this,Ocelot.class,6,1,1.2));goalSelector.addGoal(4,new OriginalMeleeGoal(this,null,1,false,null));goalSelector.addGoal(5,new OriginalWanderGoal(this,.8));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
  targetSelector.addGoal(1,new OriginalNearestTargetGoal(this,Player.class,0,true));targetSelector.addGoal(2,new OriginalHurtByGoal(this,false));refreshDimensions();
 }
 @Override protected void registerGoals(){}
 public static AttributeSupplier.Builder attributes(){return Creeper.createAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,32).add(Attributes.ATTACK_DAMAGE,2);}
 public boolean infernal(){return getType()==SourceCreeperContent.INFERNAL.get();}
 public int cactusCountdown(){return cactusTicks;}
 public void iteFuse(int value){this.fuse=Math.max(1,value);}
 public int iteFuse(){return this.fuse;}
 public void iteRadius(float value){this.radius=Math.max(0.5F,value);}
 public boolean hasExplodedSource(){return exploded;}
 public float naturalDefense(DamageSource source){return infernal()&&(source==null||!ClassicDefense.mundaneBypass(source))?2:0;}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.6f,1.8f);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height*.85f;}
 @Override public int getMaxSpawnClusterSize(){return 2;}
 @Override public int getExperienceReward(){return infernal()?15:5;}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override public float getSwelling(float partial){return ((float)previousSwell+(float)(swell-previousSwell)*partial)/(float)(fuse-2);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){return InteractionResult.PASS;}
 @Override public void tick(){
  if(!level().isClientSide&&harm!=null)harm.beforeTick();if(cactusTicks>0)--cactusTicks;
  if(isAlive()){
   previousSwell=swell;int direction=getSwellDir();if(direction>0&&swell==0&&level() instanceof ServerLevel server)server.playSeededSound(null,getX(),getY(),getZ(),SoundEvents.CREEPER_PRIMED,SoundSource.HOSTILE,1,.5f,0);
   swell+=direction;if(swell<0)swell=0;
   if(swell>=fuse){swell=fuse;if(level() instanceof ServerLevel server){float blocks=radius*.715f,living=radius*1.1f,power=isPowered()?2:1;SourceExplosion.create(server,this,getX(),getY()+(double)(getBbHeight()/4f),getZ(),blocks*power,living*power,false,server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING));exploded=true;SourceCreeperFragments.send(this);discard();}}
  }
  ((SourceCreeperNativeParent)(Object)this).mite$creeperParentTick();
 }
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){boolean hurt=((SourceCreeperNativeParent)(Object)this).mite$creeperParentFall(distance,multiplier,source);swell=(int)((float)swell+distance*1.5f);if(swell>fuse-5)swell=fuse-5;return hurt;}
 @Override public void setTarget(LivingEntity target){((SourceCreeperNativeParent)(Object)this).mite$creeperParentTarget(target);}
 @Override public boolean canDropMobsSkull(){return false;}
 @Override public boolean doHurtTarget(Entity target){return ((SourceCreeperNativeParent)(Object)this).mite$creeperParentStrike(target);}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.getDirectEntity() instanceof Snowball&&!SourceSnowballTraits.proper(source)){if(source.getEntity() instanceof Player)return false;amount=1;}
  if(level().isClientSide||harm==null)return super.hurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->{
   if(outcome.lostHealth()&&source.is(DamageTypes.CACTUS)&&SourceCactusHarm.activeKills()>1&&random.nextInt(2)==0)cactusTicks=120;
   harm.afterDamage(source,outcome.negativelyAffected());var responsible=source.getEntity();if(!outcome.destroyed()&&outcome.negativelyAffected()&&responsible!=this&&responsible!=getVehicle()&&responsible!=getFirstPassenger()&&(responsible==null||responsible instanceof LivingEntity))setTarget((LivingEntity)responsible);
  })){boolean accepted=super.hurt(source,amount);frame.finish();return accepted;}
 }
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,position().add(0,(double)(getBbHeight()/4f),0),Double.MAX_VALUE,false)||super.hasLineOfSight(target);if(seen&&target instanceof Player)OriginalDespawn.refresh(this,-1200);return seen;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putShort("Fuse",(short)fuse);tag.putFloat("ExplosionRadius",radius);tag.remove("ignited");if(harm!=null)harm.save(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("Fuse"))fuse=tag.getShort("Fuse");if(tag.contains("ExplosionRadius"))radius=tag.getFloat("ExplosionRadius");if(harm!=null)harm.load(tag);}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){
  int count=infernal()?random.nextInt(4):random.nextInt(3);if(infernal()&&count==0)count=random.nextInt(3);if(looting>0)count+=random.nextInt(looting+1);if(count>0&&!recent)count-=random.nextInt(count+1);
  for(int i=0;i<count;i++)if(((SourceMobAi)this).mite$recentPlayerHitTicks()>0||random.nextInt(3)==0)spawnAtLocation(Items.GUNPOWDER);
 }
 @Override public void die(DamageSource source){
  super.die(source);
  if(source.getEntity() instanceof AbstractSkeleton){var discs=new Item[]{Items.MUSIC_DISC_13,Items.MUSIC_DISC_CAT,Items.MUSIC_DISC_BLOCKS,Items.MUSIC_DISC_CHIRP,Items.MUSIC_DISC_FAR,Items.MUSIC_DISC_MALL,Items.MUSIC_DISC_MELLOHI,Items.MUSIC_DISC_STAL,Items.MUSIC_DISC_STRAD,Items.MUSIC_DISC_WARD,Items.MUSIC_DISC_11,Items.MUSIC_DISC_WAIT};spawnAtLocation(discs[random.nextInt(discs.length)]);}
 }
}
