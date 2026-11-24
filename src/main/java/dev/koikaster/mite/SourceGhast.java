package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** tj: one flying bl owner, not a tm/Monster goal or retaliation owner. */
public final class SourceGhast extends Ghast {
 private final SourceGhastAI sourceAi;
 private int sourceExplosionPower=1;
 public SourceGhast(EntityType<? extends Ghast> type,Level world){super(type,world);getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(16);getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((double).1f);xpReward=10;moveControl=new MoveControl(this);sourceAi=new SourceGhastAI(this);}
 @Override protected void registerGoals(){}
 public SourceGhastAI sourceAi(){return sourceAi;}
 private SourceGhastNativeParent parent(){return (SourceGhastNativeParent)(Object)this;}
 @Override public void tick(){if(!level().isClientSide&&sourceAi!=null)sourceAi.harm.beforeTick();super.tick();}
 @Override protected void customServerAiStep(){if(sourceAi!=null)sourceAi.step();}
 @Override public void checkDespawn(){} // tj.bl owns the single source despawn call.
 @Override public int getMaxSpawnClusterSize(){return 1;}
 @Override protected float getSoundVolume(){return 10;}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height*.85f;}
 @Override public int getExplosionPower(){return sourceExplosionPower;}
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);if(seen&&target instanceof Player)OriginalDespawn.refresh(this,-1200);return seen;}
 @Override public boolean checkSpawnRules(LevelAccessor world,MobSpawnType reason){return random.nextInt(20)==0&&world.isUnobstructed(this)&&world.noCollision(this)&&!world.containsAnyLiquid(getBoundingBox())&&world.getDifficulty()!=Difficulty.PEACEFUL;}
 static boolean reflected(DamageSource source){return "fireball".equals(source.getMsgId())&&source.getEntity() instanceof Player;}
 @Override public boolean isInvulnerableTo(DamageSource source){return reflected(source)?isInvulnerable()||isRemoved():parent().mite$ghastParentInvulnerable(source);}
 @Override public boolean hurt(DamageSource source,float amount){
  if(isRemoved()||isInvulnerableTo(source))return false;
  if(reflected(source))amount=1000;
  var effective=reflected(source)?new ReflectedDamage(source):source;
  if(level().isClientSide||sourceAi==null)return parent().mite$ghastParentHurt(effective,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->sourceAi.harm.afterDamage(source,outcome.negativelyAffected()))){
   boolean accepted=parent().mite$ghastParentHurt(effective,amount);var outcome=frame.finish();
   if(outcome.destroyed()&&reflected(source)&&source.getEntity() instanceof ServerPlayer player){SourceAchievementService570.grant(player,"ghast");var advancement=player.server.getAdvancements().getAdvancement(new ResourceLocation("minecraft:nether/return_to_sender"));if(advancement!=null)for(var criterion:advancement.getCriteria().keySet())player.getAdvancements().award(advancement,criterion);}
   return accepted;
  }
 }
 /** setIgnoreSpecificImmunities: retain fireball identity/attribution and bypass
  * the native fire-resistance early return, after the generic invulnerability gate. */
 private static final class ReflectedDamage extends DamageSource {
  ReflectedDamage(DamageSource source){super(source.typeHolder(),source.getDirectEntity(),source.getEntity());}
  @Override public boolean is(TagKey<DamageType> tag){return !tag.equals(DamageTypeTags.IS_FIRE)&&super.is(tag);}
 }
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){int tears=random.nextInt(2);if(reflected(source)&&tears<1)tears=1;for(int i=0;i<tears;i++)spawnAtLocation(Items.GHAST_TEAR);int powder=random.nextInt(3);for(int i=0;i<powder;i++)spawnAtLocation(Items.GUNPOWDER);}
 @Override public void addAdditionalSaveData(CompoundTag tag){parent().mite$ghastParentSave(tag);tag.putInt("ExplosionPower",sourceExplosionPower);if(sourceAi!=null)sourceAi.harm.save(tag);tag.putBoolean("R195GhastOwner",true);}
 @Override public void readAdditionalSaveData(CompoundTag tag){parent().mite$ghastParentLoad(tag);if(!tag.getBoolean("R195GhastOwner")){if(getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).getBaseValue()==100)getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(16);double speed=getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue();if(speed==.7||speed==(double).7f)getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue((double).1f);}if(tag.contains("ExplosionPower"))sourceExplosionPower=tag.getInt("ExplosionPower");if(sourceAi!=null)sourceAi.harm.load(tag);}
}
