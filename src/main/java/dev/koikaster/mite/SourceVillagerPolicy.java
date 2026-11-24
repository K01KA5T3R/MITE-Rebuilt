package dev.koikaster.mite;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Human V060 removes professions, jobs, farming and breeding. Source076
 * owns ordinary NPC behavior; the old filter remains for native compatibility. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceVillagerPolicy {
 private SourceVillagerPolicy(){}
 public static VillagerData data(VillagerData value){return value.setProfession(VillagerProfession.NONE).setLevel(1);}
 public static ImmutableList<Pair<Integer,? extends BehaviorControl<? super Villager>>> core(ImmutableList<Pair<Integer,? extends BehaviorControl<? super Villager>>> value){
  // Pinned 1.20.1 package order: keep swimming, doors, looking, danger,
  // waking, bells, raids, movement, home and meeting points. Job acquisition,
  // job competition, trading and food collection are omitted.
  if(value.size()!=20)throw new IllegalStateException("Unexpected villager core package");
  var result=ImmutableList.<Pair<Integer,? extends BehaviorControl<? super Villager>>>builder();
  for(int i:new int[]{0,1,2,3,4,5,6,9,16,17})result.add(value.get(i));return result.build();
 }
 @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
  if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof Villager v)||v.getType()!=EntityType.VILLAGER)return;
  v.setVillagerData(data(v.getVillagerData()));
  // Release saved job reservations before forgetting their locations. HOME
  // and movement memories are retained, so villagers remain active actors.
  releaseJob(v,MemoryModuleType.JOB_SITE,level);releaseJob(v,MemoryModuleType.POTENTIAL_JOB_SITE,level);
  if(v instanceof SourceVillager){releaseJob(v,MemoryModuleType.HOME,level);releaseJob(v,MemoryModuleType.MEETING_POINT,level);if(v.isSleeping())v.stopSleeping();v.getBrain().eraseMemory(MemoryModuleType.HOME);v.getBrain().eraseMemory(MemoryModuleType.MEETING_POINT);}
  var brain=v.getBrain();brain.eraseMemory(MemoryModuleType.JOB_SITE);brain.eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);brain.eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);brain.eraseMemory(MemoryModuleType.BREED_TARGET);
  v.setCanPickUpLoot(false);v.refreshBrain(level);
 }
 private static void releaseJob(Villager v,MemoryModuleType<GlobalPos> memory,ServerLevel level){
  v.getBrain().getMemory(memory).ifPresent(pos->{var owner=level.getServer().getLevel(pos.dimension());if(owner!=null)owner.getPoiManager().getType(pos.pos()).ifPresent(type->owner.getPoiManager().release(pos.pos()));});
 }
}
