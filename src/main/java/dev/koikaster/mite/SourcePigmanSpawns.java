package dev.koikaster.mite;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** tn has no light/Nether-wart-floor condition. Physical checks run on the
 * actual entity after the existing source spawner selects its candidate. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SourcePigmanSpawns {
 @SubscribeEvent public static void placements(SpawnPlacementRegisterEvent event){event.register(EntityType.ZOMBIFIED_PIGLIN,SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,world,reason,pos,random)->world.getDifficulty()!=Difficulty.PEACEFUL,SpawnPlacementRegisterEvent.Operation.REPLACE);}
 private SourcePigmanSpawns(){}
}
