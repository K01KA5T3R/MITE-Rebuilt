package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
/** Original uf/jv/bdi/bcw/ka shadow vision lifecycle, including max-on-receive and client save feedback. */
public final class OriginalShadowVision {
 public static void add(ServerPlayer player,float amount){var state=(SourceShadowVision)player;state.mite$visionDimming(state.mite$visionDimming()+amount);}
 public static void tick(Object actor){if(!(actor instanceof Player player))return;var state=(SourceShadowVision)player;if(player.level().isClientSide){float dim=state.mite$visionDimming();state.mite$visionDimming(dim<.01f?0:dim>2?2:dim-.01f);if(player==net.minecraft.client.Minecraft.getInstance().player&&state.mite$visionDimming()>0&&player.tickCount%10==0)Network.visionReport(state.mite$visionDimming());}else if(player instanceof ServerPlayer server&&state.mite$visionDimming()>0){Network.shadowVision(server,state.mite$visionDimming());state.mite$visionDimming(0);}}
 public static void receive(float value){var player=net.minecraft.client.Minecraft.getInstance().player;if(player==null)return;var state=(SourceShadowVision)player;if(state.mite$visionDimming()<value)state.mite$visionDimming(value);}
 public static void report(ServerPlayer player,float value){if(!Float.isFinite(value))return;((SourceShadowVision)player).mite$reportedVision(value<.2f?0:Math.min(value,2));}
 public static void save(Object actor,Object rawTag){if(!(actor instanceof Player))return;var state=(SourceShadowVision)actor;var tag=(CompoundTag)rawTag;tag.putFloat("vision_dimming",state.mite$visionDimming());if(actor instanceof ServerPlayer&&state.mite$reportedVision()>=.2f)tag.putFloat("vision_dimming_on_client",state.mite$reportedVision());}
 public static void load(Object actor,Object rawTag){if(!(actor instanceof Player))return;var tag=(CompoundTag)rawTag;var state=(SourceShadowVision)actor;state.mite$visionDimming(tag.getFloat("vision_dimming"));if(actor instanceof ServerPlayer&&tag.contains("vision_dimming_on_client"))state.mite$visionDimming(tag.getFloat("vision_dimming_on_client"));}
 private OriginalShadowVision(){}
}
