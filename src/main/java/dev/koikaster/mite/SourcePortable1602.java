package dev.koikaster.mite;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import com.google.gson.*;import net.minecraft.tags.BlockTags;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraftforge.registries.ForgeRegistries;
/** Registered original isPortable dispatch; owner-specific strongboxes stay contextual. */
public final class SourcePortable1602 {
 private static final Map<String,Boolean> DATA=load();
 private static Map<String,Boolean> load(){try(var in=SourcePortable1602.class.getResourceAsStream("/data/mite_reforged/original_portable1602.json")){if(in==null)throw new IOException("missing portable block facts");var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();var result=new HashMap<String,Boolean>();for(var e:root.getAsJsonObject("blocks").entrySet())result.put(e.getKey(),e.getValue().getAsJsonObject().get("portable").getAsBoolean());return Map.copyOf(result);}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 public static Boolean portable(BlockState state){
  var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());if(id==null)return null;var exact=DATA.get(id.toString());if(exact!=null)return exact;
  if(state.getBlock() instanceof ThermalFurnace.BlockImpl)return true;
  if(state.getBlock() instanceof R196Workbench)return DATA.get("minecraft:crafting_table");
  if(!id.getNamespace().equals("minecraft"))return null;
  if(state.getBlock() instanceof AbstractFurnaceBlock)return true;
  if(state.is(BlockTags.BEDS)||state.getBlock() instanceof AnvilBlock||state.getBlock() instanceof StandingSignBlock||state.getBlock() instanceof WallSignBlock)return true;
  return null;
 }
 private SourcePortable1602(){}
}
