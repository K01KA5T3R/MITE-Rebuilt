package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** afp's Underworld table and mk.generateChestContents. The human music rule
 * removes its four custom records. Native slots intentionally overwrite rather
 * than merge; eight weighted draws do not promise eight occupied slots. */
final class SourceDungeonLoot {
 record Entry(ItemStack item,int minimum,int maximum,int weight){}
 private static Entry entry(String id,int low,int high,int weight){
  var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id));
  if(item==null||item==Items.AIR)throw new IllegalStateException("Missing original dungeon item: "+id);
  return new Entry(new ItemStack(item),low,high,weight);
 }
 static List<Entry> table(RandomSource random){
  var entries=new ArrayList<Entry>();
  entries.add(entry("mite_reforged:ancient_metal_nugget",1,4,10));
  entries.add(entry("mite_reforged:ancient_metal_ingot",1,4,10));
  entries.add(entry("minecraft:gunpowder",1,4,10));
  entries.add(entry("minecraft:string",1,4,10));
  entries.add(entry("minecraft:bowl",1,1,3));
  entries.add(entry("mite_reforged:ancient_metal_bucket",1,1,2));
  entries.add(entry("mite_reforged:ancient_metal_coin",1,1,1));
  entries.add(entry("minecraft:name_tag",1,1,2));
  entries.add(entry("mite_reforged:ancient_metal_horse_armor",1,1,1));
  for(String name:new String[]{"fishing_rod","pickaxe","shovel","dagger","sword","bow","chainmail_boots","chainmail_leggings","chainmail_chestplate","chainmail_helmet"})entries.add(entry("mite_reforged:ancient_metal_"+name,1,1,2));
  // xo.b chooses an original enchantment uniformly, then a level uniformly.
  var definitions=ClassicEnchanting.entries(false);
  var chosen=definitions.get(random.nextInt(definitions.size()));
  var book=new ItemStack(Items.ENCHANTED_BOOK);
  EnchantedBookItem.addEnchantment(book,new EnchantmentInstance(chosen.enchantment(),1+random.nextInt(chosen.definition().levels())));
  entries.add(new Entry(book,1,1,1));
  return entries;
 }
 static void fill(Container inventory,RandomSource random){
  var entries=table(random);int total=entries.stream().mapToInt(Entry::weight).sum();
  for(int draw=0;draw<8;draw++){
   int weight=random.nextInt(total);Entry selected=null;
   for(var entry:entries)if((weight-=entry.weight())<0){selected=entry;break;}
   if(selected==null)throw new IllegalStateException("Empty original dungeon lottery");
   int count=selected.minimum()+random.nextInt(selected.maximum()-selected.minimum()+1);
   int maximum=selected.item().getMaxStackSize();
   if(maximum>=count){var stack=selected.item().copyWithCount(count);damageForChest(stack);inventory.setItem(random.nextInt(inventory.getContainerSize()),stack);}
   else for(int i=0;i<count;i++){var stack=selected.item().copyWithCount(1);damageForChest(stack);inventory.setItem(random.nextInt(inventory.getContainerSize()),stack);}
  }
  inventory.setChanged();
 }
 /** ye.applyRandomItemStackDamageForChest uses a separate Math.random source. */
 private static void damageForChest(ItemStack stack){if(stack.isDamageableItem())stack.setDamageValue(Math.min(stack.getMaxDamage()-1,stack.getMaxDamage()*(int)(Math.random()*7)/10));}
 private SourceDungeonLoot(){}
}
