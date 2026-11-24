package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.entity.Mob;import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.network.*;import net.minecraftforge.network.simple.SimpleChannel;
/** Only the scheduled Mob.baseTick ambient call is routed here. */
public final class SourceWolfHowl {
 private static SimpleChannel channel;
 public static void register(SimpleChannel value){channel=value;value.messageBuilder(Message.class,33,NetworkDirection.PLAY_TO_CLIENT).encoder(Message::write).decoder(Message::read).consumerMainThread(Message::handle).add();}
 public static float nearest(Wolf wolf){double nearest=Double.MAX_VALUE;for(var player:wolf.level().players())if(!player.isSpectator()){double distance=player.distanceToSqr(wolf.getX(),wolf.getY(),wolf.getZ());if(distance<nearest)nearest=distance;}return nearest>65536?Float.MAX_VALUE:(float)Math.sqrt(nearest);}
 public static void ambient(Object raw){
  var mob=(Mob)raw;if(!(mob instanceof Wolf wolf)||!OriginalWolfOwners.handles(wolf)){mob.playAmbientSound();return;}if(wolf.level().isClientSide)return;
  float nearest=nearest(wolf);if(nearest<=16){wolf.playAmbientSound();return;}if(nearest>64||wolf.isSilent())return;
  var state=OriginalWolfOwners.owner(wolf);if(SourceContextSoundRules.howl(SourceSpecialSpawns.sourceDay(wolf.level()),state.attacking,state.flee.fleeing,wolf.getRandom()::nextFloat,()->SourceSpawnOutdoors.isOutdoors(wolf.level(),wolf.blockPosition())))send(wolf);
 }
 public static void send(Wolf wolf){if(channel==null||wolf.level().isClientSide||wolf.isSilent())return;var message=new Message(wolf.level().dimension().location(),(float)wolf.getX(),(float)wolf.getY(),(float)wolf.getZ(),OriginalWolfOwners.soundVolume(wolf),wolf.getVoicePitch());
  // Slight overdelivery keeps the inclusive client64 boundary; the client retains the exact16..64 filter.
  channel.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(message.x,message.y,message.z,65,wolf.level().dimension())),message);
 }
 public record Message(ResourceLocation dimension,float x,float y,float z,float volume,float pitch){
  static void write(Message m,FriendlyByteBuf b){b.writeResourceLocation(m.dimension);b.writeFloat(m.x);b.writeFloat(m.y);b.writeFloat(m.z);b.writeFloat(m.volume);b.writeFloat(m.pitch);}
  static Message read(FriendlyByteBuf b){return new Message(b.readResourceLocation(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat());}
  static void handle(Message m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->ClientContextSounds.howl(m));c.get().setPacketHandled(true);}
 }
 private SourceWolfHowl(){}
}
