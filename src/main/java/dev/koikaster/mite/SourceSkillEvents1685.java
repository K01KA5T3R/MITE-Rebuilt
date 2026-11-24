package dev.koikaster.mite;
import net.minecraft.server.level.ServerPlayer;import net.minecraftforge.event.entity.player.PlayerEvent;import net.minecraftforge.eventbus.api.*;import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceSkillEvents1685 {
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void clone(PlayerEvent.Clone event){SourceSkills1685.copy(event.getOriginal(),event.getEntity());}
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player)Network.sync(player);}
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer player)Network.sync(player);}
 private SourceSkillEvents1685(){}
}
