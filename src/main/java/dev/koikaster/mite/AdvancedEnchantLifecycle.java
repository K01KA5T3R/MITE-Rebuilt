package dev.koikaster.mite;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Return this module's input before death drops/graves and player/world saves. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class AdvancedEnchantLifecycle {
 static void close(ServerPlayer player){if(player.containerMenu instanceof AdvancedEnchantMenu)player.closeContainer();}
 // Automatic death closure runs inside the grave drop collection; retain this method for binary callers.
 public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer player)close(player);}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)close(player);}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void stopping(ServerStoppingEvent event){for(var player:event.getServer().getPlayerList().getPlayers())close(player);}
 private AdvancedEnchantLifecycle(){}
}
