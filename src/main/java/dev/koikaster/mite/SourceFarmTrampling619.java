package dev.koikaster.mite;
import net.minecraft.core.BlockPos;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.entity.Entity;import net.minecraft.world.entity.player.Player;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.phys.shapes.*;import net.minecraftforge.common.MinecraftForge;import net.minecraftforge.event.level.BlockEvent;
/** Source farmland mutation, with native Forge cancellation before mutations.
 * The native Block.fallOn damage call remains in the transformed method. */
public final class SourceFarmTrampling619 {
 private static final VoxelShape SUPPORT=Shapes.box(0,0,0,1,15d/16,1);
 public static VoxelShape collision(){return Shapes.block();}
 public static VoxelShape support(){return SUPPORT;}
 public static void fallen(Level level,BlockState state,BlockPos pos,Entity entity,float distance){
  if(!(level instanceof ServerLevel server)||entity instanceof SourceGelatinousCube||!server.getBlockState(pos).is(Blocks.FARMLAND))return;
  float roll=server.random.nextFloat();if(!(roll<distance-.5f))return;
  if(MinecraftForge.EVENT_BUS.post(new BlockEvent.FarmlandTrampleEvent(server,pos,Blocks.DIRT.defaultBlockState(),distance,entity))||!server.getBlockState(pos).is(Blocks.FARMLAND))return;
  var actor=new SourceFarmFall619.Actor(){public boolean gelatinous(){return false;}public boolean player(){return entity instanceof Player;}};
  var world=new SourceFarmFall619.World(){
   public boolean client(){return false;}public float nextFloat(){return roll;}
   public void tramplePlant(int x,int y,int z,SourceFarmFall619.Actor who){SourcePlantTrampling619.trample(server,new BlockPos(x,y,z),entity);}
   public int metadata(int x,int y,int z){var current=server.getBlockState(new BlockPos(x,y,z));return current.is(Blocks.FARMLAND)?Agriculture.soilMetadata(current):8;}
   public boolean mobGriefing(){return server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);}
   public void dirt(int x,int y,int z){var at=new BlockPos(x,y,z);if(server.getBlockState(at).is(Blocks.FARMLAND))server.setBlock(at,Blocks.DIRT.defaultBlockState(),3);}
  };
  SourceFarmFall619.fall(world,pos.getX(),pos.getY(),pos.getZ(),actor,distance);
 }
 private SourceFarmTrampling619(){}
}
