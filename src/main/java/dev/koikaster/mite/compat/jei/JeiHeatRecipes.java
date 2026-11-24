package dev.koikaster.mite.compat.jei;

import dev.koikaster.mite.*;
import java.util.*;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;

/** Presentation snapshots from the actual production resolver, never a second recipe table. */
public final class JeiHeatRecipes {
 public record Device(String key,int capacity,boolean large,int mode,int ticks,List<ItemStack> catalysts){
  public RecipeType<View> type(){return RecipeType.create(MiteReforged.ID,"heat/"+key,View.class);}
  public boolean menu(SourceFurnaceMenu menu){return mode>=0&&menu.capacity==capacity&&menu.acceptsLarge==large&&menu.cookingMode==mode;}
 }
 public record View(Device device,ItemStack input,ItemStack output,List<ItemStack> fuels,int minHeat,int maxHeat){}
 public static List<Device> devices(){
  var result=new ArrayList<Device>();
  for(var entry:Content.FURNACES.entrySet()){
   var block=entry.getValue().get();var catalysts=new ArrayList<ItemStack>();catalysts.add(new ItemStack(block));
   if(entry.getKey().equals("stone_furnace"))catalysts.add(new ItemStack(Items.FURNACE));
   result.add(new Device(entry.getKey(),block.capacity,block.acceptsLarge,ModernCooking.CLASSIC,200,List.copyOf(catalysts)));
  }
  if(MiteJeiPlugin.rebuilt()){
   result.add(new Device("smoker",1,true,ModernCooking.SMOKER,100,List.of(new ItemStack(Items.SMOKER))));
   result.add(new Device("blast_furnace",2,true,ModernCooking.BLAST,100,List.of(new ItemStack(Items.BLAST_FURNACE))));
   result.add(new Device("campfire",1,true,-1,ModernCooking.CAMPFIRE_TICKS,List.of(new ItemStack(Items.CAMPFIRE),new ItemStack(Items.SOUL_CAMPFIRE))));
  }
  return List.copyOf(result);
 }
 static boolean hidden(ItemStack stack){return !MiteJeiPlugin.allowed(stack)
  ||LegacyMetalOres.hidden(stack)||stack.getItem() instanceof dev.koikaster.mite.SourceCreatureFragments.FragItem|| ModernContentPolicy.removed(stack)||ClassicMusic.removed(stack)||stack.is(Items.SHIELD)||ModernContentPolicy.onlyRemovedEnchantments(stack);}
 static int fuelHeat(ItemStack stack){return ClassicFuel.resolve(stack,ForgeHooks.getBurnTime(stack,net.minecraft.world.item.crafting.RecipeType.SMELTING)).heat();}
 public static List<ItemStack> variants(){
  var result=new ArrayList<ItemStack>();
  for(var item:ForgeRegistries.ITEMS.getValues())if(item instanceof SpecialItems.MetalBucket)for(String fluid:List.of("water","lava","milk","stone"))result.add(ClassicVessels.peer(new ItemStack(item),fluid));
  for(var block:List.of(SourceRunestoneContent.MITHRIL.get(),SourceRunestoneContent.ADAMANTIUM.get()))for(int rune=0;rune<16;rune++)result.add(SourceRunestoneItem.select(new ItemStack(block),rune));
  return result;
 }
 public static Map<String,List<View>> resolve(Level level){
  var samples=new ArrayList<ItemStack>();var fuels=new ArrayList<ItemStack>();
  for(var item:ForgeRegistries.ITEMS.getValues())if(item!=Items.AIR){var stack=new ItemStack(item);if(!hidden(stack)){samples.add(stack.copyWithCount(Math.min(64,stack.getMaxStackSize())));if(fuelHeat(stack)>0)fuels.add(stack);}}
  for(var stack:variants())if(fuelHeat(stack)>0)fuels.add(stack);
  var cache=new HashMap<String,ClassicFurnaces.Plan>();var result=new LinkedHashMap<String,List<View>>();
  for(var device:devices()){
   var rows=new ArrayList<View>();
   for(var sample:samples){
    if(!device.large()&&ClassicFurnaces.large(sample))continue;
    var groups=new LinkedHashMap<String,Mutable>();int mode=device.mode()<0?ModernCooking.SMOKER:device.mode();
    for(int heat=1;heat<=device.capacity();heat++){
     String cacheKey=ForgeRegistries.ITEMS.getKey(sample.getItem())+":"+device.mode()+":"+heat;
     int actualHeat=heat;
     var plan=cache.computeIfAbsent(cacheKey,k->device.mode()<0?ModernCooking.campfireFood(level,sample):ModernCooking.recipe(level,sample,actualHeat,mode));
     if(plan.output().isEmpty()||hidden(plan.output())||heat<plan.requiredHeat())continue;
     var accepted=new ArrayList<ItemStack>();
     if(device.mode()<0)accepted.add(new ItemStack(Items.CHARCOAL));else for(var fuel:fuels)if(fuelHeat(fuel)==heat&&ClassicFurnaces.fuelAllowed(fuel,device.capacity(),device.large()))accepted.add(fuel.copy());
     if(accepted.isEmpty())continue;
     String key=plan.output().save(new net.minecraft.nbt.CompoundTag()).toString()+":"+plan.consumed();
     var row=groups.computeIfAbsent(key,k->new Mutable(sample.copyWithCount(plan.consumed()),plan.output().copy()));
     row.min=Math.min(row.min,heat);row.max=Math.max(row.max,heat);row.fuels.addAll(accepted);
    }
    for(var row:groups.values())rows.add(new View(device,row.input,row.output,List.copyOf(row.fuels),row.min,row.max));
   }
   result.put(device.key(),List.copyOf(rows));
  }
  return Collections.unmodifiableMap(result);
 }
 private static final class Mutable{
  final ItemStack input,output;final List<ItemStack> fuels=new ArrayList<>();int min=5,max=0;
  Mutable(ItemStack input,ItemStack output){this.input=input;this.output=output;}
 }
 private JeiHeatRecipes(){}
}
