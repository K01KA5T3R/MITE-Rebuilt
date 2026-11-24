package dev.koikaster.mite;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
public final class ClassicBlocking {
 public static boolean handled(Object stack){return ClassicToolTraits.canBlock((ItemStack)stack);}
 public static boolean useHandled(Object item,Object rawPlayer,Object rawHand){var stack=((Player)rawPlayer).getItemInHand((InteractionHand)rawHand);return stack.getItem()==item&&handled(stack);}
 public static InteractionResultHolder<ItemStack> use(Object rawPlayer,Object rawHand){var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
 public static UseAnim animation(Object stack){return UseAnim.BLOCK;}
 public static int duration(Object stack){return 72000;}
 public static InteractionResultHolder<ItemStack> shield(Object rawPlayer,Object rawHand){return InteractionResultHolder.fail(((Player)rawPlayer).getItemInHand((InteractionHand)rawHand));}
 private ClassicBlocking(){}
}
