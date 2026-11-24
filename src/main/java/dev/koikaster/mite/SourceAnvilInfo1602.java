package dev.koikaster.mite;
import net.minecraft.nbt.Tag;import net.minecraft.network.chat.Component;import net.minecraft.world.item.*;import net.minecraft.world.level.block.*;import net.minecraftforge.registries.ForgeRegistries;
/** Read carried anvil state without changing its gameplay damage representation. */
public final class SourceAnvilInfo1602 {
 public record Facts(String material,int maximum,int damage,int stage,Component baseName){}
 public static Facts facts(ItemStack stack){
  if(!(stack.getItem() instanceof BlockItem item))return null;var id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null||!id.getNamespace().equals("minecraft")&&!id.getNamespace().equals(MiteReforged.ID))return null;
  String material;int damage;Component base;
  if(item.getBlock() instanceof MetalAnvilBlock anvil){material=anvil.material;var tag=stack.getTagElement("BlockEntityTag");damage=tag!=null&&tag.contains("damage",Tag.TAG_ANY_NUMERIC)?Math.max(0,tag.getInt("damage")):0;base=Component.translatable(item.getDescriptionId());}
  else if(item.getBlock() instanceof AnvilBlock){material="iron";int stage=item.getBlock()==Blocks.DAMAGED_ANVIL?2:item.getBlock()==Blocks.CHIPPED_ANVIL?1:0;damage=SourceAnvil.minimumStage(material,stage);base=Component.translatable("block.mite_reforged.iron_anvil");}
  else return null;
  return new Facts(material,SourceAnvil.durability(material),damage,Math.min(2,SourceAnvil.stage(material,damage)),base);
 }
 public static Component name(Component nativeName,ItemStack stack){var f=facts(stack);if(f==null)return null;if(stack.hasCustomHoverName())return nativeName;return f.stage==0?f.baseName:Component.translatable(f.stage==1?"block.mite_reforged.worn_anvil":"block.mite_reforged.badly_worn_anvil",f.baseName);}
 private SourceAnvilInfo1602(){}
}
