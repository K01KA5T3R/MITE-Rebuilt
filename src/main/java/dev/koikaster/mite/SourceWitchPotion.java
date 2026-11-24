package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;

/** uu/uq native owner. Constructor Gaussian draws, blunt raycast, integration
 * order and splash effects belong to this class, not native ThrownPotion. */
public final class SourceWitchPotion extends ThrownPotion {
 private Entity sourceOwner;private int airTicks,groundTicks,raycastOffset;
 private boolean embedded;private BlockPos tile=new BlockPos(-1,-1,-1);private Block inTile=Blocks.AIR;private int shake;
 public SourceWitchPotion(EntityType<? extends ThrownPotion> type,Level level){super(type,level);}
 SourceWitchPotion(SourceWitch witch,int subtype){
  this(SourceWitchContent.PROJECTILE.get(),witch.level());setOwner(witch);setItem(SourceWitchContent.potion(subtype));
  moveTo(witch.getX(),witch.getEyeY(),witch.getZ(),witch.getYRot(),witch.getXRot());
  float yaw=getYRot()/180f*(float)Math.PI,pitch=getXRot()/180f*(float)Math.PI;
  setPos(getX()-(double)(Mth.cos(yaw)*.16f),getY()-(double).1f,getZ()-(double)(Mth.sin(yaw)*.16f));
  float power=.4f;double x=-Mth.sin(yaw)*Mth.cos(pitch)*power,z=Mth.cos(yaw)*Mth.cos(pitch)*power,y=-Mth.sin((getXRot()-20)/180f*(float)Math.PI)*power;
  // nn.isSuspendedInLiquid first excludes grounded/riding actors. Its final
  // getBlockRestingOn3 query always returns null for this airborne branch.
  float wander=!witch.isPassenger()&&!witch.onGround()&&(witch.isInWater()||witch.isInLava())?2:1;shoot(x,y,z,.5f,wander);
 }
 @Override public void setOwner(Entity owner){sourceOwner=owner;super.setOwner(owner);}
 @Override public Entity getOwner(){if(sourceOwner!=null)return sourceOwner;return sourceOwner=super.getOwner();}
 @Override public void shoot(double x,double y,double z,float velocity,float wander){
  float length=(float)Math.sqrt(x*x+y*y+z*z);x/=length;y/=length;z/=length;
  x+=random.nextGaussian()*(double).0075f*(double)wander;y+=random.nextGaussian()*(double).0075f*(double)wander;z+=random.nextGaussian()*(double).0075f*(double)wander;
  x*=velocity;y*=velocity;z*=velocity;setDeltaMovement(x,y,z);
  float horizontal=(float)Math.sqrt(x*x+z*z);setYRot((float)(Math.atan2(x,z)*180/Math.PI));setXRot((float)(Math.atan2(y,horizontal)*180/Math.PI));yRotO=getYRot();xRotO=getXRot();groundTicks=0;
 }
 int nextRaycastSeed(){return ++raycastOffset+getId()&32767;}
 @Override public void tick(){
  ((SourceThrowableNativeParent)(Object)this).mite$throwableEntityTick();if(shake>0)--shake;
  if(embedded){if(level().getBlockState(tile).is(inTile)){if(++groundTicks==1200)discard();return;}
   embedded=false;var old=getDeltaMovement();setDeltaMovement(old.x*(double)(random.nextFloat()*.2f),old.y*(double)(random.nextFloat()*.2f),old.z*(double)(random.nextFloat()*.2f));groundTicks=airTicks=0;
  }else ++airTicks;
  var motion=getDeltaMovement();var origin=position();var future=origin.add(motion);var block=SourceWitchPotionRaycast.blocks(this,origin,future);var limit=block==null?future:block.getLocation();HitResult hit=block;
  if(!level().isClientSide){var entity=SourceWitchPotionRaycast.entities(this,origin,limit,airTicks);if(entity!=null)hit=entity;}
  if(hit instanceof BlockHitResult at&&level().getBlockState(at.getBlockPos()).is(Blocks.NETHER_PORTAL))handleInsidePortal(at.getBlockPos());
  else if(hit!=null&&!ForgeEventFactory.onProjectileImpact(this,hit))onHit(hit);
  setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
  float horizontal=(float)Math.sqrt(motion.x*motion.x+motion.z*motion.z),yaw=(float)(Math.atan2(motion.x,motion.z)*180/Math.PI),pitch=(float)(Math.atan2(motion.y,horizontal)*180/Math.PI);
  while(pitch-xRotO< -180)xRotO-=360;while(pitch-xRotO>=180)xRotO+=360;while(yaw-yRotO< -180)yRotO-=360;while(yaw-yRotO>=180)yRotO+=360;
  setXRot(xRotO+(pitch-xRotO)*.2f);setYRot(yRotO+(yaw-yRotO)*.2f);float drag=.99f;
  if(isInWater()){for(int i=0;i<4;i++)level().addParticle(ParticleTypes.BUBBLE,getX()-motion.x*(double).25f,getY()-motion.y*(double).25f,getZ()-motion.z*(double).25f,motion.x,motion.y,motion.z);drag=.8f;}
  setDeltaMovement(motion.x*(double)drag,motion.y*(double)drag-(double).05f,motion.z*(double)drag);setPos(getX(),getY(),getZ());
 }
 @Override protected void onHit(HitResult hit){
  if(level().isClientSide)return;var thrower=getOwner();var direct=hit instanceof EntityHitResult entity?entity.getEntity():null;
  for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(4,2,4))){
   double squared=distanceToSqr(target);if(!(squared<16))continue;double factor=target==direct?1:1-Math.sqrt(squared)/4;
   for(var definition:PotionUtils.getMobEffects(getItem())){
    var effect=definition.getEffect();boolean harmful=effect.getCategory()==MobEffectCategory.HARMFUL;
    if(harmful&&(target==thrower&&!(thrower instanceof Player)||thrower instanceof Witch&&target instanceof Wolf wolf&&OriginalWolfOwners.handles(wolf)&&OriginalWolfOwners.owner(wolf).life.witchAlly))continue;
    // ni.a uses the victim as indirectMagic's direct entity and the thrower
    // as responsible entity. Keep that attribution rather than inserting uu.
    if(effect.isInstantenous())effect.applyInstantenousEffect(target,thrower,target,definition.getAmplifier(),factor);
    else{int duration=(int)(factor*(double)definition.getDuration()+.5);if(duration>20)target.addEffect(new MobEffectInstance(effect,duration,definition.getAmplifier()));}
   }
  }
  level().levelEvent(2002,new BlockPos((int)Math.round(getX()),(int)Math.round(getY()),(int)Math.round(getZ())),PotionUtils.getColor(getItem()));discard();
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){
  super.addAdditionalSaveData(tag);tag.putShort("xTile",(short)tile.getX());tag.putShort("yTile",(short)tile.getY());tag.putShort("zTile",(short)tile.getZ());tag.putInt("SourceTileX",tile.getX());tag.putInt("SourceTileY",tile.getY());tag.putInt("SourceTileZ",tile.getZ());tag.putString("SourceInTile",net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(inTile).toString());tag.putByte("inGround",(byte)(embedded?1:0));tag.putByte("shake",(byte)shake);tag.putInt("potionValue",SourceWitchContent.subtype(getItem()));
 }
 @Override public void readAdditionalSaveData(CompoundTag tag){
  sourceOwner=null;super.readAdditionalSaveData(tag);tile=new BlockPos(tileCoordinate(tag,0),tileCoordinate(tag,1),tileCoordinate(tag,2));var id=ResourceLocation.tryParse(tag.getString("SourceInTile"));inTile=id==null?Blocks.AIR:net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);if(inTile==null)inTile=Blocks.AIR;
  embedded=tag.getByte("inGround")==1;shake=tag.getByte("shake")&255;airTicks=groundTicks=0;if(tag.contains("potionValue"))setItem(SourceWitchContent.potion(tag.getInt("potionValue")));
 }
 private static int tileCoordinate(CompoundTag tag,int axis){
  String full=new String[]{"SourceTileX","SourceTileY","SourceTileZ"}[axis],legacy=new String[]{"xTile","yTile","zTile"}[axis];if(tag.contains(full))return tag.getInt(full);
  int value=tag.getShort(legacy);var position=tag.getList("Pos",net.minecraft.nbt.Tag.TAG_DOUBLE);
  // Old uq stores signed shorts. Embedded tiles belong beside the saved
  // entity position; recover upper bits for old far-coordinate test/legacy saves.
  if(tag.getByte("inGround")==1&&position.size()==3){int reference=Mth.floor(position.getDouble(axis)),candidate=(reference&~65535)|(value&65535);if(candidate-reference>32768)candidate-=65536;else if(reference-candidate>32768)candidate+=65536;return candidate;}
  return value;
 }
}
