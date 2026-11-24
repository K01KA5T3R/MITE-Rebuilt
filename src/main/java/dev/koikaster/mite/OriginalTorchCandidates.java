package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
/** original block-candidate spiral for the three original light roles; older log caller is untouched. */
public final class OriginalTorchCandidates {
 private static boolean column(Mob mob,List<BlockPos> out,int x,int z,int low,int high){for(int y=low;y<=high;y++){var at=new BlockPos(x,y,z);if(OriginalTorchInteraction.light(mob.level().getBlockState(at))){out.add(at);if(out.size()==8)return true;}}return false;}
 public static List<BlockPos> find(Mob mob){float ox=(float)mob.getX(),oy=(float)(mob.getY()+mob.getBbHeight()*.75f),oz=(float)mob.getZ();int x=net.minecraft.util.Mth.floor((double)ox),y=net.minecraft.util.Mth.floor((double)oy),z=net.minecraft.util.Mth.floor((double)oz),low=y-4,high=y+4,run=1;var out=new ArrayList<BlockPos>();if(column(mob,out,x,z,low,high))return out;while(true){if(run>=33){for(int i=1;i<run;i++){++x;if(column(mob,out,x,z,low,high))return out;}return out;}for(int i=1;i<=run;i++){++x;if(column(mob,out,x,z,low,high))return out;}for(int i=1;i<=run;i++){++z;if(column(mob,out,x,z,low,high))return out;}++run;for(int i=1;i<=run;i++){--x;if(column(mob,out,x,z,low,high))return out;}for(int i=1;i<=run;i++){--z;if(column(mob,out,x,z,low,high))return out;}++run;}}
 private OriginalTorchCandidates(){}
}
