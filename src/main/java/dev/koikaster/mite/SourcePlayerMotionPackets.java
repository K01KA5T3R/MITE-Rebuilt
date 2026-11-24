package dev.koikaster.mite;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
/** ka.a(Packet10Flying): before applying received coordinates, including look-only reset. */
public final class SourcePlayerMotionPackets {
 public static void received(Object listener,Object rawPacket){
  var player=((ServerGamePacketListenerImpl)listener).player;var packet=(ServerboundMovePlayerPacket)rawPacket;var state=(SourcePlayerMotion)player;
  state.mite$lastReceivedMotionX(packet.hasPosition()?packet.getX(player.getX())-player.getX():0);
  state.mite$lastReceivedMotionZ(packet.hasPosition()?packet.getZ(player.getZ())-player.getZ():0);
 }
 /** Modern mounted movement carries vehicle coordinates instead of R195's -999 motion packet. */
 public static void receivedVehicle(Object listener,Object rawPacket){var player=((ServerGamePacketListenerImpl)listener).player;var packet=(ServerboundMoveVehiclePacket)rawPacket;var vehicle=player.getRootVehicle();var state=(SourcePlayerMotion)player;state.mite$lastReceivedMotionX(vehicle==player?0:packet.getX()-vehicle.getX());state.mite$lastReceivedMotionZ(vehicle==player?0:packet.getZ()-vehicle.getZ());}
 private SourcePlayerMotionPackets(){}
}
