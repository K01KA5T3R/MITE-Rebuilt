package dev.koikaster.mite;

import java.util.UUID;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.Tags;

/** V055/V097 server-side item state. No global deserialization/getter hook:
 * recipe prototypes and immutable grave transactions must remain untouched. */
public final class BucketLifetime336 {
 public static final long FISH_LIFETIME=72000;
 public static final String LIFE="MiteBucketLife336",VESSEL="MiteBucketVessel336";
 private static final String ID="mite_bucket_clock336";
 private static final String[] MOB_DATA={"NoAI","Silent","NoGravity","Glowing","Invulnerable","Health","BucketVariantTag","EntityTag"};
 private static final class WorldIdentity extends SavedData {
  final UUID id;
  WorldIdentity(UUID id){this.id=id;setDirty();}
  static WorldIdentity load(CompoundTag tag){return new WorldIdentity(tag.hasUUID("Id")?tag.getUUID("Id"):UUID.randomUUID());}
  @Override public CompoundTag save(CompoundTag tag){tag.putUUID("Id",id);return tag;}
 }
 public record Result(ItemStack stack,boolean changed,long remaining){}
 private static UUID identity(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(WorldIdentity::load,()->new WorldIdentity(UUID.randomUUID()),ID).id;}
 public static boolean fish(ItemStack stack){var item=stack.getItem();return item==Items.COD_BUCKET||item==Items.SALMON_BUCKET||item==Items.PUFFERFISH_BUCKET||item==Items.TROPICAL_FISH_BUCKET;}
 public static boolean mobBucket(ItemStack stack){return stack.getItem() instanceof MobBucketItem;}
 public static boolean tracked(ItemStack stack){return !stack.isEmpty()&&(fish(stack)||stack.is(Items.POWDER_SNOW_BUCKET));}
 private static final Set<String> TIMED_IDS=Set.of("minecraft:cod_bucket","minecraft:salmon_bucket","minecraft:pufferfish_bucket","minecraft:tropical_fish_bucket","minecraft:powder_snow_bucket");
 private static ListTag rows(CompoundTag serialized){
  String id=serialized.getString("id");if(id.equals("mite_reforged:grave"))return null;var tag=serialized.getCompound("tag");
  if(id.equals("minecraft:bundle"))return tag.contains("Items",Tag.TAG_LIST)?tag.getList("Items",Tag.TAG_COMPOUND):null;
  var block=tag.getCompound("BlockEntityTag");return !block.contains("LootTable",Tag.TAG_STRING)&&block.contains("Items",Tag.TAG_LIST)?block.getList("Items",Tag.TAG_COMPOUND):null;
 }
 private static boolean contains(CompoundTag serialized,int depth){if(depth>32)return false;if(TIMED_IDS.contains(serialized.getString("id")))return true;var rows=rows(serialized);if(rows!=null)for(var value:rows)if(contains((CompoundTag)value,depth+1))return true;return false;}
 public static boolean interesting(ItemStack stack){
  if(tracked(stack))return true;if(stack.isEmpty()||!stack.hasTag())return false;
  if(!(stack.getItem() instanceof BundleItem)&&(!(stack.getItem() instanceof BlockItem block)||block.getBlock()==Content.GRAVE.get()))return false;
  return contains(stack.save(new CompoundTag()),0);
 }
 public static boolean warm(ServerLevel level,BlockPos pos){var biome=level.getBiome(pos);return level.dimensionType().ultraWarm()||biome.is(Tags.Biomes.IS_HOT)||biome.is(BiomeTags.IS_BADLANDS);}
 private static ServerLevel clock(MinecraftServer server,String name){var id=ResourceLocation.tryParse(name);return id==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,id));}
 /** Settle the previous native clock before changing owner dimension. */
 public static Result update(ItemStack input,ServerLevel owner,BlockPos pos){
  if (!owner.getServer().isSameThread() || !WorldContentMode.rebuilt(owner)) return new Result(input,false,-1);
  return update(input,owner,pos,0);
 }
 private static Result update(ItemStack input,ServerLevel owner,BlockPos pos,int depth){
  if(depth>32||!owner.getServer().isSameThread())return new Result(input,false,-1);
  if(!tracked(input))return nested(input,owner,pos,depth);
  if(input.is(Items.POWDER_SNOW_BUCKET))return warm(owner,pos)?new Result(water(input),true,-1):new Result(input,false,-1);
  var server=owner.getServer();if(server.overworld()==null)return new Result(input,false,-1);
  var token=identity(server);var old=input.getTagElement(LIFE);long remaining=FISH_LIFETIME;boolean sameClock=false;
  if(old!=null){
   remaining=Math.min(FISH_LIFETIME,Math.max(0,old.getLong("Remaining")));
   if(old.hasUUID("World")&&old.getUUID("World").equals(token)){
    var previous=clock(server,old.getString("Dimension"));long at=Math.max(0,old.getLong("At"));
    if(previous!=null){long now=Math.max(0,previous.getGameTime());if(now>=at){remaining=Math.max(0,remaining-Math.min(remaining,now-at));sameClock=previous==owner;}}
   }
  }
  if(remaining==0)return new Result(water(input),true,0);
  // The anchor already expresses the entire deadline. Reading it must not
  // rewrite item NBT and broadcast an inventory packet on every game tick.
  if(sameClock)return new Result(input,false,remaining);
  var next=new CompoundTag();next.putInt("Version",1);next.putUUID("World",token);next.putString("Dimension",owner.dimension().location().toString());next.putLong("At",Math.max(0,owner.getGameTime()));next.putLong("Remaining",remaining);
  if(next.equals(old))return new Result(input,false,remaining);
  var result=input.copy();result.addTagElement(LIFE,next);return new Result(result,true,remaining);
 }
 /** Known encoded inventories are inspected without opening loot tables or
  * constructing unrelated item capabilities. Journal-bearing grave items
  * are explicitly excluded, like placed grave inventories. */
 private static Result nested(ItemStack input,ServerLevel owner,BlockPos pos,int depth){
  if(!interesting(input))return new Result(input,false,-1);var original=rows(input.save(new CompoundTag()));if(original==null)return new Result(input,false,-1);
  ItemStack result=input;ListTag output=null;long remaining=Long.MAX_VALUE;
  for(int i=0;i<original.size();i++){var row=original.getCompound(i);if(!contains(row,depth+1))continue;var child=update(ItemStack.of(row.copy()),owner,pos,depth+1);if(child.remaining()>=0)remaining=Math.min(remaining,child.remaining());
   if(child.changed()){
    if(output==null){result=input.copy();output=rows(result.save(new CompoundTag()));}
    var replacement=row.copy();for(var key:new String[]{"id","Count","tag","ForgeCaps"})replacement.remove(key);replacement.merge(child.stack().save(new CompoundTag()));output.set(i,replacement);
   }
  }
  if(output!=null){if(result.getItem() instanceof BundleItem)result.getOrCreateTag().put("Items",output);else result.getOrCreateTagElement("BlockEntityTag").put("Items",output);}
  return new Result(result,output!=null,remaining==Long.MAX_VALUE?-1:remaining);
 }
 /** Called at the actual native filled-result boundary, after fish data is saved. */
 public static ItemStack captured(ItemStack originalWater,ItemStack filled,ServerLevel level,BlockPos pos){
  return update(vessel(originalWater,filled),level,pos).stack();
 }
 public static ItemStack vessel(ItemStack originalWater,ItemStack filled){
  var result=filled.copy();
  if(originalWater.getItem() instanceof SpecialItems.MetalBucket){
   var peer=originalWater.copyWithCount(1);peer.getOrCreateTag().putString("Fluid","water");result.addTagElement(VESSEL,peer.save(new CompoundTag()));
  }
  return result;
 }
 public static ItemStack originalWater(ItemStack filled){
  var saved=filled.getTagElement(VESSEL);if(saved!=null){var source=ItemStack.of(saved.copy());if(source.getItem() instanceof SpecialItems.MetalBucket&&ClassicVessels.fluid(source).equals("water"))return source.copyWithCount(1);}
  return new ItemStack(Items.WATER_BUCKET);
 }
 /** Fish payload is discarded; unrelated item metadata is retained. */
 public static ItemStack water(ItemStack filled){
  var result=originalWater(filled).copyWithCount(filled.getCount());var tag=filled.hasTag()?filled.getTag().copy():new CompoundTag();tag.remove(LIFE);tag.remove(VESSEL);
  if(mobBucket(filled))for(var key:MOB_DATA)tag.remove(key);
  if(result.hasTag()){var restored=result.getTag().copy();restored.merge(tag);tag=restored;}
  if(!tag.isEmpty())result.setTag(tag);return result;
 }
 public static ItemStack empty(ItemStack filled){
  var water=originalWater(filled);var empty=ClassicVessels.peer(water,"");
  if(water.hasCustomHoverName())empty.setHoverName(water.getHoverName());return empty;
 }
 public static String material(ItemStack filled){var saved=filled.getTagElement(VESSEL);if(saved==null)return "";var source=ItemStack.of(saved.copy());if(source.hasTag()&&source.getTag().getBoolean("NativeVessel"))return "";return source.getItem() instanceof SpecialItems.MetalBucket b?b.material:"";}
 public static boolean waterVessel(ItemStack stack){return stack.is(Items.WATER_BUCKET)||stack.getItem() instanceof SpecialItems.MetalBucket&&ClassicVessels.fluid(stack).equals("water");}
 private BucketLifetime336(){}
}
