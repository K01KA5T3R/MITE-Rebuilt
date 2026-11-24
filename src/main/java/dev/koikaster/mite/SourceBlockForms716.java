package dev.koikaster.mite;
import net.minecraft.core.*;import net.minecraft.world.level.BlockGetter;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.piston.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.block.state.properties.*;
/** R195 form/face/precipitation overrides. Shape fallback applies to later block families. */
public final class SourceBlockForms716 {
 public static boolean standard(BlockState state,BlockGetter level,BlockPos pos){
  var b=state.getBlock();if(state.isAir())return false;
  if(b instanceof SlabBlock)return state.getValue(SlabBlock.TYPE)==SlabType.DOUBLE;
  if(b instanceof PistonBaseBlock)return !state.getValue(PistonBaseBlock.EXTENDED);
  if(b instanceof SoulSandBlock)return true;
  if(nonStandard(b))return false;
  return state.isCollisionShapeFullBlock(level,pos);
 }
 private static boolean nonStandard(Block b){return b instanceof BushBlock||b instanceof BasePressurePlateBlock||b instanceof BaseRailBlock||b instanceof ButtonBlock||b instanceof BaseFireBlock||b instanceof LeverBlock||b instanceof LiquidBlock||b instanceof NetherPortalBlock||b instanceof RedStoneWireBlock||b instanceof SugarCaneBlock||b instanceof SignBlock||b instanceof EndPortalBlock||b instanceof TorchBlock||b instanceof TripWireHookBlock||b instanceof TripWireBlock||b instanceof VineBlock||b instanceof WebBlock||b instanceof CarpetBlock||b instanceof StairBlock||b instanceof FarmBlock||b instanceof EnchantmentTableBlock||b instanceof AnvilBlock||b instanceof MetalAnvilBlock||b instanceof BedBlock||b instanceof BrewingStandBlock||b instanceof CactusBlock||b instanceof CakeBlock||b instanceof AbstractCauldronBlock||b instanceof AbstractChestBlock||b instanceof CocoaBlock||b instanceof DaylightDetectorBlock||b instanceof DiodeBlock||b instanceof DoorBlock||b instanceof DragonEggBlock||b instanceof FenceGateBlock||b instanceof FenceBlock||b instanceof FlowerPotBlock||b instanceof HopperBlock||b instanceof EndPortalFrameBlock||b instanceof IronBarsBlock||b instanceof SnowLayerBlock||b instanceof TrapDoorBlock||b instanceof WallBlock||b instanceof PistonHeadBlock||b instanceof MovingPistonBlock||b instanceof LadderBlock||b instanceof SkullBlock||b instanceof WallSkullBlock;}
 public static boolean flat(BlockState state,BlockGetter level,BlockPos pos,Direction face){
  var b=state.getBlock();
  if(b instanceof SlabBlock){var type=state.getValue(SlabBlock.TYPE);return type==SlabType.DOUBLE||face==(type==SlabType.TOP?Direction.UP:Direction.DOWN);}
  if(b instanceof StairBlock)return face==state.getValue(StairBlock.FACING)||face==(state.getValue(StairBlock.HALF)==Half.TOP?Direction.UP:Direction.DOWN);
  if(b instanceof FarmBlock||b instanceof EnchantmentTableBlock)return face==Direction.DOWN;
  if(b instanceof SnowLayerBlock){int depth=state.getValue(SnowLayerBlock.LAYERS);return depth>1&&(face==Direction.DOWN||depth==8);}
  if(b instanceof TrapDoorBlock)return !state.getValue(TrapDoorBlock.OPEN)&&face==(state.getValue(TrapDoorBlock.HALF)==Half.TOP?Direction.UP:Direction.DOWN);
  if(b instanceof PistonBaseBlock)return !state.getValue(PistonBaseBlock.EXTENDED)||face==state.getValue(PistonBaseBlock.FACING).getOpposite();
  if(b instanceof MovingPistonBlock)return face==state.getValue(BlockStateProperties.FACING).getOpposite();
  return standard(state,level,pos)&&SourcePhaseNativeWorld.solid(state);
 }
 public static boolean precipitation(BlockState state,BlockGetter level,BlockPos pos){
  var b=state.getBlock();if(state.isAir()||b instanceof SpawnerBlock)return false;
  if(b instanceof CakeBlock)return state.getValue(CakeBlock.BITES)==0;
  if(b instanceof TrapDoorBlock)return !state.getValue(TrapDoorBlock.OPEN);
  if(b instanceof BasePressurePlateBlock||b instanceof SlabBlock||b instanceof StairBlock||b instanceof FarmBlock||b instanceof EnchantmentTableBlock||b instanceof AnvilBlock||b instanceof MetalAnvilBlock||b instanceof BedBlock||b instanceof CactusBlock||b instanceof AbstractCauldronBlock||b instanceof AbstractChestBlock||b instanceof DaylightDetectorBlock||b instanceof DiodeBlock||b instanceof DragonEggBlock||b instanceof HopperBlock||b instanceof EndPortalBlock||b instanceof EndPortalFrameBlock||b instanceof SnowLayerBlock||b instanceof CarpetBlock||b instanceof WaterlilyBlock||b instanceof PistonBaseBlock||b instanceof MovingPistonBlock)return true;
  return SourcePhaseNativeWorld.liquid(state)||standard(state,level,pos)&&SourcePhaseNativeWorld.solid(state);
 }
 public static boolean neighborBrightness(BlockState state,BlockGetter level,BlockPos pos){
  var b=state.getBlock();if(b instanceof SlabBlock)return state.getValue(SlabBlock.TYPE)!=SlabType.DOUBLE;
  if(b instanceof StairBlock||b instanceof FarmBlock||b instanceof EnchantmentTableBlock)return true;
  // All other original partial forms have opacity below15; modern partial families
  // keep native opacity. In particular original pistons do not borrow neighbors.
  if(nonStandard(b)||b instanceof PistonBaseBlock)return false;
  return !standard(state,level,pos)&&state.canOcclude()&&state.getLightBlock(level,pos)>=15;
 }
 private SourceBlockForms716(){}
}
