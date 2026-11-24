package dev.koikaster.mite;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Immediate phase movement through the actual existing entity tracker.
 * The new bridge updates its relative codec and orientation along with the full
 * position packet. A stand-alone broadcast would corrupt the next relative move.
 */
public final class SourcePhasePositionUpdates {
 public interface Lookup {void mite$phasePosition(Entity actor);}
 public interface Tracked {void mite$phasePosition();}
 public static void flush(Entity actor){if(actor.level() instanceof ServerLevel level)((Lookup)(Object)level.getChunkSource().chunkMap).mite$phasePosition(actor);}
 public static void synchronize(Entity actor,VecDeltaCodec codec,Consumer<Packet<?>> broadcast){codec.setBase(actor.trackingPosition());broadcast.accept(new ClientboundTeleportEntityPacket(actor));}
 public static int rotation(float value){return Mth.floor(value*256.0f/360.0f);}
 private SourcePhasePositionUpdates(){}
}
