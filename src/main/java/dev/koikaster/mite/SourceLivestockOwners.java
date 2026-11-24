package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** rp/EntityLivestock hurt and attacker memory. Native breeding/feeding stays
 * in Animal; wolf and other animal owners keep the original native hurt body. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceLivestockOwners {
 public static final class State implements OriginalLivestockFleeDecision.Host {
  final Animal animal;Entity harming;String pending;int memory;boolean decided,fleeing;
  State(Animal animal){this.animal=animal;}
  public Object lastHarmingEntity(){return harming;}
  public float distanceTo(Object entity){return animal.distanceTo((Entity)entity);}
  public void decided(boolean value){decided=value;}
  public void fleeing(boolean value){fleeing=value;}
  void harming(Entity value){harming=value;memory=value==null?0:900+animal.getRandom().nextInt(200);}
  boolean consider(){return OriginalLivestockFleeDecision.consider(this);}
  boolean stop(){return OriginalLivestockFleeDecision.stop(this);}
  void beforeAi(){
   if(memory>0)--memory;
   if(harming!=null&&(harming.isRemoved()||memory==0))harming(null);
   if(memory==0)pending=null;
   else if(harming==null&&pending!=null&&animal.tickCount%10==0)for(var entity:animal.level().getEntities(animal,animal.getBoundingBox().inflate(32,8,32)))if(entity.getUUID().toString().equals(pending)){harming=entity;pending=null;break;}
   if(fleeing&&animal.tickCount%100==0)stop();
  }
  void parentOutcome(DamageSource source,SourceParentHurtFrame.Outcome outcome){
   if(!outcome.negativelyAffected())return;
   var responsible=source.getEntity();if(responsible!=null){OriginalDespawn.refresh(animal,-1200);OriginalDespawn.refresh(responsible,-1200);if(responsible instanceof net.minecraft.world.entity.player.Player)OriginalDespawn.refresh(animal,-9600);}
   if(responsible instanceof LivingEntity living){animal.setLastHurtByMob(living);harming(living);consider();}
  }
  void animalOutcome(DamageSource source,SourceParentHurtFrame.Outcome outcome){
   if(!outcome.negativelyAffected())return;
   warnPeers(animal,source.getEntity());animal.setTarget(null);animal.resetLove();
   // rp.bo=60 belongs to the unused legacy non-task AI speed branch. Source
   // rp.bf() is final true, so no modern attribute boost is invented here.
  }
  void save(CompoundTag tag){tag.putString("last_harming_entity_unique_id",harming==null?"":harming.getUUID().toString());tag.putInt("last_harming_entity_memory_countdown",memory);tag.putBoolean("has_decided_to_flee",decided);tag.putBoolean("fleeing",fleeing);}
  void load(CompoundTag tag){if(tag.contains("last_harming_entity_unique_id")){pending=tag.getString("last_harming_entity_unique_id");if(pending.isEmpty())pending=null;}memory=tag.getInt("last_harming_entity_memory_countdown");decided=tag.getBoolean("has_decided_to_flee");fleeing=tag.getBoolean("fleeing");}
 }
 static boolean handles(Animal animal){return SourceLivestockSpook.handles(animal);}
 static State state(Animal animal){
  var bridge=(SourceAnimalNativeHurt)animal;var state=(State)bridge.mite$animalState();
  if(state==null){state=new State(animal);bridge.mite$animalState(state);install(animal,state);}return state;
 }
 static void install(Animal animal){state(animal);}
 private static void install(Animal animal,State state){var goals=((SourceMobAi)animal).mite$goals();for(var goal:java.util.List.copyOf(goals.getAvailableGoals()))if(goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.PanicGoal)goals.removeGoal(goal.getGoal());if(goals.getAvailableGoals().stream().noneMatch(goal->goal.getGoal() instanceof SourceLivestockFleeGoal))goals.addGoal(1,new SourceLivestockFleeGoal(animal,state));}
 static boolean fleeingOrDecided(Animal animal){var state=state(animal);return state.decided||state.fleeing;}
 static void warnPeers(Animal animal,Entity attacker){
  if(attacker==null)return;
  // The legacy class query includes subclasses: Cow warns Mooshroom, while
  // Mooshroom's concrete-class query does not warn ordinary Cow.
  for(var peer:animal.level().getEntitiesOfClass(Animal.class,animal.getBoundingBox().inflate(8,4,8),other->handles(other)&&animal.getClass().isInstance(other))){var state=state(peer);if(state.harming!=null)continue;state.harming(attacker);if(state.consider()){if(peer.isInLove())peer.resetLove();}else state.harming(null);}
 }
 public static boolean hurt(Object raw,Object rawSource,float amount){
  var animal=(Animal)raw;var bridge=(SourceAnimalNativeHurt)raw;var source=(DamageSource)rawSource;
  if(animal.level().isClientSide||!handles(animal))return bridge.mite$vanillaAnimalHurt(source,amount);
  var state=state(animal);
  try(var frame=SourceParentHurtFrame.begin(animal,outcome->state.parentOutcome(source,outcome))){boolean accepted=bridge.mite$animalParentHurt(source,amount);var outcome=frame.finish();state.animalOutcome(source,outcome);return accepted;}
 }
 public static void beforeAi(Object raw){var animal=(Animal)raw;if(!animal.level().isClientSide&&handles(animal))state(animal).beforeAi();}
 public static void afterAi(Object raw){var animal=(Animal)raw;if(!animal.level().isClientSide&&handles(animal)){SourceGrassTrampling.afterAi(animal);SourceLivestockSpook.propagate(animal);}}
 public static void save(Object raw,Object data){var animal=(Animal)raw;if(!animal.level().isClientSide&&handles(animal))state(animal).save((CompoundTag)data);}
 public static void load(Object raw,Object data){var animal=(Animal)raw;if(!animal.level().isClientSide&&handles(animal))state(animal).load((CompoundTag)data);}
 @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof Animal animal&&handles(animal))install(animal);}
 private SourceLivestockOwners(){}
}
