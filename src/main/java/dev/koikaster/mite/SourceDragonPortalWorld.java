package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;

/** Native block adapter for sk.c, with aqw's actual placement exception. */
public final class SourceDragonPortalWorld implements SourceDragonPortal.Host,AutoCloseable {
 private final ServerLevel world;private SourcePortalPlacementRules.Scope permission;
 public SourceDragonPortalWorld(ServerLevel world){this.world=world;}
 @Override public void allowPortalPlacement(boolean value){if(value){if(permission==null)permission=SourcePortalPlacementRules.allow();}else close();}
 @Override public void block(int x,int y,int z,int id){var state=switch(id){case 7->Blocks.BEDROCK.defaultBlockState();case 119->Blocks.END_PORTAL.defaultBlockState();case 122->Blocks.DRAGON_EGG.defaultBlockState();default->Blocks.AIR.defaultBlockState();};world.setBlock(new BlockPos(x,y,z),state,3);}
 @Override public void torch(int x,int y,int z,String face){world.setBlock(new BlockPos(x,y,z),Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,Direction.valueOf(face)),3);}
 @Override public void close(){if(permission!=null){permission.close();permission=null;}}
}
