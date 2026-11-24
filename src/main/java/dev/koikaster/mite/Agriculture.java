package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class Agriculture {
 // This class is loaded while vanilla Blocks is bootstrapping: no static Block/Content lookups.
 public static final BooleanProperty FERTILIZED=BooleanProperty.create("mite_fertilized");
 @SuppressWarnings("unchecked") public static void defineSoil(Object builder){((StateDefinition.Builder<Block,BlockState>)builder).add(FERTILIZED);}
 public static BlockState soilDefaults(BlockState state){return state.setValue(FERTILIZED,false);}
 public static boolean supports(BlockState state){return state.is(Blocks.WHEAT)||state.is(Blocks.CARROTS)||state.is(Blocks.POTATOES)||state.getBlock() instanceof OnionCrop;}
 public static boolean supportsBlock(Object block){return block==Blocks.WHEAT||block==Blocks.CARROTS||block==Blocks.POTATOES||block instanceof OnionCrop;}
 public static boolean cropRandomTicks(boolean original,Object raw){
  // Blockstate caches may be built before Blocks.WHEAT/CARROTS/POTATOES is assigned.
  var block=((BlockState)raw).getBlock();return original||block.getClass()==CropBlock.class||block instanceof CarrotBlock||block instanceof PotatoBlock||block instanceof OnionCrop;
 }
 public static boolean fertilizerTarget(boolean original,Object raw){var state=(BlockState)raw;return SourceCropBlight.blighted(state)||!supports(state)&&original;}
 public static int soilMetadata(BlockState state){return state.is(Blocks.FARMLAND)?state.getValue(FarmBlock.MOISTURE)|(state.getValue(FERTILIZED)?8:0):-1;}
 public static float growthRate(ServerLevel level,BlockPos pos,BlockState crop){
  int[] soil=new int[9];int nearby=0;
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)soil[(x+1)*3+z+1]=soilMetadata(level.getBlockState(pos.offset(x,-1,z)));
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(level.getBlockState(pos.offset(x,0,z)).is(crop.getBlock()))nearby|=LegacyAgriculture.cropBit(x,z);
  return LegacyAgriculture.growthRate(level.getBiome(pos).value().getBaseTemperature(),soil,nearby);
 }
 public static boolean nearWater(LevelReader level,BlockPos soil){
  for(BlockPos p:BlockPos.betweenClosed(soil.offset(-4,0,-4),soil.offset(4,1,4)))if(level.getFluidState(p).is(FluidTags.WATER))return true;
  for(BlockPos p:BlockPos.betweenClosed(soil.offset(-1,0,-1),soil.offset(1,1,1))){var state=level.getBlockState(p);if(state.is(Blocks.ICE)||state.is(Blocks.PACKED_ICE)||state.is(Blocks.BLUE_ICE)||state.is(Blocks.SNOW)||state.is(Blocks.SNOW_BLOCK))return true;}
  return false;
 }
 public static boolean cropTick(Object rawBlock,Object rawState,Object rawLevel,Object rawPos,Object rawRandom){
  var state=(BlockState)rawState;if(!supports(state))return false;
  var block=(CropBlock)rawBlock;var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;var random=(RandomSource)rawRandom;
  if(!level.isAreaLoaded(pos,2))return true;
  if(!state.canSurvive(level,pos)){level.destroyBlock(pos,true);return true;}
  if(SourceCropBlight.beforeGrowth(level,pos,state,random))return true;
  float rate=growthRate(level,pos,state);var soilPos=pos.below();
  if(rate==0f&&!nearWater(level,soilPos)){
   if(random.nextFloat()<.05f){
    if(block.isMaxAge(state)){dropDrought(level,pos,state,random);level.removeBlock(pos,false);}
    else level.setBlock(pos,deadFor(state).defaultBlockState().setValue(CropBlock.AGE,block.getAge(state)),3);
   }
   return true;
  }
  if(rate<=0f||block.isMaxAge(state)||!LegacyAgriculture.growthLight(level.getRawBrightness(pos.above(),level.getSkyDarken())))return true;
  if(ForgeHooks.onCropsGrowPre(level,pos,state,random.nextInt(LegacyAgriculture.growthDenominator(rate))==0)){
   level.setBlock(pos,block.getStateForAge(block.getAge(state)+1),2);
   if(random.nextInt(256)==0){var soil=level.getBlockState(soilPos);if(soil.is(Blocks.FARMLAND)&&soil.getValue(FERTILIZED))level.setBlock(soilPos,soil.setValue(FERTILIZED,false),2);}
   ForgeHooks.onCropsGrowPost(level,pos,state);
  }
  return true;
 }
 public static Block deadFor(BlockState state){return state.is(Blocks.CARROTS)?Content.DEAD_CARROTS.get():state.is(Blocks.POTATOES)?Content.DEAD_POTATOES.get():state.is(Content.ONIONS.get())?Content.DEAD_ONIONS.get():Content.DEAD_WHEAT.get();}
 private static void dropDrought(ServerLevel level,BlockPos pos,BlockState crop,RandomSource random){
  var item=crop.is(Blocks.WHEAT)?Items.WHEAT:crop.is(Blocks.CARROTS)?Items.CARROT:crop.is(Blocks.POTATOES)?Items.POTATO:Content.ONION.get();
  int count=crop.is(Blocks.WHEAT)?1:random.nextFloat()<.25f?3:2;Block.popResource(level,pos,new ItemStack(item,count));
 }
 public static void soilTick(Object rawBlock,Object rawState,Object rawLevel,Object rawPos,Object rawRandom){
  var state=(BlockState)rawState;var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;var random=(RandomSource)rawRandom;
  if(!level.isAreaLoaded(pos,4))return;
  if(!state.canSurvive(level,pos)){FarmBlock.turnToDirt(null,state,level,pos);return;}
  int moisture=state.getValue(FarmBlock.MOISTURE);
  if(nearWater(level,pos)){if(moisture<7)level.setBlock(pos,state.setValue(FarmBlock.MOISTURE,7),2);return;}
  if(level.isRainingAt(pos.above())){if(moisture<7)level.setBlock(pos,state.setValue(FarmBlock.MOISTURE,moisture+1),3);return;}
  var biome=level.getBiome(pos).value();if(random.nextFloat()>=LegacyAgriculture.dryingChance(biome.getBaseTemperature(),biome.getModifiedClimateSettings().downfall()>.85f))return;
  if(moisture>0){level.setBlock(pos,state.setValue(FarmBlock.MOISTURE,moisture-1),2);return;}
  if(!level.isEmptyBlock(pos.above()))return;
  for(BlockPos p:BlockPos.betweenClosed(pos.offset(-1,1,-1),pos.offset(1,1,1))){var block=level.getBlockState(p).getBlock();if(block instanceof CropBlock||block instanceof StemBlock||block instanceof AttachedStemBlock)return;}
  FarmBlock.turnToDirt(null,state,level,pos);
 }
 @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event){
  if(event.getEntity().isSpectator()||event.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.DENY)return;
  if(event.getItemStack().is(Content.MANURE.get()))return;
  var level=event.getLevel();var clicked=event.getPos();var state=level.getBlockState(clicked);
  boolean bowl=OriginalBowls.water(event.getItemStack());
  if(!state.is(Blocks.FARMLAND)&&!(state.getBlock() instanceof CropBlock)&&!(state.getBlock() instanceof StemBlock)&&!(state.getBlock() instanceof AttachedStemBlock)&&!(bowl&&state.is(Blocks.BROWN_MUSHROOM)))return;
  var soilPos=state.is(Blocks.FARMLAND)?clicked:clicked.below();var soil=level.getBlockState(soilPos);
  if(!soil.is(Blocks.FARMLAND)||soilPos.equals(clicked)&&event.getFace()!=Direction.UP)return;
  var stack=event.getItemStack();var player=event.getEntity();
  boolean bucket=stack.is(Items.WATER_BUCKET)||stack.getItem() instanceof SpecialItems.MetalBucket m&&m.fluid(stack).equals("water");
  boolean bottle=stack.is(Items.POTION)&&net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack)==net.minecraft.world.item.alchemy.Potions.WATER;
  if(!bucket&&!bottle&&!bowl)return;
  boolean used=false;
  if(level.dimensionType().ultraWarm()){
   used=true;if(!level.isClientSide){
    level.playSound(null,soilPos,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5f,2.6f+(level.random.nextFloat()-level.random.nextFloat())*.8f);
    if(level instanceof ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,soilPos.getX()+.5,soilPos.getY()+1,soilPos.getZ()+.5,8,.5,.3,.5,0);
   }
  }
  else for(BlockPos p:BlockPos.betweenClosed(soilPos.offset(bucket?-1:0,0,bucket?-1:0),soilPos.offset(bucket?1:0,0,bucket?1:0))){var s=level.getBlockState(p);if(s.is(Blocks.FARMLAND)&&s.getValue(FarmBlock.MOISTURE)<7){used=true;if(!level.isClientSide&&level.setBlock(p,s.setValue(FarmBlock.MOISTURE,7),3))SourceMushroomWorld599.checkSoil((ServerLevel)level,p,player);}}
  if(!used)return;
  event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
  if(level.isClientSide||player.isCreative())return;
  if(bowl)ClassicVessels.replaceOne(player,event.getHand(),new ItemStack(Items.BOWL));
  else if(stack.getItem() instanceof SpecialItems.MetalBucket)ClassicVessels.replaceOne(player,event.getHand(),ClassicVessels.peer(stack,""));
  else player.setItemInHand(event.getHand(),ItemUtils.createFilledResult(stack,player,new ItemStack(bucket?Items.BUCKET:Items.GLASS_BOTTLE)));
 }
 public static int manurePeriod(Animal animal){return animal instanceof Chicken?384000:animal instanceof Pig||animal instanceof Sheep?48000:animal instanceof Cow?24000:0;}
 @SubscribeEvent public static void manure(LivingEvent.LivingTickEvent event){
  if(!(event.getEntity() instanceof Animal animal)||animal.level().isClientSide||animal.isBaby())return;int period=manurePeriod(animal);if(period==0)return;
  var data=animal.getPersistentData();int left=data.contains("MiteManureCountdown")?data.getInt("MiteManureCountdown"):animal.getRandom().nextInt(period);
  if(--left<=0){animal.spawnAtLocation(Content.MANURE.get());left=period/2+animal.getRandom().nextInt(period);}data.putInt("MiteManureCountdown",left);
 }
 public static final class DeadCrop extends CropBlock {
  public DeadCrop(){super(Properties.copy(Blocks.WHEAT));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE);}
  @Override public boolean isRandomlyTicking(BlockState state){return false;}
  @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){}
  @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,boolean client){return false;}
 }
 public static final class OnionCrop extends CropBlock {
  public OnionCrop(){super(Properties.copy(Blocks.WHEAT));}
  @Override protected net.minecraft.world.level.ItemLike getBaseSeedId(){return Content.ONION.get();}
 }
 private Agriculture(){}
}
