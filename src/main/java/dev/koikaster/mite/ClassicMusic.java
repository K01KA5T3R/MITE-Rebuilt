package dev.koikaster.mite;

import java.util.Set;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Retain registry identities for save compatibility; remove modern records from gameplay. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicMusic {
 public static boolean removed(ItemStack stack){return stack.is(Items.MUSIC_DISC_PIGSTEP)||stack.is(Items.MUSIC_DISC_OTHERSIDE)||stack.is(Items.MUSIC_DISC_5)||stack.is(Items.MUSIC_DISC_RELIC)||stack.is(Items.DISC_FRAGMENT_5);}
 public static final Set<String> TRACKS=Set.of(
  "music/game/calm1","music/game/calm2","music/game/calm3","music/game/hal1","music/game/hal2","music/game/hal3","music/game/hal4","music/game/nuance1","music/game/nuance2","music/game/piano1","music/game/piano2","music/game/piano3",
  "music/game/creative/creative1","music/game/creative/creative2","music/game/creative/creative3","music/game/creative/creative4","music/game/creative/creative5","music/game/creative/creative6",
  "music/menu/menu1","music/menu/menu2","music/menu/menu3","music/menu/menu4",
  "music/game/nether/nether1","music/game/nether/nether2","music/game/nether/nether3","music/game/nether/nether4",
  "music/game/end/end","music/game/end/boss","music/game/end/credits",
  "records/13","records/cat","records/blocks","records/chirp","records/far","records/mall","records/mellohi","records/stal","records/strad","records/ward","records/11","records/wait");
 @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event){
  if(removed(event.getItemStack())){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}
 }
 @SubscribeEvent public static void dropped(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof ItemEntity item&&removed(item.getItem()))event.setCanceled(true);}
 @SubscribeEvent public static void inventory(TickEvent.PlayerTickEvent event){
  var player=event.player;if(event.phase!=TickEvent.Phase.END||player.level().isClientSide||player.tickCount%20!=0)return;
  boolean changed=false;
  for(int i=0;i<player.getInventory().getContainerSize();i++)if(removed(player.getInventory().getItem(i))){player.getInventory().setItem(i,ItemStack.EMPTY);changed=true;}
  if(removed(player.containerMenu.getCarried())){player.containerMenu.setCarried(ItemStack.EMPTY);changed=true;}
  if(changed)player.containerMenu.broadcastChanges();
 }
 private ClassicMusic(){}
}
