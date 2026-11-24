package dev.koikaster.mite;
import net.minecraft.world.item.ItemStack;import net.minecraft.world.item.alchemy.PotionUtils;
/** Keep the exact original subtype in JEI matching; modern items delegate. */
public final class SourcePotionJei1420{
 public static String uid(Object raw){var stack=(ItemStack)raw;var subtype=SourceLegacyPotions1406.subtype(stack);if(subtype==null)return null;return "mite:potion:"+subtype+":"+(stack.hasTag()&&stack.getTag().contains(PotionUtils.TAG_CUSTOM_POTION_EFFECTS)?stack.getTag().get(PotionUtils.TAG_CUSTOM_POTION_EFFECTS).toString():"");}
 private SourcePotionJei1420(){}
}
