package dev.koikaster.mite;
import java.util.*;import net.minecraft.core.BlockPos;import net.minecraft.util.Mth;import net.minecraft.world.entity.animal.Ocelot;import net.minecraft.world.level.block.Blocks;
/** Existing primary center/Y-first fire spiral, scoped to Ocelot without changing the frozen Wolf owner. */
final class SourceOcelotFirePaths {
 private static boolean column(Ocelot wolf,List<BlockPos> result,int x,int z,int low,int high){for(int y=low;y<=high;y++)if(wolf.level().getBlockState(new BlockPos(x,y,z)).is(Blocks.FIRE)){result.add(new BlockPos(x,y,z));if(result.size()==16)return true;}return false;}
 public static List<BlockPos> fires(Ocelot wolf){float ox=(float)wolf.getX(),oy=(float)(wolf.getY()+wolf.getBbHeight()*.75f),oz=(float)wolf.getZ();int x=Mth.floor(ox),y=Mth.floor(oy),z=Mth.floor(oz),low=y-4,high=y+4,run=1;var result=new ArrayList<BlockPos>();if(column(wolf,result,x,z,low,high))return result;while(true){if(run>=33){for(int i=1;i<run;i++){++x;if(column(wolf,result,x,z,low,high))return result;}return result;}for(int i=1;i<=run;i++){++x;if(column(wolf,result,x,z,low,high))return result;}for(int i=1;i<=run;i++){++z;if(column(wolf,result,x,z,low,high))return result;}++run;for(int i=1;i<=run;i++){--x;if(column(wolf,result,x,z,low,high))return result;}for(int i=1;i<=run;i++){--z;if(column(wolf,result,x,z,low,high))return result;}++run;}}
 private SourceOcelotFirePaths(){}
}
