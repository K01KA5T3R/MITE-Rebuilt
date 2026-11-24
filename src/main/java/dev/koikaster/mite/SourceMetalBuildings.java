package dev.koikaster.mite;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.RegistryObject;

/** R195 aqy, anz/xk, BlockStrongbox/TileEntityStrongbox, BlockSlabGroup3/aqp. */
public final class SourceMetalBuildings {
 public static final List<String> METALS=List.of("copper","silver","gold","iron","mithril","adamantium","ancient_metal");
 public static final Map<String,RegistryObject<MetalDoor>> DOORS=new LinkedHashMap<>();
 public static final Map<String,RegistryObject<MetalBars>> BARS=new LinkedHashMap<>();
 public static final Map<String,RegistryObject<Strongbox>> BOXES=new LinkedHashMap<>();
 public static final RegistryObject<SlabBlock> OBSIDIAN_SLAB=block("obsidian_slab",()->new SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.2f,0).sound(SoundType.STONE).requiresCorrectToolForDrops()),8,false);
 public static final RegistryObject<StairBlock> OBSIDIAN_STAIRS=block("obsidian_stairs",()->new StairBlock(()->Blocks.OBSIDIAN.defaultBlockState(),BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.4f,2.4f).sound(SoundType.STONE).requiresCorrectToolForDrops()),4,false);
 public static final RegistryObject<BlockEntityType<BoxEntity>> BOX_ENTITY;
 public static final RegistryObject<SoundEvent> LOCKED=SourceCreatureSounds.SOUNDS.register("chest_locked",()->SoundEvent.createVariableRangeEvent(Content.id("chest_locked")));
 static {
  for(String metal:METALS){
   if(!metal.equals("iron")){
    DOORS.put(metal,block(metal+"_door",()->new MetalDoor(metal),1,false));
    BARS.put(metal,block(metal+"_bars",()->new MetalBars(metal),16,false));
   }
   BOXES.put(metal,block(metal+"_strongbox",()->new Strongbox(metal),4,true));
  }
  BOX_ENTITY=Content.BLOCK_ENTITIES.register("metal_strongbox",()->BlockEntityType.Builder.of(BoxEntity::new,BOXES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
 }
 static <T extends Block> RegistryObject<T> block(String name,Supplier<T> make,int cap,boolean entityModel){var b=Content.BLOCKS.register(name,make);Content.ALL_BLOCKS.put(name,(RegistryObject<Block>)(RegistryObject<?>)b);Content.item(name,()->entityModel?new BoxItem(b.get(),new Item.Properties().stacksTo(cap)):new BlockItem(b.get(),new Item.Properties().stacksTo(cap)));return b;}
 static BlockBehaviour.Properties metal(String material,float wood){float hardness=wood*LegacyToolWear.durability(material);return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness,hardness).sound(SoundType.METAL).requiresCorrectToolForDrops();}
 public static final class MetalBars extends IronBarsBlock {
  public final String material;MetalBars(String m){super(metal(m,.4f).noOcclusion());material=m;}
  boolean joins(BlockState other,BlockGetter world,BlockPos at){return other.is(this)||other.is(Blocks.GLASS)||other.isSolidRender(world,at)&&other.isCollisionShapeFullBlock(world,at);}
  static BooleanProperty side(Direction d){return switch(d){case NORTH->BlockStateProperties.NORTH;case SOUTH->BlockStateProperties.SOUTH;case EAST->BlockStateProperties.EAST;case WEST->BlockStateProperties.WEST;default->throw new IllegalArgumentException();};}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){var state=super.getStateForPlacement(c);for(var d:Direction.Plane.HORIZONTAL){var p=c.getClickedPos().relative(d);state=state.setValue(side(d),joins(c.getLevel().getBlockState(p),c.getLevel(),p));}return state;}
  @Override public BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor world,BlockPos p,BlockPos q){var state=super.updateShape(s,d,n,world,p,q);return d.getAxis().isHorizontal()?state.setValue(side(d),joins(n,world,q)):state;}
 }
 public static final class MetalDoor extends DoorBlock {
  public final String material;MetalDoor(String m){super(metal(m,.5f).noOcclusion(),BlockSetType.IRON);material=m;}
  static boolean frameMaterial(BlockState s){var b=s.getBlock();return !s.is(BlockTags.DIRT)&&!s.is(BlockTags.SAND)&&!s.is(BlockTags.LEAVES)&&!s.is(BlockTags.WOOL)&&!(b instanceof FarmBlock)&&!s.is(Blocks.CLAY)&&!s.is(Blocks.SNOW_BLOCK)&&!(b instanceof PumpkinBlock)&&!(b instanceof MelonBlock)&&!s.is(Blocks.SPONGE)&&!s.is(Blocks.WET_SPONGE)&&!(b instanceof AbstractGlassBlock);}
  boolean frame(BlockState s,LevelReader world,BlockPos lower){var facing=s.getValue(FACING);var side=s.getValue(HINGE)==DoorHingeSide.RIGHT?facing.getClockWise():facing.getCounterClockWise();var at=lower.relative(side);var neighbor=world.getBlockState(at);
   // anz's unflipped adjacent leaf searches the opposite outer jamb.
   if(s.getValue(HINGE)==DoorHingeSide.RIGHT&&neighbor.is(this)&&neighbor.getValue(FACING)==facing&&neighbor.getValue(HINGE)==DoorHingeSide.RIGHT){side=side.getOpposite();at=lower.relative(side);}
   for(int y=0;y<2;y++){var p=at.above(y);var block=world.getBlockState(p);if(!frameMaterial(block)||!block.isFaceSturdy(world,p,side.getOpposite()))return false;}return true;
  }
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){var p=c.getClickedPos();if(p.getY()>=c.getLevel().getMaxBuildHeight()-1||!c.getLevel().getBlockState(p.above()).canBeReplaced(c))return null;var facing=c.getHorizontalDirection().getOpposite();var s=defaultBlockState().setValue(FACING,facing).setValue(HINGE,DoorHingeSide.RIGHT);var other=c.getLevel().getBlockState(p.relative(facing.getClockWise()));if(other.is(this)&&other.getValue(FACING)==facing&&other.getValue(HINGE)==DoorHingeSide.RIGHT)s=s.setValue(HINGE,DoorHingeSide.LEFT);boolean powered=c.getLevel().hasNeighborSignal(p)||c.getLevel().hasNeighborSignal(p.above());s=s.setValue(OPEN,powered).setValue(POWERED,powered);return frame(s,c.getLevel(),p)?s:null;}
  @Override public boolean canSurvive(BlockState s,LevelReader world,BlockPos p){if(s.getValue(HALF)==DoubleBlockHalf.UPPER){var below=world.getBlockState(p.below());return below.is(this)&&below.getValue(HALF)==DoubleBlockHalf.LOWER&&frame(below,world,p.below());}return frame(s,world,p);}
  @Override public BlockState updateShape(BlockState s,Direction side,BlockState neighbor,LevelAccessor world,BlockPos p,BlockPos q){if(!canSurvive(s,world,p))return Blocks.AIR.defaultBlockState();return super.updateShape(s,side,neighbor,world,p,q);}
 }
 public static final class Strongbox extends ChestBlock {
  public final String material;Strongbox(String m){super(metal(m,1.2f),()->BOX_ENTITY.get());material=m;}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:s.setValue(TYPE,ChestType.SINGLE);}
  @Override public BlockState updateShape(BlockState s,Direction side,BlockState n,LevelAccessor world,BlockPos p,BlockPos q){return super.updateShape(s,side,n,world,p,q).setValue(TYPE,ChestType.SINGLE);}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new BoxEntity(p,s);}
  @Override public void setPlacedBy(Level world,BlockPos p,BlockState s,LivingEntity who,ItemStack item){super.setPlacedBy(world,p,s,who,item);if(who instanceof Player player&&world.getBlockEntity(p) instanceof BoxEntity box){box.owner=player.getGameProfile().getName();box.setChanged();world.sendBlockUpdated(p,s,s,3);}}
  @Override public MenuProvider getMenuProvider(BlockState s,Level world,BlockPos p){return world.getBlockEntity(p) instanceof BoxEntity b?b:null;}
  @Override public InteractionResult use(BlockState s,Level world,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(!world.isClientSide&&world.getBlockEntity(p) instanceof BoxEntity b){boolean allowed=b.allowed(player)&&!OriginalCurseEffects.has(player,11,true)&&world.getBlockState(p.above()).getCollisionShape(world,p.above()).isEmpty();if(allowed)player.openMenu(b);else if(b.getOpenNess(0)==0)world.playSound(null,p,LOCKED.get(),SoundSource.BLOCKS,.2f,1f);}return InteractionResult.sidedSuccess(world.isClientSide);}
  @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder p){var who=responsible(p.getOptionalParameter(LootContextParams.THIS_ENTITY));return p.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BoxEntity b&&who!=null&&!who.isCreative()&&b.ownedBy(who)?List.of(new ItemStack(this)):List.of();}
  @Override public boolean canHarvestBlock(BlockState s,BlockGetter world,BlockPos p,Player player){if(world.getBlockEntity(p) instanceof BoxEntity b&&b.ownedBy(player))return true;return LegacyHarvest.allows(HarvestRules.toolKind(player.getMainHandItem()),HarvestRules.harvestLevel(player.getMainHandItem()),LegacyHarvest.Family.PICKAXE,LegacyHarvest.material(material)+1,false);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world,BlockState state,BlockEntityType<T> type){return type==BOX_ENTITY.get()?(l,p,s,b)->ChestBlockEntity.lidAnimateTick(l,p,s,(BoxEntity)b):null;}
 }
 static Player responsible(Entity e){if(e instanceof Player p)return p;if(e instanceof net.minecraft.world.entity.item.PrimedTnt t)return responsible(t.getOwner());if(e instanceof net.minecraft.world.entity.projectile.Projectile p)return responsible(p.getOwner());return null;}
 public static final class BoxEntity extends ChestBlockEntity {
  String owner;
  public BoxEntity(BlockPos p,BlockState s){super(BOX_ENTITY.get(),p,s);}
  public boolean ownedBy(Player player){return player.getGameProfile().getName().equals(owner);}
  public boolean allowed(Player player){return player.isCreative()||ownedBy(player);}
  @Override public boolean canOpen(Player player){return allowed(player);}
  @Override public boolean stillValid(Player player){return allowed(player)&&super.stillValid(player);}
  @Override protected Component getDefaultName(){return Component.translatable("container.mite_reforged.strongbox."+((Strongbox)getBlockState().getBlock()).material);}
  @Override public void load(CompoundTag tag){super.load(tag);owner=tag.contains("owner_name")?tag.getString("owner_name"):null;}
  @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(owner!=null)tag.putString("owner_name",owner);}
  @Override public CompoundTag getUpdateTag(){var tag=new CompoundTag();if(owner!=null)tag.putString("owner_name",owner);return tag;}
  @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 }
 public static final class BoxItem extends BlockItem {
  BoxItem(Block block,Properties p){super(block,p);}
  @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(ClientMetalBuildings.ITEM);}
 }
 public static boolean portable(BlockState s,Level world,BlockPos pos,Entity who){return who instanceof Player p&&world.getBlockEntity(pos) instanceof BoxEntity b&&b.ownedBy(p);}
 public static void register(){}
 private SourceMetalBuildings(){}
}
