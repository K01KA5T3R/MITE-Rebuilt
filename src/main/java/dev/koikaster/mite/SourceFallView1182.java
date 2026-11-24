package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
/** Native world/effect/type providers for the qualified source fall calculator. */
public final class SourceFallView1182 implements SourceFallRules1151.View<SourceFallView1182.Support,Block> {
 public record Support(BlockState state,BlockPos pos){}
 private final LivingEntity actor;
 public SourceFallView1182(LivingEntity actor){this.actor=actor;}
 public Integer jumpAmplifier(){var effect=actor.getEffect(MobEffects.JUMP);return effect==null?null:effect.getAmplifier();}
 public Support resting(float allowance){
  var box=actor.getBoundingBox();var level=actor.level();var context=CollisionContext.of(actor);
  return SourceRestingRules1182.resting(new SourceRestingRules1182.View<BlockState,Support>(){
   public boolean grounded(){return actor.onGround();}public boolean riding(){return actor.isPassenger();}
   public double minX(){return box.minX;}public double maxX(){return box.maxX;}public double minY(){return box.minY;}public double maxY(){return box.maxY;}public double minZ(){return box.minZ;}public double maxZ(){return box.maxZ;}public double x(){return actor.getX();}public double z(){return actor.getZ();}
   public BlockState block(int x,int y,int z){var state=level.getBlockState(new BlockPos(x,y,z));return state.isAir()?null:state;}
   public Double collisionTop(BlockState state,int x,int y,int z){
    // R195 arh excludes a WoodSpider penetrating the top of its leaves.
    if(woodSpider()&&state.is(BlockTags.LEAVES)&&(double)((float)y+.99f)>actor.getY())return null;
    var shape=state.getCollisionShape(level,new BlockPos(x,y,z),context);return shape.isEmpty()?null:shape.bounds().maxY+(double)y;
   }
   public Support support(BlockState state,int x,int y,int z){return new Support(state,new BlockPos(x,y,z));}
  });
 }
 public int blockId(Support support){return BuiltInRegistries.BLOCK.getId(support.state().getBlock());}
 public Block block(int id){return id==0?null:BuiltInRegistries.BLOCK.byId(id);}
 public int metadata(Support support,int dy){var state=actor.level().getBlockState(support.pos().offset(0,dy,0));return state.getBlock() instanceof SnowLayerBlock?state.getValue(SnowLayerBlock.LAYERS)-1:0;}
 public float cushioning(Block block,int metadata){return SourceCushioning1182.get(block,metadata);}
 public boolean liquid(Block block){return block instanceof LiquidBlock;}
 public Block below(Support support){var state=actor.level().getBlockState(support.pos().below());return state.isAir()?null:state.getBlock();}
 public int aboveId(Support support){return BuiltInRegistries.BLOCK.getId(actor.level().getBlockState(support.pos().above()).getBlock());}
 public boolean arachnid(){return actor instanceof Spider;}
 public boolean woodSpider(){return actor instanceof Spider&&Mobs.name(actor).equals("wood_spider");}
 public static void calculate(LivingEntity actor,float distance,float[] damages){SourceFallRules1151.calculate(new SourceFallView1182(actor),distance,damages);}
}
