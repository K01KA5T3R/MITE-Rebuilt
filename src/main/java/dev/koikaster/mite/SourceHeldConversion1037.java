package dev.koikaster.mite;

import java.util.IdentityHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;

/** Selected-item conversion transport. Restored 0.4.16 effective ClassicQuality equality checks. */
public final class SourceHeldConversion1037 {
 public static void convert(Player player,InteractionHand hand,ItemStack result){
  if(player.level().isClientSide){com.mojang.logging.LogUtils.getLogger().error("convertOneItem: only meant to be called on server");return;}
  var input=player.getItemInHand(hand);if(input.isEmpty())return;
  if(hand!=InteractionHand.MAIN_HAND){input.shrink(1);if(input.isEmpty())player.setItemInHand(hand,result);else if(!result.isEmpty()&&!player.getInventory().add(result))player.drop(result,false);return;}
  var inventory=new Inventory(player);inventory.remember(result);SourceInventoryConvert1035.convert(inventory,player.getInventory().selected,input,result.isEmpty()?null:result);
 }
 static final class Inventory implements SourceInventoryConvert1035.Inventory<ItemStack> {
  final Player player;final net.minecraft.world.entity.player.Inventory inventory;
  final IdentityHashMap<ItemStack,ItemStack> templates=new IdentityHashMap<>();
  Inventory(Player player){this.player=player;inventory=player.getInventory();for(var stack:inventory.items)remember(stack);}
  void remember(ItemStack s){if(s!=null&&!s.isEmpty())templates.putIfAbsent(s,s.copyWithCount(1));}
  ItemStack value(ItemStack s){if(!s.isEmpty())return s;var template=templates.get(s);if(template==null)throw new IllegalArgumentException("Missing source stack identity");return template;}
  public int size(){return inventory.items.size();}public int selected(){return inventory.selected;}public int limit(){return inventory.getMaxStackSize();}public boolean client(){return player.level().isClientSide;}public boolean creative(){return player.getAbilities().instabuild;}
  public ItemStack get(int slot){var s=inventory.items.get(slot);return s.isEmpty()&&!templates.containsKey(s)?null:s;}
  public void set(int slot,ItemStack s){if(s==null)s=ItemStack.EMPTY;else remember(s);if(inventory.items.get(slot)!=s){inventory.setItem(slot,s);if(!client())changed(slot);}}
  public int count(ItemStack s){return s.getCount();}public void count(ItemStack s,int n){remember(s);s.setCount(n);}
  public int maximum(ItemStack s){return value(s).getMaxStackSize();}
  int damage(ItemStack s){var live=value(s);if(live.getItem() instanceof BlockItem item){if(item.getBlock() instanceof MetalAnvilBlock anvil){var tag=live.getTagElement("BlockEntityTag");return tag==null?0:Math.max(0,tag.getInt("damage"));}if(item.getBlock() instanceof AnvilBlock)return SourceAnvil.minimumStage("iron",item.getBlock()==Blocks.DAMAGED_ANVIL?2:item.getBlock()==Blocks.CHIPPED_ANVIL?1:0);}return live.getDamageValue();}
  public boolean damaged(ItemStack s){return SourceStockIdentity1026.damageable(value(s))&&damage(s)!=0;}
  public boolean stackable(ItemStack s){return maximum(s)>1&&(!SourceStockIdentity1026.damageable(value(s))||damage(s)==0);}
  public ItemStack copy(ItemStack s){var live=value(s);var out=live.copyWithCount(1);templates.put(out,out.copy());out.setCount(count(s));out.setPopTime(0);return out;}
  static boolean artifact(ItemStack s){return s.hasTag()&&s.getTag().getBoolean("is_artifact");}
  static CompoundTag sourceTag(ItemStack s){
   if(!s.hasTag())return null;var tag=s.getTag().copy();boolean projected=false;
   if(tag.contains("is_artifact")){tag.remove("is_artifact");projected=true;}
   if(tag.contains(ClassicQuality.NBT_KEY)){tag.remove(ClassicQuality.NBT_KEY);projected=true;}
   if(ClassicVessels.handled(s)){if(tag.contains("Fluid")){tag.remove("Fluid");projected=true;}if(tag.contains("NativeVessel")){tag.remove("NativeVessel");projected=true;}}
   return projected&&tag.isEmpty()?null:tag;
  }
  public boolean equal(ItemStack a,ItemStack b,boolean ignoreTag){
   return equal(a,b,ignoreTag,false);
  }
  public boolean equal(ItemStack a,ItemStack b,boolean ignoreTag,boolean ignoreQuality){
   if(a==b)return true;if(a==null||b==null)return false;var x=value(a);var y=value(b);var first=SourceStockIdentity1026.view(x);var second=SourceStockIdentity1026.view(y);
   if(!first.item().identity().equals(second.item().identity())||first.subtype()!=second.subtype()||damage(a)!=damage(b)||artifact(x)!=artifact(y))return false;
   if(!ignoreQuality&&!ClassicQuality.sameQuality(x,y))return false;
   return ignoreTag||java.util.Objects.equals(sourceTag(x),sourceTag(y));
  }
  public void pop(ItemStack s,int ticks){s.setPopTime(ticks);}
  public void changed(int slot){inventory.setChanged();player.inventoryMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastChanges();}
  public void restock(ItemStack previous){SourceRootTransport1001.beforeLastHeld(player,previous);}
  public void drop(ItemStack stack){player.drop(stack,false);}
  public void clientConversionError(){com.mojang.logging.LogUtils.getLogger().error("convertOneItem: only meant to be called on server");}
  public RuntimeException insertionFailure(ItemStack stack,Throwable error){return new IllegalStateException("Adding item to inventory",error);}
 }
 private SourceHeldConversion1037(){}
}
