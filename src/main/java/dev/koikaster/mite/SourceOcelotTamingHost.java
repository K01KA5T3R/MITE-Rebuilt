package dev.koikaster.mite;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
/** New hand interaction state, separate from frozen wild Ocelot lifecycle. */
public interface SourceOcelotTamingHost {
 boolean mite$ocelotTempting();
 int mite$tamingCooldown();void mite$tamingCooldown(int value);
 InteractionResult mite$ocelotAnimalInteract(Player player,InteractionHand hand);
}
