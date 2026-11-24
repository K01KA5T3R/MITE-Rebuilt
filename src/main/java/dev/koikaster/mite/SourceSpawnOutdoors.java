package dev.koikaster.mite;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
/** R195 abw.isOutdoors copied with its fixed49-slot traversal and two upper
 * starts. Used only by new spawn selection; existing combat outdoor owners
 * remain independent. Modern build limits and collision shapes adapt source
 * passability/standard-form precipitation flags. Cache lasts for one query. */
final class SourceSpawnOutdoors {
 private final Level level;private final Map<Long,Integer> heights=new HashMap<>();
 private SourceSpawnOutdoors(Level level){this.level=level;}
 static boolean isOutdoors(Level level,BlockPos pos){return new SourceSpawnOutdoors(level).outdoors(pos.getX(),pos.getY(),pos.getZ(),true);}
 private boolean passable(int x,int y,int z,boolean liquids){return SourceTroopPlacement.passable(level,new BlockPos(x,y,z),liquids);}
 private int precipitationHeight(int x,int z){
  long key=(long)x<<32|z&0xffffffffL;
  return heights.computeIfAbsent(key,unused->{
   var at=new BlockPos.MutableBlockPos(x,0,z);
   if(!level.hasChunkAt(at))return Integer.MAX_VALUE;
   for(int y=level.getMaxBuildHeight()-1;y>=level.getMinBuildHeight();--y){
    at.setY(y);var state=level.getBlockState(at);
    if(SourceBlockForms716.precipitation(state,level,at))return y+1;
   }
   return level.getMinBuildHeight();
  });
 }
    private boolean outdoors(int x, int y, int z, boolean initial_call) {
        if (level.dimensionType().hasCeiling()) {
            return false;
        }
        boolean[] check_block = new boolean[49];
        if (initial_call && this.passable(x, y + 1, z, false)) {
            if (this.outdoors(x, y + 1, z, false)) {
                return true;
            }
            if (this.passable(x, y + 2, z, false) && this.outdoors(x, y + 2, z, false)) {
                return true;
            }
        }
        if (this.passable(x, y, z, false) && this.precipitationHeight(x, z) <= y) {
            return true;
        }
        if (this.passable(x + 1, y, z, false)) {
            if (this.precipitationHeight(x + 1, z) <= y) {
                return true;
            }
            check_block[18] = true;
            check_block[26] = true;
            check_block[32] = true;
        }
        if (this.passable(x - 1, y, z, false)) {
            if (this.precipitationHeight(x - 1, z) <= y) {
                return true;
            }
            check_block[16] = true;
            check_block[22] = true;
            check_block[30] = true;
        }
        if (this.passable(x, y, z + 1, false)) {
            if (this.precipitationHeight(x, z + 1) <= y) {
                return true;
            }
            check_block[30] = true;
            check_block[38] = true;
            check_block[32] = true;
        }
        if (this.passable(x, y, z - 1, false)) {
            if (this.precipitationHeight(x, z - 1) <= y) {
                return true;
            }
            check_block[16] = true;
            check_block[10] = true;
            check_block[18] = true;
        }
        if (check_block[18] && this.passable(x + 1, y, z - 1, false)) {
            if (this.precipitationHeight(x + 1, z - 1) <= y) {
                return true;
            }
            check_block[11] = true;
            check_block[19] = true;
        }
        if (check_block[32] && this.passable(x + 1, y, z + 1, false)) {
            if (this.precipitationHeight(x + 1, z + 1) <= y) {
                return true;
            }
            check_block[33] = true;
            check_block[39] = true;
        }
        if (check_block[30] && this.passable(x - 1, y, z + 1, false)) {
            if (this.precipitationHeight(x - 1, z + 1) <= y) {
                return true;
            }
            check_block[29] = true;
            check_block[37] = true;
        }
        if (check_block[16] && this.passable(x - 1, y, z - 1, false)) {
            if (this.precipitationHeight(x - 1, z - 1) <= y) {
                return true;
            }
            check_block[9] = true;
            check_block[15] = true;
        }
        if (check_block[26] && this.passable(x + 2, y, z, false)) {
            if (this.precipitationHeight(x + 2, z) <= y) {
                return true;
            }
            check_block[19] = true;
            check_block[27] = true;
            check_block[33] = true;
        }
        if (check_block[38] && this.passable(x, y, z + 2, false)) {
            if (this.precipitationHeight(x, z + 2) <= y) {
                return true;
            }
            check_block[37] = true;
            check_block[39] = true;
            check_block[45] = true;
        }
        if (check_block[22] && this.passable(x - 2, y, z, false)) {
            if (this.precipitationHeight(x - 2, z) <= y) {
                return true;
            }
            check_block[15] = true;
            check_block[21] = true;
            check_block[29] = true;
        }
        if (check_block[10] && this.passable(x, y, z - 2, false)) {
            if (this.precipitationHeight(x, z - 2) <= y) {
                return true;
            }
            check_block[3] = true;
            check_block[9] = true;
            check_block[11] = true;
        }
        if (check_block[11] && this.passable(x + 1, y, z - 2, false)) {
            if (this.precipitationHeight(x + 1, z - 2) <= y) {
                return true;
            }
            check_block[12] = true;
        }
        if (check_block[19] && this.passable(x + 2, y, z - 1, false)) {
            if (this.precipitationHeight(x + 2, z - 1) <= y) {
                return true;
            }
            check_block[12] = true;
        }
        if (check_block[33] && this.passable(x + 2, y, z + 1, false)) {
            if (this.precipitationHeight(x + 2, z + 1) <= y) {
                return true;
            }
            check_block[40] = true;
        }
        if (check_block[39] && this.passable(x + 1, y, z + 2, false)) {
            if (this.precipitationHeight(x + 1, z + 2) <= y) {
                return true;
            }
            check_block[40] = true;
        }
        if (check_block[37] && this.passable(x - 1, y, z + 2, false)) {
            if (this.precipitationHeight(x - 1, z + 2) <= y) {
                return true;
            }
            check_block[36] = true;
        }
        if (check_block[29] && this.passable(x - 2, y, z + 1, false)) {
            if (this.precipitationHeight(x - 2, z + 1) <= y) {
                return true;
            }
            check_block[36] = true;
        }
        if (check_block[15] && this.passable(x - 2, y, z - 1, false)) {
            if (this.precipitationHeight(x - 2, z - 1) <= y) {
                return true;
            }
            check_block[8] = true;
        }
        if (check_block[9] && this.passable(x - 1, y, z - 2, false)) {
            if (this.precipitationHeight(x - 1, z - 2) <= y) {
                return true;
            }
            check_block[8] = true;
        }
        if (check_block[12] && this.passable(x + 2, y, z - 2, false) && this.precipitationHeight(x + 2, z - 2) <= y) {
            return true;
        }
        if (check_block[40] && this.passable(x + 2, y, z + 2, false) && this.precipitationHeight(x + 2, z + 2) <= y) {
            return true;
        }
        if (check_block[36] && this.passable(x - 2, y, z + 2, false) && this.precipitationHeight(x - 2, z + 2) <= y) {
            return true;
        }
        if (check_block[8] && this.passable(x - 2, y, z - 2, false) && this.precipitationHeight(x - 2, z - 2) <= y) {
            return true;
        }
        if (check_block[27] && this.passable(x + 3, y, z, false) && this.precipitationHeight(x + 3, z) <= y) {
            return true;
        }
        if (check_block[45] && this.passable(x, y, z + 3, false) && this.precipitationHeight(x, z + 3) <= y) {
            return true;
        }
        if (check_block[21] && this.passable(x - 3, y, z, false) && this.precipitationHeight(x - 3, z) <= y) {
            return true;
        }
        return check_block[3] && this.passable(x, y, z - 3, false) && this.precipitationHeight(x, z - 3) <= y;
    }

}
