package dev.koikaster.mite;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
/** Native storage access only; implementations are added at exact fields. */
public final class BucketStorageAccess337 {
 public interface Loot {boolean mite$bucketUnopened();}
 public interface Horse {Container mite$bucketInventory();}
 public interface Display {ItemStack mite$bucketDisplay();void mite$bucketDisplay(ItemStack stack);}
 private BucketStorageAccess337(){}
}
