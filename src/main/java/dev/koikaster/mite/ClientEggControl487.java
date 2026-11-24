package dev.koikaster.mite;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
/** Runs inside the vanilla prediction action, before the matching use packet is sent. */
public final class ClientEggControl487 {
 public static boolean beforeUse(InteractionHand hand){boolean ctrl=Screen.hasControlDown();SourceEggUse487.send(hand,ctrl);return ctrl;}
 private ClientEggControl487(){}
}
