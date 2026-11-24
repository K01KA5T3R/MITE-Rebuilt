package dev.koikaster.mite;

import java.util.ArrayList;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Apply after vanilla and the original fixed recipe restoration, including /reload. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ModernRecipePolicy {
 public static boolean removed(Recipe<?> recipe,RegistryAccess access){
  // Original1740 actual matcher rejects these pre1.6 vanilla shortcuts.
  if(recipe.getType()==RecipeType.CRAFTING&&recipe.getId().getNamespace().equals("minecraft")&&java.util.Set.of("sandstone","melon").contains(recipe.getId().getPath()))return true;
  if(recipe.getId().getPath().contains("deepslate")&&java.util.Set.of("minecraft",MiteReforged.ID).contains(recipe.getId().getNamespace()))return true;
  if(recipe.getType()==RecipeType.STONECUTTING||recipe instanceof SmithingTrimRecipe||recipe instanceof net.minecraft.world.item.crafting.ShieldDecorationRecipe||ModernContentPolicy.removed(recipe.getResultItem(access))||LegacyMetalOres.hidden(recipe.getResultItem(access)))return true;
  return recipe.getIngredients().stream().anyMatch(ingredient->removedIngredient(ingredient.toJson()));
 }
 /** Reload listeners run before registry tags are published. Expanding tag candidates here
  * would cache an empty Ingredient list and break otherwise valid tag-based recipes. */
 private static boolean removedId(ResourceLocation id){return ModernContentPolicy.removedId(id)||LegacyMetalOres.hiddenId(id);}
 private static boolean removedIngredient(JsonElement json){
  if(json.isJsonArray()){var choices=json.getAsJsonArray();if(choices.isEmpty())return false;for(var choice:choices)if(!removedIngredient(choice))return false;return true;}
  if(!json.isJsonObject())return false;var value=json.getAsJsonObject();
  if(value.has("tag"))return false; // Keep unresolved tag alternatives; removed outputs and actual items remain guarded.
  if(value.has("item"))return removedId(new ResourceLocation(value.get("item").getAsString()));
  if(value.has("items")&&value.get("items").isJsonArray()){var choices=value.getAsJsonArray("items");if(choices.isEmpty())return false;for(var choice:choices)if(!choice.isJsonPrimitive()||!removedId(new ResourceLocation(choice.getAsString())))return false;return true;}
  return false;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void reload(AddReloadListenerEvent event){
  var recipes=event.getServerResources().getRecipeManager();var access=event.getRegistryAccess();
  event.addListener(new SimplePreparableReloadListener<Boolean>(){
   protected Boolean prepare(ResourceManager manager,ProfilerFiller profiler){return true;}
   protected void apply(Boolean ready,ResourceManager manager,ProfilerFiller profiler){var kept=new ArrayList<Recipe<?>>();for(var recipe:recipes.getRecipes())if(!removed(recipe,access))kept.add(recipe);recipes.replaceRecipes(kept);}
  });
 }
 private ModernRecipePolicy(){}
}
