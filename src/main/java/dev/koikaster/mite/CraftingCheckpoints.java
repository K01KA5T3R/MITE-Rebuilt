package dev.koikaster.mite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/** A bounded set of per-grid checkpoints so intermediate recipes cannot erase a closing checkpoint. */
public final class CraftingCheckpoints {
 private static final String ROOT="CraftingCheckpoints";
 private static final int LIMIT=32;
 private static final String BEACON_ROUND="IteBeaconRound";
 record TableRound(int progress,boolean beaconRound) {}
 private static final String ORDER="CraftingCheckpointOrder",VERSION="CraftingCheckpointOrderVersion";
 private static java.util.Comparator<String> order(net.minecraft.nbt.CompoundTag root){return java.util.Comparator.comparingLong((String key)->root.getCompound(key).getLong("Touched")).thenComparing(java.util.Comparator.naturalOrder());}
 private static long nextOrder(net.minecraft.nbt.CompoundTag data,net.minecraft.nbt.CompoundTag root){
  long current=data.getLong(ORDER);for(String key:root.getAllKeys())current=Math.max(current,root.getCompound(key).getLong("Touched"));
  if(data.getInt(VERSION)!=1||current<0||current==Long.MAX_VALUE){
   var keys=new java.util.ArrayList<>(root.getAllKeys());keys.sort(order(root));current=0;for(String key:keys)root.getCompound(key).putLong("Touched",++current);data.putInt(VERSION,1);
  }
  data.putLong(ORDER,current+1);return current+1;
 }
 public static String id(String kind,String signature){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((kind+'\n'+signature).getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException error){throw new IllegalStateException(error);}
 }
 public static int restore(Player owner,String kind,String signature,int total){
  var root=PlayerState.data(owner).getCompound(ROOT);return Math.max(0,Math.min(total,root.getCompound(id(kind,signature)).getInt("Progress")));
 }
 static TableRound restoreTable(Player owner,String signature){
  var root=PlayerState.data(owner).getCompound(ROOT);
  return decodeTableRound(root.getCompound(id("table",signature)));
 }
 static TableRound decodeTableRound(CompoundTag entry){
  int progress=Math.max(0,Math.min(Integer.MAX_VALUE-1,entry.getInt("Progress")));
  return new TableRound(progress,progress>0&&entry.contains(BEACON_ROUND,Tag.TAG_BYTE)&&entry.getByte(BEACON_ROUND)==1);
 }
 public static void save(Player owner,String kind,String signature,int progress){
  save(owner,kind,signature,progress,false);
 }
 static void saveTable(Player owner,String signature,int progress,boolean beaconRound){
  save(owner,"table",signature,progress,beaconRound);
 }
 private static void save(Player owner,String kind,String signature,int progress,boolean beaconRound){
  if(owner.level().isClientSide||signature.isEmpty()||progress<=0)return;
  var data=PlayerState.data(owner);var root=data.getCompound(ROOT);String key=id(kind,signature);
  var entry=new net.minecraft.nbt.CompoundTag();entry.putInt("Progress",progress);entry.putLong("Touched",nextOrder(data,root));
  if(beaconRound)entry.putBoolean(BEACON_ROUND,true);
  root.put(key,entry);
  while(root.size()>LIMIT){String oldest=root.getAllKeys().stream().filter(candidate->!candidate.equals(key)).min(order(root)).orElse(null);if(oldest==null)break;root.remove(oldest);}
  data.put(ROOT,root);
 }
 public static void consume(Player owner,String kind,String signature){
  if(owner.level().isClientSide)return;
  var data=PlayerState.data(owner);var root=data.getCompound(ROOT);root.remove(id(kind,signature));data.put(ROOT,root);
 }
 private CraftingCheckpoints(){}
}
