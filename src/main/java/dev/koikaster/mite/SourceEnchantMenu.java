package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;

/** vm/vn/vo: one item, three offers, raw XP100/point, no lapis or modern level debit. */
public final class SourceEnchantMenu extends AbstractContainerMenu {
 public final BlockPos pos;public final boolean emerald;public final int[] levels=new int[3];public final SimpleContainer input;private final Player owner;private final RandomSource random=RandomSource.create();private final DataSlot seed=DataSlot.standalone();private boolean committing;
 public SourceEnchantMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,buffer.readBlockPos(),buffer.readBoolean());}
 public SourceEnchantMenu(int id,Inventory inventory,BlockPos pos,boolean emerald){super(Content.ENCHANT_MENU.get(),id);this.owner=inventory.player;this.pos=pos;this.emerald=emerald;input=new SimpleContainer(1){public int getMaxStackSize(){return 1;}public void setChanged(){super.setChanged();SourceEnchantMenu.this.slotsChanged(this);}};
  addSlot(new Slot(input,0,25,47){public int getMaxStackSize(){return 1;}public boolean mayPlace(ItemStack stack){return accepts(stack);}});for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));for(int i=0;i<3;i++)addDataSlot(DataSlot.shared(levels,i));addDataSlot(seed);
 }
 private boolean accepts(ItemStack stack){
  return WorldContentEvents.itemActionAllowed(owner.level(),stack)&&ClassicEnchanting.accepts(stack);
 }
 public int glyphSeed(){return seed.get();}
 public int cost(int option){return option>=0&&option<3?levels[option]*100:0;}
 public boolean canAfford(Player player,int option){int cost=cost(option);return cost>0&&(player.isCreative()||PlayerState.data(player).getInt("Xp")>=cost);}
 public static int offer(RandomSource random,int option,int shelves,ItemStack stack,boolean emerald){if(ClassicEnchanting.special(stack))return 2;int enchantability=stack.getEnchantmentValue();if(enchantability<=0)return 0;int power=(1+Math.min(shelves,24))*(emerald?2:4);int altered=ClassicEnchanting.handles(stack)?ClassicEnchanting.alterLevels(power,enchantability):power;float fraction=(1+option)/3f;if(option<2)fraction+=(random.nextFloat()-.5f)*.2f;return Math.max(Math.round(altered*fraction),1);}
 public void slotsChanged(Container container){if(container!=input||committing||owner.level().isClientSide)return;var stack=input.getItem(0);if(accepts(stack)){seed.set((int)random.nextLong());int shelves=OriginalEnchantingTables.bookshelves(owner.level(),pos);for(int i=0;i<3;i++)levels[i]=offer(random,i,shelves,stack,emerald);}else Arrays.fill(levels,0);broadcastChanges();}
 @Override public boolean clickMenuButton(Player player, int option) {
  if (!SourceSkillWorld1820.enchanting(player)) return false;
  if (option < 0 || option >= levels.length || player != owner || !stillValid(player) || !canAfford(player, option)) return false;
  var stack = input.getItem(0);
  if (!accepts(stack)) return false;
  if (!(player instanceof ServerPlayer server)) return true;

  int xp = cost(option);
  ItemStack output;
  if (ClassicEnchanting.special(stack)) {
   output = new ItemStack(stack.is(Items.GOLDEN_APPLE) ? Items.ENCHANTED_GOLDEN_APPLE : Items.EXPERIENCE_BOTTLE);
  } else {
   var selected = ClassicEnchanting.handles(stack)
    ? ClassicEnchanting.select(random, stack, levels[option], false)
    : EnchantmentHelper.selectEnchantment(random, stack, levels[option], false);
   if (selected.isEmpty()) return true;

   if (stack.is(Items.BOOK)) {
    output = new ItemStack(Items.ENCHANTED_BOOK);
    if (stack.hasTag()) output.setTag(stack.getTag().copy());
    EnchantedBookItem.addEnchantment(output, selected.get(random.nextInt(selected.size())));
   } else {
    output = stack.copy();
    for (var entry : selected) output.enchant(entry.enchantment, entry.level);
   }
  }

  committing = true;
  try {
   PlayerState.addXp(server, -xp);
   PlayerState.tick(server);
   input.setItem(0, output);
  } finally {
   committing = false;
  }
  slotsChanged(input);

  var level = server.serverLevel();
  var soundRandom = level.getRandom();
  float volume = .9f + (soundRandom.nextFloat() + soundRandom.nextFloat()) / 10f;
  float pitch = .9f + (soundRandom.nextFloat() + soundRandom.nextFloat()) / 10f;
  level.playSound(null, server, SourceCreatureSounds.SKILL_DRAIN.get(), SoundSource.PLAYERS, volume, pitch);
  return true;
 }
 public boolean stillValid(Player player){return OriginalEnchantingTables.table(player.level().getBlockState(pos))&&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
 public void removed(Player player){super.removed(player);if(!player.level().isClientSide)clearContainer(player,input);}
 @Override public ItemStack quickMoveStack(Player player, int index) {
  if (player != owner || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
  var slot = slots.get(index);
  if (!slot.hasItem()) return ItemStack.EMPTY;
  var stack = slot.getItem();
  var previous = stack.copy();

  if (index == 0) {
   if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
  } else {
   var target = slots.get(0);
   if (target.hasItem() || !target.mayPlace(stack)) return ItemStack.EMPTY;
   target.setByPlayer(stack.copyWithCount(1));
   stack.shrink(1);
  }
  if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
  else slot.setChanged();
  if (stack.getCount() == previous.getCount()) return ItemStack.EMPTY;
  slot.onTake(player, stack);
  return previous;
 }
}
