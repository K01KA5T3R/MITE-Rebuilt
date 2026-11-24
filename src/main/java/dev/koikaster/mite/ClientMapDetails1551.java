package dev.koikaster.mite;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/** Original yh scaling row, with the retained modern cartography preview and lock. */
public final class ClientMapDetails1551 {
 private static boolean generated(Component line){return line.getContents() instanceof TranslatableContents t&&(t.getKey().equals("filled_map.scale")||t.getKey().equals("filled_map.level")||t.getKey().equals("filled_map.unknown"));}
 public static void append(ItemTooltipEvent event,boolean extended){
  var stack=event.getItemStack();if(!extended||!stack.is(Items.FILLED_MAP))return;
  var lines=event.getToolTip();lines.removeIf(c->!SourceTooltipLore1669.authored(stack,c)&&generated(c));
  var level=event.getEntity()==null?Minecraft.getInstance().level:event.getEntity().level();
  var data=level==null?null:MapItem.getSavedData(stack,level);
  if(data==null){lines.add(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));return;}
  int scale=data.scale;
  if(stack.hasTag())scale=stack.getTag().getBoolean("map_is_scaling")?scale+1:Mth.clamp(scale+stack.getTag().getByte("map_scale_direction"),0,4);
  lines.add(Component.translatable("tooltip.mite_reforged.map_scaling",1<<scale).withStyle(ChatFormatting.GRAY));
 }
 private ClientMapDetails1551(){}
}
