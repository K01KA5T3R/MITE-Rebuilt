package dev.koikaster.mite;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Render reports are hints. Ownership, visibility, counter and rate are
 * checked on the server; the old packet layout remains compatible. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourcePerceptionReports {
 private static final Map<ServerPlayer,Observer> OBSERVERS=new WeakHashMap<>();
 private static final class Observer {
  final Map<Integer,Mob> tracked=new HashMap<>();
  int tokens=2;
  long lastRefill;
  Observer(ServerPlayer player){lastRefill=Integer.toUnsignedLong(player.getServer().getTickCount());}
  boolean permit(ServerPlayer player){long now=Integer.toUnsignedLong(player.getServer().getTickCount()),elapsed=(now-lastRefill)&0xffffffffL;if(elapsed>=20){long steps=elapsed/20;tokens=(int)Math.min(2,tokens+steps);lastRefill=(lastRefill+steps*20)&0xffffffffL;}if(tokens==0)return false;tokens--;return true;}
 }
 @SubscribeEvent public static void start(PlayerEvent.StartTracking event){if(event.getEntity() instanceof ServerPlayer player&&event.getTarget() instanceof Mob mob&&OriginalDespawn.handled(mob))OBSERVERS.computeIfAbsent(player,Observer::new).tracked.put(mob.getId(),mob);}
 @SubscribeEvent public static void stop(PlayerEvent.StopTracking event){if(event.getEntity() instanceof ServerPlayer player){var observer=OBSERVERS.get(player);if(observer!=null)observer.tracked.remove(event.getTarget().getId());}}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)OBSERVERS.remove(player);}
 static void apply(ServerPlayer player,int[] ids){
  if(ids.length==0||ids.length>64||!player.isAlive()||player.isSpectator())return;
  var observer=OBSERVERS.get(player);if(observer==null||!observer.permit(player))return;
  var seen=new HashSet<Integer>();
  for(int id:ids){
   if(!seen.add(id))continue;
   var entity=player.serverLevel().getEntity(id);
   if(!(entity instanceof Mob mob)||observer.tracked.get(id)!=mob||!mob.isAlive()||mob.isRemoved()||!OriginalDespawn.handled(mob)||!OriginalDespawn.inPlayerView(mob,player))continue;
   OriginalDespawn.refresh(mob,OriginalDespawn.renderedCap(mob));
  }
 }
 private SourcePerceptionReports(){}
}
