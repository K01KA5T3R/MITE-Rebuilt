package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class GiantSkeletonArena {
    private GiantSkeletonArena() {}

    static Vec3 summonSite(ServerLevel level, ServerPlayer player) {
        if (level.dimension()!=WorldEvents.UNDERWORLD || player.isSpectator() || !player.isAlive()) return null;
        Vec3 front=Vec3.directionFromRotation(0,player.getYRot());
        for(double length:new double[]{10,12,8}) {
            var ray=player.position().add(front.scale(length));
            Vec3 floor=BossAttackGeometry.ground(level,player,ray.add(0,2,0),5);
            if(floor!=null && clear(level,floor,3,7) && waveSites(level,player,floor).size()>=8
                    && BossAttackGeometry.visible(level,player,player.getEyePosition(),floor.add(0,2,0))) return floor;
        }
        return null;
    }

    static List<Vec3> waveSites(ServerLevel level, net.minecraft.world.entity.Entity anchor, Vec3 center) {
        var result=new ArrayList<Vec3>();
        for(double radius:new double[]{6.5,10.5}) {
            for(int slice=0;slice<12;slice++) {
                var desired=center.add(BossAttackGeometry.flatDirection(slice*Math.PI/6).scale(radius));
                var floor=BossAttackGeometry.ground(level,anchor,desired.add(0,2.5,0),6);
                if(floor!=null && clear(level,floor,1.05,2.7)
                        && BossAttackGeometry.visible(level,anchor,center.add(0,1.45,0),floor.add(0,1.10,0))) result.add(floor);
            }
        }
        return result;
    }

    private static boolean clear(ServerLevel level,Vec3 site,double width,double height) {
        BlockPos below=BlockPos.containing(site.add(0,-.025,0));
        if(!level.hasChunkAt(below)||level.canSeeSky(below.above())
                || !level.getBlockState(below).isFaceSturdy(level,below,Direction.UP)) return false;
        double half=width*.5;
        var space=new AABB(site.x-half,site.y+.02,site.z-half,site.x+half,site.y+height,site.z+half);
        return level.hasChunksAt(BlockPos.containing(space.minX,space.minY,space.minZ),BlockPos.containing(space.maxX,space.maxY,space.maxZ))
            && level.getWorldBorder().isWithinBounds(space) && level.noCollision(space) && !level.containsAnyLiquid(space);
    }
}
