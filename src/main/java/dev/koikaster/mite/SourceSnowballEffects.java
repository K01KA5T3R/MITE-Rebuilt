package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Old hurt_without_red_tint_refreshed limb animation. The server emits the
 * hurt voice before parent harming-memory RNG; this packet never resets tint. */
public record SourceSnowballEffects(int entity) {
 private static SimpleChannel channel;
 static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceSnowballEffects.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceSnowballEffects::write).decoder(SourceSnowballEffects::read).consumerMainThread(SourceSnowballEffects::handle).add();}
 static void noTint(LivingEntity actor){channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->actor),new SourceSnowballEffects(actor.getId()));}
 private static void write(SourceSnowballEffects value,FriendlyByteBuf data){data.writeInt(value.entity);}
 private static SourceSnowballEffects read(FriendlyByteBuf data){return new SourceSnowballEffects(data.readInt());}
 private static void handle(SourceSnowballEffects value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->{var level=net.minecraft.client.Minecraft.getInstance().level;if(level!=null&&level.getEntity(value.entity) instanceof LivingEntity living)living.walkAnimation.setSpeed(1.5f);});context.get().setPacketHandled(true);}
 static void bob(InteractionHand hand){net.minecraft.client.Minecraft.getInstance().gameRenderer.itemInHandRenderer.itemUsed(hand);}
}
