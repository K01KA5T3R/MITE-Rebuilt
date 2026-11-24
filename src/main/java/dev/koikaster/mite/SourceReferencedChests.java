package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;

/** R195 ahl/ahm/aia tables and mk's artifact branch at generation time, not first opening. */
public final class SourceReferencedChests {
 private static final Map<Object,float[]> CHANCES=Collections.synchronizedMap(new WeakHashMap<>());
 record Entry(ItemStack item,int low,int high,int weight){}
 public static void begin(Object piece,int kind){CHANCES.put(piece,new float[]{kind==0?.1f:kind==1?.25f:.5f});}
 static Entry entry(Item item,int low,int high,int weight){return new Entry(new ItemStack(item),low,high,weight);}
 static List<Entry> table(RandomSource random,int kind){
  var rows=new ArrayList<Entry>();
  if(kind==2){rows.add(entry(Items.BOOK,1,3,20));rows.add(entry(Items.PAPER,2,7,20));rows.add(entry(Items.MAP,1,1,1));rows.add(entry(Items.COMPASS,1,1,1));}
  else{
   rows.add(entry(Content.DIAMOND_SHARD.get(),1,3,3));rows.add(entry(Items.GOLD_NUGGET,2,7,15));rows.add(entry(Content.EMERALD_SHARD.get(),1,3,2));rows.add(entry(Items.BONE,4,6,20));rows.add(entry(Items.ROTTEN_FLESH,3,7,16));rows.add(entry(Items.SADDLE,1,1,3));
   if(kind==0){rows.add(entry(Items.BOWL,1,1,3));rows.add(entry(Items.PAPER,1,1,1));rows.add(entry(Items.FLOWER_POT,1,1,1));}
   rows.add(entry(Items.IRON_HORSE_ARMOR,1,1,1));rows.add(entry(Content.get("silver_horse_armor"),1,1,1));rows.add(entry(Items.GOLDEN_HORSE_ARMOR,1,1,1));
  }
  var definitions=ClassicEnchanting.entries(false);var selected=definitions.get(random.nextInt(definitions.size()));var book=new ItemStack(Items.ENCHANTED_BOOK);EnchantedBookItem.addEnchantment(book,new EnchantmentInstance(selected.enchantment(),1+random.nextInt(selected.definition().levels())));rows.add(new Entry(book,1,kind==2?5:1,kind==2?2:1));return rows;
 }
 static ItemStack tryArtifact(SourceBookWorld world,RandomSource random,int day,float[] chances){
  if(chances==null)return ItemStack.EMPTY;int type=-1;for(int i=0;i<chances.length;i++)if(random.nextFloat()<chances[i]){type=i;break;}
  if(type<0)return ItemStack.EMPTY;ItemStack stack=ItemStack.EMPTY;
  if(type==0&&day>=40&&world.bookcase)for(int attempt=0;attempt<10;attempt++){int index=random.nextInt(9)+1;if(world.contains(index))continue;stack=SourceReferencedBooks.generate(index);break;}
  if(stack.isEmpty())return stack;Arrays.fill(chances,0);return stack;
 }
 static void fill(Container inventory,RandomSource random,List<Entry> rows,int draws,SourceBookWorld ledger,int day,float[] chances){
  synchronized(ledger){
   ItemStack unique=draws>0?tryArtifact(ledger,random,day,chances):ItemStack.EMPTY;if(!unique.isEmpty())draws--;
   int total=rows.stream().mapToInt(Entry::weight).sum();
   for(int draw=0;draw<draws;draw++){
    int weight=random.nextInt(total);Entry selected=null;for(var row:rows)if((weight-=row.weight())<0){selected=row;break;}if(selected==null)throw new IllegalStateException("Empty original reference chest lottery");
    int count=selected.low()+random.nextInt(selected.high()-selected.low()+1);int maximum=selected.item().getMaxStackSize();
    if(maximum>=count)put(inventory,random,selected.item().copyWithCount(count));else for(int i=0;i<count;i++)put(inventory,random,selected.item().copyWithCount(1));
   }
   if(!unique.isEmpty()){ledger.add(SourceReferencedBooks.index(unique));inventory.setItem(random.nextInt(inventory.getContainerSize()),unique.copy());}inventory.setChanged();
  }
 }
 static void put(Container inventory,RandomSource random,ItemStack stack){if(stack.isDamageableItem())stack.setDamageValue(Math.min(stack.getMaxDamage()-1,stack.getMaxDamage()*(int)(Math.random()*7)/10));inventory.setItem(random.nextInt(inventory.getContainerSize()),stack);}
 /** Native structure geometry retained. The original eligible chests own their actual source tables. */
 public static boolean create(Object piece,Object rawWorld,Object rawBox,Object rawRandom,Object rawPos,int kind,int x,int y,int z){
  var world=(WorldGenLevel)rawWorld;var box=(BoundingBox)rawBox;var random=(RandomSource)rawRandom;var pos=(BlockPos)rawPos;
  if(!box.isInside(pos)||world.getBlockState(pos).is(Blocks.CHEST))return false;
  world.setBlock(pos,StructurePiece.reorient(world,pos,Blocks.CHEST.defaultBlockState()),2);
  if(world.getBlockEntity(pos) instanceof Container inventory){var rows=table(random,kind);int draws=(kind==2?1:2)+random.nextInt(kind==2?4:5);var ledger=SourceBookWorld.get(world.getLevel());boolean eligible=kind!=1||x==9&&y==-3&&z==10;float[] chances=eligible?CHANCES.get(piece):null;int day=(int)((world.getLevel().getDayTime()+6000)/24000)+1;fill(inventory,random,rows,draws,ledger,day,chances);}
  return true;
 }
 /** Discard from damage/expiry releases a signature; pickup has already emptied the stack. */
 public static void removed(Object raw,Object reason){if(!(raw instanceof net.minecraft.world.entity.item.ItemEntity entity)||entity.level().isClientSide||reason!=net.minecraft.world.entity.Entity.RemovalReason.DISCARDED&&reason!=net.minecraft.world.entity.Entity.RemovalReason.KILLED)return;int index=SourceReferencedBooks.index(entity.getItem());if(index>0)SourceBookWorld.get((ServerLevel)entity.level()).release(index);}
 private SourceReferencedChests(){}
}
