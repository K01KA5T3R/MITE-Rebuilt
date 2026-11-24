package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
/** abw nearest block candidates: source center-first/Y-ascending outward spiral; deliberately no distance sort. */
public final class OriginalLogCandidates {
 private static boolean column(Mob mob,List<BlockPos> result,int x,int z,int low,int high,int maximum){for(int y=low;y<=high;y++)if(mob.level().getBlockState(new BlockPos(x,y,z)).is(BlockTags.LOGS_THAT_BURN)){result.add(new BlockPos(x,y,z));if(result.size()==maximum)return true;}return false;}
 public static List<BlockPos> find(Mob mob,int horizontal,int vertical,int maximum){float ox=(float)mob.getX(),oy=(float)(mob.getY()+mob.getBbHeight()*.75f),oz=(float)mob.getZ();int x=net.minecraft.util.Mth.floor((double)ox),y=net.minecraft.util.Mth.floor((double)oy),z=net.minecraft.util.Mth.floor((double)oz),low=y-vertical,high=y+vertical,width=horizontal*2+1,run=1;var out=new ArrayList<BlockPos>();if(column(mob,out,x,z,low,high,maximum))return out;while(true){if(run>=width){for(int i=1;i<run;i++){++x;if(column(mob,out,x,z,low,high,maximum))return out;}return out;}for(int i=1;i<=run;i++){++x;if(column(mob,out,x,z,low,high,maximum))return out;}for(int i=1;i<=run;i++){++z;if(column(mob,out,x,z,low,high,maximum))return out;}++run;for(int i=1;i<=run;i++){--x;if(column(mob,out,x,z,low,high,maximum))return out;}for(int i=1;i<=run;i++){--z;if(column(mob,out,x,z,low,high,maximum))return out;}++run;}}
 private OriginalLogCandidates(){}
}
