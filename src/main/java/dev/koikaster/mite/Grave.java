package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
public final class Grave {
 public static final class BlockImpl extends BaseEntityBlock {
  public BlockImpl(Properties p){super(p);}
  public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
  public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Entity(p,s);}
  @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.entity.Entity actor){return !protectedAt(level,pos)&&super.canEntityDestroy(state,level,pos,actor);}
  public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
   if(!l.isClientSide&&l.getBlockEntity(pos) instanceof Entity e){
    if(!e.allowed(p)){p.displayClientMessage(Component.translatable("message.mite_reforged.grave_locked"),true);return InteractionResult.FAIL;}
    if(l instanceof net.minecraft.server.level.ServerLevel server&&!GraveRecovery.prepareClaim(server,e)){l.removeBlock(pos,false);return InteractionResult.SUCCESS;}
    if(p instanceof ServerPlayer player){
     GraveRecovery.restoreInventory(player);
     var plan=new GraveClaimPlan207(player,e);
     if(!plan.changed())return InteractionResult.SUCCESS;
     var receipt=GraveRecovery.commitClaim(player,e,plan.afterInventory,plan.remaining);
     if(receipt==null)return InteractionResult.FAIL;
     GraveClaimPlan207.applyInventory(player,plan.afterInventory);
     GraveRecovery.stamp(player,receipt);
     e.contents=plan.remaining;e.setChanged();GraveClaimPlan207.sync(player);
     if(e.contents.stream().allMatch(ItemStack::isEmpty))l.removeBlock(pos,false);
    }
   }
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  public void onRemove(BlockState s,Level l,BlockPos pos,BlockState ns,boolean moving){
   if(!s.is(ns.getBlock())&&l instanceof net.minecraft.server.level.ServerLevel server&&l.getBlockEntity(pos) instanceof Entity e&&!e.contents.stream().allMatch(ItemStack::isEmpty)){GraveRecovery.removed(server,e);e.contents.clear();}
   super.onRemove(s,l,pos,ns,moving);
  }
 }
 public static final class Entity extends BlockEntity {
  UUID owner;
  UUID recoveryToken;
  NonNullList<ItemStack> contents=NonNullList.withSize(54,ItemStack.EMPTY);
  public Entity(BlockPos p,BlockState s){super(Content.GRAVE_ENTITY.get(),p,s);}
  boolean allowed(Player p){return owner==null||owner.equals(p.getUUID())||p.hasPermissions(2);}
  protected void saveAdditional(CompoundTag t){super.saveAdditional(t);if(owner!=null)t.putUUID("Owner",owner);if(recoveryToken!=null)t.putUUID("RecoveryToken",recoveryToken);t.putInt("Size",contents.size());if(contents.size()<=256)ContainerHelper.saveAllItems(t,contents);else t.put("OverflowItems",items(contents));}
  public void load(CompoundTag t){super.load(t);owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;recoveryToken=t.hasUUID("RecoveryToken")?t.getUUID("RecoveryToken"):null;if(t.contains("OverflowItems",Tag.TAG_LIST))contents=contents(t.getList("OverflowItems",Tag.TAG_COMPOUND));else{contents=NonNullList.withSize(Math.max(54,Math.min(256,t.getInt("Size"))),ItemStack.EMPTY);ContainerHelper.loadAllItems(t,contents);}}
 }
 static ListTag items(Collection<ItemStack> stacks){var list=new ListTag();for(var stack:stacks)if(!stack.isEmpty())list.add(stack.save(new CompoundTag()));return list;}
 static NonNullList<ItemStack> contents(ListTag list){var result=NonNullList.withSize(Math.max(54,list.size()),ItemStack.EMPTY);for(int i=0;i<list.size();i++)result.set(i,ItemStack.of(list.getCompound(i)));return result;}
 public static boolean protectedAt(BlockGetter level,BlockPos pos){return level.getBlockEntity(pos) instanceof Entity e&&!e.contents.stream().allMatch(ItemStack::isEmpty);}
 public static boolean store(ServerPlayer player,Collection<ItemEntity> drops){
  if(drops.isEmpty())return false;
  var payload=new ListTag();for(var drop:drops)if(!drop.getItem().isEmpty())payload.add(drop.getItem().copy().save(new CompoundTag()));
  if(payload.isEmpty())return false;
  // Persist ownership and the complete payload before touching the live drops.
  // Clone, logout and player saves retain this queue if placement is impossible.
  var data=PlayerState.data(player);var pending=data.getList("PendingGraves",Tag.TAG_COMPOUND);var entry=new CompoundTag();entry.put("Items",payload);var id=UUID.randomUUID();entry.putUUID("Id",id);pending.add(entry);data.put("PendingGraves",pending);
  if(!GraveRecovery.enqueue(player,id,payload)){pending.remove(pending.size()-1);data.put("PendingGraves",pending);return false;}
  if(place(player,player.serverLevel(),player.blockPosition(),payload,id)){pending.remove(pending.size()-1);data.put("PendingGraves",pending);}
  drops.clear();return true;
 }
 static boolean place(ServerPlayer player,net.minecraft.server.level.ServerLevel l,BlockPos origin,ListTag payload){
  return place(player,l,origin,payload,null);
 }
 static boolean place(ServerPlayer player,net.minecraft.server.level.ServerLevel l,BlockPos origin,ListTag payload,UUID id){
  if(l.isOutsideBuildHeight(origin))return false;
  // Closest first, including replaceable fluids/plants, never another inventory.
  var positions=new ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(origin.offset(-3,-2,-3),origin.offset(3,4,3)))positions.add(p.immutable());
  positions.sort(Comparator.comparingInt(origin::distManhattan));
  BlockPos found=null;
  for(var pos:positions){if(l.isOutsideBuildHeight(pos)||!l.hasChunkAt(pos)||!l.getWorldBorder().isWithinBounds(pos)||l.getBlockEntity(pos)!=null)continue;var state=l.getBlockState(pos);if(state.canBeReplaced()&&!state.hasBlockEntity()){found=pos;break;}}
  if(found==null)return false;var previous=l.getBlockState(found);
  if(!l.setBlock(found,Content.GRAVE.get().defaultBlockState(),3))return false;
  if(!(l.getBlockEntity(found) instanceof Entity grave)){l.setBlock(found,previous,3);return false;}
  grave.owner=player.getUUID();grave.contents=contents(payload);grave.recoveryToken=id;grave.setChanged();
  if(id!=null)GraveRecovery.placed(l,grave);
  var t=PlayerState.data(player);t.putLong("GravePos",found.asLong());t.putString("GraveDimension",l.dimension().location().toString());
  player.sendSystemMessage(Component.translatable("message.mite_reforged.grave",found.getX(),found.getY(),found.getZ()));
  return true;
 }
 static void recover(ServerPlayer player){
  if(!player.isAlive()||player.isSpectator())return;GraveRecovery.restoreInventory(player);GraveRecovery.recover(player);var data=PlayerState.data(player);var pending=data.getList("PendingGraves",Tag.TAG_COMPOUND);
  for(int i=0;i<pending.size();){var entry=pending.getCompound(i);var payload=entry.getList("Items",Tag.TAG_COMPOUND);if(payload.isEmpty()||entry.hasUUID("Id")&&GraveRecovery.known(player.serverLevel(),entry.getUUID("Id"))){pending.remove(i);continue;}var id=entry.hasUUID("Id")?entry.getUUID("Id"):UUID.randomUUID();entry.putUUID("Id",id);if(!GraveRecovery.enqueue(player,id,payload)){i++;continue;}if(place(player,player.serverLevel(),player.blockPosition(),payload,id))pending.remove(i);else i++;}
  if(pending.isEmpty())data.remove("PendingGraves");else data.put("PendingGraves",pending);
 }
 private Grave(){}
}
