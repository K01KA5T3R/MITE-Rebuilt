package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;

/** N017: a persistent recipe template, real hopper inventory and atomic paid crafts.
 * The machine uses the existing level-zero table duration. Experience is an explicit
 * stored resource; the virtual recipe owner supplies no experience or inventory.
 */
public final class MithrilAutoWorkbench {
 public static final RegistryObject<BlockImpl> BLOCK=Content.BLOCKS.register("mithril_auto_workbench",BlockImpl::new);
 public static final RegistryObject<Item> ITEM=Content.ITEMS.register("mithril_auto_workbench",()->new BlockItem(BLOCK.get(),new Item.Properties().stacksTo(1)));
 public static final RegistryObject<BlockEntityType<Entity>> ENTITY=Content.BLOCK_ENTITIES.register("mithril_auto_workbench",()->BlockEntityType.Builder.of(Entity::new,BLOCK.get()).build(null));
 public static final RegistryObject<MenuType<Menu>> MENU=Content.MENUS.register("mithril_auto_workbench",()->net.minecraftforge.common.extensions.IForgeMenuType.create(Menu::new));
 public static void register(){Content.ALL_BLOCKS.put("mithril_auto_workbench",(RegistryObject)BLOCK);Content.ALL_ITEMS.put("mithril_auto_workbench",ITEM);}
 public static final int CREDIT_CAP=100000;
 public static final BooleanProperty WORKING=BooleanProperty.create("crafting");
 public static final int[] INPUT=java.util.stream.IntStream.range(0,9).toArray(),OUTPUT=java.util.stream.IntStream.range(9,36).toArray(),ALL=java.util.stream.IntStream.range(0,36).toArray();
 public static class BlockImpl extends BaseEntityBlock {
  BlockImpl(){super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(6,1200).sound(SoundType.METAL));registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(WORKING,false));}
  protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING,WORKING);}
  public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());}
  public BlockState rotate(BlockState s,Rotation r){return s.setValue(BlockStateProperties.HORIZONTAL_FACING,r.rotate(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
  public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(BlockStateProperties.HORIZONTAL_FACING)));}
  public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
  public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Entity(p,s);}
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide||!WorldContentMode.rebuilt(l)?null:createTickerHelper(t,ENTITY.get(),Entity::tick);}
  public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(!WorldContentMode.rebuilt(l))return InteractionResult.PASS;if(!l.isClientSide&&p instanceof ServerPlayer sp&&l.getBlockEntity(pos) instanceof Entity e)NetworkHooks.openScreen(sp,e,pos);return InteractionResult.sidedSuccess(l.isClientSide);}
  public boolean hasAnalogOutputSignal(BlockState s){return true;}
  public int getAnalogOutputSignal(BlockState s,Level l,BlockPos pos){return l.getBlockEntity(pos) instanceof Entity e?AbstractContainerMenu.getRedstoneSignalFromContainer(e):0;}
  public void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moving){if(!s.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(pos) instanceof Entity e){Containers.dropContents(l,pos,e);if(e.credit>0&&l instanceof ServerLevel server){ExperienceOrb.award(server,net.minecraft.world.phys.Vec3.atCenterOf(pos),e.credit);e.credit=0;}e.items.clear();}super.onRemove(s,l,pos,next,moving);}
 }
 private static CraftingContainer grid(ServerLevel level,Container from){
  var c=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(Player p){return false;}},3,3);
  for(int i=0;i<9;i++)c.setItem(i,from.getItem(i).copy());
  var owner=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("124cb956-1457-4ce2-b43b-c72ed7573949"),"MITEAutoWorkbench"));PlayerState.data(owner).putInt("Xp",0);ClassicCoins.bind(c,owner);return c;
 }
 private static boolean permitted(ItemStack s){return !s.isEmpty()&&!ModernContentPolicy.removed(s)&&!ClassicMusic.removed(s)&&!s.is(Items.SHIELD)&&!ModernContentPolicy.onlyRemovedEnchantments(s);}
 public static boolean sameMaterial(ItemStack pattern,ItemStack stack){
  if(pattern.isEmpty()||!pattern.is(stack.getItem()))return false;
  if(ClassicVessels.handled(pattern))return ClassicVessels.fluid(pattern).equals(ClassicVessels.fluid(stack));
  if(pattern.getItem() instanceof PotionItem)return PotionUtils.getPotion(pattern)==PotionUtils.getPotion(stack);
  return !(pattern.getItem() instanceof SourceRunestoneItem)||SourceRunestoneItem.rune(pattern)==SourceRunestoneItem.rune(stack);
 }
 private static Ingredient[] accepted(CraftingRecipe recipe,Container pattern){
  if(recipe instanceof ShapedRecipe shaped){for(int y=0;y<=3-shaped.getHeight();y++)for(int x=0;x<=3-shaped.getWidth();x++)for(boolean mirror:List.of(false,true)){var cells=new Ingredient[9];boolean ok=true;for(int i=0;i<9;i++){int px=i%3-x,py=i/3-y;var ingredient=px>=0&&py>=0&&px<shaped.getWidth()&&py<shaped.getHeight()?shaped.getIngredients().get(py*shaped.getWidth()+(mirror?shaped.getWidth()-1-px:px)):Ingredient.EMPTY;cells[i]=ingredient;if(!ingredient.test(pattern.getItem(i)))ok=false;}if(ok)return cells;}}
  if(recipe instanceof ShapelessRecipe){var slots=new ArrayList<Integer>();for(int i=0;i<9;i++)if(!pattern.getItem(i).isEmpty())slots.add(i);var ingredients=recipe.getIngredients().stream().filter(i->!i.isEmpty()).toList();if(slots.size()==ingredients.size()){var result=new Ingredient[9];if(assign(0,slots,ingredients,new boolean[ingredients.size()],pattern,result))return result;}}
  return new Ingredient[9];
 }
 private static boolean assign(int n,List<Integer> slots,List<Ingredient> ingredients,boolean[] used,Container pattern,Ingredient[] result){if(n==slots.size())return true;int slot=slots.get(n);for(int i=0;i<ingredients.size();i++)if(!used[i]&&ingredients.get(i).test(pattern.getItem(slot))){used[i]=true;result[slot]=ingredients.get(i);if(assign(n+1,slots,ingredients,used,pattern,result))return true;used[i]=false;}return false;}
 public static final class Entity extends BlockEntity implements WorldlyContainer,MenuProvider {
  private NonNullList<ItemStack> items=NonNullList.withSize(36,ItemStack.EMPTY);
  private boolean loading;
  public final SimpleContainer pattern=new SimpleContainer(9){public void setChanged(){if(!loading)templateChanged();}};
  private final SimpleContainer preview=new SimpleContainer(1);
  private ResourceLocation selected;
  private CraftingRecipe mappedRecipe;private Ingredient[] accepted=new Ingredient[9];
  private String signature="";
  private int progress,total=1,credit,status,rune;
  private LazyOptional<IItemHandlerModifiable>[] handlers=SidedInvWrapper.create(this,Direction.UP,Direction.DOWN,Direction.NORTH,null);
  private final ContainerData clock=new ContainerData(){public int getCount(){return 8;}public int get(int i){return switch(i){case 0->progress&65535;case 1->progress>>>16;case 2->total&65535;case 3->total>>>16;case 4->credit&65535;case 5->credit>>>16;case 6->status;case 7->rune;default->0;};}public void set(int i,int v){}};
  public Entity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}
  public int progress(){return progress;}public int total(){return total;}public int credit(){return credit;}public ResourceLocation recipeId(){return selected;}
  public Component getDisplayName(){return Component.translatable("block.mite_reforged.mithril_auto_workbench");}
  public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){if(level instanceof ServerLevel server){var c=grid(server,pattern);preview.setItem(0,result(recipe(c),c));if(plan()!=null)status=4;}return new Menu(id,inv,this,pattern,preview,clock,worldPosition);}
  private CraftingRecipe recipe(CraftingContainer c){if(!(level instanceof ServerLevel server)||!WorldContentEvents.ingredientsAllowed(level,c))return null;var manager=server.getRecipeManager();if(selected==null){var found=manager.getRecipeFor(RecipeType.CRAFTING,c,level);if(found.isEmpty())return null;selected=found.get().getId();}var r=manager.byKey(selected).orElse(null);return r instanceof CraftingRecipe craft&&craft.matches(c,level)&&!ModernRecipePolicy.removed(craft,level.registryAccess())?craft:null;}
  private ItemStack result(CraftingRecipe recipe,CraftingContainer c){if(recipe==null)return ItemStack.EMPTY;var out=recipe.assemble(c,level.registryAccess());if(out.getItem() instanceof SourceRunestoneItem)out=SourceRunestoneItem.select(out,rune);return permitted(out)&&WorldContentEvents.itemActionAllowed(level,out)?out:ItemStack.EMPTY;}
  private void templateChanged(){selected=null;mappedRecipe=null;accepted=new Ingredient[9];signature="";progress=0;rune=0;if(level instanceof ServerLevel server){var c=grid(server,pattern);preview.setItem(0,result(recipe(c),c));}status=pattern.isEmpty()?0:selected==null?6:1;setChanged();}
  private boolean accepts(int slot,ItemStack stack){var sample=pattern.getItem(slot);if(sample.isEmpty()||stack.isEmpty())return false;CraftingRecipe r=null;if(level!=null&&selected!=null&&level.getRecipeManager().byKey(selected).orElse(null) instanceof CraftingRecipe c)r=c;if(r!=mappedRecipe){mappedRecipe=r;accepted=r==null?new Ingredient[9]:MithrilAutoWorkbench.accepted(r,pattern);}if(accepted[slot]!=null&&!ClassicVessels.handled(sample)&&!(sample.getItem() instanceof PotionItem)&&!(sample.getItem() instanceof SourceRunestoneItem))return accepted[slot].test(stack);return sameMaterial(sample,stack);}
  public void setTemplate(int i,ItemStack sample){if(i<0||i>=9)return;pattern.setItem(i,sample.isEmpty()?ItemStack.EMPTY:sample.copyWithCount(1));}
  public void clearTemplate(){for(int i=0;i<9;i++)pattern.setItem(i,ItemStack.EMPTY);}
  private record Plan(CraftingRecipe recipe,CraftingContainer grid,ItemStack output,NonNullList<ItemStack> remaining,int[] consume,int xpDelta,int ticks,String signature){}
  private Plan plan(){
   if(!(level instanceof ServerLevel server))return null;if(selected==null){status=pattern.isEmpty()?0:6;return null;}
   boolean any=false;for(int i=0;i<9;i++){var p=pattern.getItem(i);var in=items.get(i);any|=!p.isEmpty();if(p.isEmpty()?!in.isEmpty():!accepts(i,in)){status=1;return null;}}
   if(!any){status=0;return null;}var c=grid(server,this);var r=recipe(c);var out=result(r,c);if(out.isEmpty()){status=1;return null;}preview.setItem(0,out.copy());
   if(!SourceR196Crafting.allows(level,c,r)){status=7;return null;}
   if(!SourceR196Crafting.allowsWorkbench(level,c,r,out,R196Workbench.Tier.MITHRIL.hardness)){status=8;return null;}
   var consume=new int[9];int refund=0;for(int i=0;i<9;i++)if(!c.getItem(i).isEmpty()){consume[i]=1;if(c.getItem(i).getItem() instanceof SpecialItems.Coin coin)refund+=coin.value;}
   if(r instanceof ClassicCraftRepair){var repair=ClassicCraftRepair.plan(c);if(repair==null)return null;if(repair.threadSlot()>=0)consume[repair.threadSlot()]=repair.consumed();}
   int cost=ClassicCoins.mintCost(out);if(credit<cost){status=3;return null;}if((long)credit+refund-cost>CREDIT_CAP){status=5;return null;}
   var rem=r.getRemainingItems(c);if(!fits(out,rem,out.getItem() instanceof MapItem)){status=2;return null;}
   var owner=ClassicCoins.owner(c);
   int ticks=WorldContentMode.r196(level)?CraftingDifficulty.automaticR196Period(owner,c,r,out):CraftingDifficulty.period(owner,c,r,true);
   var key=new StringBuilder(r.getId().toString()).append('|').append(out.save(new CompoundTag()));for(int i=0;i<9;i++)key.append('|').append(c.getItem(i).copyWithCount(c.getItem(i).isEmpty()?0:1).save(new CompoundTag())).append(':').append(consume[i]);
   return new Plan(r,c,out,rem,consume,refund-cost,ticks,key.toString());
  }
  private static boolean insert(NonNullList<ItemStack> slots,ItemStack incoming,boolean emptyOnly){var stack=incoming.copy();if(stack.isEmpty())return true;if(!emptyOnly)for(int i=0;i<slots.size();i++){var held=slots.get(i);if(held.isEmpty()||!ItemHandlerHelper.canItemStacksStack(held,stack))continue;int count=Math.min(stack.getCount(),Math.max(0,Math.min(64,held.getMaxStackSize())-held.getCount()));held.grow(count);stack.shrink(count);if(stack.isEmpty())return true;}for(int i=0;i<slots.size();i++)if(slots.get(i).isEmpty()){int count=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize()));slots.set(i,stack.copyWithCount(count));stack.shrink(count);if(stack.isEmpty())return true;}return false;}
  private NonNullList<ItemStack> outputCopy(){var out=NonNullList.withSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)out.set(i,items.get(i+9).copy());return out;}
  private boolean fits(ItemStack output,NonNullList<ItemStack> remainder,boolean reserveEmpty){var next=outputCopy();if(!insert(next,output,reserveEmpty))return false;for(var s:remainder)if(!insert(next,s,false))return false;return true;}
  private void finish(Plan p){
   // Map scaling/locking is a native onCraftedBy operation. Reserve a fresh
   // output slot before running it, so its new map ID cannot invalidate a merge.
   var output=p.output.copy();if(output.getItem() instanceof MapItem)output.onCraftedBy(level,ClassicCoins.owner(p.grid),output.getCount());
   var next=outputCopy();if(!insert(next,output,output.getItem() instanceof MapItem))throw new IllegalStateException("Reserved automatic workbench result no longer fits");for(var s:p.remaining)if(!insert(next,s,false))throw new IllegalStateException("Reserved automatic workbench remainder no longer fits");
   for(int i=0;i<9;i++){items.get(i).shrink(p.consume[i]);if(items.get(i).isEmpty())items.set(i,ItemStack.EMPTY);}for(int i=0;i<27;i++)items.set(i+9,next.get(i));credit+=p.xpDelta;progress=0;signature="";setChanged();
  }
  public static void tick(Level level,BlockPos pos,BlockState state,Entity e){if(e.preview.isEmpty()&&level instanceof ServerLevel server){var c=grid(server,e.pattern);e.preview.setItem(0,e.result(e.recipe(c),c));}var p=e.plan();boolean running=p!=null;if(p!=null){if(!e.signature.equals(p.signature)){e.signature=p.signature;e.progress=0;}e.total=p.ticks;e.status=4;if(e.progress<e.total)e.progress++;if(e.progress>=e.total)e.finish(p);e.setChanged();}if(state.getValue(WORKING)!=running)level.setBlock(pos,state.setValue(WORKING,running),3);}
  public boolean transferCredit(ServerPlayer p,boolean deposit){int xp=PlayerState.data(p).getInt("Xp");int amount=deposit?Math.min(100,Math.min(xp,CREDIT_CAP-credit)):(int)Math.min(credit,(long)Rules.MAX_XP-xp);if(amount<=0)return false;PlayerState.addXp(p,deposit?-amount:amount);credit+=deposit?amount:-amount;setChanged();return true;}
  public void cycleRune(){rune=(rune+1)&15;signature="";progress=0;if(level instanceof ServerLevel server){var c=grid(server,pattern);preview.setItem(0,result(recipe(c),c));}setChanged();}
  public int getContainerSize(){return 36;}public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}public ItemStack getItem(int i){return items.get(i);}
  public ItemStack removeItem(int i,int n){var s=ContainerHelper.removeItem(items,i,n);if(!s.isEmpty())setChanged();return s;}
  public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}public void setItem(int i,ItemStack s){items.set(i,s);if(s.getCount()>Math.min(64,s.getMaxStackSize()))s.setCount(Math.min(64,s.getMaxStackSize()));setChanged();}
  public void clearContent(){items.clear();setChanged();}
  public boolean stillValid(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
  public boolean canPlaceItem(int i,ItemStack s){if(i>=9||!accepts(i,s))return false;int count=items.get(i).getCount();for(int n=0;n<9;n++)if(n!=i&&accepts(n,s)&&items.get(n).getCount()<count)return false;return true;}
  public int[] getSlotsForFace(Direction side){return side==null?ALL:side==Direction.DOWN?OUTPUT:INPUT;}
  public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction side){return i<9&&side!=Direction.DOWN&&canPlaceItem(i,s);}public boolean canTakeItemThroughFace(int i,ItemStack s,Direction side){return i>=9;}
  public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(!isRemoved()&&cap==ForgeCapabilities.ITEM_HANDLER)return handlers[side==null?3:side==Direction.UP?0:side==Direction.DOWN?1:2].cast();return super.getCapability(cap,side);}
  public void invalidateCaps(){super.invalidateCaps();for(var h:handlers)h.invalidate();}public void reviveCaps(){super.reviveCaps();handlers=SidedInvWrapper.create(this,Direction.UP,Direction.DOWN,Direction.NORTH,null);}
  protected void saveAdditional(CompoundTag t){super.saveAdditional(t);ContainerHelper.saveAllItems(t,items);var p=new CompoundTag();var list=NonNullList.withSize(9,ItemStack.EMPTY);for(int i=0;i<9;i++)list.set(i,pattern.getItem(i));ContainerHelper.saveAllItems(p,list);t.put("Pattern",p);if(selected!=null)t.putString("Recipe",selected.toString());t.putInt("Progress",progress);t.putInt("Total",total);t.putString("Signature",signature);t.putInt("Credit",credit);t.putInt("Rune",rune);}
  public void load(CompoundTag t){super.load(t);items=NonNullList.withSize(36,ItemStack.EMPTY);ContainerHelper.loadAllItems(t,items);var list=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(t.getCompound("Pattern"),list);loading=true;for(int i=0;i<9;i++)pattern.setItem(i,list.get(i).isEmpty()?ItemStack.EMPTY:list.get(i).copyWithCount(1));loading=false;selected=ResourceLocation.tryParse(t.getString("Recipe"));mappedRecipe=null;accepted=new Ingredient[9];progress=Math.max(0,t.getInt("Progress"));total=Math.max(1,t.getInt("Total"));signature=t.getString("Signature");credit=Math.max(0,Math.min(CREDIT_CAP,t.getInt("Credit")));rune=t.getInt("Rune")&15;}
 }
 public static final class Menu extends AbstractContainerMenu {
  public static final int INPUT_START=10,OUTPUT_START=19,PLAYER_START=46;
  public final Container machine,pattern,preview;public final BlockPos pos;private final ContainerData clock;
  public Menu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,new SimpleContainer(36),new SimpleContainer(9),new SimpleContainer(1),new SimpleContainerData(8),b.readBlockPos());}
  private Menu(int id,Inventory inv,Container machine,Container pattern,Container preview,ContainerData clock,BlockPos pos){super(MENU.get(),id);this.machine=machine;this.pattern=pattern;this.preview=preview;this.clock=clock;this.pos=pos;
   for(int i=0;i<9;i++)addSlot(new Slot(pattern,i,8+i%3*18,30+i/3*18){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
   addSlot(new Slot(preview,0,144,30){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
   for(int i=0;i<9;i++)addSlot(new Slot(machine,i,80+i%3*18,30+i/3*18){public boolean mayPlace(ItemStack s){return !(machine instanceof Entity e)||e.canPlaceItem(getContainerSlot(),s);}});
   for(int i=9;i<36;i++)addSlot(new Slot(machine,i,172+(i-9)%9*18,30+(i-9)/9*18){public boolean mayPlace(ItemStack s){return false;}});
   for(int i=0;i<27;i++)addSlot(new Slot(inv,9+i,89+i%9*18,136+i/9*18));for(int i=0;i<9;i++)addSlot(new Slot(inv,i,89+i*18,194));addDataSlots(clock);
  }
  public int progress(){return (clock.get(0)&65535)|(clock.get(1)&65535)<<16;}public int total(){return Math.max(1,(clock.get(2)&65535)|(clock.get(3)&65535)<<16);}public int credit(){return (clock.get(4)&65535)|(clock.get(5)&65535)<<16;}public int status(){return clock.get(6);}public int rune(){return clock.get(7);}
  public boolean stillValid(Player p){return p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64&&p.level().getBlockState(pos).is(BLOCK.get());}
  public void clicked(int slot,int button,ClickType type,Player p){if(slot>=0&&slot<10){if(slot<9&&type==ClickType.PICKUP&&!p.level().isClientSide&&machine instanceof Entity e){e.setTemplate(slot,button==1?ItemStack.EMPTY:getCarried());broadcastChanges();}return;}super.clicked(slot,button,type,p);}
  public boolean clickMenuButton(Player p,int button){if(!stillValid(p)||!(machine instanceof Entity e)||!(p instanceof ServerPlayer sp))return false;if(button==0)return e.transferCredit(sp,true);if(button==1)return e.transferCredit(sp,false);if(button==2){e.clearTemplate();return true;}if(button==3&&e.preview.getItem(0).getItem() instanceof SourceRunestoneItem){e.cycleRune();return true;}return false;}
  public ItemStack quickMoveStack(Player p,int index){if(index<10||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();if(index<PLAYER_START){if(!moveItemStackTo(stack,PLAYER_START,slots.size(),true))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,INPUT_START,OUTPUT_START,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return stack.getCount()==before.getCount()?ItemStack.EMPTY:before;}
 }
 private MithrilAutoWorkbench(){}
}
