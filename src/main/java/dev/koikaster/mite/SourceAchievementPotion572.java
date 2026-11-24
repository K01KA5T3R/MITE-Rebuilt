package dev.koikaster.mite;
import java.util.*;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.inventory.Slot;import net.minecraft.world.item.*;import net.minecraft.world.item.alchemy.*;
/** Preserve actual removed identity when native quick-moving empties its live slot stack. */
public final class SourceAchievementPotion572 {
 private static final Map<Slot,ItemStack> REMOVED=Collections.synchronizedMap(new WeakHashMap<>());
 public static ItemStack removed(ItemStack actual,Object raw){if(!actual.isEmpty())REMOVED.put((Slot)raw,actual.copy());return actual;}
 public static void quick(Object raw,ItemStack original,int count){if(count>0&&!original.isEmpty())REMOVED.put((Slot)raw,original.copyWithCount(count));}
 public static boolean qualifies(ItemStack item){var subtype=SourceLegacyPotions1406.subtype(item);if(subtype!=null)return !item.isEmpty()&&subtype>0;var potion=PotionUtils.getPotion(item);return !item.isEmpty()&&item.getItem() instanceof PotionItem&&potion!=Potions.EMPTY&&(potion!=Potions.WATER||item.getItem() instanceof ThrowablePotionItem);}
 public static void take(Object raw,Object actor,ItemStack supplied){var removed=REMOVED.remove((Slot)raw);var actual=!supplied.isEmpty()?supplied:removed==null?ItemStack.EMPTY:removed;if(actor instanceof ServerPlayer p&&qualifies(actual))SourceAchievementService570.grant(p,"potion");}
 private SourceAchievementPotion572(){}
}
