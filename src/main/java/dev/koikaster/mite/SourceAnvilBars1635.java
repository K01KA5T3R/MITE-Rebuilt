package dev.koikaster.mite;
import net.minecraft.world.item.ItemStack;
/** bgw display arithmetic reads the existing carried state; no damageability changes. */
public final class SourceAnvilBars1635 {
 public static int value(Object raw,int property){var f=SourceAnvilInfo1602.facts((ItemStack)raw);if(f==null)return -1;int damage=Math.min(f.maximum(),f.damage());if(property==0)return damage>0?1:0;if(property==1)return (int)Math.round(13.0-(double)damage*13.0/f.maximum());int green=(int)Math.round(255.0-(double)damage*255.0/f.maximum());return property==2?(255-green)<<16|green<<8:(255-green)/4<<16|0x3f00;}
 private SourceAnvilBars1635(){}
}
