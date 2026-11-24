package dev.koikaster.mite;
import java.util.*;import net.minecraft.core.BlockPos;import net.minecraft.server.level.ServerPlayer;import net.minecraft.tags.FluidTags;import net.minecraft.util.Mth;import net.minecraft.world.entity.vehicle.*;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.*;
/** uf.k ride-delta entry and jv's horizontal annulus / rider-foot water volume. */
public final class SourceAchievementMotion572 {
 private static final Map<ServerPlayer,BlockPos> RAIL=new WeakHashMap<>();
 private static boolean has(ServerPlayer p,String name){return SourceAchievementState561.has(PlayerState.data(p),SourceAchievementCatalog561.node(name).id());}
 public static void clear(ServerPlayer p){RAIL.remove(p);}
 public static void rail(Object raw,double x,double y,double z){if(!(raw instanceof ServerPlayer p)||!(p.getVehicle() instanceof AbstractMinecart)||Math.round((float)Math.sqrt(x*x+y*y+z*z)*100f)<=0)return;var at=BlockPos.containing(p.position());var first=RAIL.putIfAbsent(p,at);if(first!=null&&first.distSqr(at)>=1_000_000d)SourceAchievementService570.grant(p,"onARail");}
 public static boolean explorer(ServerPlayer p){var spawn=p.level().getSharedSpawnPos();double x=p.getX()-spawn.getX(),z=p.getZ()-spawn.getZ(),distance=x*x+z*z;return p.level().dimension()==Level.OVERWORLD&&distance>=1e8&&distance<4e8;}
 public static boolean seaworthy(ServerPlayer p){if(!(p.getVehicle() instanceof Boat))return false;int x=Mth.floor(p.getX()),y=Mth.floor(p.getY()+1e-4f),z=Mth.floor(p.getZ());var pos=new BlockPos.MutableBlockPos();for(int dy=-4;dy<=-1;dy++)for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){pos.set(x+dx,y+dy,z+dz);var state=p.level().getBlockState(pos);if(!(state.getBlock() instanceof LiquidBlock||state.is(Blocks.BUBBLE_COLUMN))||!state.getFluidState().is(FluidTags.WATER))return false;}return true;}
 public static void tick(ServerPlayer p){if(!p.isPassenger())clear(p);if(!has(p,"explorer")&&explorer(p))SourceAchievementService570.grant(p,"explorer");if(p.tickCount%10==0&&!has(p,"seaworthy")&&seaworthy(p))SourceAchievementService570.grant(p,"seaworthy");}
 private SourceAchievementMotion572(){}
}
