package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.RegistryObject;

/** Direct BlockBush / BlockGrowingPlant / BlockPlant owner. Growth has three
 * stages, one berry per harvest and no modern sweet-berry damage or slowdown.
 * ForestHills shares modern FOREST; other modern forest biomes are not aliases. */
public final class SourceBlueberryBush extends BushBlock implements BonemealableBlock {
 public static final IntegerProperty GROWTH=IntegerProperty.create("growth",0,3);
 public static RegistryObject<Block> BUSH;
 private static final VoxelShape SHAPE=Block.box(3.2,0,3.2,12.8,9.6,12.8);
 public static void register(){BUSH=Content.BLOCKS.register("bush",SourceBlueberryBush::new);Content.ALL_BLOCKS.put("bush",BUSH);Content.item("bush",()->new BlockItem(BUSH.get(),new Item.Properties().stacksTo(8)));}
 public SourceBlueberryBush(){super(Properties.copy(Blocks.DEAD_BUSH).strength(.05f).randomTicks());registerDefaultState(stateDefinition.any().setValue(GROWTH,0));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(GROWTH);}
 public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
 protected boolean mayPlaceOn(BlockState below,BlockGetter level,BlockPos pos){return below.is(Blocks.GRASS_BLOCK)||below.is(Blocks.DIRT)||below.is(Blocks.FARMLAND);}
 public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return mayPlaceOn(level.getBlockState(pos.below()),level,pos.below())&&(level.canSeeSky(pos)||level.getRawBrightness(pos,0)>=8);}
 public static boolean canPlace(LevelReader level,BlockPos pos){return level.getBiome(pos).is(Biomes.FOREST)&&level.canSeeSky(pos)&&BUSH.get().defaultBlockState().canSurvive(level,pos);}
 public BlockState getStateForPlacement(BlockPlaceContext context){var pos=context.getClickedPos();if(!canPlace(context.getLevel(),pos))return null;return defaultBlockState().setValue(GROWTH,context.getPlayer()!=null&&context.getPlayer().isCreative()?3:0);}
 public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){if(!state.canSurvive(level,pos))level.scheduleTick(pos,this,1);return state;}
 private boolean removeIllegal(BlockState state,ServerLevel level,BlockPos pos){if(state.canSurvive(level,pos))return false;if(level.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,16,false)!=null)dropRipe(state,level,pos);level.removeBlock(pos,false);return true;}
 public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){removeIllegal(state,level,pos);}
 public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(removeIllegal(state,level,pos)||state.getValue(GROWTH)==3)return;float temperature=level.getBiome(pos).value().getBaseTemperature();float rate=.1f*.25f*Math.max(1f-Math.abs(temperature-.7f),0f);if(rate>0&&level.getRawBrightness(pos.above(),level.getSkyDarken())==15&&random.nextFloat()<rate)level.setBlock(pos,state.setValue(GROWTH,state.getValue(GROWTH)+1),2);}
 public static void dropRipe(BlockState state,Level level,BlockPos pos){if(state.getValue(GROWTH)==3)popResource(level,pos,new ItemStack(Content.get("blueberries")));}
 public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(state.getValue(GROWTH)!=3)return InteractionResult.PASS;if(!level.isClientSide){dropRipe(state,level,pos);level.setBlock(pos,state.setValue(GROWTH,0),2);}return InteractionResult.sidedSuccess(level.isClientSide);}
 public List<ItemStack> getDrops(BlockState state,LootParams.Builder params){var result=new ArrayList<ItemStack>();var actor=params.getOptionalParameter(LootContextParams.THIS_ENTITY);var tool=params.getOptionalParameter(LootContextParams.TOOL);boolean silk=tool!=null&&(tool.getItem() instanceof ShearsItem||EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0);if(silk)result.add(new ItemStack(asItem()));if(actor instanceof Player&&state.getValue(GROWTH)==3)result.add(new ItemStack(Content.get("blueberries")));return result;}
 public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,boolean client){return state.getValue(GROWTH)<3;}
 public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state){return true;}
 public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state){level.setBlock(pos,state.setValue(GROWTH,Math.min(3,state.getValue(GROWTH)+1+random.nextInt(2))),2);}
}
