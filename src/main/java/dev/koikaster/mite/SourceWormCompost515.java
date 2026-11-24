package dev.koikaster.mite;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;

/** Native chest random-tick and saved-inventory adapter for the original worm compost route. */
public final class SourceWormCompost515 {
 private static final String SAVED="MiteWormCompost";
 private static boolean known(Block block){var type=block.getClass();return type==ChestBlock.class||type==TrappedChestBlock.class||block instanceof SourceMetalBuildings.Strongbox;}
 /** Class identity also works while vanilla Blocks is still constructing its state cache. */
 public static boolean randomlyTicking(Object rawState,boolean original){return original||known(((BlockState)rawState).getBlock());}
 private static final class Tables {
  static final Map<String,Float> VALUES=new HashMap<>();static final Map<String,List<String>> REMAINS=new HashMap<>();
  static {try(var in=SourceWormCompost515.class.getResourceAsStream("/data/mite_reforged/source_worm_compost515.json")){
   if(in==null)throw new IllegalStateException("Missing worm compost data");var data=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
   for(var e:data.getAsJsonObject("values").entrySet()){float v=e.getValue().getAsFloat();if(!Float.isFinite(v)||v<0)throw new IllegalStateException("Invalid worm compost value");VALUES.put(e.getKey(),v);}
   for(var e:data.getAsJsonObject("remains").entrySet()){var values=new ArrayList<String>();for(var v:e.getValue().getAsJsonArray())values.add(v.getAsString());REMAINS.put(e.getKey(),List.copyOf(values));}
  }catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}}
 }
 private static String key(ItemStack stack){return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));}
 private static ItemStack stack(String id){var key=net.minecraft.resources.ResourceLocation.tryParse(id);var item=key==null?null:ForgeRegistries.ITEMS.getValue(key);return item==null?ItemStack.EMPTY:new ItemStack(item);}
 public static SourceWormCompostRules515.Food food(ItemStack stack){
  if(stack.isEmpty())return null;String id=key(stack);Float explicit=Tables.VALUES.get(id);float value;
  if(explicit==null&&stack.is(Items.COOKED_RABBIT))return food(new ItemStack(Items.RABBIT));
  if(explicit!=null)value=explicit;
  else if(stack.getItem() instanceof BlockItem block&&block.getBlock() instanceof LeavesBlock)value=1;
  else if(stack.is(ItemTags.FLOWERS))value=.25f;
  else {var energy=SurvivalFood.energy(stack,null);value=(energy.satiation()+energy.nutrition())*.1f;}
  if(!(value>0)||!Float.isFinite(value))return null;
  var remains=Tables.REMAINS.get(id);
  if(remains==null){
   var item=stack.getItem();
   if(OriginalBowls.full(stack)||item instanceof BowlFoodItem||item instanceof SuspiciousStewItem)remains=List.of("minecraft:bowl");
   else if(item instanceof HoneyBottleItem)remains=List.of("minecraft:glass_bottle");
   else if(stack.hasCraftingRemainingItem())remains=List.of(key(stack.getCraftingRemainingItem()));
   else remains=List.of();
  }
  return new SourceWormCompostRules515.Food(value,remains);
 }
 public static float fraction(ChestBlockEntity chest){return chest.getPersistentData().getFloat(SAVED);}
 private static final class Snapshot implements SourceWormCompostRules515.Inventory {
  final int maximum;final List<ItemStack> slots;
  Snapshot(ChestBlockEntity chest){maximum=chest.getMaxStackSize();slots=new ArrayList<>(chest.getContainerSize());for(int i=0;i<chest.getContainerSize();i++)slots.add(chest.getItem(i).copy());}
  Snapshot(Snapshot other){maximum=other.maximum;slots=new ArrayList<>(other.slots.size());for(var s:other.slots)slots.add(s.copy());}
  public int size(){return slots.size();}public String item(int slot){var s=slots.get(slot);return s.isEmpty()?"":key(s);}public int count(int slot){return slots.get(slot).getCount();}
  public int limit(int slot){return Math.min(maximum,slots.get(slot).getMaxStackSize());}public int limit(String item){var s=stack(item);return s.isEmpty()?0:Math.min(maximum,s.getMaxStackSize());}
  public void removeOne(int slot){var s=slots.get(slot);s.shrink(1);if(s.isEmpty())slots.set(slot,ItemStack.EMPTY);}
  public void putOne(int slot,String item){slots.set(slot,stack(item));}public void growOne(int slot){slots.get(slot).grow(1);}
  public SourceWormCompostRules515.Inventory fork(){return new Snapshot(this);}public void apply(SourceWormCompostRules515.Inventory plan){var other=(Snapshot)plan;for(int i=0;i<slots.size();i++)slots.set(i,other.slots.get(i));}
  void commit(ChestBlockEntity chest){for(int i=0;i<slots.size();i++){var old=chest.getItem(i);var next=slots.get(i);if(ItemStack.matches(old,next))continue;if(!old.isEmpty()&&!next.isEmpty()&&ItemStack.isSameItemSameTags(old,next))old.setCount(next.getCount());else chest.setItem(i,next);}}
 }
 /** true consumes only this block's original random-tick route; other chest subclasses keep their native fallback. */
 public static boolean tick(Object rawState,Object rawLevel,Object rawPos,Object rawRandom){
  var state=(BlockState)rawState;if(!known(state.getBlock()))return false;var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;
  if(!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)||!(chest instanceof SourceChestLoot515 flags)||!flags.mite$lootUnpacked())return true;
  var inv=new Snapshot(chest);var foods=new HashMap<String,SourceWormCompostRules515.Food>();
  for(var s:inv.slots)if(!s.isEmpty())foods.computeIfAbsent(key(s),id->food(s));
  var result=SourceWormCompostRules515.tick(inv,id->{if(!foods.containsKey(id))foods.put(id,food(stack(id)));return foods.get(id);},fraction(chest),((RandomSource)rawRandom)::nextInt);
  if(result.changed()){inv.commit(chest);chest.getPersistentData().putFloat(SAVED,result.compost());chest.setChanged();}
  return true;
 }
 private SourceWormCompost515(){}
}
