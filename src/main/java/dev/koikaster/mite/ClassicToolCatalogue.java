package dev.koikaster.mite;
import net.minecraft.world.item.*;
/** R195 xj/akc/tool subclasses plus wp/xv/ItemArrow constants; explicit selected expansion materials. */
public final class ClassicToolCatalogue {
 public static final String[] ARROW_MATERIALS={"flint","obsidian","copper","silver","rusted_iron","gold","iron","mithril","adamantium","ancient_metal","holy_steel","vibranium"};
 public static float materialDamage(String m){return switch(m){case "wood"->0;case "flint"->1;case "obsidian","rusted_iron","gold"->2;case "copper","silver"->3;case "iron","ancient_metal","diamond"->4;case "mithril"->5;case "adamantium"->6;case "holy_steel"->7;case "vibranium"->9;case "stone"->1;case "bronze"->3.5f;case "steel"->4.5f;case "netherite"->5.5f;default->0;};}
 public static float baseDamage(LegacyToolWear.Kind k){return switch(k){case PICKAXE,WARHAMMER,HATCHET,DAGGER,CLUB->2;case AXE->3;case SWORD,BATTLEAXE->4;case SHOVEL,HOE,MATTOCK,SCYTHE,KNIFE,CUDGEL->1;case SHEARS->0;};}
 public static float reachBonus(LegacyToolWear.Kind k){return switch(k){case KNIFE,CUDGEL->.25f;case DAGGER,HATCHET,CLUB,SHEARS->.5f;case SCYTHE->1f;default->.75f;};}
 public static boolean canBlock(LegacyToolWear.Kind k){return switch(k){case HATCHET,HOE,DAGGER,KNIFE,MATTOCK,CUDGEL,SHEARS->false;default->true;};}
 public static int bowDurability(String material){return material.equals("vibranium")?512:material.equals("mithril")?128:material.equals("ancient_metal")?64:32;}
 public static float bowVelocity(String material){return material.equals("vibranium")?1.75f:material.equals("mithril")?1.25f:material.equals("ancient_metal")?1.1f:1f;}
 public static int rodDurability(String material){return rodDurability(material,false);}
 static int rodDurability(String material,boolean r196){return (int)(2f*LegacyToolWear.durability(material,r196)+(material.equals("flint")?1:0));}
 public static float arrowDamage(String material){return .5f+materialDamage(material)*.5f;}
 public static float arrowRecovery(String material){return switch(material){case "flint"->.3f;case "obsidian"->.4f;case "copper","silver"->.6f;case "rusted_iron","gold"->.5f;case "iron"->.7f;case "mithril","ancient_metal"->.8f;case "adamantium"->.9f;case "holy_steel"->.95f;case "vibranium"->1f;case "bronze"->.65f;case "steel"->.75f;default->.3f;};}
 public static String material(ItemStack stack){if(stack.getItem() instanceof TorchBow)return "wood";var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null)return "wood";var path=id.getPath();if(path.equals("bow"))return "wood";if(path.equals("fishing_rod")||path.equals("carrot_on_a_stick"))return "iron";if(path.equals("arrow"))return "flint";if(path.equals("shears"))return "iron";if(path.startsWith("rusted_iron_"))return "rusted_iron";if(path.startsWith("ancient_metal_"))return "ancient_metal";if(path.startsWith("holy_steel_"))return "holy_steel";int split=path.indexOf('_');var m=split<0?path:path.substring(0,split);return m.equals("wooden")?"wood":m.equals("golden")?"gold":m;}
 public static boolean sourceItem(ItemStack stack){if(net.minecraftforge.registries.ForgeRegistries.ITEMS==null)return false;var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());return id!=null&&(id.getNamespace().equals("minecraft")||id.getNamespace().equals(MiteReforged.ID));}
 private ClassicToolCatalogue(){}
}
