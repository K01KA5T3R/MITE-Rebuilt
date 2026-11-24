package dev.koikaster.mite;
import net.minecraft.world.item.*;
/** Original bgw arithmetic for owned, damageable equipment. Existing max/visibility stay native. */
public final class SourceEquipmentBars1659 {
 public static int value(Object raw,int property){var stack=(ItemStack)raw;if(stack.getItem() instanceof BlockItem||!ClassicToolCatalogue.sourceItem(stack)||!stack.isDamageableItem())return -1;int max=stack.getMaxDamage();if(max<=0)return -1;int damage=Math.max(0,Math.min(max,stack.getDamageValue()));if(property==1)return (int)Math.round(13.0-(double)damage*13.0/max);int green=(int)Math.round(255.0-(double)damage*255.0/max);return property==2?(255-green)<<16|green<<8:(255-green)/4<<16|0x3f00;}
 private SourceEquipmentBars1659(){}
}
