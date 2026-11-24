package dev.koikaster.mite;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
/** Source ItemMeat identity, including fish/flesh/worms; explicit modern animal/fish counterparts. */
public final class OriginalMeatItems {
 private static final Set<String> ITEMS=load();
 private static Set<String> load(){try(var stream=OriginalMeatItems.class.getResourceAsStream("/data/mite_reforged/source_meat_items.json")){if(stream==null)throw new IOException("source meat identities missing");var root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();var result=new HashSet<String>();for(String key:List.of("source_counterparts","modern_adaptations"))for(var item:root.getAsJsonArray(key))result.add(item.getAsString());return Set.copyOf(result);}catch(IOException failure){throw new ExceptionInInitializerError(failure);}}
 public static boolean includes(ItemStack stack){return !stack.isEmpty()&&ITEMS.contains(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));}
 private OriginalMeatItems(){}
}
