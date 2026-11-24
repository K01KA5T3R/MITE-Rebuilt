package dev.koikaster.mite;
import net.minecraft.core.BlockPos;import net.minecraft.tags.BlockTags;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.levelgen.feature.*;import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
/** aep's16attempt origins and the full afc four-offset patch, restricted to the Nether. */
public final class SourceWitherwoodFeature1120 extends Feature<NoneFeatureConfiguration> {
 public SourceWitherwoodFeature1120(){super(NoneFeatureConfiguration.CODEC);}
 @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){var world=c.level();if(world.getLevel().dimension()!=Level.NETHER)return false;var random=c.random();var origin=c.origin();var h=new World(world);for(int i=0;i<16;i++){int x=origin.getX()+random.nextInt(16)+8,y=random.nextInt(128),z=origin.getZ()+random.nextInt(16)+8;SourceWitherwoodRules1120.patch(h,random::nextInt,x,y,z);}return true;}
 static final class World implements SourceWitherwoodRules1120.World {
  final WorldGenLevel world;World(WorldGenLevel world){this.world=world;}public boolean nether(){return world.getLevel().dimension()==Level.NETHER;}
  public boolean airOrLeaves(int x,int y,int z){var state=world.getBlockState(new BlockPos(x,y,z));return state.isAir()||state.is(BlockTags.LEAVES);}public boolean empty(int x,int y,int z){return world.isEmptyBlock(new BlockPos(x,y,z));}
  public boolean occurs(int x,int y,int z,int metadata){var pos=new BlockPos(x,y,z);return y>=world.getMinBuildHeight()&&y<world.getMaxBuildHeight()&&(metadata==1?SourceWitherwoodContent1120.WITHERWOOD.get():Blocks.DEAD_BUSH).defaultBlockState().canSurvive(world,pos);}
  public void place(int x,int y,int z,int metadata,int flags){world.setBlock(new BlockPos(x,y,z),(metadata==1?SourceWitherwoodContent1120.WITHERWOOD.get():Blocks.DEAD_BUSH).defaultBlockState(),flags);}
 }
}
