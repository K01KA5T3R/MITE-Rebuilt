package dev.koikaster.mite;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.item.ItemStack;
/** Confirmed R195 declaration/constructor caps; unresolved counterparts keep their existing adapter. */
final class OriginalStackLimits {
 private static final Map<String,Integer> CAPS=load();
 private static Map<String,Integer> load(){try(var input=OriginalStackLimits.class.getResourceAsStream("/data/mite_reforged/source_stack_limits_041.json")){if(input==null)throw new IOException("missing source stack limits");var rows=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("caps");var caps=new HashMap<String,Integer>();for(var row:rows.entrySet()){int cap=row.getValue().getAsInt();if(cap<1||cap>64)throw new IOException("invalid source stack cap: "+row.getKey());caps.put(row.getKey(),cap);}return Map.copyOf(caps);}catch(IOException error){throw new ExceptionInInitializerError(error);}}
 static Integer cap(ItemStack stack){var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());return key==null?null:CAPS.get(key.toString());}
 private OriginalStackLimits(){}
}
