package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
public final class BucketMaterial350 {
 public static final List<String> MATERIALS=List.of("copper","silver","gold","bronze","iron","steel","mithril","adamantium","ancient_metal");
 public static boolean filled(ItemStack stack){var item=stack.getItem();for(var candidate:BucketUse342.FILLED)if(item==candidate)return true;return false;}
 public static float model(ItemStack stack){return filled(stack)?MATERIALS.indexOf(BucketLifetime336.material(stack))+1:0;}
 public static Component name(Component nativeName,ItemStack stack){Component anvil=SourceAnvilInfo1602.name(nativeName,stack);if(anvil!=null)return anvil;if(!filled(stack)||stack.hasCustomHoverName())return nativeName;var material=BucketLifetime336.material(stack);return MATERIALS.contains(material)?Component.translatable("item.mite_reforged."+material+"_"+BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()):nativeName;}
 /** Capture time and fish health do not generate distinct JEI entries. */
 public static String subtype(ItemStack stack){var tag=stack.getTag();String variant=tag==null?"":stack.is(Items.TROPICAL_FISH_BUCKET)?Integer.toString(tag.getInt("BucketVariantTag")):stack.is(Items.AXOLOTL_BUCKET)?Integer.toString(tag.getInt("Variant")):"";return BucketLifetime336.material(stack)+":"+variant;}
 public static List<ItemStack> variants(){var result=new ArrayList<ItemStack>();for(var material:MATERIALS)for(var item:BucketUse342.FILLED){var water=ClassicVessels.peer(new ItemStack(Content.get(material+"_bucket")),"water");result.add(BucketLifetime336.vessel(water,new ItemStack(item)));}return result;}
 private BucketMaterial350(){}
}
