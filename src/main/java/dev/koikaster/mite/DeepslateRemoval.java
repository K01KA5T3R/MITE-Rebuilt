package dev.koikaster.mite;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Human063: replace all deepslate with ordinary stone counterparts. Native
 * identities remain available only as migration inputs. No registry deletion,
 * ore retrogen or player-facing migration/debug text is introduced. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class DeepslateRemoval {
 private static final Map<String,String> IDS=ids();
 private static final Map<Block,Block> BLOCKS=new ConcurrentHashMap<>();
 private static Map<String,String> ids(){
  var map=new HashMap<String,String>();
  map.put("deepslate","stone");map.put("cobbled_deepslate","cobblestone");map.put("polished_deepslate","stone");map.put("chiseled_deepslate","chiseled_stone_bricks");map.put("reinforced_deepslate","stone");map.put("infested_deepslate","infested_stone");
  map.put("deepslate_bricks","stone_bricks");map.put("cracked_deepslate_bricks","cracked_stone_bricks");map.put("deepslate_tiles","stone_bricks");map.put("cracked_deepslate_tiles","cracked_stone_bricks");
  for(String shape:new String[]{"stairs","slab","wall"}){map.put("cobbled_deepslate_"+shape,"cobblestone_"+shape);map.put("polished_deepslate_"+shape,"stone_brick_"+shape);map.put("deepslate_brick_"+shape,"stone_brick_"+shape);map.put("deepslate_tile_"+shape,"stone_brick_"+shape);}
  for(String ore:new String[]{"coal","iron","copper","gold","redstone","lapis","diamond","emerald"})map.put("deepslate_"+ore+"_ore",ore+"_ore");
  return Map.copyOf(map);
 }
 public static ResourceLocation target(ResourceLocation id){
  if(id==null)return null;
  if(id.getNamespace().equals("minecraft")){var value=IDS.get(id.getPath());return value==null?null:new ResourceLocation("minecraft",value);}
  if(id.getNamespace().equals(MiteReforged.ID)&&id.getPath().startsWith("deepslate_")&&id.getPath().endsWith("_ore"))return new ResourceLocation(id.getNamespace(),id.getPath().substring(10));
  return null;
 }
 public static boolean removed(ResourceLocation id){return target(id)!=null;}
 private static Block block(Block original){
  if(DeepDarkRemoval.replaces(original))return Blocks.STONE;
  var known=BLOCKS.get(original);if(known!=null)return known;if(ForgeRegistries.BLOCKS==null)return original;
  var id=ForgeRegistries.BLOCKS.getKey(original);if(id==null)return original;var target=target(id);if(target==null){BLOCKS.put(original,original);return original;}
  if(!ForgeRegistries.BLOCKS.containsKey(target))return original;var value=ForgeRegistries.BLOCKS.getValue(target);if(value==null)return original;BLOCKS.put(original,value);return value;
 }
 private static <T extends Comparable<T>> String name(BlockState state,Property<T> property){return property.getName(state.getValue(property));}
 private static <T extends Comparable<T>> BlockState value(BlockState state,Property<T> property,String name){return property.getValue(name).map(v->state.setValue(property,v)).orElse(state);}
 public static BlockState state(Object raw){
  var original=(BlockState)raw;var target=block(original.getBlock());if(target==original.getBlock())return original;var result=target.defaultBlockState();
  for(var property:original.getProperties()){var other=target.getStateDefinition().getProperty(property.getName());if(other!=null)result=value(result,other,name(original,property));}return result;
 }
 /** Preserve count, custom/Forge NBT and the caller's original input object. */
 public static CompoundTag stackTag(Object raw){var original=(CompoundTag)raw;var id=ResourceLocation.tryParse(original.getString("id"));var target=target(id);if(target==null)return original;var copy=original.copy();copy.putString("id",target.toString());return copy;}
 public static ItemStack stack(ItemStack original){if(original.isEmpty()||!removed(ForgeRegistries.ITEMS.getKey(original.getItem())))return original;return ItemStack.of(stackTag(original.save(new CompoundTag())));}
 /** Keep hidden native stack identities while creative/recipe registries are
  * built: rewriting the constructor would create duplicate creative entries.
  * Convert at actual acquisition/storage boundaries before removal policies. */
 public static void joined(Entity entity){
  if(entity instanceof ItemEntity item)item.setItem(stack(item.getItem()));
  if(entity instanceof LivingEntity living)for(var slot:EquipmentSlot.values()){var old=living.getItemBySlot(slot);var next=stack(old);if(next!=old)living.setItemSlot(slot,next);}
  // Inventory access unpacks native loot. Defer conversion until the first real access.
  if(entity instanceof Container container&&(!(entity instanceof ContainerEntity loot)||loot.getLootTable()==null)){
   for(int i=0;i<container.getContainerSize();i++){
    var old=container.getItem(i);var next=stack(old);if(next!=old)container.setItem(i,next);
   }
  }
 }
 public static void inventory(Player player){
  var inventory=player.getInventory();boolean changed=false;
  for(int i=0;i<inventory.getContainerSize();i++){var old=inventory.getItem(i);var next=stack(old);if(next!=old){inventory.setItem(i,next);changed=true;}}
  for(var slot:player.containerMenu.slots)if(slot.container!=inventory){var old=slot.getItem();var next=stack(old);if(next!=old){slot.set(next);changed=true;}}
  var old=player.containerMenu.getCarried();var next=stack(old);if(next!=old){player.containerMenu.setCarried(next);changed=true;}
  if(changed){inventory.setChanged();player.containerMenu.broadcastChanges();}
 }
 @SubscribeEvent public static void opened(PlayerContainerEvent.Open event){if(!event.getEntity().level().isClientSide)inventory(event.getEntity());}
 /** Forge emits this on the actual chunk before making its read-only imposter.
  * Migrate the decoded palette, not NBT palette IDs: multiple old states may
  * collapse to one state without corrupting packed index widths. */
 @SubscribeEvent public static void loaded(ChunkDataEvent.Load event){
  var chunk=event.getChunk();boolean changed=false, deepDarkBlocksChanged=false;
  var sections=chunk.getSections();
  for(int index=0;index<sections.length;index++){
   var section=sections[index];
   if(section==null||!section.maybeHas(s->block(s.getBlock())!=s.getBlock()))continue;
   int bottom=chunk.getSectionYFromSectionIndex(index)<<4;
   for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
    var old=section.getBlockState(x,y,z);var next=state(old);
    if(next==old)continue;
    section.setBlockState(x,y,z,next,false);changed=true;
    if(DeepDarkRemoval.replaces(old.getBlock())){
     for(var map:chunk.getHeightmaps())map.getValue().update(x,bottom+y,z,next);
     deepDarkBlocksChanged=true;
    }
   }
  }
  if(deepDarkBlocksChanged){chunk.initializeLightSources();chunk.setLightCorrect(false);}
  changed|=DeepDarkRemoval.removeOldBlockEntities(chunk,event.getData());
  changed|=DeepDarkRemoval.migrateBiomes(chunk);
  if(changed)chunk.setUnsaved(true);
 }
 private DeepslateRemoval(){}
}
