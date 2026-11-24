package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/** Wild rx active lifecycle with the existing modern trust adapter. Modern Cat
 * and breeding follow the human exceptions; Ocelot hand-taming policy is pending. */
public final class SourceOcelotOwners {
 static final class Owner implements OriginalParentHarmState.Flags {
  final Ocelot cat;final OriginalParentHarmState harm;final SourceOcelotFood food;final SourceOcelotGoals.AvoidPlayer avoid;boolean decided,fleeing,installed;
  Owner(Ocelot cat){this.cat=cat;harm=new OriginalParentHarmState(cat,this);food=new SourceOcelotFood(cat);avoid=new SourceOcelotGoals.AvoidPlayer(cat);}
  public boolean hasDecidedToFlee(){return decided;}public void hasDecidedToFlee(boolean value){decided=value;}public boolean fleeing(){return fleeing;}public void fleeing(boolean value){fleeing=value;}
  void parentOutcome(DamageSource source,SourceParentHurtFrame.Outcome outcome){if(!outcome.negativelyAffected())return;var responsible=source.getEntity();if(responsible!=null){OriginalDespawn.refresh(cat,-1200);OriginalDespawn.refresh(responsible,-1200);if(responsible instanceof Player)OriginalDespawn.refresh(cat,-9600);}harm.afterDamage(source,true);}
  void animalOutcome(DamageSource source,SourceParentHurtFrame.Outcome outcome){if(!outcome.negativelyAffected())return;if(!trusting(cat))warnPeers(cat,source.getEntity());cat.setTarget(null);cat.resetLove();}
 }
 static Owner owner(Ocelot cat){var host=(SourceOcelotNativeHost)cat;var owner=(Owner)host.mite$ocelotOwner();if(owner==null){owner=new Owner(cat);host.mite$ocelotOwner(owner);}return owner;}
 static boolean trusting(Ocelot cat){return ((SourceOcelotState)cat).mite$isTrusting();}
 public static void install(Object raw){
  var cat=(Ocelot)raw;var owner=owner(cat);if(owner.installed)throw new IllegalStateException("Ocelot source goals installed twice");var access=(SourceMobAi)cat;var goals=access.mite$goals();var targets=access.mite$targets();var tempt=goals.getAvailableGoals().stream().map(entry->entry.getGoal()).filter(goal->goal instanceof TemptGoal).findFirst().orElseThrow();
  goals.removeAllGoals(goal->true);targets.removeAllGoals(goal->true);
  // rp entries precede rx entries. The retained native tempt object is also
  // referenced by Ocelot.mobInteract; never replace it with a disconnected goal.
  goals.addGoal(1,new SourceOcelotFood.Move(cat,owner.food));goals.addGoal(2,new SourceOcelotAvoidFire(cat));cat.setPathfindingMalus(BlockPathTypes.WATER,8);
  goals.addGoal(1,OriginalHellhoundProfile.swimGoal(cat));goals.addGoal(3,tempt);goals.addGoal(4,owner.avoid);goals.addGoal(7,new SourceOcelotGoals.Leap(cat));goals.addGoal(8,new SourceOcelotGoals.Attack(cat));goals.addGoal(9,new BreedGoal(cat,.8));goals.addGoal(10,new SourceOcelotWander(cat));goals.addGoal(11,new SourceOcelotGoals.Watch(cat));
  targets.addGoal(1,new SourceOcelotGoals.Prey(cat,owner.food,Chicken.class));targets.addGoal(1,new SourceOcelotGoals.Prey(cat,owner.food,net.minecraft.world.entity.ambient.Bat.class));owner.installed=true;
 }
 public static void reassess(Object raw){var cat=(Ocelot)raw;var owner=owner(cat);var goals=((SourceMobAi)cat).mite$goals();goals.removeGoal(owner.avoid);if(!trusting(cat))goals.addGoal(4,owner.avoid);cat.refreshDimensions();}
 static void warnPeers(Ocelot cat,Entity attacker){if(attacker==null)return;for(var peer:cat.level().getEntitiesOfClass(Ocelot.class,cat.getBoundingBox().inflate(8,4,8),other->cat.getClass().isInstance(other)&&!trusting(other))){var state=owner(peer).harm;if(state.harming()!=null)continue;state.harming(attacker);if(state.considerFleeing()){if(peer.isInLove())peer.resetLove();}else state.harming(null);}}
 public static boolean hurt(Object raw,Object damage,float amount){var cat=(Ocelot)raw;var source=(DamageSource)damage;if(cat.level().isClientSide)return ((SourceAnimalNativeHurt)cat).mite$vanillaAnimalHurt(source,amount);var state=owner(cat);try(var frame=SourceParentHurtFrame.begin(cat,outcome->state.parentOutcome(source,outcome))){boolean accepted=((SourceAnimalNativeHurt)cat).mite$animalParentHurt(source,amount);var outcome=frame.finish();state.animalOutcome(source,outcome);return accepted;}}
 public static void beforeAi(Object raw){var cat=(Ocelot)raw;if(!cat.level().isClientSide)owner(cat).harm.beforeTick();}
 public static void afterMobAi(Object raw){if(raw instanceof Ocelot cat&&!cat.level().isClientSide)owner(cat).food.tick();}
 public static void save(Object raw,Object tag){owner((Ocelot)raw).harm.save((CompoundTag)tag);}
 public static void load(Object raw,Object tag){owner((Ocelot)raw).harm.load((CompoundTag)tag);}
 public static boolean strike(Object raw,Object victim){var cat=(Ocelot)raw;return ((Entity)victim).hurt(cat.damageSources().mobAttack(cat),3f);}
 public static net.minecraft.sounds.SoundEvent hurtSound(){return net.minecraft.sounds.SoundEvents.CAT_HURT;}
 public static float motor(Object raw,float requested){return .1f;}
 public static float volume(){return .4f;}
 public static EntityDimensions dimensions(Object raw){var cat=(Ocelot)raw;return EntityDimensions.scalable(.6f,.8f).scale(cat.isBaby()?.5f:1);}
 public static SpawnGroupData initialize(Object raw,Object region,Object reason,Object data){
  var cat=(Ocelot)raw;var level=(ServerLevelAccessor)region;var group=SourceSpawnInitialization.base(cat,reason,data);
  if(!cat.isSpawnCancelled()&&level.getRandom().nextInt(7)==0)for(int i=0;i<2;i++){var child=EntityType.OCELOT.create(level.getLevel());if(child==null)throw new IllegalStateException("Source ocelot child factory");child.moveTo(cat.getX(),cat.getY(),cat.getZ(),cat.getYRot(),0);child.setAge(-24000);OriginalOrigin.capture(child);level.addFreshEntity(child);}
  return group;
 }
 private SourceOcelotOwners(){}
}
