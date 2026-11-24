package dev.koikaster.mite;
import com.google.gson.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Immutable kp61 source definitions. Parent relations are presentation hints, not server grant gates. */
public final class SourceAchievementCatalog561 {
 public record Node(int id,String name,int x,int y,int parent,int secondParent,boolean challenge,boolean flipped,int tooltipWidth,String icon,String title,String description){public int statId(){return 0x500000+id;}}
 public static final List<Node> NODES=load();
 private static final List<Node> R196_NODES=R196AchievementLayout.apply(NODES);
 private static final Map<Integer,Node> IDS=byId(NODES), R196_IDS=byId(R196_NODES);
 private static final Map<String,Node> NAMES=byName();
 public static List<Node> nodes(boolean r196){return r196?R196_NODES:NODES;}
 private static List<Node> load(){try(var in=SourceAchievementCatalog561.class.getResourceAsStream("/data/mite_reforged/source_achievements561.json")){if(in==null)throw new IOException("Missing original achievement definitions");var a=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("nodes");var list=new ArrayList<Node>();for(var v:a){var n=v.getAsJsonObject();list.add(new Node(n.get("id").getAsInt(),n.get("name").getAsString(),n.get("x").getAsInt(),n.get("y").getAsInt(),n.get("parent").getAsInt(),n.get("second_parent").getAsInt(),n.get("challenge").getAsBoolean(),n.get("flipped").getAsBoolean(),n.get("tooltip_width").getAsInt(),n.get("icon").getAsString(),n.get("title").getAsString(),n.get("description").getAsString()));}if(list.size()!=61)throw new IOException("Original achievement roster mismatch");return List.copyOf(list);}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 private static Map<Integer,Node> byId(List<Node> nodes){var map=new LinkedHashMap<Integer,Node>();for(var n:nodes)if(map.put(n.id(),n)!=null)throw new IllegalStateException("Duplicate source achievement ID");return Collections.unmodifiableMap(map);}
 private static Map<String,Node> byName(){var map=new LinkedHashMap<String,Node>();for(var n:R196_NODES)if(map.put(n.name(),n)!=null)throw new IllegalStateException("Duplicate source achievement name");return Collections.unmodifiableMap(map);}
 public static Node node(int id){return node(id,id==R196AchievementLayout.BETTER_TOOLS);}
 public static Node node(int id,boolean r196){var n=(r196?R196_IDS:IDS).get(id);if(n==null)throw new IllegalArgumentException("Unknown source achievement "+id);return n;}
 public static Node node(String name){var n=NAMES.get(name);if(n==null)throw new IllegalArgumentException("Unknown source achievement "+name);return node(n.id());}
 public static boolean known(int id){return R196_IDS.containsKey(id);}
 private SourceAchievementCatalog561(){}
}
