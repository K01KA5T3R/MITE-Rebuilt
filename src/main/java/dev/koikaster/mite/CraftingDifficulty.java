package dev.koikaster.mite;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Original component costs plus explicit adaptations for materials added after R195. */
public final class CraftingDifficulty {
 private static final Map<String,Float> EXACT=new HashMap<>(),OVERRIDES=new HashMap<>();
 private static final Map<RecipeManager,Profile> CACHE=new WeakHashMap<>();
 static {
  try(var input=CraftingDifficulty.class.getResourceAsStream("/data/mite_reforged/crafting_difficulty.json")){
   if(input==null)throw new IOException("missing crafting difficulty profiles");
   var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
   root.getAsJsonObject("components").entrySet().forEach(e->EXACT.put(e.getKey(),e.getValue().getAsFloat()));
   root.getAsJsonObject("block_hardness").entrySet().forEach(e->EXACT.put(e.getKey(),e.getValue().getAsFloat()*100f));
   root.getAsJsonObject("recipe_overrides").entrySet().forEach(e->OVERRIDES.put(e.getKey(),e.getValue().getAsFloat()));
  }catch(IOException error){throw new ExceptionInInitializerError(error);}
 }
 private static Float base(Item item,boolean r196){
  if(r196){
   float goldIngot=R196Gold.DURABILITY*100f;
   if(item==Items.GOLD_INGOT)return goldIngot;
   if(item==Items.GOLD_NUGGET)return goldIngot/9f;
   if(item==Items.GOLD_BLOCK)return goldIngot*9f;
  }
  if(item==IteContent0416.DIAMOND_CHUNK.get()){Float d=EXACT.get("minecraft:diamond");if(d!=null)return d/4f;}
  if(item instanceof DisenchantingBottle)return 25f;
  if(item instanceof SpecialItems.Coin||item instanceof SpecialItems.MetalBucket||item instanceof BucketItem)return 25f;
  var key=ForgeRegistries.ITEMS.getKey(item);if(key==null)return null;var exact=EXACT.get(key.toString());if(exact!=null)return exact;
  // RecipeHelper: naturally found rusted chains inherit their copper peer's
  // component cost, scaled by material durability (both source materials are4).
  if(key.equals(Content.id("rusted_iron_chain")))return EXACT.get("mite_reforged:copper_nugget")*4f*LegacyToolWear.durability("rusted_iron")/LegacyToolWear.durability("copper");
  if(key.equals(Content.id("rusted_iron_nugget")))return EXACT.get("mite_reforged:copper_nugget");
  if(key.equals(Content.id("rusted_iron_ingot")))return EXACT.get("mite_reforged:copper_nugget")*9f;
  if(item instanceof BlockItem block){var state=block.getBlock().defaultBlockState();
   String name=key.getPath();
   // Original anvils/furnaces override component=-1; original item forms such as
   // doors, beds and signs derive their cost from recipes rather than block hardness.
   if(block.getBlock() instanceof SourceMetalBuildings.Strongbox||block.getBlock() instanceof SourceMetalBuildings.MetalDoor||block.getBlock() instanceof MetalAnvilBlock||block.getBlock() instanceof ThermalFurnace.BlockImpl||state.is(BlockTags.ANVIL)||state.is(BlockTags.WOODEN_DOORS)||state.is(BlockTags.BEDS)||state.is(BlockTags.ALL_SIGNS)||Set.of("iron_door","cake","repeater","comparator","brewing_stand","cauldron","flower_pot").contains(name))return null;
   if(state.is(BlockTags.LOGS))return 1.2f*100f;
   if(state.is(BlockTags.PLANKS))return (1.2f*2f/3f)*100f;
   if(state.is(BlockTags.WOODEN_SLABS))return .4f*100f;
   if(state.is(BlockTags.WOODEN_STAIRS))return (1.2f*2f/3f)*100f;
   if(state.is(BlockTags.WOOL))return 80f;
   // Post-R195 blocks have no original counterpart: use their own material hardness.
   return Math.max(0f,block.getBlock().defaultDestroyTime())*100f;
  }
  if(item.isEdible())return 25f;
  return null;
 }
 private static final class Profile {
  final IdentityHashMap<Item,Float> costs=new IdentityHashMap<>();
  final Map<ResourceLocation,Recipe<?>> snapshot=new HashMap<>();
  final int count;
  final boolean r196;
  Profile(RecipeManager manager,RegistryAccess access,boolean r196){
   this.r196=r196;
   var recipes=manager.getAllRecipesFor(RecipeType.CRAFTING);count=manager.getRecipes().size();for(var recipe:recipes)snapshot.put(recipe.getId(),recipe);
   var fixed=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());var products=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
   for(var item:ForgeRegistries.ITEMS){var value=base(item,r196);if(value!=null){costs.put(item,value);fixed.add(item);}}
   for(var recipe:recipes){var output=recipe.getResultItem(access);if(!output.isEmpty()&&!recipe.getIngredients().isEmpty())products.add(output.getItem());}
   // Source RecipeHelper resolves a product component to its lowest recipe cost per unit.
   // Never seed a craftable product with an arbitrary cheap value before resolving its DAG.
   for(var item:ForgeRegistries.ITEMS)if(!costs.containsKey(item)&&!products.contains(item))costs.put(item,25f);
   derive(recipes,access,fixed);
   // Only unresolved modern/custom cycles use the documented modern component fallback.
   for(var item:ForgeRegistries.ITEMS)if(!costs.containsKey(item))costs.put(item,25f);
  }
  private void derive(java.util.List<CraftingRecipe> recipes,RegistryAccess access,Set<Item> fixed){
   for(int pass=0;pass<64;pass++){boolean changed=false;
    for(var recipe:recipes){if(recipe instanceof SourceCarrotStickRecipe carrot&&carrot.dismantling())continue;var out=recipe.getResultItem(access);if(out.isEmpty()||fixed.contains(out.getItem())||recipe.getIngredients().isEmpty())continue;
     float difficulty=0;boolean known=true;
     for(var ingredient:recipe.getIngredients()){if(ingredient.isEmpty())continue;float best=Float.POSITIVE_INFINITY;for(var stack:ingredient.getItems()){var value=costs.get(stack.getItem());if(value!=null)best=Math.min(best,componentValue(stack,costs));}if(!Float.isFinite(best)){known=false;break;}difficulty+=best;}
     if(!known)continue;difficulty=(recipe instanceof SourceCarrotStickRecipe?40f:OVERRIDES.getOrDefault(recipe.getId().toString(),difficulty))/out.getCount();
     if(!Float.isFinite(difficulty)||difficulty<0)continue;var old=costs.get(out.getItem());if(old==null||difficulty<old){costs.put(out.getItem(),difficulty);changed=true;}
    }
    if(!changed)break;
   }
  }
 }
 private static synchronized Profile profile(Player player,Recipe<?> current){
  var manager=player.level().getRecipeManager();var profile=CACHE.get(manager);
  boolean r196=WorldContentMode.r196(player.level());
  if(profile==null||profile.r196!=r196||profile.count!=manager.getRecipes().size()||current!=null&&profile.snapshot.get(current.getId())!=current){profile=new Profile(manager,player.level().registryAccess(),r196);CACHE.put(manager,profile);}
  return profile;
 }
 public static float component(Player player,Item item){return profile(player,null).costs.getOrDefault(item,25f);}
 private static float componentValue(ItemStack stack,IdentityHashMap<Item,Float> values){return stack.getItem() instanceof SpecialItems.MetalBucket bucket&&bucket.fluid(stack).equals("milk")?100f:values.getOrDefault(stack.getItem(),25f);}
 public static float productCost(Player player,Item item){var armor=OriginalArmorItems.profile(new ItemStack(item));if(armor!=null&&armor.material().equals("rusted_iron"))item=Content.get("copper_"+(armor.chain()?"chainmail_":"")+armor.type().getName()); // aaf.checkRecipe: same kind/chain copper peer, no invented rusted ingot recipe.
  var tool=ClassicToolWear.profile(new ItemStack(item));if(tool!=null){var canonical=Content.ALL_ITEMS.get(tool.material()+"_"+tool.kind().name().toLowerCase(Locale.ROOT));if(canonical!=null)item=canonical.get();}return profile(player,null).costs.getOrDefault(item,25f);}
 public static float difficulty(Player player,CraftingContainer grid,Recipe<?> recipe){
  if(recipe instanceof SourceCarrotStickRecipe)return 40f;
  if(recipe instanceof R196BenchRecipe bench&&bench.dismantling())return 25f;
  if(recipe instanceof ClassicCraftRepair)return ClassicCraftRepair.difficulty(player,grid);
  if(recipe==null)return 0;var override=OVERRIDES.get(recipe.getId().toString());if(override!=null)return override;
  var values=profile(player,recipe).costs;float result=0;
  for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(!stack.isEmpty())result+=componentValue(stack,values);}
  return result;
 }
 public static float qualityAdjustedDifficulty(Player player,CraftingContainer grid,Recipe<?> recipe,ItemStack output){
  float base=difficulty(player,grid,recipe);
  if(ClassicQualityCrafting.supportsQuality(recipe,output)){
   ClassicQuality.Level quality=ClassicQuality.get(output);
   if(quality!=null)return base*quality.craftingDifficultyMultiplier();
  }
  return base;
 }
 public static int period(Player player,CraftingContainer grid,Recipe<?> recipe,boolean table){
  if(recipe==null)return 1;
  float workbenchModifier=table?.25f:0f;
  ItemStack output;
  TimedCrafting.Menu tableMenu=table&&player.containerMenu instanceof TimedCrafting.Menu current&&current.grid==grid?current:null;
  if(tableMenu!=null){
   output=tableMenu.getSlot(0).getItem();
  }else if(!table&&player.inventoryMenu!=null&&player.inventoryMenu.slots.size()>1&&player.inventoryMenu.getSlot(1).container==grid){
   output=player.inventoryMenu.getSlot(0).getItem();
  }else{
   output=recipe.getResultItem(player.level().registryAccess());
  }
  if(table&&WorldContentMode.r196(player.level())){
   var tier=tableMenu==null?R196Workbench.Tier.FLINT:tableMenu.workbenchTier();
   workbenchModifier=SourceR196Crafting.requiredHardness(grid,output,recipe)>0?tier.modifier:.20f;
  }
  float diff=qualityAdjustedDifficulty(player,grid,recipe,output);
  int period=LegacyCrafting.period(diff,PlayerState.level(player),workbenchModifier,OriginalCurseEffects.has(player,8,false));
  return finalizePeriod(recipe,period);
 }
 public static int automaticR196Period(Player player,CraftingContainer grid,Recipe<?> recipe,ItemStack output){
  float modifier=SourceR196Crafting.requiredHardness(grid,output,recipe)>0?R196Workbench.Tier.MITHRIL.modifier:.20f;
  return finalizePeriod(recipe,LegacyCrafting.period(difficulty(player,grid,recipe),0,modifier,false));
 }
 private static int finalizePeriod(Recipe<?> recipe,int period){
  if(recipe instanceof AdvancedTableRecipe)period=Math.max(period,AdvancedEnchantConfig.policy().minimumCraftTicks());
  return (int)Math.max(1,Math.min(Integer.MAX_VALUE-1d,period*Settings.CRAFT_TIME));
 }
 private CraftingDifficulty(){}
}
