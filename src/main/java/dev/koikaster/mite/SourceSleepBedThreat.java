package dev.koikaster.mite;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.AABB;

/** uf.getHostileEntityNearBed and isHostileEntityDiggingNearBed. Unlike the
 * sleep ambush helper, the entry query retains the actor's door-opening flag. */
public final class SourceSleepBedThreat {
 public static Mob nearest(ServerLevel level,BlockPos bed){
  Mob nearest=null;double closest=1000;
  for(var mob:level.getEntitiesOfClass(Mob.class,box(bed),SourceSleepSpecies::hostile)){
   double distance=OriginalEnvironmentPaths.sq(mob.getBlockX()-bed.getX(),mob.getBlockY()-bed.getY(),mob.getBlockZ()-bed.getZ());
   if(nearest!=null&&!(distance<closest))continue;
   var navigation=mob.getNavigation();if(!(navigation instanceof SourceSleepNavigationAccess access))throw new IllegalStateException("Source sleep navigation bridge missing");
   if(!access.mite$sleepCanUpdatePath())continue;
   int maximum=24,radius=maximum+8;var origin=mob.blockPosition();var region=new PathNavigationRegion(level,origin.offset(-radius,-radius,-radius),origin.offset(radius,radius,radius));
   var path=new PathFinder(navigation.getNodeEvaluator(),maximum*16).findPath(region,mob,Set.of(bed),maximum,0,1);
   if(path==null||path.getEndNode()==null)continue;
   var end=path.getEndNode();if(Math.abs(end.x-bed.getX())>1||Math.abs(end.y-bed.getY())>1||Math.abs(end.z-bed.getZ())>1)continue;
   nearest=mob;closest=distance;
  }return nearest;
 }
 public static boolean digging(ServerLevel level,BlockPos bed){
  for(var mob:level.getEntitiesOfClass(Mob.class,box(bed))){
   var id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());if(id==null)continue;
   boolean zombie=id.getNamespace().equals("minecraft")&&Set.of("zombie","zombie_villager","zombified_piglin").contains(id.getPath())||id.getNamespace().equals(MiteReforged.ID)&&id.getPath().equals("revenant");
   if(zombie&&mob instanceof OriginalDigging.Host owner&&owner.originalDigging().state.destroying)return true;
  }return false;
 }
 private static AABB box(BlockPos p){return new AABB(p.getX()-8,p.getY()-5,p.getZ()-8,p.getX()+8,p.getY()+5,p.getZ()+8);}
 private SourceSleepBedThreat(){}
}
