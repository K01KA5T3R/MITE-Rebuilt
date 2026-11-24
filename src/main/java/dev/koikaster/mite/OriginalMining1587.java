package dev.koikaster.mite;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.registries.ForgeRegistries;

/** Registered R195 material capabilities and fixed explosion resistance. */
public final class OriginalMining1587 {
 private record Facts(int id,int metadata,String material,boolean requiresTool,Float resistance){}
 private record Tool(Set<String> materials,Set<Integer> blocks,float base,float sandstoneBase){}
 private static final Map<String,Facts> BLOCKS=new HashMap<>();
 private static final Map<String,Tool> TOOLS=new HashMap<>();
 private static final Set<String> TOOL_MATERIALS=new HashSet<>();
 static {
  try(var in=OriginalMining1587.class.getResourceAsStream("/data/mite_reforged/original_mining1587.json")){
   if(in==null)throw new IOException("missing original mining facts");
   var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
   for(var e:root.getAsJsonObject("blocks").entrySet()){
    var f=e.getValue().getAsJsonObject();
    BLOCKS.put(e.getKey(),new Facts(f.get("id").getAsInt(),f.get("metadata").getAsInt(),f.get("material").getAsString(),f.get("requires_tool").getAsBoolean(),f.get("resistance").getAsFloat()));
   }
   for(var e:root.getAsJsonObject("tools").entrySet()){
    var t=e.getValue().getAsJsonObject();var materials=new HashSet<String>();var blocks=new HashSet<Integer>();
    for(var m:t.getAsJsonArray("materials"))materials.add(m.getAsString());
    for(var b:t.getAsJsonArray("explicit_blocks"))blocks.add(b.getAsInt());
    TOOLS.put(e.getKey(),new Tool(Set.copyOf(materials),Set.copyOf(blocks),t.get("base").getAsFloat(),t.get("sandstone_base").getAsFloat()));
   }
   for(var e:root.getAsJsonObject("material_tool_flags").entrySet())if(e.getValue().getAsBoolean())TOOL_MATERIALS.add(e.getKey());
  }catch(IOException e){throw new ExceptionInInitializerError(e);}
 }
 private static Facts entry(String name,boolean doubled){return BLOCKS.get(name+(doubled?"#double":""));}
 private static Facts facts(BlockState state){
  var key=ForgeRegistries.BLOCKS.getKey(state.getBlock());if(key==null)return null;
  boolean doubled=state.getBlock() instanceof SlabBlock&&state.getValue(SlabBlock.TYPE)==SlabType.DOUBLE;
  if(state.is(Blocks.LAVA))return BLOCKS.get(state.getValue(LiquidBlock.LEVEL)==0?"minecraft:lava#source":"minecraft:lava");
  var exact=entry(key.toString(),doubled);if(exact!=null)return exact;
  if(state.getBlock() instanceof R196Workbench)return entry("minecraft:crafting_table",false);
  if(key.getNamespace().equals("minecraft")){
   String other=null,path=key.getPath();
   if(path.startsWith("deepslate_")&&path.endsWith("_ore"))other=path.substring(10);
   else if(state.is(BlockTags.LOGS))other="oak_log";
   else if(state.is(BlockTags.PLANKS))other="oak_planks";
   else if(state.is(BlockTags.WOODEN_STAIRS))other="oak_stairs";
   else if(state.is(BlockTags.WOODEN_SLABS))other="oak_slab";
   else if(state.is(BlockTags.WOODEN_DOORS))other="oak_door";
   else if(state.is(BlockTags.WOODEN_TRAPDOORS))other="oak_trapdoor";
   else if(state.is(BlockTags.WOODEN_FENCES))other="oak_fence";
   else if(state.is(BlockTags.WOOL))other="white_wool";
   else if(state.is(BlockTags.WOOL_CARPETS))other="white_carpet";
   else if(state.getBlock() instanceof SaplingBlock)other="oak_sapling";
   else if(state.getBlock() instanceof WallSignBlock)other="oak_wall_sign";
   else if(state.getBlock() instanceof StandingSignBlock)other="oak_sign";
   else if(state.getBlock() instanceof BedBlock)other="red_bed";
   else if(state.getBlock() instanceof InfestedBlock)other="infested_stone";
   else if(state.getBlock() instanceof StainedGlassPaneBlock)other="glass_pane";
   else if(state.getBlock() instanceof AbstractGlassBlock)other="glass";
   else if(state.is(BlockTags.TERRACOTTA))other="terracotta";
   else if(state.getBlock() instanceof AnvilBlock)other="anvil";
   if(other!=null){var alias=entry("minecraft:"+other,doubled);if(alias!=null)return alias;}
  }
  // Existing confirmed modern material adapters retain their own hardness and level.
  String material=OriginalBlockPhysics1566.material(state);if(material==null)return null;
  material=switch(material){case "bronze"->"copper";case "steel"->"iron";case "netherite"->"mithril";case "holy_steel"->"adamantium";default->material;};
  if(!key.getNamespace().equals("minecraft")&&!key.getNamespace().equals(MiteReforged.ID))return null;
  return new Facts(-1,0,material,TOOL_MATERIALS.contains(material)||HarvestRules.requiredLevel(state)>0,null);
 }
 private static boolean knownItem(ItemStack stack){var id=ForgeRegistries.ITEMS.getKey(stack.getItem());return id!=null&&(id.getNamespace().equals("minecraft")||id.getNamespace().equals(MiteReforged.ID));}
 private static String kind(LegacyToolWear.Kind kind){return switch(kind){case WARHAMMER->"war_hammer";case BATTLEAXE->"battle_axe";default->kind.name().toLowerCase(Locale.ROOT);};}
 private static Tool tool(ClassicToolWear.Profile p){
  String kind=kind(p.kind());boolean metal=!Set.of("wood","flint","obsidian","stone").contains(p.material());
  var t=TOOLS.get(kind+(metal?"/metal":"/nonmetal"));return t!=null?t:TOOLS.get(kind+(metal?"/nonmetal":"/metal"));
 }
 private static Float strength(ItemStack stack,BlockState state,Facts f){
  var p=ClassicToolWear.profile(stack);if(p==null)return stack.isEmpty()||knownItem(stack)?0f:null;
  var t=tool(p);if(t==null)return null;
  boolean slab=f.id==44&&f.metadata==1&&(p.kind()==LegacyToolWear.Kind.AXE||p.kind()==LegacyToolWear.Kind.HATCHET||p.kind()==LegacyToolWear.Kind.BATTLEAXE);
  boolean effective=slab||(t.materials.contains(f.material)||t.blocks.contains(f.id))&&LegacyHarvest.material(p.material())>=HarvestRules.requiredLevel(state);
  if(!effective)return 0f;
  float base=f.id==24?t.sandstoneBase:t.base;
  var key=ForgeRegistries.BLOCKS.getKey(state.getBlock());
  boolean deep=state.is(Blocks.DEEPSLATE)||key!=null&&key.getPath().startsWith("deepslate_")&&key.getPath().endsWith("_ore");
  float factor=deep&&HarvestRules.toolKind(stack)==LegacyHarvest.Tool.PICKAXE&&p.material().equals("copper")?.75f:1f;
  return base*LegacyMining.materialEfficiency(p.material())*factor;
 }
 public static Float strength(ItemStack stack,BlockState state){var f=facts(state);return f==null?null:strength(stack,state,f);}
 public static Float playerStrength(ItemStack stack,BlockState state){var f=facts(state);if(f==null)return null;Float value=strength(stack,state,f);if(value==null)return null;if(value>=1)return value;float fallback=f.requiresTool?0f:1f;return state.is(Blocks.COBWEB)?fallback*.2f:fallback;}
 public static Boolean canHarvest(ItemStack stack,BlockState state){var f=facts(state);if(f==null)return null;Float value=strength(stack,state,f);return value==null?null:value>0||!f.requiresTool;}
 public static Boolean shearEffective(BlockState state){var f=facts(state);if(f==null)return null;var t=TOOLS.get("shears/metal");return (t.materials.contains(f.material)||t.blocks.contains(f.id))&&LegacyHarvest.material("iron")>=HarvestRules.requiredLevel(state);}
 public static Float explosionResistance(BlockState state){var f=facts(state);return f==null?null:f.resistance;}
 private OriginalMining1587(){}
}
