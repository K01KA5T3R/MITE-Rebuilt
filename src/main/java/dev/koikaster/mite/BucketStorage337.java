package dev.koikaster.mite;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.InvWrapper;

/** Native owner boundaries, not arbitrary ItemStack/list getters. Only
 * inventories that actually contain a timed bucket have future deadlines. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class BucketStorage337 {
 private static final Map<ServerLevel,Index> WORLDS=new WeakHashMap<>();
 private static final class Index {
  final Set<BlockPos> dirty=new LinkedHashSet<>(),mutating=new HashSet<>();
  final Map<BlockPos,Ticket> blocks=new HashMap<>();
  final TreeMap<Long,Set<BlockPos>> due=new TreeMap<>();
  final Map<UUID,WeakReference<Entity>> entities=new HashMap<>();
 }
 private record Ticket(WeakReference<BlockEntity> owner,long due){}
 private record Visit(long remaining,boolean tracked){}
 private static Index index(ServerLevel level){return WORLDS.computeIfAbsent(level,k->new Index());}
 private static boolean unopened(BlockEntity be){return be instanceof RandomizableContainerBlockEntity&&(!(be instanceof BucketStorageAccess337.Loot access)||access.mite$bucketUnopened());}
 private static long later(long now,long remaining){return remaining<0?Long.MAX_VALUE:now+Math.min(Math.max(1,remaining),Long.MAX_VALUE-now);}
 private static void unschedule(Index ix,BlockPos pos){var old=ix.blocks.remove(pos);if(old==null)return;var set=ix.due.get(old.due());if(set!=null){set.remove(pos);if(set.isEmpty())ix.due.remove(old.due());}}
 /** Native LevelChunk registration and BlockEntity.setChanged delegate here. */
 public static void changedBlock(Object raw){if(!(raw instanceof BlockEntity be)||be instanceof Grave.Entity||!(be.getLevel() instanceof ServerLevel level))return;changedPosition(level,be.getBlockPos());}
 public static void changedPosition(Object raw,Object rawPos){if(!(raw instanceof ServerLevel level)||!(rawPos instanceof BlockPos mutable))return;var pos=mutable.immutable();if(!level.getServer().isSameThread()){level.getServer().execute(()->changedPosition(level,pos));return;}if(!WorldContentMode.rebuilt(level))return;var ix=index(level);if(!ix.mutating.contains(pos))ix.dirty.add(pos);}
 private static void drop(ServerLevel level,BlockPos pos,ItemStack stack){while(!stack.isEmpty()){int n=Math.min(stack.getCount(),stack.getMaxStackSize());var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack.split(n));item.setDefaultPickUpDelay();level.addFreshEntity(item);}}
 private static Visit container(ServerLevel level,BlockPos pos,Container inventory){
  long remaining=Long.MAX_VALUE;boolean found=false,changed=false;var overflow=new ArrayList<ItemStack>();
  for(int slot=0;slot<inventory.getContainerSize();slot++){
   var before=inventory.getItem(slot);if(!BucketLifetime336.interesting(before))continue;found=true;var result=BucketLifetime336.update(before,level,pos);if(result.remaining()>=0)remaining=Math.min(remaining,result.remaining());else if(BucketLifetime336.fish(before))remaining=Math.min(remaining,1);
   if(result.changed()){
    var next=result.stack();int limit=Math.max(1,Math.min(inventory.getMaxStackSize(),next.getMaxStackSize()));if(next.getCount()>limit){var extra=next.copyWithCount(next.getCount()-limit);next=next.copyWithCount(limit);overflow.add(extra);}
    inventory.setItem(slot,next);changed=true;
   }
  }
  for(var extra:overflow){var rest=ItemHandlerHelper.insertItemStacked(new InvWrapper(inventory),extra,false);if(!rest.isEmpty())drop(level,pos,rest);}
  if(changed)inventory.setChanged();return new Visit(remaining==Long.MAX_VALUE?-1:remaining,found);
 }
 private static Visit handler(ServerLevel level,BlockPos pos,IItemHandlerModifiable inventory){
  long remaining=Long.MAX_VALUE;boolean found=false;var overflow=new ArrayList<ItemStack>();
  for(int slot=0;slot<inventory.getSlots();slot++){var old=inventory.getStackInSlot(slot);if(!BucketLifetime336.interesting(old))continue;found=true;var result=BucketLifetime336.update(old,level,pos);if(result.remaining()>=0)remaining=Math.min(remaining,result.remaining());else if(BucketLifetime336.fish(old))remaining=Math.min(remaining,1);if(result.changed()){var next=result.stack();int limit=Math.max(1,Math.min(inventory.getSlotLimit(slot),next.getMaxStackSize()));if(next.getCount()>limit){overflow.add(next.copyWithCount(next.getCount()-limit));next=next.copyWithCount(limit);}inventory.setStackInSlot(slot,next);}}
  for(var extra:overflow){var rest=ItemHandlerHelper.insertItemStacked(inventory,extra,false);if(!rest.isEmpty())drop(level,pos,rest);}return new Visit(remaining==Long.MAX_VALUE?-1:remaining,found);
 }
 private static Visit visitBlock(ServerLevel level,BlockEntity be){
  if(be instanceof Grave.Entity||unopened(be))return new Visit(-1,false);var pos=be.getBlockPos();
  if(be instanceof Container c)return container(level,pos,c);
  if(be instanceof CampfireBlockEntity fire){long left=Long.MAX_VALUE;boolean found=false,changed=false;var items=fire.getItems();for(int i=0;i<items.size();i++){var before=items.get(i);if(!BucketLifetime336.interesting(before))continue;found=true;var result=BucketLifetime336.update(before,level,pos);if(result.remaining()>=0)left=Math.min(left,result.remaining());if(result.changed()){var next=result.stack();if(next.getCount()>next.getMaxStackSize()){var rest=next.copyWithCount(next.getCount()-next.getMaxStackSize());next=next.copyWithCount(next.getMaxStackSize());drop(level,pos,rest);}items.set(i,next);changed=true;}}if(changed)fire.setChanged();return new Visit(left==Long.MAX_VALUE?-1:left,found);}
  var cap=be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);return cap instanceof IItemHandlerModifiable editable?handler(level,pos,editable):new Visit(-1,false);
 }
 private static void block(ServerLevel level,Index ix,BlockPos pos){
  unschedule(ix,pos);var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)return;var be=chunk.getBlockEntities().get(pos);if(be==null||be.isRemoved()||be.getLevel()!=level||be instanceof Grave.Entity)return;
  ix.mutating.add(pos);Visit result;try{result=visitBlock(level,be);}finally{ix.mutating.remove(pos);}
  if(!result.tracked())return;long due=later(Math.max(0,level.getGameTime()),result.remaining());ix.blocks.put(pos,new Ticket(new WeakReference<>(be),due));ix.due.computeIfAbsent(due,k->new HashSet<>()).add(pos);
 }
 private static boolean equipment(LivingEntity entity){for(var slot:EquipmentSlot.values())if(BucketLifetime336.interesting(entity.getItemBySlot(slot)))return true;return false;}
 public static void changedEntity(Object raw){if(!(raw instanceof Entity entity)||!(entity.level() instanceof ServerLevel level)||entity instanceof ServerPlayer)return;if(!level.getServer().isSameThread()){var weak=new WeakReference<>(entity);level.getServer().execute(()->{var e=weak.get();if(e!=null)changedEntity(e);});return;}if(!WorldContentMode.rebuilt(level))return;if(entity instanceof ItemEntity item){dropped(item);return;}if(entity instanceof ContainerEntity||entity instanceof InventoryCarrier||entity instanceof AbstractHorse||entity instanceof ItemFrame||entity instanceof BucketStorageAccess337.Display||entity instanceof LivingEntity living&&equipment(living))index(level).entities.put(entity.getUUID(),new WeakReference<>(entity));}
 private static void entity(ServerLevel level,Entity entity){
  var pos=entity.blockPosition();
  if(entity instanceof ContainerEntity c){if(c.getLootTable()==null)container(level,pos,c);}
  else if(entity instanceof InventoryCarrier c)container(level,pos,c.getInventory());
  else if(entity instanceof BucketStorageAccess337.Horse h)container(level,pos,h.mite$bucketInventory());
  else{var cap=entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);if(cap instanceof IItemHandlerModifiable editable)handler(level,pos,editable);}
  if(entity instanceof ItemFrame frame){var before=frame.getItem();if(BucketLifetime336.interesting(before)){var r=BucketLifetime336.update(before,level,pos);if(r.changed())frame.setItem(r.stack());}}
  if(entity instanceof BucketStorageAccess337.Display display){var before=display.mite$bucketDisplay();if(BucketLifetime336.interesting(before)){var r=BucketLifetime336.update(before,level,pos);if(r.changed())display.mite$bucketDisplay(r.stack());}}
  if(entity instanceof LivingEntity living)for(var slot:EquipmentSlot.values()){var before=living.getItemBySlot(slot);if(BucketLifetime336.interesting(before)){var r=BucketLifetime336.update(before,level,pos);if(r.changed()){var next=r.stack();if(next.getCount()>next.getMaxStackSize()){drop(level,pos,next.copyWithCount(next.getCount()-next.getMaxStackSize()));next=next.copyWithCount(next.getMaxStackSize());}living.setItemSlot(slot,next);}}}
 }
 /** Called before the existing dropped-vessel environment conversion. */
 public static void dropped(Object raw){if(!(raw instanceof ItemEntity item)||!(item.level() instanceof ServerLevel level)||!WorldContentMode.rebuilt(level))return;var before=item.getItem();if(!BucketLifetime336.interesting(before))return;var r=BucketLifetime336.update(before,level,item.blockPosition());if(r.changed()){var next=r.stack();if(next.getCount()>next.getMaxStackSize()){drop(level,item.blockPosition(),next.copyWithCount(next.getCount()-next.getMaxStackSize()));next=next.copyWithCount(next.getMaxStackSize());}item.setItem(next);}}
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void player(TickEvent.PlayerTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;var level=player.serverLevel();if(!WorldContentMode.rebuilt(level))return;var pos=player.blockPosition();container(level,pos,player.getInventory());container(level,pos,player.getEnderChestInventory());var seen=Collections.newSetFromMap(new IdentityHashMap<Container,Boolean>());for(var slot:player.containerMenu.slots)if(slot.container instanceof CraftingContainer&&seen.add(slot.container))container(level,pos,slot.container);var carried=player.containerMenu.getCarried();if(BucketLifetime336.interesting(carried)){var r=BucketLifetime336.update(carried,level,pos);if(r.changed()){var next=r.stack();if(next.getCount()>next.getMaxStackSize()){drop(level,pos,next.copyWithCount(next.getCount()-next.getMaxStackSize()));next=next.copyWithCount(next.getMaxStackSize());}player.containerMenu.setCarried(next);}}}
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)public static void join(EntityJoinLevelEvent event){if(!event.isCanceled())changedEntity(event.getEntity());}
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void equipment(LivingEquipmentChangeEvent event){if(BucketLifetime336.interesting(event.getFrom())||BucketLifetime336.interesting(event.getTo()))changedEntity(event.getEntity());}
 @SubscribeEvent public static void unloaded(ChunkEvent.Unload event){if(!(event.getLevel() instanceof ServerLevel level))return;long key=event.getChunk().getPos().toLong();if(!level.getServer().isSameThread()){level.getServer().execute(()->unload(level,key));return;}unload(level,key);}
 private static void unload(ServerLevel level,long key){var ix=WORLDS.get(level);if(ix==null)return;for(var pos:List.copyOf(ix.blocks.keySet()))if(net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4)==key)unschedule(ix,pos);ix.dirty.removeIf(pos->net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4)==key);}
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void tick(TickEvent.LevelTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;if(!WorldContentMode.rebuilt(level)){WORLDS.remove(level);return;}var ix=WORLDS.get(level);if(ix==null)return;long now=Math.max(0,level.getGameTime());while(!ix.due.isEmpty()&&ix.due.firstKey()<=now){var due=ix.due.pollFirstEntry();ix.dirty.addAll(due.getValue());}var positions=List.copyOf(ix.dirty);ix.dirty.clear();for(var pos:positions)block(level,ix,pos);
  for(var entry:List.copyOf(ix.entities.entrySet())){var e=entry.getValue().get();if(e==null||e.isRemoved()||e.level()!=level){ix.entities.remove(entry.getKey());continue;}entity(level,e);if(e instanceof LivingEntity living&&!(e instanceof InventoryCarrier)&&!(e instanceof AbstractHorse)&&!equipment(living))ix.entities.remove(entry.getKey());}
 }
 @SubscribeEvent public static void tags(net.minecraftforge.event.TagsUpdatedEvent event){if(!event.shouldUpdateStaticData())return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server!=null)server.execute(()->{for(var entry:WORLDS.entrySet())entry.getValue().dirty.addAll(entry.getValue().blocks.keySet());});}
 private BucketStorage337(){}
}
