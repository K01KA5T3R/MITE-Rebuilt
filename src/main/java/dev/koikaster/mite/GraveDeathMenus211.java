package dev.koikaster.mite;
import java.util.Collection;
import java.util.IdentityHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Close player-owned temporary inputs while the death collection is still
 * available. Native menu removal distinguishes transient inputs from a block
 * inventory, and clears result previews without making them into items. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class GraveDeathMenus211 {
 private static final ThreadLocal<IdentityHashMap<ServerPlayer,Collection<ItemEntity>>> ACTIVE=new ThreadLocal<>();
 /** Forge's two-argument Player.drop replaces captureDrops and then spawns
  * through ItemTossEvent. Redirect only this player's active menu-close scope. */
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
 public static void toss(ItemTossEvent event){
  var active=ACTIVE.get();if(active==null||!(event.getPlayer() instanceof ServerPlayer player))return;
  var drops=active.get(player);if(drops==null)return;
  player.captureDrops(drops);
  if(event.isCanceled())return;
  drops.add(event.getEntity());event.setCanceled(true);
 }
 static void capture(ServerPlayer player,Collection<ItemEntity> drops){
  if(player.isAlive())return;
  var inventory=player.inventoryMenu;
  if(player.containerMenu==inventory&&inventory.getCarried().isEmpty()){
   boolean input=false;for(int i=1;i<=4&&i<inventory.slots.size();i++)if(inventory.getSlot(i).hasItem()){input=true;break;}
   if(!input)return;
  }
  var active=ACTIVE.get();if(active==null){active=new IdentityHashMap<>();ACTIVE.set(active);}
  var outer=active.put(player,drops);var previous=player.captureDrops(drops);
  try{
   if(player.containerMenu!=inventory)player.closeContainer();
   inventory.removed(player);
  }finally{player.captureDrops(previous);if(outer==null)active.remove(player);else active.put(player,outer);if(active.isEmpty())ACTIVE.remove();}
 }
 private GraveDeathMenus211(){}
}
