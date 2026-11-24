package dev.koikaster.mite;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A one-tick contact belongs to the live player, never to saved ForgeData. */
public final class SourcePlayerWebContact {
 private static final String LEGACY_TICK="MiteSourceWebTick";
 private static final Map<Player,Contact> CONTACTS=new WeakHashMap<>();
 private record Contact(ResourceKey<Level> dimension,int tick){}

 public static void mark(Player player){
  player.getPersistentData().remove(LEGACY_TICK);
  synchronized(CONTACTS){CONTACTS.put(player,new Contact(player.level().dimension(),player.tickCount));}
 }
 public static boolean active(Player player){
  // Older releases saved an entity-age counter. It cannot establish a live contact.
  player.getPersistentData().remove(LEGACY_TICK);
  synchronized(CONTACTS){
   var contact=CONTACTS.get(player);if(contact==null)return false;
   long elapsed=(long)player.tickCount-contact.tick();
   if(contact.dimension()!=player.level().dimension()||elapsed<0||elapsed>1){CONTACTS.remove(player);return false;}
   return true;
  }
 }
 private SourcePlayerWebContact(){}
}
