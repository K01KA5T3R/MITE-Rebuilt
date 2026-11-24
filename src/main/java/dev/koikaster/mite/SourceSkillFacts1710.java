package dev.koikaster.mite;

import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraftforge.registries.ForgeRegistries;

/** Observed original item/recipe facts. 0.4.17 removes the user38D quality skill bypass. */
public final class SourceSkillFacts1710 {
 public record Identity(int id,int subtype){}
 public record Fact(boolean quality,int[] repair){}
 public record Decision(boolean known,String origin,int[] skillsets){}
 private record Rule(int index,String owner,Identity output,int count,List<Identity> parts,int[] skillsets){}
 private static final Map<Integer,int[]> SMELTING=new HashMap<>();
 private static final Map<Integer,Fact> ITEMS=new HashMap<>();private static final Map<String,Identity> IDENTITIES=new HashMap<>(),VESSELS=new HashMap<>();private static final Map<String,Fact> ADAPTED=new HashMap<>();private static final Map<Integer,List<Rule>> RECIPES=new HashMap<>();
 private static final ResourceLocation BEEHIVE_ID=ResourceLocation.tryParse("minecraft:beehive");
 private static final ResourceLocation SPYGLASS_ID=ResourceLocation.tryParse("minecraft:spyglass");
 private static int[] sets(JsonElement e){if(e==null||e.isJsonNull())return null;int[] values=new int[e.getAsJsonArray().size()];for(int i=0;i<values.length;i++)values[i]=e.getAsJsonArray().get(i).getAsInt();return values;}
 private static Identity identity(JsonObject e){return new Identity(e.get("id").getAsInt(),e.get("subtype").getAsInt());}
 private static Identity sourceStack(JsonObject e){return new Identity(e.get("item_id").getAsInt(),e.get("subtype").getAsInt());}
 static {try(var input=SourceSkillFacts1710.class.getResourceAsStream("/data/mite_reforged/source_skill_facts1709.json")){if(input==null)throw new IOException("Missing original skill facts");var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();for(var e:root.getAsJsonObject("items").entrySet()){var row=e.getValue().getAsJsonObject();ITEMS.put(Integer.parseInt(e.getKey()),new Fact(row.get("has_quality").getAsBoolean(),sets(row.get("repair_skillsets"))));}for(var e:root.getAsJsonObject("items").entrySet())SMELTING.put(Integer.parseInt(e.getKey()),sets(e.getValue().getAsJsonObject().get("smelt_skillsets")));for(var e:root.getAsJsonObject("identities").entrySet())IDENTITIES.put(e.getKey(),identity(e.getValue().getAsJsonObject()));for(var e:root.getAsJsonObject("vessels").entrySet())VESSELS.put(e.getKey(),identity(e.getValue().getAsJsonObject()));for(var e:root.getAsJsonObject("adapted_items").entrySet()){var row=e.getValue().getAsJsonObject();ADAPTED.put(e.getKey(),new Fact(row.get("quality").getAsBoolean(),sets(row.get("repair"))));}for(var e:root.getAsJsonArray("recipes")){var row=e.getAsJsonObject();if(row.get("output").isJsonNull())continue;var output=row.getAsJsonObject("output");var parts=new ArrayList<Identity>();for(var p:row.getAsJsonArray("components"))if(!p.isJsonNull())parts.add(sourceStack(p.getAsJsonObject()));var rule=new Rule(row.get("index").getAsInt(),row.get("class").getAsString(),sourceStack(output),output.get("count").getAsInt(),List.copyOf(parts),sets(row.get("skillsets")));RECIPES.computeIfAbsent(rule.output.id,k->new ArrayList<>()).add(rule);}}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 public static Identity original(ItemStack stack){if(stack.isEmpty())return null;if(ClassicVessels.handled(stack)){var vessel=VESSELS.get(ClassicVessels.material(stack)+":"+ClassicVessels.fluid(stack));if(vessel!=null)return vessel;}if(stack.getItem() instanceof PotionItem){Integer subtype=SourceLegacyPotions1406.subtype(stack);if(subtype!=null)return new Identity(373,subtype);}var id=ForgeRegistries.ITEMS.getKey(stack.getItem());return id==null?null:IDENTITIES.get(id.toString());}
 public static Fact fact(ItemStack stack){var id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id!=null&&ADAPTED.containsKey(id.toString()))return ADAPTED.get(id.toString());var source=original(stack);return source==null?null:ITEMS.get(source.id);}
 private static Decision known(String origin,int[] sets){return new Decision(true,origin,sets);}
 private static boolean components(Rule rule,List<Identity> actual){if(rule.parts.size()!=actual.size())return false;var remaining=new ArrayList<>(actual);var sorted=new ArrayList<>(rule.parts);sorted.sort(Comparator.comparingInt(p->p.subtype==32767?1:0));for(var p:sorted){int at=-1;for(int i=0;i<remaining.size();i++){var a=remaining.get(i);if(a.id==p.id&&(p.subtype==32767||p.subtype==a.subtype)){at=i;break;}}if(at<0)return false;remaining.remove(at);}return true;}
 public static Decision crafting(Recipe<?> recipe,CraftingContainer grid,ItemStack output){
  if(recipe==null)return new Decision(false,"no_recipe",null);
  var namespace=recipe.getId().getNamespace();if(!namespace.equals("minecraft")&&!namespace.equals(MiteReforged.ID))return known("third_party_recipe_preserved",null);
  if(recipe instanceof ArmorDyeRecipe)return output.is(Items.LEATHER_HORSE_ARMOR)?known("human1741_modern_leather_horse_dye_no_added_skill",null):known("original_zw_leather_dyeing",new int[]{SourceSkillRules1685.FINE_ARTS});
  if(recipe instanceof MapCloningRecipe)return known("original_aac_map_cloning",new int[]{SourceSkillRules1685.FINE_ARTS});
  if(recipe instanceof FireworkRocketRecipe||recipe instanceof FireworkStarRecipe||recipe instanceof FireworkStarFadeRecipe)return known("original_zz_fireworks",null);
  if(recipe instanceof BookCloningRecipe)return known("human1741_modern_book_copy_no_added_skill",null);
  if(recipe.getId().equals(BEEHIVE_ID)||recipe.getId().equals(SPYGLASS_ID))return known("human1741_explicit_modern_example_no_added_skill",null);
  var lineage=SourceSkillRecipeLineage1857.crafting(recipe,output);if(lineage!=null)return lineage;
  var fact=fact(output);
  if(recipe instanceof ClassicCraftRepair)return fact==null?new Decision(false,"unmapped_repair_item",null):known("original_aaf_workbench_repair_item_requirements",fact.repair);
  var identity=original(output);if(identity==null)return new Decision(false,"no_original_output_identity",null);var rows=RECIPES.getOrDefault(identity.id,List.of()).stream().filter(r->r.output.subtype==identity.subtype).toList();if(rows.isEmpty())return new Decision(false,"no_original_output_recipe",null);
  var actual=new ArrayList<Identity>();boolean complete=true;for(int i=0;i<grid.getContainerSize();i++){var s=grid.getItem(i);if(s.isEmpty())continue;var mapped=original(s);if(mapped==null){complete=false;break;}actual.add(mapped);}
  Rule matched=null;if(complete)for(var rule:rows)if(rule.count==output.getCount()&&components(rule,actual)){if(matched!=null&&!Arrays.equals(matched.skillsets,rule.skillsets))return new Decision(false,"conflicting_source_component_or_shape_requirements",null);matched=rule;}
  if(matched!=null)return known("original_recipe_"+matched.index,matched.skillsets);
  var first=rows.get(0);if(rows.stream().allMatch(r->Arrays.equals(first.skillsets,r.skillsets)))return known("all_original_output_recipes_same_requirement_user_recipe_adapter",first.skillsets);
  return new Decision(false,"unmatched_conflicting_source_output_recipe",null);
 }
 public static Decision repair(ItemStack stack){var fact=fact(stack);return fact==null?new Decision(false,"no_original_repair_item_or_adapter",null):known("original_item_repair_requirements_or_declared_adapter",fact.repair);}
 public static Decision smelting(ItemStack stack){var identity=original(stack);return identity==null||!SMELTING.containsKey(identity.id)?known("human1741_no_original_smelting_item_no_added_skill",null):known("original_asg_item_smelting_skillsets",SMELTING.get(identity.id));}
 private SourceSkillFacts1710(){}
}
