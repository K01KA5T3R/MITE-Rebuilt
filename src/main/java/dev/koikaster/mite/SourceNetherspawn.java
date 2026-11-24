package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/** Actual EntityNetherspawn leaf of tq; retains fire versus lava distinction. */
public final class SourceNetherspawn extends SourceSilverfish {
 private int fizzTicks;
 public SourceNetherspawn(EntityType<? extends Silverfish> type,Level level){super(type,level);setPathfindingMalus(BlockPathTypes.WATER,8);}
 @Override protected boolean canTakePlayerSnowball(){return true;}
 public boolean sourceWet(){return isInWater()||OriginalWeather.rain(level(),blockPosition())||OriginalWeather.rain(level(),BlockPos.containing(getX(),getY()+getBbHeight(),getZ()));}
 @Override public void aiStep(){
  if(level().isClientSide)SourceNetherspawnClient.tick(this);
  else if(sourceWet()&&--fizzTicks<=0){
   float pitch=1.6f+(random.nextFloat()-random.nextFloat())*.4f;((ServerLevel)level()).playSeededSound(null,getX(),getY(),getZ(),SoundEvents.FIRE_EXTINGUISH,SoundSource.HOSTILE,.7f,pitch,0);
   fizzTicks=random.nextInt(7)+2;if(random.nextInt(isInWater()?1:4)==0)hurt(OriginalElementalDamage.water(this),1);
  }
  super.aiStep();
 }
 private boolean sourceEyeInLava(){double eye=getY()+(double)getEyeHeight();var at=BlockPos.containing(getX(),eye,getZ());var state=level().getBlockState(at);if(!state.getFluidState().is(FluidTags.LAVA))return false;int depth=state.hasProperty(LiquidBlock.LEVEL)?state.getValue(LiquidBlock.LEVEL):0;if(depth>=8)depth=0;return eye<(double)((float)(at.getY()+1)-(float)depth/9f);}
 @Override public boolean isOnFire(){return false;}
 @Override public void setSecondsOnFire(int seconds){}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.is(DamageTypeTags.IS_FIRE)&&!source.is(net.minecraft.world.damagesource.DamageTypes.LAVA))return false;
  boolean alive=isAlive();boolean affected=super.hurt(source,amount);
  if(alive&&!isAlive()&&level() instanceof ServerLevel world&&!sourceWet()&&!sourceEyeInLava()&&!(source.getDirectEntity() instanceof Snowball)&&!source.is(OriginalElementalDamage.WATER)){
   SourceCreatureFragments.send(this);SourceExplosion.create(world,this,getX(),getY()+(double)(getBbHeight()/4f),getZ(),1,1,true,true);discard();
  }
  return affected;
 }
 @Override public boolean shouldBlockExplode(Explosion explosion,BlockGetter world,BlockPos pos,BlockState state,float force){return !state.is(Blocks.NETHERRACK)&&!state.is(Blocks.NETHER_QUARTZ_ORE)&&!state.is(Blocks.GOLD_ORE)&&!state.is(Blocks.DEEPSLATE_GOLD_ORE)&&super.shouldBlockExplode(explosion,world,pos,state,force);}
}
