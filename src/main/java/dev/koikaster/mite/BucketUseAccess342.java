package dev.koikaster.mite;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public interface BucketUseAccess342 {
 interface Mob {
  InteractionResultHolder<ItemStack> mite$bucketNativeUse(Level level,Player player,InteractionHand hand);
 }
 interface Powder {
  InteractionResult mite$bucketNativePlace(UseOnContext context);
 }
}
