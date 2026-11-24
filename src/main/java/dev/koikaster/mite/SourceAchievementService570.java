package dev.koikaster.mite;
import java.util.*;import net.minecraft.server.level.ServerPlayer;import net.minecraftforge.common.util.FakePlayer;
/** Server-only source state owner. World-scoped policy applies before either player or world mutation. */
public final class SourceAchievementService570 {
 public static boolean grant(ServerPlayer player,String name){return grant(player,SourceAchievementCatalog561.node(name).id());}
 public static boolean grant(ServerPlayer player,int id){
  if(player instanceof FakePlayer||!SourceAchievementPolicy567.active(id,player.level())||!SourceAchievementState561.unlock(PlayerState.data(player),id))return false;
  boolean first=SourceAchievementWorld561.get(player.serverLevel()).first(id,player.getGameProfile().getName(),SourceMoonCycle.dayOfWorld(player.serverLevel().getDayTime()));
  if(player.connection!=null)Network.sync(player);
  if(first)for(var other:player.server.getPlayerList().getPlayers())SourceAchievementNetwork570.send(other);else SourceAchievementNetwork570.send(player);
  return true;
 }
 public static void legacyCallback(ServerPlayer player,String name){switch(name){case "bookcase","enlightenment"->grant(player,name);case "well_rested"->grant(player,"wellRested");default->{}}}
 public static void migrate(ServerPlayer player){
  var data=PlayerState.data(player);if(data.getBoolean("R195AchievementBookMigration1"))return;
  for(var name:List.of("bookcase","enlightenment")){var old=player.server.getAdvancements().getAdvancement(Content.id(name));if(old!=null&&player.getAdvancements().getOrStartProgress(old).isDone()){
   int id=SourceAchievementCatalog561.node(name).id();SourceAchievementState561.unlock(data,id);SourceAchievementWorld561.get(player.serverLevel()).first(id,"",0);
  }}data.putBoolean("R195AchievementBookMigration1",true);
 }
 private SourceAchievementService570(){}
}
