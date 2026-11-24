package dev.koikaster.mite;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** ItemRunestone: two block IDs, sixteen metadata values, stacks of four. */
public final class SourceRunestoneItem extends BlockItem {
 public static final String[] NAMES={"Nul","Quas","Por","An","Nox","Flam","Vas","Des","Ort","Tym","Corp","Lor","Mani","Jux","Ylem","Sanct"};
 public SourceRunestoneItem(SourceRunestoneBlock block){super(block,new Properties().stacksTo(4));}
 public static int rune(ItemStack stack){
  var tag=stack.getTagElement("BlockStateTag");if(tag==null)return 0;
  try{return Math.max(0,Math.min(15,Integer.parseInt(tag.getString("rune"))));}catch(NumberFormatException ignored){return 0;}
 }
 public static ItemStack select(ItemStack stack,int rune){
  var out=stack.copy();out.getOrCreateTagElement("BlockStateTag").putString("rune",Integer.toString(rune&15));return out;
 }
 public Component getName(ItemStack stack){return Component.translatable(getDescriptionId()).append(" \""+NAMES[rune(stack)]+"\"");}
}
