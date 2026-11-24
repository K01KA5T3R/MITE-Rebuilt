package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Source placement rule and retained modern ritual constructor transport. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceEndPortalPlacement {
 private static final String INTERRUPTED_CREDITS=MiteReforged.ID+":interrupted_end_credits";
 public static void placed(BlockState state,Level level,BlockPos position,BlockState old,boolean moved){if(!level.isClientSide&&!SourcePortalPlacementRules.mayExist(SourcePortalPlacementRules.constructing(),level.dimension()==Level.OVERWORLD))level.removeBlock(position,false);}
 public static void nativeExit(EndDragonFight fight,boolean active){try(var scope=SourcePortalPlacementRules.allow()){((SourcePortalBuilderAccess)(Object)fight).mite$nativeExit072(active);}}
 public static ServerLevel legacyTarget(MinecraftServer server,ResourceKey<Level> target,Level source,BlockPos position,Entity entity){
  if(source.dimension()==Level.OVERWORLD&&target==Level.END&&!WorldContentMode.rebuilt(source))return server.getLevel(target);
  if(!(source instanceof ServerLevel end)||end.dimension()!=Level.END||BossArenaPortals.blocked(end,position)
      ||DragonArrivalReturn.travel(end,position,entity))return null;
  return server.getLevel(target);
 }
 /** Normal End exits open credits each time. An interrupted exit can finish on reconnect. */
 public static boolean resumeInterruptedCredits(ServerPlayer player){
  var data=player.getPersistentData();
  boolean resume=data.contains(INTERRUPTED_CREDITS,Tag.TAG_LONG)
   &&BlockPos.of(data.getLong(INTERRUPTED_CREDITS)).distManhattan(player.blockPosition())<=1;
  data.remove(INTERRUPTED_CREDITS);
  return resume;
 }
 @SubscribeEvent public static void saveInterruptedCredits(PlayerEvent.PlayerLoggedOutEvent event){
  if(event.getEntity() instanceof ServerPlayer player&&player.wonGame&&player.serverLevel().dimension()==Level.END)
   player.getPersistentData().putLong(INTERRUPTED_CREDITS,player.blockPosition().asLong());
 }
 private SourceEndPortalPlacement(){}
}
