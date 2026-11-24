package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned escrow survives removals while the owner is offline. Restored
 * graves retain their record until claimed, so interrupted replacement is
 * idempotent rather than choosing between a missing item and a second copy. */
final class GraveRecovery extends SavedData {
 private final Map<UUID,CompoundTag> records=new LinkedHashMap<>();
 private final Map<UUID,CompoundTag> inventoryReceipts=new LinkedHashMap<>();
 private static final String RECEIPT="GraveInventoryReceipt207";
 private final Set<UUID> checked=new HashSet<>();
 private final Set<UUID> completed=new HashSet<>();
 private static GraveRecovery get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(GraveRecovery::load,GraveRecovery::new,"mite_grave_recovery");}
 private static GraveRecovery load(CompoundTag tag){var result=new GraveRecovery();for(var raw:tag.getList("Records",Tag.TAG_COMPOUND)){var record=(CompoundTag)raw;if(record.hasUUID("Id"))result.records.put(record.getUUID("Id"),record.copy());}for(var raw:tag.getList("Completed",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;if(row.hasUUID("Id"))result.completed.add(row.getUUID("Id"));}for(var raw:tag.getList("InventoryReceipts",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;if(row.hasUUID("Player")&&row.hasUUID("Id"))result.inventoryReceipts.put(row.getUUID("Player"),row.copy());}return result;}
 @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();for(var record:records.values())list.add(record.copy());tag.put("Records",list);var done=new ListTag();for(var id:completed){var row=new CompoundTag();row.putUUID("Id",id);done.add(row);}tag.put("Completed",done);var receipts=new ListTag();for(var row:inventoryReceipts.values())receipts.add(row.copy());tag.put("InventoryReceipts",receipts);return tag;}
 private static java.nio.file.Path file(ServerLevel level){return level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/mite_grave_recovery.dat").toAbsolutePath().normalize();}
 private void writeAtomic(java.nio.file.Path path)throws java.io.IOException{var envelope=new CompoundTag();envelope.put("data",save(new CompoundTag()));NbtUtils.addCurrentDataVersion(envelope);AtomicGraveFile207.write(path,envelope);setDirty(false);}
 @Override public void save(java.io.File path){if(!isDirty())return;try{writeAtomic(path.toPath());}catch(java.io.IOException error){setDirty();com.mojang.logging.LogUtils.getLogger().error("Could not save grave recovery",error);}}
 private void flush(ServerLevel level){setDirty();try{writeAtomic(file(level));}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}}
 /** The death/claim commit is strict. Later position updates retain that
  * committed payload and must not abort the death/removal callback if the
  * journal is temporarily unavailable. SavedData retries the dirty image. */
 private void flushDeferred(ServerLevel level){try{flush(level);}catch(java.io.UncheckedIOException error){setDirty();com.mojang.logging.LogUtils.getLogger().error("Could not update grave recovery; retaining pending save",error);}}
 private static CompoundTag snapshot(ServerLevel level,Grave.Entity grave){return snapshot(level,grave,grave.recoveryToken);}
 private static CompoundTag snapshot(ServerLevel level,Grave.Entity grave,UUID id){var tag=new CompoundTag();tag.putUUID("Id",id);if(grave.owner!=null)tag.putUUID("Owner",grave.owner);tag.putString("Dimension",level.dimension().location().toString());tag.putLong("Pos",grave.getBlockPos().asLong());tag.put("Items",Grave.items(grave.contents));return tag;}
 static boolean known(ServerLevel level,UUID id){var data=get(level);return data.records.containsKey(id)||data.completed.contains(id);}
 static boolean enqueue(ServerPlayer player,UUID id,ListTag payload){
  var level=player.serverLevel();var data=get(level);var row=new CompoundTag();row.putUUID("Id",id);row.putUUID("Owner",player.getUUID());row.putString("Dimension",level.dimension().location().toString());row.putLong("Pos",player.blockPosition().asLong());row.put("Items",payload.copy());
  var old=data.records.put(id,row);var previous=data.inventoryReceipts.get(player.getUUID());var receipt=receipt(player,player.getInventory().save(new ListTag()),"death");data.inventoryReceipts.put(player.getUUID(),receipt);data.checked.remove(id);
  try{data.flush(level);}catch(java.io.UncheckedIOException error){if(old==null)data.records.remove(id);else data.records.put(id,old);if(previous==null)data.inventoryReceipts.remove(player.getUUID());else data.inventoryReceipts.put(player.getUUID(),previous);com.mojang.logging.LogUtils.getLogger().error("Could not commit grave death",error);return false;}
  stamp(player,receipt.getUUID("Id"));return true;
 }
 private static CompoundTag receipt(ServerPlayer player,ListTag inventory,String kind){var row=new CompoundTag();row.putUUID("Player",player.getUUID());row.putUUID("Id",UUID.randomUUID());row.put("Inventory",inventory.copy());row.putString("Kind",kind);return row;}
 static void stamp(ServerPlayer player,UUID id){PlayerState.data(player).putUUID(RECEIPT,id);}
 static void restoreInventory(ServerPlayer player){var row=get(player.serverLevel()).inventoryReceipts.get(player.getUUID());if(row==null)return;var state=PlayerState.data(player);if(state.hasUUID(RECEIPT)&&row.getUUID("Id").equals(state.getUUID(RECEIPT)))return;GraveClaimPlan207.applyInventory(player,row.getList("Inventory",Tag.TAG_COMPOUND));stamp(player,row.getUUID("Id"));GraveClaimPlan207.sync(player);}
 static UUID commitClaim(ServerPlayer player,Grave.Entity grave,ListTag inventory,java.util.Collection<net.minecraft.world.item.ItemStack> remainder){
  var level=player.serverLevel();var data=get(level);var id=grave.recoveryToken==null?UUID.randomUUID():grave.recoveryToken;
  var old=data.records.get(id);var oldReceipt=data.inventoryReceipts.get(player.getUUID());boolean oldCompleted=data.completed.contains(id),oldChecked=data.checked.contains(id);
  var payload=Grave.items(remainder);var row=snapshot(level,grave,id);row.put("Items",payload);var receipt=receipt(player,inventory,"claim");
  if(payload.isEmpty()){data.records.remove(id);data.completed.add(id);data.checked.remove(id);}else{data.records.put(id,row);data.checked.add(id);}data.inventoryReceipts.put(player.getUUID(),receipt);
  try{data.flush(level);}catch(java.io.UncheckedIOException error){if(old==null)data.records.remove(id);else data.records.put(id,old);if(oldReceipt==null)data.inventoryReceipts.remove(player.getUUID());else data.inventoryReceipts.put(player.getUUID(),oldReceipt);if(!oldCompleted)data.completed.remove(id);if(oldChecked)data.checked.add(id);else data.checked.remove(id);com.mojang.logging.LogUtils.getLogger().error("Could not commit grave claim",error);return null;}
  grave.recoveryToken=id;return receipt.getUUID("Id");
 }
 static void placed(ServerLevel level,Grave.Entity grave){var data=get(level);data.records.put(grave.recoveryToken,snapshot(level,grave));data.checked.add(grave.recoveryToken);data.flushDeferred(level);}
 /** A stale chunk or pending-player image must not reopen a completed claim. */
 static boolean prepareClaim(ServerLevel level,Grave.Entity grave){if(grave.recoveryToken==null)return true;var data=get(level);if(data.completed.contains(grave.recoveryToken)){grave.contents.clear();grave.setChanged();return false;}var row=data.records.get(grave.recoveryToken);if(row!=null){grave.contents=Grave.contents(row.getList("Items",Tag.TAG_COMPOUND));grave.setChanged();}return true;}
 static void removed(ServerLevel level,Grave.Entity grave){var data=get(level);if(grave.recoveryToken==null)grave.recoveryToken=UUID.randomUUID();if(!prepareClaim(level,grave))return;data.records.put(grave.recoveryToken,snapshot(level,grave));data.checked.remove(grave.recoveryToken);data.flushDeferred(level);}
 static void changed(ServerLevel level,Grave.Entity grave){if(grave.recoveryToken==null)return;var data=get(level);if(!data.records.containsKey(grave.recoveryToken))return;if(grave.contents.stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty)){data.records.remove(grave.recoveryToken);data.checked.remove(grave.recoveryToken);data.completed.add(grave.recoveryToken);}else{data.records.put(grave.recoveryToken,snapshot(level,grave));data.checked.add(grave.recoveryToken);}data.flushDeferred(level);}
 private static Grave.Entity existing(ServerLevel level,BlockPos origin,UUID id,CompoundTag record,ListTag payload){
  if(level.getBlockEntity(origin) instanceof Grave.Entity grave&&(id.equals(grave.recoveryToken)||grave.recoveryToken==null&&Objects.equals(grave.owner,record.hasUUID("Owner")?record.getUUID("Owner"):null)&&Grave.items(grave.contents).equals(LegacyFlintDurability749.recoveryPayload(payload))))return grave;
  // A death commit precedes block placement. If its location update did not
  // reach disk, the saved grave can be in another cell of the placement area.
  // Only the exact transaction token may match away from the recorded cell.
  for(var pos:BlockPos.betweenClosed(origin.offset(-3,-2,-3),origin.offset(3,4,3))){
   if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))continue;
   level.getChunkAt(pos);
   if(level.getBlockEntity(pos) instanceof Grave.Entity grave&&id.equals(grave.recoveryToken))return grave;
  }
  return null;
 }
 static void recover(ServerPlayer player){
  var data=get(player.serverLevel());
  for(var entry:new ArrayList<>(data.records.entrySet())){
   var id=entry.getKey();var record=entry.getValue();if(data.checked.contains(id))continue;
   if(record.hasUUID("Owner")&&!record.getUUID("Owner").equals(player.getUUID()))continue;
   var dimension=ResourceLocation.tryParse(record.getString("Dimension"));if(dimension==null)continue;
   var level=player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,dimension));if(level==null)continue;
   var origin=BlockPos.of(record.getLong("Pos"));
   // Old ownerless graves stay public, but are recovered only for someone nearby.
   if(!record.hasUUID("Owner")&&(level!=player.serverLevel()||origin.distSqr(player.blockPosition())>4096))continue;
   level.getChunkAt(origin);var payload=record.getList("Items",Tag.TAG_COMPOUND);
   var existing=existing(level,origin,id,record,payload);
   if(existing!=null){
    existing.recoveryToken=id;existing.contents=Grave.contents(payload);existing.setChanged();data.checked.add(id);
    if(!existing.getBlockPos().equals(origin)){data.records.put(id,snapshot(level,existing));data.flushDeferred(level);}
    continue;
   }
   if(payload.isEmpty()){data.records.remove(id);data.flushDeferred(level);continue;}
   // place already records the actual position and marks this token checked.
   Grave.place(player,player.serverLevel(),player.blockPosition(),payload,id);
  }
 }
}
