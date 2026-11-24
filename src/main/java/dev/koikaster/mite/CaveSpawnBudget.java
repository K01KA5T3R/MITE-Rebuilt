package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** User terrain accommodation: a shared budget for both natural-spawn owners.
 * Existing entities and non-natural creation are never removed or cancelled. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class CaveSpawnBudget {
 private record Arrival(BlockPos pos,long time,Mob pending){}
 private static final Map<ServerLevel,List<Arrival>> RECENT=new IdentityHashMap<>();
 static boolean cave(ServerLevel level,BlockPos pos){return level.dimension()==Level.OVERWORLD&&!level.canSeeSky(pos)&&(pos.getY()<level.getSeaLevel()||level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())>=pos.getY()+8);}
 private static int count(ServerLevel level,AABB box){return level.getEntitiesOfClass(Mob.class,box,m->m.isAlive()&&!m.isRemoved()&&m.getType().getCategory()==MobCategory.MONSTER).size();}
 static boolean allows(Mob mob){
  if(!(mob.level() instanceof ServerLevel level)||mob.getType().getCategory()!=MobCategory.MONSTER||!WorldContentMode.rebuilt(level)||!cave(level,mob.blockPosition()))return true;
  var pos=mob.blockPosition();
  // Include the vertical extent of modern caverns; the old +/-8 box missed it.
  if(count(level,new AABB(pos).inflate(32,24,32))>=6)return false;
  for(var p:level.players())if(p.isAlive()&&!p.isSpectator()){
   double dx=p.getX()-mob.getX(),dz=p.getZ()-mob.getZ();
   if(dx*dx+dz*dz<24*24&&Math.abs(p.getY()-mob.getY())<24)return false;
   if(dx*dx+dz*dz<=64*64&&Math.abs(p.getY()-mob.getY())<=48
    &&count(level,p.getBoundingBox().inflate(64,48,64))>=8)return false;
  }
  var recent=RECENT.get(level);if(recent==null)return true;long now=level.getGameTime();
  recent.removeIf(a->now-a.time>=200||now<a.time);
  return recent.stream().filter(a->Math.abs(a.pos.getX()-pos.getX())<=32&&Math.abs(a.pos.getZ()-pos.getZ())<=32&&Math.abs(a.pos.getY()-pos.getY())<=24).count()<2;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void position(MobSpawnEvent.PositionCheck event){
  if(event.getSpawnType()==MobSpawnType.NATURAL&&!allows(event.getEntity()))event.setResult(Event.Result.DENY);
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void joined(EntityJoinLevelEvent event){
  if(event.loadedFromDisk()||!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof Mob mob)
   ||!WorldContentMode.rebuilt(level)||mob.getSpawnType()!=MobSpawnType.NATURAL||mob.getType().getCategory()!=MobCategory.MONSTER||!cave(level,mob.blockPosition()))return;
  // Reserve until the native return so a nested spawn cannot take the same slot.
  if(!allows(mob)){event.setCanceled(true);return;}
  var list=RECENT.computeIfAbsent(level,k->new ArrayList<>());long now=level.getGameTime();list.removeIf(a->now-a.time>=200||now<a.time);list.add(new Arrival(mob.blockPosition(),now,mob));
 }
 static void finishSpawn(Entity entity, boolean added) {
  if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
  var arrivals = RECENT.get(level);
  if (arrivals == null) return;
  for (int i = arrivals.size() - 1; i >= 0; i--) {
   var arrival = arrivals.get(i);
   if (arrival.pending() != mob) continue;
   if (added) {
    arrivals.set(i, new Arrival(arrival.pos(), arrival.time(), null));
   } else {
    arrivals.remove(i);
   }
   return;
  }
 }
 @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)RECENT.remove(level);}
 private CaveSpawnBudget(){}
}
