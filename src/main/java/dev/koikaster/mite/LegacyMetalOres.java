package dev.koikaster.mite;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** The old ore-block -> ingot route. Raw identities exist only for native/save compatibility. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class LegacyMetalOres {
 private static final Map<String,String> RAW=Map.of(
  "minecraft:raw_copper","minecraft:copper_ore","minecraft:raw_iron","minecraft:iron_ore","minecraft:raw_gold","minecraft:gold_ore",
  "mite_reforged:raw_tin","mite_reforged:tin_ore","mite_reforged:raw_silver","mite_reforged:silver_ore",
  "mite_reforged:raw_mithril","mite_reforged:mithril_ore","mite_reforged:raw_adamantium","mite_reforged:adamantium_ore");
 private static final Map<String,String> BLOCKS=Map.of("minecraft:raw_copper_block","minecraft:copper_ore","minecraft:raw_iron_block","minecraft:iron_ore","minecraft:raw_gold_block","minecraft:gold_ore");
 public static boolean hiddenId(ResourceLocation id){return id!=null&&(RAW.containsKey(id.toString())||BLOCKS.containsKey(id.toString()));}
 public static boolean hidden(ItemStack stack){return !stack.isEmpty()&&hiddenId(ForgeRegistries.ITEMS.getKey(stack.getItem()));}
 /** Copy the full native tag, changing only the identity; count and Forge/custom NBT remain intact. */
 public static CompoundTag migrate(Object raw){if(raw==null)return null;var tag=(CompoundTag)raw;String target=RAW.get(tag.getString("id"));if(target==null)return tag;var copy=tag.copy();copy.putString("id",target);return copy;}
 @SubscribeEvent public static void missing(MissingMappingsEvent event){
  for(var mapping:event.getMappings(ForgeRegistries.Keys.ITEMS,MiteReforged.ID)){String target=RAW.get(mapping.getKey().toString());if(target!=null)mapping.remap(ForgeRegistries.ITEMS.getValue(new ResourceLocation(target)));}
 }
 /** Old raw storage blocks remain readable; mining them recovers all nine ore units per block. */
 public static void loot(List<ItemStack> loot){
  for(int index=0;index<loot.size();index++){
   var stack=loot.get(index);var id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null)continue;String target=RAW.get(id.toString());int multiplier=1;
   if(target==null){target=BLOCKS.get(id.toString());multiplier=9;}if(target==null)continue;
   int count=Math.multiplyExact(stack.getCount(),multiplier);var ore=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(target)));if(stack.hasTag())ore.setTag(stack.getTag().copy());
   var replacements=new ArrayList<ItemStack>();while(count>0){int take=Math.min(count,ore.getMaxStackSize());replacements.add(ore.copyWithCount(take));count-=take;}
   loot.remove(index);loot.addAll(index,replacements);index+=replacements.size()-1;
  }
 }
 private LegacyMetalOres(){}
}
