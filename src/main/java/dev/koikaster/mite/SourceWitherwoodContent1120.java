package dev.koikaster.mite;
import java.util.List;
import net.minecraft.core.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.server.level.ServerLevel;import net.minecraft.util.RandomSource;import net.minecraft.world.entity.Entity;import net.minecraft.world.item.*;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockBehaviour;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.levelgen.feature.Feature;import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;import net.minecraft.world.level.storage.loot.LootParams;import net.minecraft.world.level.storage.loot.parameters.LootContextParams;import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;import net.minecraftforge.registries.*;
public final class SourceWitherwoodContent1120 {
 public static final RegistryObject<Block> WITHERWOOD=Content.BLOCKS.register("witherwood",Bush::new);
 public static final RegistryObject<Block> POTTED=Content.BLOCKS.register("potted_witherwood",Pot::new);
 public static final RegistryObject<Item> ITEM=Content.item("witherwood",()->new BlockItem(WITHERWOOD.get(),new Item.Properties().stacksTo(32)));
 public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,MiteReforged.ID);
 public static final RegistryObject<Feature<NoneFeatureConfiguration>> FEATURE=FEATURES.register("source_witherwood",SourceWitherwoodFeature1120::new);
 private static boolean registered;
 public static void register(){if(registered)return;registered=true;Content.ALL_BLOCKS.put("witherwood",WITHERWOOD);Content.ALL_BLOCKS.put("potted_witherwood",POTTED);var bus=FMLJavaModLoadingContext.get().getModEventBus();FEATURES.register(bus);bus.addListener((FMLCommonSetupEvent e)->e.enqueueWork(()->((FlowerPotBlock)Blocks.FLOWER_POT).addPlant(new ResourceLocation(MiteReforged.ID,"witherwood"),POTTED)));}
 public static final class Bush extends DeadBushBlock {
  public Bush(){super(BlockBehaviour.Properties.copy(Blocks.DEAD_BUSH).strength(.02f).randomTicks().noCollission().noOcclusion());}
  @Override public boolean canSurvive(BlockState state,LevelReader world,BlockPos pos){return world.getBlockState(pos.below()).is(Content.NETHER_GRAVEL.get());}
  @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return Shapes.box(.5f-.4f,0,.5f-.4f,.5f+.4f,.8f,.5f+.4f);}
  @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor world,BlockPos pos,BlockPos neighborPos){if(!state.canSurvive(world,pos)){if(world instanceof ServerLevel server)SourceWitherwood1120.illegal(server,pos);return Blocks.AIR.defaultBlockState();}return state;}
  @Override public void randomTick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random){if(!state.canSurvive(world,pos))SourceWitherwood1120.illegal(world,pos);}
  @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){return SourceWitherwood1120.queriedDrops(loot.getLevel(),loot.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS)!=null);}
  @Override public boolean dropFromExplosion(Explosion explosion){return false;}
  @Override public void entityInside(BlockState state,Level world,BlockPos pos,Entity entity){SourceWitherwood1120.contact(world,entity,false);}
  @Override public void animateTick(BlockState state,Level world,BlockPos pos,RandomSource random){SourceWitherwood1120.particles(world,pos);}
  @Override public int getFlammability(BlockState state,BlockGetter world,BlockPos pos,Direction face){return 100;}
  @Override public int getFireSpreadSpeed(BlockState state,BlockGetter world,BlockPos pos,Direction face){return 60;}
 }
 public static final class Pot extends FlowerPotBlock {
  public Pot(){super(()->(FlowerPotBlock)Blocks.FLOWER_POT,WITHERWOOD,BlockBehaviour.Properties.copy(Blocks.FLOWER_POT));}
  @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){return loot.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS)!=null?List.of():List.of(new ItemStack(Items.FLOWER_POT),new ItemStack(WITHERWOOD.get()));}
  @Override public boolean dropFromExplosion(Explosion explosion){return false;}
  @Override public void entityInside(BlockState state,Level world,BlockPos pos,Entity entity){SourceWitherwood1120.contact(world,entity,true);}
  @Override public void animateTick(BlockState state,Level world,BlockPos pos,RandomSource random){SourceWitherwood1120.particles(world,pos);}
 }
 private SourceWitherwoodContent1120(){}
}
