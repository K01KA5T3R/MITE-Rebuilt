package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;

/** uv over the sealed source uj parent. Native identity and packet transport only. */
public final class SourceWitherSkull extends WitherSkull implements SourceBlazeRaycast.Seed {
 private int airTicks,groundTicks,raycastOffset;private BlockPos tile=BlockPos.ZERO;private net.minecraft.world.level.block.Block inTile=Blocks.AIR;private boolean embedded;private Entity sourceOwner;
 private Vec3 markedLanding;
 private float waveAngle;
 private boolean ritualShot;
 public SourceWitherSkull(EntityType<? extends WitherSkull> type,Level level){super(type,level);}
 @Override public void setOwner(Entity owner){super.setOwner(owner);sourceOwner=owner;}
 /** uj retains the removed shooter reference; native UUID lookup forgets it. */
 @Override public Entity getOwner(){if(sourceOwner==null)sourceOwner=super.getOwner();return sourceOwner;}
 public SourceWitherSkull(Level level,LivingEntity owner,double dx,double dy,double dz){
  this(EntityType.WITHER_SKULL,level);setOwner(owner);moveTo(owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot());setDeltaMovement(Vec3.ZERO);
  dx+=random.nextGaussian()*.4;dy+=random.nextGaussian()*.4;dz+=random.nextGaussian()*.4;
  double length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);xPower=dx/length*.1;yPower=dy/length*.1;zPower=dz/length*.1;
 }
 void markLanding(Vec3 landing,float angle){
  markedLanding=landing;waveAngle=angle;setDangerous(true);
  Vec3 direction=landing.add(0,-.18,0).subtract(position()).normalize();
  xPower=direction.x*.1;yPower=direction.y*.1;zPower=direction.z*.1;
  setDeltaMovement(direction.scale(.40));
 }
 void markRitualShot(){ritualShot=true;}
 void aimRitualShot(Vec3 target){
  markRitualShot();
  Vec3 direction=target.subtract(position()).normalize();
  xPower=direction.x*.1;yPower=direction.y*.1;zPower=direction.z*.1;
  setDeltaMovement(direction.scale(.53));
 }
 @Override public boolean shouldBeSaved(){return !ritualShot&&super.shouldBeSaved();}
 @Override public int nextRaycastSeed(){return ++raycastOffset+getId()&32767;}
 @Override public void tick(){
  var owner=getOwner();if(!level().isClientSide&&(owner!=null&&owner.isRemoved()||!level().hasChunkAt(blockPosition()))){discard();return;}
  if(!level().isClientSide&&ritualShot&&(owner==null||!owner.isAlive()||airTicks>=104)){discard();return;}
  ((SourceFireballNativeParent)(Object)this).mite$fireballEntityTick();setSecondsOnFire(1);
  if(embedded){if(level().getBlockState(tile).is(inTile)){if(++groundTicks==600)discard();return;}embedded=false;var velocity=getDeltaMovement();setDeltaMovement(velocity.x*(double)(random.nextFloat()*.2f),velocity.y*(double)(random.nextFloat()*.2f),velocity.z*(double)(random.nextFloat()*.2f));groundTicks=0;airTicks=0;}else ++airTicks;
  var motion=getDeltaMovement();var from=position();var to=from.add(motion);var block=SourceWitherSkullRaycast.blocks(this,from,to);var limit=block==null?to:block.getLocation();HitResult hit=block;var entity=SourceWitherSkullRaycast.entities(this,from,limit,airTicks);
  if(entity!=null)hit=entity;
  if(level() instanceof ServerLevel world&&ritualShot&&owner instanceof SourceWitherBoss boss&&airTicks%8==1)
   BossSkullTrail.sync(world,boss,this);
  if(hit!=null&&!ForgeEventFactory.onProjectileImpact(this,hit))impact(hit);
  if(!level().isClientSide&&markedLanding!=null&&to.subtract(markedLanding).dot(new Vec3(xPower,yPower,zPower))>.18){
   discard();return;
  }
  setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
  float horizontal=(float)Math.sqrt(motion.x*motion.x+motion.z*motion.z);float yaw=(float)(Math.atan2(motion.z,motion.x)*180/Math.PI)+90,pitch=(float)(Math.atan2(horizontal,motion.y)*180/Math.PI)-90;
  while(pitch-xRotO< -180)xRotO-=360;while(pitch-xRotO>=180)xRotO+=360;while(yaw-yRotO< -180)yRotO-=360;while(yaw-yRotO>=180)yRotO+=360;
  setXRot(xRotO+(pitch-xRotO)*.2f);setYRot(yRotO+(yaw-yRotO)*.2f);
  float drag=SourceWitherSkullRules.drag(isDangerous());if(isInWater()){for(int i=0;i<4;i++)level().addParticle(ParticleTypes.BUBBLE,getX()-motion.x*(double).25f,getY()-motion.y*(double).25f,getZ()-motion.z*(double).25f,motion.x,motion.y,motion.z);drag=.8f;}
  setDeltaMovement((motion.x+xPower)*(double)drag,(motion.y+yPower)*(double)drag,(motion.z+zPower)*(double)drag);level().addParticle(ParticleTypes.SMOKE,getX(),getY()+.5,getZ(),0,0,0);setPos(getX(),getY(),getZ());
 }
 private void impact(HitResult hit){
  SourceWitherSkullRules.impact(new SourceWitherSkullRules.Host(){
   final Entity victim=hit instanceof EntityHitResult contact?contact.getEntity():null;
   final LivingEntity shooter=getOwner() instanceof LivingEntity living?living:null;
   private boolean accepted;
   public boolean client(){return level().isClientSide;}public boolean entityHit(){return victim!=null;}public boolean owner(){return shooter!=null;}public boolean livingHit(){return victim instanceof LivingEntity;}public int difficulty(){return level().getDifficulty().getId();}
   public boolean damage(float amount,boolean owned){var damage=owned?damageSources().mobAttack(shooter):damageSources().magic();
    if(owned&&ritualShot)amount=markedLanding==null?15:17;
    if(victim instanceof LivingEntity living)try(var frame=SourceParentHurtFrame.beginObserver(living,outcome->{})){accepted=victim.hurt(damage,amount);return frame.finish().destroyed();}
    boolean alive=victim.isAlive();return victim.hurt(damage,amount)&&alive&&!victim.isAlive();
   }
   public void healOwner(float amount){SourceWitherFx.heal(shooter,amount);}
   public void wither(int ticks,int amplifier){
    if(ritualShot&&victim instanceof net.minecraft.server.level.ServerPlayer){
     if(!accepted)return;
     ticks=BossCombatTuning.witherDuration(ticks);
    }
    ((LivingEntity)victim).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,ticks,amplifier));
   }
   public void explode(){if(level() instanceof ServerLevel world)explodeAt(world,hit);}
   public void discard(){SourceWitherSkull.this.discard();}
  });
 }
 private void explodeAt(ServerLevel level,HitResult hit){
  boolean griefing=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
  Vec3 contact=hit.getLocation();
  if(markedLanding!=null&&hit instanceof BlockHitResult block&&block.getDirection()==Direction.UP
      &&contact.distanceToSqr(markedLanding)<=1&&getOwner() instanceof SourceWitherBoss wither&&wither.ritual()!=null){
   float power=WitherCombat.HEAVY_POWER;
   if(ritualShot)BossCombatVisual.burst(level,wither,contact.add(0,.35,0),2.2);
   SourceExplosion.create(level,this,contact.x,contact.y,contact.z,power,power,false,griefing);
   wither.ritual().combat.landed(contact,waveAngle);
  }else {
   if(ritualShot&&getOwner() instanceof SourceWitherBoss wither)BossCombatVisual.burst(level,wither,position(),1.6);
   SourceExplosion.create(level,this,getX(),getY(),getZ(),1,1,false,griefing);
  }
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putShort("xTile",(short)tile.getX());tag.putShort("yTile",(short)tile.getY());tag.putShort("zTile",(short)tile.getZ());tag.putString("SourceInTile",net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(inTile).toString());tag.putByte("inGround",(byte)(embedded?1:0));var motion=getDeltaMovement();tag.put("direction",newDoubleList(motion.x,motion.y,motion.z));tag.put("acceleration",newDoubleList(xPower,yPower,zPower));
  if(markedLanding!=null){tag.put("MiteWitherLanding",newDoubleList(markedLanding.x,markedLanding.y,markedLanding.z));tag.putFloat("MiteWitherWaveAngle",waveAngle);}
 }
 @Override public void readAdditionalSaveData(CompoundTag tag){sourceOwner=null;super.readAdditionalSaveData(tag);tile=new BlockPos(tag.getShort("xTile"),tag.getShort("yTile"),tag.getShort("zTile"));var id=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("SourceInTile"));inTile=id==null?Blocks.AIR:net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);if(inTile==null)inTile=Blocks.AIR;embedded=tag.getByte("inGround")==1;airTicks=groundTicks=0;var direction=tag.getList("direction",Tag.TAG_DOUBLE);var acceleration=tag.getList("acceleration",Tag.TAG_DOUBLE);if(direction.size()==3&&acceleration.size()==3){setDeltaMovement(direction.getDouble(0),direction.getDouble(1),direction.getDouble(2));xPower=acceleration.getDouble(0);yPower=acceleration.getDouble(1);zPower=acceleration.getDouble(2);}else if(!tag.contains("power",Tag.TAG_LIST))discard();
  var landing=tag.getList("MiteWitherLanding",Tag.TAG_DOUBLE);
  markedLanding=landing.size()==3?new Vec3(landing.getDouble(0),landing.getDouble(1),landing.getDouble(2)):null;
  waveAngle=tag.getFloat("MiteWitherWaveAngle");
 }
}
