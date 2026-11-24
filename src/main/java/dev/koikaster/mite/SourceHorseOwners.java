package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original rs/rp hurt lifecycle. Breeding/feeding remains native and source
 * horse group initialization remains in its already verified owner. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceHorseOwners {
 static final class State implements OriginalLivestockFleeDecision.Host {
  final AbstractHorse horse;Entity harming;String pending;int memory;boolean decided,fleeing;
  State(AbstractHorse horse){this.horse=horse;}
  public Object lastHarmingEntity(){return harming;}
  public float distanceTo(Object value){return horse.distanceTo((Entity)value);}
  public void decided(boolean value){decided=value;}
  public void fleeing(boolean value){fleeing=value;}
  void harming(Entity value){harming=value;memory=value==null?0:900+horse.getRandom().nextInt(200);}
  boolean consider(){return OriginalLivestockFleeDecision.consider(this);}
  boolean stop(){return OriginalLivestockFleeDecision.stop(this);}
  void beforeAi(){
   if(memory>0)--memory;
   if(harming!=null&&(harming.isRemoved()||memory==0))harming(null);
   if(memory==0)pending=null;
   else if(harming==null&&pending!=null&&horse.tickCount%10==0)for(var entity:horse.level().getEntities(horse,horse.getBoundingBox().inflate(32,8,32)))if(entity.getUUID().toString().equals(pending)){harming=entity;pending=null;break;}
   if(fleeing&&horse.tickCount%100==0)stop();
  }
  void parentOutcome(DamageSource source,SourceParentHurtFrame.Outcome outcome){
   if(!outcome.negativelyAffected())return;
   var responsible=source.getEntity();if(responsible!=null){OriginalDespawn.refresh(horse,-1200);OriginalDespawn.refresh(responsible,-1200);if(responsible instanceof Player)OriginalDespawn.refresh(horse,-9600);}
   if(responsible instanceof LivingEntity living){horse.setLastHurtByMob(living);harming(living);consider();}
  }
  void animalOutcome(DamageSource source,SourceParentHurtFrame.Outcome outcome){
   if(!outcome.negativelyAffected())return;
   warnPeers(horse,source.getEntity());horse.setTarget(null);horse.resetLove();
  }
  void save(CompoundTag tag){tag.putString("last_harming_entity_unique_id",harming==null?"":harming.getUUID().toString());tag.putInt("last_harming_entity_memory_countdown",memory);tag.putBoolean("has_decided_to_flee",decided);tag.putBoolean("fleeing",fleeing);}
  void load(CompoundTag tag){if(tag.contains("last_harming_entity_unique_id")){pending=tag.getString("last_harming_entity_unique_id");if(pending.isEmpty())pending=null;}memory=tag.getInt("last_harming_entity_memory_countdown");decided=tag.getBoolean("has_decided_to_flee");fleeing=tag.getBoolean("fleeing");}
 }
 static boolean handles(AbstractHorse horse){return horse instanceof Horse||horse instanceof Donkey||horse instanceof Mule||horse instanceof ZombieHorse||horse instanceof SkeletonHorse;}
 static State state(AbstractHorse horse){var bridge=(SourceHorseNativeHurt)horse;var state=(State)bridge.mite$horseState();if(state==null){state=new State(horse);bridge.mite$horseState(state);install(horse,state);}return state;}
 private static void install(AbstractHorse horse,State state){var goals=((SourceMobAi)horse).mite$goals();for(var entry:java.util.List.copyOf(goals.getAvailableGoals()))if(entry.getGoal() instanceof net.minecraft.world.entity.ai.goal.PanicGoal)goals.removeGoal(entry.getGoal());if(goals.getAvailableGoals().stream().noneMatch(entry->entry.getGoal() instanceof SourceHorseFleeGoal))goals.addGoal(1,new SourceHorseFleeGoal(horse,state));}
 static void warnPeers(AbstractHorse horse,Entity attacker){
  if(attacker==null)return;
  // All five modern horse registries were one rs class in the original.
  for(var peer:horse.level().getEntitiesOfClass(AbstractHorse.class,horse.getBoundingBox().inflate(8,4,8),SourceHorseOwners::handles)){var state=state(peer);if(state.harming!=null)continue;state.harming(attacker);if(state.consider()){if(peer.isInLove())peer.resetLove();}else state.harming(null);}
 }
 public static boolean hurt(Object raw,Object damage,float amount){
  var horse=(AbstractHorse)raw;var source=(DamageSource)damage;var bridge=(SourceHorseNativeHurt)horse;
  if(horse.level().isClientSide||!handles(horse))return bridge.mite$vanillaHorseHurt(source,amount);
  var rider=horse.getFirstPassenger();if(rider!=null&&rider==source.getEntity())return false;
  var state=state(horse);float before=horse.getHealth();
  try(var frame=SourceParentHurtFrame.begin(horse,outcome->state.parentOutcome(source,outcome))){
   boolean accepted=((SourceAnimalNativeHurt)horse).mite$animalParentHurt(source,amount);var outcome=frame.finish();state.animalOutcome(source,outcome);
   if(outcome.destroyed())return accepted;
   if(before-horse.getHealth()>=4&&source.getEntity() instanceof Mob&&horse.getFirstPassenger()!=null&&horse.getRandom().nextInt(2)==0){
    bridge.mite$horseHurtRear();var sound=bridge.mite$horseAngrySound();if(sound!=null)horse.playSound(sound,.8f,horse.getVoicePitch());var passenger=horse.getFirstPassenger();if(passenger instanceof LivingEntity&&horse.getRandom().nextInt(3)==0)passenger.stopRiding();
   }
   if(outcome.negativelyAffected()&&source.getEntity() instanceof Player&&!horse.isTamed())horse.modifyTemper(-10);
   if(outcome.negativelyAffected()&&horse.isEating())horse.setEating(false);
   return accepted;
  }
 }
 public static void beforeAi(Object raw){var horse=(AbstractHorse)raw;if(!horse.level().isClientSide&&handles(horse))state(horse).beforeAi();}
 public static void save(Object raw,Object tag){var horse=(AbstractHorse)raw;if(!horse.level().isClientSide&&handles(horse))state(horse).save((CompoundTag)tag);}
 public static void load(Object raw,Object tag){var horse=(AbstractHorse)raw;if(!horse.level().isClientSide&&handles(horse))state(horse).load((CompoundTag)tag);}
 public static boolean allowIdlePose(Object raw,boolean requested){var horse=(AbstractHorse)raw;return !requested||!handles(horse)||horse.hurtTime==0&&!( ((SourceHorseNativeHurt)horse).mite$horseState() instanceof State state&&state.decided);}
 public static float motor(Object raw,float requested){var horse=(AbstractHorse)raw;if(!handles(horse)||horse.isVehicle())return requested;float value=horse instanceof Horse||horse instanceof SkeletonHorse?.5f:horse instanceof ZombieHorse?.3f:.4f;return horse.isBaby()?value*.75f:value;}
 public static void forward(Object raw,float requested){var horse=(AbstractHorse)raw;if(handles(horse)&&!horse.isVehicle())horse.setZza(requested);}
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof AbstractHorse horse&&handles(horse))state(horse);}
 private SourceHorseOwners(){}
}
