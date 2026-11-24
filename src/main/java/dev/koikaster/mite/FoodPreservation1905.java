package dev.koikaster.mite;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.*;

/** E21/E22/E28/E29: fixed four-place drying, followed by optional native-smoker work. */
public final class FoodPreservation1905 {
 public static final int DRY_TICKS=4800;
 public record Food(String key,String raw,String cooked,int satiation,int nutrition,int nutrients){}
 public static final List<Food> FOODS=load();
 public static final Map<String,RegistryObject<Item>> DRIED=new LinkedHashMap<>(),SMOKED=new LinkedHashMap<>();
 public static final RegistryObject<Rack> BLOCK=Content.BLOCKS.register("drying_rack",Rack::new);
 public static final RegistryObject<Item> ITEM=Content.ITEMS.register("drying_rack",()->new BlockItem(BLOCK.get(),new Item.Properties().stacksTo(1)));
 public static final RegistryObject<BlockEntityType<RackEntity>> ENTITY=Content.BLOCK_ENTITIES.register("drying_rack",()->BlockEntityType.Builder.of(RackEntity::new,BLOCK.get()).build(null));
 static {for(var food:FOODS){DRIED.put(food.key(),food(food,"dried_",1));if(food.cooked()!=null)SMOKED.put(food.key(),food(food,"smoked_",2));}}
 private static List<Food> load(){
  try(var in=FoodPreservation1905.class.getResourceAsStream("/data/mite_reforged/food_preservation1905.json")){
   if(in==null)throw new IOException("Missing preservation definitions");var result=new ArrayList<Food>();var keys=new HashSet<String>();var raws=new HashSet<String>();
   for(var value:JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonArray()){
    var row=value.getAsJsonObject();var food=new Food(row.get("key").getAsString(),row.get("raw").getAsString(),row.get("cooked").getAsString(),row.get("satiation").getAsInt(),row.get("nutrition").getAsInt(),row.get("nutrients").getAsInt());
    if(!food.key().matches("[a-z_]+")||!keys.add(food.key())||!raws.add(food.raw())||food.satiation()<0||food.satiation()>38||food.nutrition()<1||food.nutrition()>38||food.nutrients()<0||food.nutrients()>7)throw new IOException("Invalid preservation definition");result.add(food);
   }result.addAll(FruitForaging.preservation());return List.copyOf(result);
  }catch(IOException error){throw new ExceptionInInitializerError(error);}
 }
 private static RegistryObject<Item> food(Food food,String prefix,int extra){
  return Content.item(prefix+food.key(),()->{
   int nutrition=food.nutrition()+extra;
   var properties=new FoodProperties.Builder().nutrition(nutrition).saturationMod((food.satiation()+extra)/(2f*nutrition));
   boolean fruit=food.cooked()==null;
   if(!fruit)properties.meat();
   boolean chorus=food.key().equals("chorus_fruit");
   if(chorus)properties.alwaysEat();
   var item=new Item.Properties().stacksTo(fruit?FruitForaging.driedStack(food.key()):16).food(properties.build());
   return chorus?new ChorusFruitItem(item):new Item(item);
  });
 }
 public static void register(){Content.ALL_BLOCKS.put("drying_rack",(RegistryObject)BLOCK);Content.ALL_ITEMS.put("drying_rack",ITEM);}
 public static Item resolve(String key){var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(key));if(item==null||item==Items.AIR)throw new IllegalStateException("Missing preservation food:"+key);return item;}
 private static String key(ItemStack stack){return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));}
 public static Food raw(ItemStack stack){if(stack.isEmpty())return null;String key=key(stack);for(var food:FOODS)if(food.raw().equals(key))return food;return null;}
 public static Food dried(ItemStack stack){if(stack.isEmpty())return null;for(var food:FOODS)if(stack.is(DRIED.get(food.key()).get()))return food;return null;}
 public static boolean finished(ItemStack stack){return dried(stack)!=null;}
 public static ClassicFurnaces.Plan smoke(ItemStack stack){var food=dried(stack);var smoked=food==null?null:SMOKED.get(food.key());return smoked==null?ClassicFurnaces.NONE:new ClassicFurnaces.Plan(new ItemStack(smoked.get()),1,1,1);}
 public static boolean exposed(Level level,BlockPos pos){var above=pos.above();return level.getFluidState(pos).isEmpty()&&level.getFluidState(above).isEmpty()&&level.canSeeSky(above)&&level.getHeight(Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ())<=pos.getY()+1&&!(level.isRaining()&&level.getBiome(above).value().hasPrecipitation())&&!LocalWeather1981.preventsDrying(level,pos);}

 public static final class Rack extends BaseEntityBlock {
  private static final VoxelShape SHAPE=Block.box(1,0,1,15,12,15);
  Rack(){super(Properties.copy(Blocks.OAK_PLANKS).strength(.6f).noOcclusion());}
  public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
  public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
  @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return context.getLevel().getFluidState(context.getClickedPos()).isEmpty()?defaultBlockState():null;}
  public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RackEntity(pos,state);}
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ENTITY.get(),RackEntity::tick);}
  public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
   if(!WorldContentMode.rebuilt(level)||!(level.getBlockEntity(pos) instanceof RackEntity rack))return InteractionResult.PASS;
   var held=player.getItemInHand(hand);var at=hit.getLocation().subtract(pos.getX(),pos.getY(),pos.getZ());int slot=(at.x>=.5?1:0)+(at.z>=.5?2:0);
   if(raw(held)!=null){if(!rack.getItem(slot).isEmpty())return InteractionResult.sidedSuccess(level.isClientSide);if(!level.isClientSide){rack.setItem(slot,held.copyWithCount(1));if(!player.isCreative())held.shrink(1);}return InteractionResult.sidedSuccess(level.isClientSide);}
   if(held.isEmpty()&&!rack.getItem(slot).isEmpty()){if(!level.isClientSide){var take=rack.removeItemNoUpdate(slot);ItemHandlerHelper.giveItemToPlayer(player,take);rack.changed();}return InteractionResult.sidedSuccess(level.isClientSide);}
   return InteractionResult.PASS;
  }
  public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof RackEntity rack){Containers.dropContents(level,pos,rack);level.updateNeighbourForOutputSignal(pos,this);}super.onRemove(state,level,pos,next,moving);}
  @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 20;}
  @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 5;}
 }
 public static final class RackEntity extends BlockEntity implements WorldlyContainer {
  private static final int[] SLOTS={0,1,2,3};private final NonNullList<ItemStack> items=NonNullList.withSize(4,ItemStack.EMPTY);private final int[] progress=new int[4];
  private final Map<Direction,LazyOptional<IItemHandler>> capabilities=new EnumMap<>(Direction.class);
  RackEntity(BlockPos pos,BlockState state){super(ENTITY.get(),pos,state);}
  public int getContainerSize(){return 4;}public int getMaxStackSize(){return 1;}public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}public ItemStack getItem(int slot){return items.get(slot);}
  public int progress(int slot){return progress[slot];}
  public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty()){progress[slot]=0;changed();}return result;}
  public ItemStack removeItemNoUpdate(int slot){progress[slot]=0;return ContainerHelper.takeItem(items,slot);}
  public void setItem(int slot,ItemStack stack){if(!ItemStack.matches(items.get(slot),stack))progress[slot]=0;items.set(slot,stack);changed();}
  public void clearContent(){items.clear();Arrays.fill(progress,0);changed();}
  public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
  public boolean canPlaceItem(int slot,ItemStack stack){return level!=null&&WorldContentMode.rebuilt(level)&&items.get(slot).isEmpty()&&raw(stack)!=null;}
  public int[] getSlotsForFace(Direction side){return SLOTS.clone();}
  public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return side!=Direction.DOWN&&canPlaceItem(slot,stack);}
  public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return finished(stack);}
  @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){if(capability==ForgeCapabilities.ITEM_HANDLER&&!isRemoved())return capabilities.computeIfAbsent(side==null?Direction.UP:side,d->LazyOptional.of(()->new SidedInvWrapper(this,d))).cast();return super.getCapability(capability,side);}
  @Override public void invalidateCaps(){super.invalidateCaps();capabilities.values().forEach(LazyOptional::invalidate);capabilities.clear();}
  @Override public void load(CompoundTag tag){super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);var saved=tag.getIntArray("Drying");for(int i=0;i<4;i++)progress[i]=raw(items.get(i))!=null&&i<saved.length?Math.max(0,Math.min(DRY_TICKS,saved[i])):0;}
  @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putIntArray("Drying",progress.clone());}
  @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
  @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
  void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
  public static void tick(Level level,BlockPos pos,BlockState state,RackEntity rack){
   if(level.isClientSide||!WorldContentMode.rebuilt(level)||!exposed(level,pos))return;boolean dirty=false,updated=false;
   for(int i=0;i<4;i++){var stack=rack.items.get(i);var food=raw(stack);if(food==null||stack.getCount()!=1)continue;int next=Math.min(DRY_TICKS,rack.progress[i]+1);rack.progress[i]=next;dirty=true;if(next==DRY_TICKS){rack.items.set(i,new ItemStack(DRIED.get(food.key()).get()));rack.progress[i]=0;updated=true;}}
   if(updated)rack.changed();else if(dirty&&level.getGameTime()%20==0)rack.setChanged();
  }
 }
 static boolean addedFood(String name){
  return name.startsWith("dried_")&&DRIED.containsKey(name.substring("dried_".length()))
      ||name.startsWith("smoked_")&&SMOKED.containsKey(name.substring("smoked_".length()));
 }
 private FoodPreservation1905(){}
}
