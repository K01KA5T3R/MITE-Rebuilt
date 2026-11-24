package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
/** tj's actor RNG owns chance1/20; no second world-RNG roll or native floor rule. */
public final class SourceGhastContent {
 static void placements(SpawnPlacementRegisterEvent event){event.register(EntityType.GHAST,SpawnPlacements.Type.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->true,SpawnPlacementRegisterEvent.Operation.REPLACE);}
 private SourceGhastContent(){}
}
