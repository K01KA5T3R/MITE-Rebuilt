package dev.koikaster.mite;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.saveddata.SavedData;

/** Curse/js/WorldInfoShared/tv state port.0.22 consumers and natural trigger are still being wired. */
public final class OriginalCurses extends SavedData {
 public record Definition(int id,String name,String key){}
 private static final Definition[] DEFINITIONS=new Definition[64];
 static {define(1,"equipment_decays_faster","equipmentDecay");define(2,"cannot_hold_breath","cantHoldBreath");define(3,"cannot_run","cantRun");define(4,"cannot_eat_animals","cantEatAnimals");define(5,"cannot_eat_plants","cantEatPlants");define(6,"cannot_drink","cantDrink");define(7,"endermen_aggro","endermenEnemy");define(8,"clumsiness","clumsiness");define(9,"entanglement","entanglement");define(10,"cannot_wear_armor","cantWearArmor");define(11,"cannot_open_chests","cantOpenChests");define(12,"cannot_sleep","cantSleep");define(13,"fear_of_spiders","fearOfSpiders");define(14,"fear_of_wolves","fearOfWolves");define(15,"fear_of_creepers","fearOfCreepers");define(16,"fear_of_undead","fearOfUndead");}
 private static void define(int id,String name,String key){DEFINITIONS[id]=new Definition(id,name,key);}
 public static Definition definition(int id){return id>=0&&id<DEFINITIONS.length?DEFINITIONS[id]:null;}
 public static Definition random(Random random){Definition value;do{value=DEFINITIONS[random.nextInt(DEFINITIONS.length)];}while(value==null);return value;}
 public static final class Instance {
  public final String username;public final UUID witch;public final Definition definition;public final long due;
  private boolean realized,known,learned;
  private Instance(String username,UUID witch,Definition definition,long due,boolean realized,boolean known){this.username=username;this.witch=witch;this.definition=definition;this.due=due;this.realized=realized;this.known=known;}
  public boolean realized(){return realized;}public boolean known(){return known;}public boolean learned(){return learned;}
 }
 private final List<Instance> curses=new ArrayList<>();
 public static OriginalCurses get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(OriginalCurses::load,OriginalCurses::new,"mite_reforged_world_curses");}
 private static String username(ServerPlayer player){return player.getGameProfile().getName();}
 public Instance find(ServerPlayer player){String name=username(player);for(var curse:curses)if(curse.username.equals(name))return curse;return null;}
 public boolean pending(ServerPlayer player){var curse=find(player);return curse!=null&&!curse.realized;}
 public int activeId(ServerPlayer player){var curse=find(player);return curse!=null&&curse.realized?curse.definition.id():0;}
 public List<Instance> instances(){return Collections.unmodifiableList(curses);}
 /** tv creates the seed once per witch; Forge persistent entity NBT adapts its original dedicated field. */
 public static int witchSeed(Witch witch){if(witch instanceof SourceWitch source)return source.sourceCurseSeed();var data=witch.getPersistentData();if(!data.contains("MiteCurseSeed",Tag.TAG_INT))data.putInt("MiteCurseSeed",new Random().nextInt());return data.getInt("MiteCurseSeed");}
 public boolean tryCurse(ServerPlayer player,Witch witch){if(!witch.isAlive()||find(player)!=null)return false;int hash=0;String name=username(player);for(int i=0;i<name.length();i++)hash+=name.charAt(i)*i;return add(player,witch,random(new Random(witchSeed(witch)+hash)),6000);}
 public boolean add(ServerPlayer player,Witch witch,Definition definition,int ticksDelay){if(!witch.isAlive()||find(player)!=null)return false;curses.add(new Instance(username(player),witch.getUUID(),Objects.requireNonNull(definition),player.serverLevel().getGameTime()+ticksDelay,false,false));setDirty();return true;}
 /** Returns true only on the realization edge; effects/notifications must be applied by the server consumer. */
 public boolean realize(ServerPlayer player){var curse=find(player);if(curse==null||curse.realized||curse.due>player.serverLevel().getGameTime())return false;curse.realized=true;setDirty();return true;}
 public boolean learn(ServerPlayer player){var curse=find(player);if(curse==null||!curse.realized||curse.known)return false;curse.known=true;setDirty();return true;}
 public boolean markLearned(ServerPlayer player){var curse=find(player);if(curse==null||!curse.realized||!curse.known||curse.learned)return false;curse.learned=true;return true;}
 public boolean removeFromPlayer(ServerPlayer player){String name=username(player);boolean removed=curses.removeIf(curse->curse.username.equals(name));if(removed)setDirty();return removed;}
 public boolean removeForWitch(UUID witch){boolean removed=curses.removeIf(curse->curse.witch.equals(witch));if(removed)setDirty();return removed;}
 public static OriginalCurses load(CompoundTag tag){var data=new OriginalCurses();var list=tag.getList("Curses",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var row=list.getCompound(i);var definition=definition(row.getInt("Id"));if(definition==null||!row.hasUUID("Witch"))continue;var curse=new Instance(row.getString("Username"),row.getUUID("Witch"),definition,row.getLong("Due"),row.getBoolean("Realized"),row.getBoolean("Known"));curse.learned=curse.known;data.curses.add(curse);}return data;}
 @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();for(var curse:curses){var row=new CompoundTag();row.putString("Username",curse.username);row.putUUID("Witch",curse.witch);row.putInt("Id",curse.definition.id());row.putLong("Due",curse.due);row.putBoolean("Realized",curse.realized);row.putBoolean("Known",curse.known);list.add(row);}tag.put("Curses",list);return tag;}
}
