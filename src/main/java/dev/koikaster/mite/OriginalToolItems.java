package dev.koikaster.mite;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraft.tags.BlockTags;
/** Registration is generated from the supplied original declarations, rather than guessed material permutations. */
public final class OriginalToolItems {
 public static final Map<String,JsonObject> ENTRIES=load();
 private static Map<String,JsonObject> load(){try(var stream=OriginalToolItems.class.getResourceAsStream("/data/mite_reforged/classic_tool_roster.json")){if(stream==null)throw new IOException("missing original tool roster");var root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();var result=new LinkedHashMap<String,JsonObject>();for(var e:root.getAsJsonArray("new_items")){var row=e.getAsJsonObject();result.put(row.get("name").getAsString(),row);}return Collections.unmodifiableMap(result);}catch(IOException error){throw new ExceptionInInitializerError(error);}}
 public static void register(){
  Content.item("ancient_metal_ingot",()->new Item(new Item.Properties().stacksTo(8)));Content.item("ancient_metal_nugget",()->new Item(new Item.Properties().stacksTo(64)));Content.item("sinew",()->new Item(new Item.Properties().stacksTo(16)));
  for(String m:new String[]{"rusted_iron","ancient_metal","obsidian","flint","wood"})if(!Content.TIERS.containsKey(m))Content.TIERS.put(m,new ForgeTier(LegacyHarvest.material(m),LegacyToolWear.maxDamage(LegacyToolWear.Kind.PICKAXE,m),LegacyMining.materialEfficiency(m),ClassicToolCatalogue.materialDamage(m),m.equals("obsidian")||m.equals("flint")?0:30,BlockTags.NEEDS_IRON_TOOL,()->Ingredient.of(m.equals("ancient_metal")?Content.get("ancient_metal_nugget"):m.equals("rusted_iron")?Items.IRON_NUGGET:m.equals("obsidian")?Items.OBSIDIAN:m.equals("flint")?Items.FLINT:Items.STICK)));
  for(var entry:ENTRIES.entrySet()){String name=entry.getKey(),kind=entry.getValue().get("kind").getAsString(),material=entry.getValue().get("material").getAsString();if(Content.ALL_ITEMS.containsKey(name))throw new IllegalStateException("duplicate original item "+name);Content.item(name,()->create(kind,material));}
  for(String material:new String[]{"obsidian","rusted_iron","ancient_metal"})Content.item(material+"_arrow",()->new Tools.MetalArrow(material,new Item.Properties().stacksTo(16)));
 }
 private static Item create(String kind,String material){var tier=Content.TIERS.get(material);var p=new Item.Properties();return switch(kind){case "pickaxe","warhammer"->new PickaxeItem(tier,0,-2.4f,p);case "axe","hatchet","battleaxe"->new Tools.Axe(tier,0,-2.4f,p);case "shovel"->new ShovelItem(tier,0,-2.4f,p);case "hoe","scythe"->new Tools.Hoe(tier,0,-2.4f,p);case "mattock"->new Tools.Mattock(tier,0,-2.4f,p);case "sword","dagger","knife","cudgel","club"->new Tools.Sword(tier,0,-2.4f,p);case "shears"->new ShearsItem(p.durability((int)(800*LegacyToolWear.durability(material))));case "bow"->new Tools.MetalBow(2,p.durability(ClassicToolCatalogue.bowDurability(material)));case "fishing_rod"->new FishingRodItem(p.durability(ClassicToolCatalogue.rodDurability(material)));default->throw new IllegalStateException(kind);};}
 private OriginalToolItems(){}
}
