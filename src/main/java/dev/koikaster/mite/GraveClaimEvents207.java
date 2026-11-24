package dev.koikaster.mite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Both integrated-owner level.dat and dedicated playerdata have already
 * loaded when this event runs. Restore before normal mod login syncs. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class GraveClaimEvents207 {
 @SubscribeEvent(priority=EventPriority.HIGHEST)
 public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)GraveRecovery.restoreInventory(player);}
 private GraveClaimEvents207(){}
}
