package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.LinkedHashMap;
import net.minecraft.world.entity.Entity;
/** Uses the existing mod channel. Source entity FX arrives before the projectile removal. */
public final class SourceWebEffects {
 private static SimpleChannel channel;
 private static final LinkedHashMap<Integer,Entity> locallyRetired=new LinkedHashMap<>();
 public static void register(SimpleChannel existingChannel,int packetId){channel=existingChannel;channel.messageBuilder(Packet.class,packetId,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceWebEffects::write).decoder(SourceWebEffects::read).consumerMainThread(SourceWebEffects::handle).add();SourceWebProjectile.effects(SourceWebEffects::send);}
 public static void send(SourceWebProjectile entity,SourceWebProjectile.ImpactFx kind){if(channel==null)throw new IllegalStateException("Web FX packet is not registered");var motion=entity.getDeltaMovement();var packet=new Packet(entity.getId(),kind.ordinal(),entity.getX(),entity.getY(),entity.getZ(),entity.getBbWidth(),entity.getBbHeight(),entity.getBoundingBox().minY,motion.x,motion.y,motion.z,entity.getUUID().getLeastSignificantBits()^entity.level().getGameTime());channel.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),packet);}
 /** Native client prediction may already remove the web. Keep its exact RNG when available;
  * the self-contained impact geometry also survives the native S2C removal packet. */
 public static void retainClientActor(SourceWebProjectile actor){if(!actor.level().isClientSide)return;locallyRetired.put(actor.getId(),actor);while(locallyRetired.size()>128)locallyRetired.remove(locallyRetired.keySet().iterator().next());}
 public static Entity takeRetiredClientActor(int entityId){return locallyRetired.remove(entityId);}
 public record Packet(int entity,int kind,double x,double y,double z,float width,float height,double minY,double motionX,double motionY,double motionZ,long fallbackSeed){public Packet{if(kind<0||kind>=SourceWebProjectile.ImpactFx.values().length)throw new IllegalArgumentException("Unknown source web impact FX");}}
 public static void write(Packet v,FriendlyByteBuf b){b.writeInt(v.entity);b.writeByte(v.kind);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);b.writeFloat(v.width);b.writeFloat(v.height);b.writeDouble(v.minY);b.writeDouble(v.motionX);b.writeDouble(v.motionY);b.writeDouble(v.motionZ);b.writeLong(v.fallbackSeed);}
 public static Packet read(FriendlyByteBuf b){return new Packet(b.readInt(),b.readUnsignedByte(),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readLong());}
 public static void handle(Packet value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourceWebClientEffects.play(value));context.get().setPacketHandled(true);}
 private SourceWebEffects(){}
}
