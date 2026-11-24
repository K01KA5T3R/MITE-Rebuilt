package dev.koikaster.mite;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/** uf product checks in source order; independent of already verified nutrient masks. */
public final class CurseIngestion {
 private static final Map<String,Integer> FLAGS=load();
 private static Map<String,Integer> load(){var flags=new HashMap<String,Integer>();try(var stream=CurseIngestion.class.getResourceAsStream("/data/mite_reforged/curse_ingestion.json")){if(stream==null)throw new IllegalStateException("Missing curse ingestion flags");var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();for(String group:new String[]{"counterparts","modern_adaptations"})for(var entry:json.getAsJsonObject(group).entrySet())flags.put(entry.getKey(),entry.getValue().getAsInt());}catch(java.io.IOException error){throw new IllegalStateException(error);}return Map.copyOf(flags);}
 public static int flags(ItemStack stack){if(ClassicVessels.fluid(stack).equals("milk"))return 5;String key=String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()));return FLAGS.getOrDefault(key,stack.getUseAnimation()==UseAnim.DRINK?4:0);}
 public static boolean forbidden(Player player,ItemStack stack){int flags=flags(stack);if((flags&1)!=0&&OriginalCurseEffects.has(player,4,true))return true;if((flags&2)!=0&&OriginalCurseEffects.has(player,5,true))return true;return (flags&4)!=0&&OriginalCurseEffects.has(player,6,true)&&!stack.is(Content.BOTTLE_OF_DISENCHANTING.get());}
 private CurseIngestion(){}
}
