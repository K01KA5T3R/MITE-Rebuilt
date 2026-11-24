package dev.koikaster.mite;
import net.minecraft.network.chat.Component;import net.minecraftforge.event.entity.player.ItemTooltipEvent;
/** ye's creative/advanced and extended durability gate, including creative indexing. */
public final class ClientAnvilInfo1602 {
 public static void append(ItemTooltipEvent event,boolean extended){
  if(!extended)return;var player=event.getEntity();if(!((player==null?event.getFlags().isCreative():player.isCreative())||event.getFlags().isAdvanced()))return;
  var f=SourceAnvilInfo1602.facts(event.getItemStack());if(f==null)return;var lines=event.getToolTip();if(!lines.isEmpty()&&!lines.get(lines.size()-1).getString().isBlank())lines.add(Component.empty());
  lines.add(f.damage()>0?Component.translatable("tooltip.mite_reforged.anvil_durability_detail",Math.max(0,f.maximum()-f.damage()),f.maximum()):Component.translatable("tooltip.mite_reforged.anvil_durability",f.maximum()));
 }
 private ClientAnvilInfo1602(){}
}
