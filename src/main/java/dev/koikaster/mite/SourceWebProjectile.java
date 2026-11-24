package dev.koikaster.mite;
import java.util.Objects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
/** EntityWeb + the active flying path of uq. No modern ThrowableProjectile collision/launch approximation. */
public final class SourceWebProjectile extends Projectile {
 public enum ImpactFx {LAVA_BURN,SPLASH}
 public interface Effects {void send(SourceWebProjectile projectile,ImpactFx kind);}
 private static Effects effects;
 private int airTicks;
 public static void effects(Effects sender){effects=Objects.requireNonNull(sender);}
 private void fx(ImpactFx kind){if(effects==null)throw new IllegalStateException("Source web impact FX owner is not connected");effects.send(this,kind);}
 public SourceWebProjectile(EntityType<? extends SourceWebProjectile> type,Level level){super(type,level);}
 public SourceWebProjectile(Level level,LivingEntity thrower){
  this(SourceArachnidContent.WEB.get(),level);setOwner(thrower);
  if(!(thrower instanceof Mob))throw new IllegalArgumentException("This constructor only ports Arachnid-fired webs, not player throwable accuracy");
  setYRot(thrower.getYRot());setXRot(thrower.getXRot());
  float yaw=getYRot()/180f*(float)Math.PI,pitch=getXRot()/180f*(float)Math.PI;
  setPos(thrower.getX()-(double)(Mth.cos(yaw)*.16f),thrower.getY()+thrower.getEyeHeight()-(double).1f,thrower.getZ()-(double)(Mth.sin(yaw)*.16f));
  float wander=thrower.isInWater()||thrower.isInLava()?2:1;
  shootSource(-Mth.sin(yaw)*Mth.cos(pitch)*.4f,-Mth.sin(pitch)*.4f,Mth.cos(yaw)*Mth.cos(pitch)*.4f,.8f,wander);
 }
 @Override protected void defineSynchedData(){}
 /** uq.c normalizes by the source float sqrt and consumes three Gaussian calls even with wander zero. */
 public void shootSource(double x,double y,double z,float speed,float wander){
  float length=(float)Math.sqrt(x*x+y*y+z*z);x/=length;y/=length;z/=length;
  x+=random.nextGaussian()*(double).0075f*(double)wander;y+=random.nextGaussian()*(double).0075f*(double)wander;z+=random.nextGaussian()*(double).0075f*(double)wander;
  x*=(double)speed;y*=(double)speed;z*=(double)speed;setDeltaMovement(x,y,z);
  float horizontal=(float)Math.sqrt(x*x+z*z);setYRot((float)(Math.atan2(x,z)*180/Math.PI));setXRot((float)(Math.atan2(y,horizontal)*180/Math.PI));yRotO=getYRot();xRotO=getXRot();
 }
 @Override public void tick(){
  super.tick();++airTicks;Vec3 from=position(),motion=getDeltaMovement(),to=from.add(motion);
  var block=SourceWebRaycast.blocks(this,from,to);Vec3 end=block==null?to:block.getLocation();HitResult hit=block;
  if(!level().isClientSide){var entity=SourceWebRaycast.entities(this,from,end,target->target!=getOwner()||airTicks>=5);if(entity!=null&&(block==null||from.distanceTo(entity.getLocation())<=from.distanceTo(block.getLocation())))hit=entity;}
  if(hit instanceof BlockHitResult solid&&level().getBlockState(solid.getBlockPos()).is(Blocks.NETHER_PORTAL))handleInsidePortal(solid.getBlockPos());
  else if(hit!=null)impact(hit);
  // uq continues its final flight step even when onImpact removes the entity.
  setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
  float horizontal=(float)Math.sqrt(motion.x*motion.x+motion.z*motion.z);
  float yaw=(float)(Math.atan2(motion.x,motion.z)*180/Math.PI),pitch=(float)(Math.atan2(motion.y,horizontal)*180/Math.PI);
  while(pitch-xRotO< -180)xRotO-=360;while(pitch-xRotO>=180)xRotO+=360;while(yaw-yRotO< -180)yRotO-=360;while(yaw-yRotO>=180)yRotO+=360;
  setXRot(xRotO+(pitch-xRotO)*.2f);setYRot(yRotO+(yaw-yRotO)*.2f);
  float drag=.99f;
  if(isInWater()){for(int i=0;i<4;i++)level().addParticle(ParticleTypes.BUBBLE,getX()-motion.x*(double).25f,getY()-motion.y*(double).25f,getZ()-motion.z*(double).25f,motion.x,motion.y,motion.z);drag=.8f;}
  setDeltaMovement(motion.x*(double)drag,motion.y*(double)drag-(double).03f,motion.z*(double)drag);
 }
 private void retire(){SourceWebEffects.retainClientActor(this);discard();}
 private void impact(HitResult result){
  if(result instanceof EntityHitResult hit){if(!level().isClientSide&&hit.getEntity() instanceof LivingEntity target&&SourceWebImpact.entity(target,isOnFire()))retire();return;}
  var hit=(BlockHitResult)result;var pos=hit.getBlockPos();var neighbor=pos.relative(hit.getDirection());var state=level().getBlockState(pos);var next=level().getBlockState(neighbor);
  if(next.is(Blocks.FIRE)||state.getFluidState().is(FluidTags.LAVA)||next.getFluidState().is(FluidTags.LAVA)){
   if(!level().isClientSide)fx(ImpactFx.LAVA_BURN);retire();
  }else if(state.getBlock() instanceof LiquidBlock||next.getFluidState().is(FluidTags.WATER)){
   if(!level().isClientSide&&state.getBlock() instanceof LiquidBlock&&state.getFluidState().is(FluidTags.WATER)&&state.getFluidState().isSource())fx(ImpactFx.SPLASH);retire();
  }else{
   if(!level().isClientSide){var at=SourceWebImpact.replaceable(level(),pos)?pos:neighbor;if(!SourceWebImpact.place(level(),at,isOnFire()))SourceWebImpact.place(level(),at.above(),isOnFire());}
   retire();
  }
 }
 @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);}
 @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);airTicks=0;}
}
