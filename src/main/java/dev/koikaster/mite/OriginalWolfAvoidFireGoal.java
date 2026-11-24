package dev.koikaster.mite;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
/** Source EntityAIAvoidFire:1/10 gate,16 spiral fire candidates,16 increasing-distance path trials. */
public final class OriginalWolfAvoidFireGoal extends OriginalItemMovementGoal {
 private final Wolf wolf;private int index;private List<BlockPos> fires;
 public OriginalWolfAvoidFireGoal(Wolf wolf){super(wolf,1,true);this.wolf=wolf;index=sourceRandomIndex;OriginalEnvironmentPaths.initialize(wolf);}
 public boolean canUse(){if(wolf.getRandom().nextInt(10)>0||wolf.fireImmune())return false;for(var pos:BlockPos.betweenClosed(wolf.blockPosition().offset(-8,-2,-8),wolf.blockPosition().offset(8,2,8)))if(wolf.level().getBlockState(pos).is(Blocks.FIRE))return true;return false;}
 private double nearest(BlockPos pos){double nearest=Double.MAX_VALUE;for(var fire:fires)nearest=Math.min(nearest,OriginalEnvironmentPaths.sq(pos.getX()-fire.getX(),pos.getY()-fire.getY(),pos.getZ()-fire.getZ()));return nearest;}
 protected Path movementPath(){fires=OriginalWolfPaths.fires(wolf);if(fires.isEmpty())return null;int x=wolf.getBlockX(),y=wolf.getBlockY(),z=wolf.getBlockZ();double far=nearest(wolf.blockPosition());Path selected=null;for(int attempt=0;attempt<16;attempt++){int dx=OriginalRandomTables.int_max[++index&32767]%17-8,dy=OriginalRandomTables.int_7_minus_3[++index&32767],dz=OriginalRandomTables.int_max[++index&32767]%17-8;var trial=OriginalEnvironmentPaths.settle(wolf,new BlockPos(x+dx,y+dy,z+dz));if(!(nearest(trial)>far))continue;var path=wolf.getNavigation().createPath(trial,0,8);if(path==null)continue;var end=path.getEndNode();double distance=nearest(new BlockPos(end.x,end.y,end.z));if(!(distance>far))continue;far=distance;selected=path;}return selected;}
}
