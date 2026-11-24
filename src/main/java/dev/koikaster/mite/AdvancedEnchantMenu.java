package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;

/** One owner, one input, a versioned quote and a single main-thread commit. */
public final class AdvancedEnchantMenu extends AbstractContainerMenu {
 public final BlockPos pos;public final SimpleContainer input;private final Player owner;
 private final long session=UUID.randomUUID().getMostSignificantBits();private int revision;private boolean closing,committing;
 private ItemStack observed=ItemStack.EMPTY;private int observedXp=Integer.MIN_VALUE,observedShelves=-1;private AdvancedEnchantConfig.Policy observedPolicy;
 private ResourceLocation selected;private int rank;private List<AdvancedEnchanting.Row> rows=List.of();private AdvancedEnchanting.Quote quote=new AdvancedEnchanting.Quote(0,0,0,AdvancedEnchanting.Reason.EMPTY);
 public AdvancedEnchantNetwork.Snapshot view;
 public AdvancedEnchantMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,buffer.readBlockPos());}
 public AdvancedEnchantMenu(int id,Inventory inventory,BlockPos pos){super(AdvancedEnchantContent.MENU.get(),id);this.pos=pos.immutable();owner=inventory.player;
  input=new SimpleContainer(1){@Override public int getMaxStackSize(){return 1;}@Override public void setChanged(){super.setChanged();AdvancedEnchantMenu.this.slotsChanged(this);}};
  addSlot(new Slot(input,0,25,70){@Override public int getMaxStackSize(){return 1;}@Override public boolean mayPlace(ItemStack stack){return !stack.isEmpty()&&stack.getMaxStackSize()==1&&stack.isDamageableItem()&&WorldContentEvents.itemActionAllowed(owner.level(),stack);}});
  for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,61+x*18,154+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,61+x*18,212));
 }
 public long session(){return session;}
 public int revision(){return revision;}
 public List<AdvancedEnchanting.Row> rows(){return rows;}
 public AdvancedEnchanting.Quote quote(){return quote;}
 private int shelves(){return Math.min(15,OriginalEnchantingTables.bookshelves(owner.level(),pos));}
 @Override public void slotsChanged(Container container){if(container==input&&!committing&&!closing&&!owner.level().isClientSide)refresh(false);}
 private void refresh(boolean force){
  if(!(owner instanceof ServerPlayer server)||closing||committing)return;
  var stack=input.getItem(0);int xp=PlayerState.data(owner).getInt("Xp"),shelves=shelves();var policy=AdvancedEnchantConfig.policy();
  if(!force&&ItemStack.matches(observed,stack)&&xp==observedXp&&shelves==observedShelves&&policy.equals(observedPolicy))return;
  boolean changedItem=!ItemStack.matches(observed,stack);observed=stack.copy();observedXp=xp;observedShelves=shelves;observedPolicy=policy;
  rows=WorldContentEvents.itemActionAllowed(owner.level(),stack)?AdvancedEnchanting.rows(stack,policy):List.of();if(changedItem){selected=null;rank=0;}
  if(selected!=null){var row=rows.stream().filter(r->r.id().equals(selected)).findFirst().orElse(null);if(row==null){selected=null;rank=0;}}
  quote=selected==null?new AdvancedEnchanting.Quote(0,0,0,stack.isEmpty()?AdvancedEnchanting.Reason.EMPTY:AdvancedEnchanting.accepts(stack,policy)?AdvancedEnchanting.Reason.INVALID:AdvancedEnchanting.Reason.MATERIAL):AdvancedEnchanting.quote(stack,selected,rank,shelves,xp,policy);
  revision++;view=new AdvancedEnchantNetwork.Snapshot(containerId,session,revision,Math.min(shelves,policy.maximumShelves()),AdvancedEnchanting.shelfTier(shelves,policy),xp,selected,rank,quote,rows);
  AdvancedEnchantNetwork.send(server,view);
 }
 @Override public void broadcastChanges(){super.broadcastChanges();if(!owner.level().isClientSide)refresh(false);}
 public boolean request(ServerPlayer player,AdvancedEnchantNetwork.Request request){
  if(player!=owner||closing||committing||player.containerMenu!=this||request.menu()!=containerId||request.session()!=session||!stillValid(player))return false;
  refresh(false);if(request.revision()!=revision){refresh(true);return false;}
  if(request.confirm()){
   if(selected==null||!selected.equals(request.enchantment())||rank!=request.rank())return false;
   var stack=input.getItem(0);
   if(!WorldContentEvents.itemActionAllowed(player.level(),stack))return false;
   var next=AdvancedEnchanting.quote(stack,selected,rank,shelves(),PlayerState.data(player).getInt("Xp"),AdvancedEnchantConfig.policy());
   if(next.reason()!=AdvancedEnchanting.Reason.READY||!next.equals(quote)){refresh(true);return false;}
   // Validate and construct a full copy before mutating either authority.
   var enchantment=net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(selected);var output=stack.copy();var enchants=new LinkedHashMap<>(EnchantmentHelper.getEnchantments(output));enchants.put(enchantment,rank);EnchantmentHelper.setEnchantments(enchants,output);
   int xp=PlayerState.data(player).getInt("Xp");ItemStack original=stack.copy();
   committing=true;
   try{input.setItem(0,output);PlayerState.addXp(player,-next.points());}
   catch(RuntimeException failure){input.setItem(0,original);PlayerState.data(player).putInt("Xp",xp);PlayerState.syncVanillaXp(player);com.mojang.logging.LogUtils.getLogger().error("Advanced enchanting transaction rolled back",failure);return false;}
   finally{committing=false;}
   selected=null;rank=0;PlayerState.tick(player);refresh(true);super.broadcastChanges();player.level().playSound(null,pos,SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.BLOCKS,1,1);return true;
  }
  var row=rows.stream().filter(r->r.id().equals(request.enchantment())).findFirst().orElse(null);
  if(row==null||row.reason()!=AdvancedEnchanting.Reason.READY||request.rank()<row.minimum()||request.rank()>row.maximum())return false;
  selected=request.enchantment();rank=request.rank();refresh(true);return true;
 }
 @Override public boolean clickMenuButton(Player player,int button){return false;}
 @Override public boolean stillValid(Player player){return WorldContentMode.rebuilt(player.level())&&player==owner&&!closing&&player.isAlive()&&!player.isSpectator()&&player.level().hasChunkAt(pos)&&player.level().getBlockState(pos).is(AdvancedEnchantContent.BLOCK.get())&&OriginalEnchantingTables.passable(player.level(),pos.above())&&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
 @Override public void removed(Player player){if(closing)return;closing=true;super.removed(player);if(!player.level().isClientSide)clearContainer(player,input);}
 @Override public ItemStack quickMoveStack(Player player,int index){if(player!=owner||index<0||index>=slots.size()||closing)return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();if(index==0){if(!moveItemStackTo(stack,1,37,true))return ItemStack.EMPTY;}else{var target=slots.get(0);if(target.hasItem()||!target.mayPlace(stack))return ItemStack.EMPTY;target.setByPlayer(stack.copyWithCount(1));stack.shrink(1);}if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return before;}
}
