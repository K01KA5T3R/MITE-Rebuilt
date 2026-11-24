package dev.koikaster.mite;
import net.minecraft.server.level.ServerLevel;import net.minecraft.server.level.ServerPlayer;import net.minecraftforge.event.TickEvent;import net.minecraftforge.event.entity.player.PlayerEvent;import net.minecraftforge.event.server.ServerStartedEvent;import net.minecraftforge.eventbus.api.*;import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceAchievementEvents570 {
 @SubscribeEvent public static void started(ServerStartedEvent event){var level=event.getServer().overworld();SourceAchievementEra570.get(level);if(SourceBookWorld.get(level).bookcase)SourceAchievementWorld561.get(level).first(SourceAchievementCatalog561.node("bookcase").id(),"",0);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p){SourceAchievementService570.migrate(p);SourceAchievementNetwork570.send(p);}}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){SourceAchievementNetwork570.clear(p);SourceAchievementMotion572.clear(p);}}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer p)SourceAchievementNetwork570.send(p);}
 @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p){SourceAchievementActions572.dimension(p,event.getFrom(),event.getTo());SourceAchievementNetwork570.send(p);}}
 @SubscribeEvent public static void picked(PlayerEvent.ItemPickupEvent event){if(event.getEntity() instanceof ServerPlayer p)SourceAchievementItems570.picked(p,event.getStack());}
 @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event){if(event.getEntity() instanceof ServerPlayer p)SourceAchievementItems570.crafted(p,event.getCrafting());}
 @SubscribeEvent public static void smelted(PlayerEvent.ItemSmeltedEvent event){if(event.getEntity() instanceof ServerPlayer p)SourceAchievementItems570.smelted(p,event.getSmelting());}
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer p){SourceAchievementItems570.armor(p);SourceAchievementMotion572.tick(p);}}
 private SourceAchievementEvents570(){}
}
