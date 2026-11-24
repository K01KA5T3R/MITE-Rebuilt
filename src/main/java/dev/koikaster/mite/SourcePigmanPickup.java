package dev.koikaster.mite;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

/** tn gold/ItemMeat policy and of identity-only containment. Equipment pickup
 * must run first. Source tn is unharmed by fire, so a burning item passes that
 * ss gate; water is not a gate in either the search or consumption path. */
public final class SourcePigmanPickup implements OriginalPickupState.Policy {
 private static final String KEY="R195PigmanContainedItems";
 private static final Set<ResourceLocation> GOLD=goldCatalogue();
 private final Mob actor;private final List<Item> contained=new ArrayList<>();
 public final OriginalPickupState state;
 public SourcePigmanPickup(Mob actor){this.actor=actor;state=new OriginalPickupState(actor,this);}
 private static JsonObject data(String name){try(var in=SourcePigmanPickup.class.getResourceAsStream("/data/mite_reforged/"+name)){return JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException error){throw new ExceptionInInitializerError(error);}}
 private static Set<ResourceLocation> goldCatalogue(){
  var ids=new HashSet<ResourceLocation>();
  var catalogue=data("source_chemical_materials_039.json");
  for(String category:new String[]{"items","blocks"})for(var entry:catalogue.getAsJsonObject(category).entrySet())for(var material:entry.getValue().getAsJsonObject().getAsJsonArray("materials"))if(material.getAsString().equals("gold"))ids.add(new ResourceLocation(entry.getKey()));
  // xy adds gold to fruit; subtype1 is excluded separately below. xx's golden
  // carrot has vegetable material only, despite its recipe/name.
  ids.add(new ResourceLocation("minecraft:golden_apple"));
  ids.add(new ResourceLocation("minecraft:glistering_melon_slice"));
  for(var row:OriginalArmorItems.ROSTER.getAsJsonArray("chains")){var value=row.getAsJsonObject();if(value.get("material").getAsString().equals("gold"))ids.add(Content.id(value.get("name").getAsString()));}
  for(String name:new String[]{"gold_arrow","gold_coin"})ids.add(Content.id(name));
  return Set.copyOf(ids);
 }
 static boolean goldMaterial(ItemStack stack){
  var item=stack.getItem();var id=ForgeRegistries.ITEMS.getKey(item);if(id!=null&&GOLD.contains(id))return true;
  var tool=ClassicToolWear.profile(stack);if(tool!=null&&tool.material().equals("gold"))return true;
  var armor=OriginalArmorItems.profile(stack);if(armor!=null&&armor.material().equals("gold"))return true;
  if(item instanceof OriginalArmorItems.HorseArmor horse&&horse.material.equals("gold"))return true;
  if(ClassicVessels.handled(stack)&&ClassicVessels.material(stack).equals("gold"))return true;
  if((SourceCarrotSticks.handles(stack)||item instanceof FishingRodItem)&&ClassicToolCatalogue.sourceItem(stack)&&ClassicToolCatalogue.material(stack).equals("gold"))return true;
  if(item instanceof BlockItem block){var value=block.getBlock();return value instanceof MetalAnvilBlock anvil&&anvil.material.equals("gold")||value instanceof SourceMetalBuildings.MetalDoor door&&door.material.equals("gold")||value instanceof SourceMetalBuildings.MetalBars bars&&bars.material.equals("gold")||value instanceof SourceMetalBuildings.Strongbox box&&box.material.equals("gold");}
  return false;
 }
 public static boolean valuable(ItemStack stack){
  if(stack.isEmpty())return false;
  return SourcePigmanLife.valuable(new SourcePigmanLife.Valuable(){public int subtype(){return stack.is(Items.ENCHANTED_GOLDEN_APPLE)?1:0;}public int damage(){return stack.getDamageValue();}public boolean gold(){return goldMaterial(stack);}});
 }
 public boolean allowed(ItemEntity item){return !item.hasPickUpDelay()&&ray(item);}
 public boolean ray(ItemEntity item){for(float fraction:new float[]{.25f,.5f,.75f})if(OriginalRaycast.pickupLine(item,item.getBoundingBox().getCenter(),actor.position().add(0,actor.getBbHeight()*fraction,0)))return true;return false;}
 public boolean willEat(ItemStack stack){return OriginalMeatItems.includes(stack)&&!actor.isBaby()&&!actor.isRemoved()&&actor.getHealth()>0;}
 public void ate(ItemStack stack){/* og.onFoodEaten does not heal undead. */}
 public boolean willRepair(ItemStack stack){return false;}public void repaired(ItemStack stack){}
 public boolean willTakeAsValuable(ItemStack stack){return valuable(stack);}
 public void contain(Item item){contained.add(item);}
 public void retainRemainder(ItemEntity item){((SourceItemAge)item).mite$itemAge(-18000);}
 public void tick(){state.tick();}
 public ItemEntity first(float range){if(IteEliteSpawns.isElite(actor))return null;for(var item:actor.level().getEntitiesOfClass(ItemEntity.class,actor.getBoundingBox().inflate(range,range*.25f,range)))if(valuable(item.getItem()))return item;return null;}
 public void drop(){for(var item:contained)actor.spawnAtLocation(item);}
 public int size(){return contained.size();}
 public void save(CompoundTag tag){var list=new ListTag();for(var item:contained)list.add(StringTag.valueOf(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString()));tag.put(KEY,list);}
 public void load(CompoundTag tag){contained.clear();var list=tag.getList(KEY,Tag.TAG_STRING);for(int i=list.size()-1;i>=0;i--){var id=ResourceLocation.tryParse(list.getString(i));if(id==null)continue;var item=ForgeRegistries.ITEMS.getValue(id);if(item!=null&&item!=Items.AIR)contained.add(item);}}
}
