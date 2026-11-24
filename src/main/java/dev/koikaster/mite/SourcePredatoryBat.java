package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Shared contact adapter for the two source sibling branches: VampireBat and Nightwing. */
public abstract class SourcePredatoryBat extends SourceBat {
 protected final OriginalBatFeeding<LivingEntity> feeding=new OriginalBatFeeding<>();
 private final FeedingWorld feedingWorld=new FeedingWorld();
 protected SourcePredatoryBat(EntityType<? extends Bat> type,Level level){super(type,level);}
 protected boolean sourceNightwing(){return false;}
 protected boolean sourceGiant(){return false;}
 boolean sourcePreysUpon(LivingEntity prey){return feeding.preysUpon(feedingWorld,prey);}
 @Override protected boolean sourcePredatory(){return true;}
 @Override public void tick(){super.tick();if(!level().isClientSide)feeding.afterBaseTick(feedingWorld);}
 @Override protected void sourceCollision(Entity other){if(other instanceof LivingEntity living)feeding.collided(feedingWorld,living);else sourceCollisionSound();}
 @Override public int getMaxSpawnClusterSize(){return 8;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);int cooldown=feeding.savedFeedCooldown();if(!sourceNightwing()&&cooldown>0)tag.putShort("feed_cooldown",(short)cooldown);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(!sourceNightwing())feeding.loadFeedCooldown(tag.getShort("feed_cooldown"));}
 private final class FeedingWorld implements OriginalBatFeeding.World<LivingEntity> {
  public boolean vampire(){return !sourceNightwing();}public boolean giant(){return sourceGiant();}public boolean nightwing(){return sourceNightwing();}
  public float health(){return getHealth();}public float maximumHealth(){return getMaxHealth();}public int ticksWithOffset(){return tickCount+getId()*47;}
  public LivingEntity target(){return getTarget();}public void target(LivingEntity value){setTarget(value);}
  public LivingEntity closestPrey(double radius,boolean lineOfSight,boolean path){
   if(path)throw new IllegalArgumentException("Source bats query prey without a path requirement");
   LivingEntity found=null;double best=radius*radius;
   for(var candidate:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius))){if(candidate==SourcePredatoryBat.this||!candidate.isAlive()||candidate instanceof Player p&&p.isSpectator()||!feeding.preysUpon(this,candidate))continue;double distance=distanceToSqr(candidate);if(distance>=best||lineOfSight&&!OriginalVision.see(SourcePredatoryBat.this,candidate))continue;best=distance;found=candidate;}return found;
  }
  public boolean player(LivingEntity target){return target instanceof Player;}public boolean creative(LivingEntity target){return target instanceof Player p&&p.isCreative();}public boolean trueAnimal(LivingEntity target){return target instanceof Animal;}public boolean villager(LivingEntity target){return target instanceof Villager;}
  public boolean ownHalfBoxIntersects(LivingEntity other){var b=getBoundingBox();double mx=b.getXsize()*.25,mz=b.getZsize()*.25;return new AABB(b.minX+mx,b.minY,b.minZ+mz,b.maxX-mx,b.maxY,b.maxZ-mz).intersects(other.getBoundingBox());}
  public OriginalBatFeeding.Hit attack(LivingEntity victim){float before=victim.getHealth();try(var frame=SourceParentHurtFrame.begin(victim,outcome->{})){boolean accepted=doHurtTarget(victim);var result=frame.finish();return accepted||result.negativelyAffected()?new OriginalBatFeeding.Hit(result.negativelyAffected(),Math.max(0,before-victim.getHealth())):null;}}
  public void heal(float lost){SourcePredatoryBat.this.heal(lost);}
  public void provokeLivingUntargetedOcelot(LivingEntity other){if(other instanceof Ocelot cat&&cat.getHealth()>0&&cat.getTarget()==null)cat.setTarget(SourcePredatoryBat.this);}
  public void dimPlayerVision(LivingEntity target,float amount,int resistanceKind){if(resistanceKind!=4)throw new IllegalArgumentException("Nightwing uses source resistance kind4");if(target instanceof ServerPlayer player)OriginalShadowVision.add(player,amount*(1-ClassicDefense.silverCoverage(player)*.5f));}
  public void parentCollision(LivingEntity other){sourceCollisionSound();}
 }
}
