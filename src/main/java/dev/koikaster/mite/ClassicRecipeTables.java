package dev.koikaster.mite;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Forge's stock data pack overrides native arrow/rod JSON after mod resources. Restore these fixed original recipes after decoding. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicRecipeTables {
 @SubscribeEvent public static void reload(AddReloadListenerEvent event){var recipes=event.getServerResources().getRecipeManager();event.addListener(new SimplePreparableReloadListener<Map<ResourceLocation,Recipe<?>>>(){
  protected Map<ResourceLocation,Recipe<?>> prepare(ResourceManager manager,ProfilerFiller profiler){try(var in=ClassicRecipeTables.class.getResourceAsStream("/data/mite_reforged/classic_native_tool_recipes.json")){if(in==null)throw new IOException("missing fixed native tool recipes");var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();var result=new LinkedHashMap<ResourceLocation,Recipe<?>>();for(var entry:root.entrySet()){var id=new ResourceLocation(entry.getKey());result.put(id,RecipeManager.fromJson(id,entry.getValue().getAsJsonObject()));}return result;}catch(IOException error){throw new IllegalStateException(error);}}
  protected void apply(Map<ResourceLocation,Recipe<?>> fixed,ResourceManager manager,ProfilerFiller profiler){var combined=new ArrayList<Recipe<?>>();for(var recipe:recipes.getRecipes())if(!fixed.containsKey(recipe.getId()))combined.add(recipe);combined.addAll(fixed.values());recipes.replaceRecipes(combined);}
 });}
 private ClassicRecipeTables(){}
}
