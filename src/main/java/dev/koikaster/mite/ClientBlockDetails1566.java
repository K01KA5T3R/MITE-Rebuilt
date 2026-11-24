package dev.koikaster.mite;
import java.util.Locale;import net.minecraft.ChatFormatting;import net.minecraft.core.BlockPos;import net.minecraft.network.chat.Component;import net.minecraft.world.item.BlockItem;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.block.state.properties.Property;import net.minecraftforge.event.entity.player.ItemTooltipEvent;import net.minecraftforge.registries.ForgeRegistries;
/** zh's creative-and-extended gate; values come from the actual mining path. */
public final class ClientBlockDetails1566 {
 private static <T extends Comparable<T>> BlockState value(BlockState s,Property<T> p,String raw){return p.getValue(raw).map(v->s.setValue(p,v)).orElse(s);}
 static BlockState state(net.minecraft.world.item.ItemStack stack,BlockItem item){var s=item.getBlock().defaultBlockState();var tag=stack.getTagElement("BlockStateTag");if(tag!=null)for(var p:s.getProperties())if(tag.contains(p.getName(),8))s=value(s,p,tag.getString(p.getName()));return s;}
 private static String number(float n){String s=String.format(Locale.ROOT,"%.2f",n);return s.endsWith("0")?s.substring(0,s.length()-1):s;}
 private static String materialKey(String token){var result=new StringBuilder();boolean upper=false;for(char c:token.toCharArray()){if(c=='_'){upper=true;continue;}result.append(upper?Character.toUpperCase(c):c);upper=false;}return "material."+result+".name";}
 public static void append(ItemTooltipEvent event,boolean extended){
  if(!extended||!(event.getItemStack().getItem() instanceof BlockItem item))return;var player=event.getEntity();if(player==null?!event.getFlags().isCreative():!player.isCreative())return;
  var id=ForgeRegistries.BLOCKS.getKey(item.getBlock());if(id==null||!id.getNamespace().equals("minecraft")&&!id.getNamespace().equals(MiteReforged.ID))return;
  var state=state(event.getItemStack(),item);var world=player==null?net.minecraft.client.Minecraft.getInstance().level:player.level();if(world==null)return;
  float hardness=ClassicMining.hardness(state,world,BlockPos.ZERO);int level=HarvestRules.requiredLevel(state);var lines=event.getToolTip();lines.add(level>0?Component.translatable("tooltip.mite_reforged.block_hardness_level",number(hardness),level).withStyle(ChatFormatting.GRAY):Component.translatable("tooltip.mite_reforged.block_hardness",number(hardness)).withStyle(ChatFormatting.GRAY));
  String material=OriginalBlockPhysics1566.material(state);if(material!=null)lines.add(Component.translatable("tooltip.mite_reforged.block_material",Component.translatable(materialKey(material))).withStyle(ChatFormatting.GRAY));
 }
 private ClientBlockDetails1566(){}
}
