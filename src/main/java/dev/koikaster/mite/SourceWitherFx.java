package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.core.particles.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.*;

/** of.setHealth -> bcw vampiric_gain; actual integer health gain only. */
public final class SourceWitherFx {
 public static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,MiteReforged.ID);
 public static final RegistryObject<SimpleParticleType> VAMPIRIC=PARTICLES.register("source_vampiric_gain",()->new SimpleParticleType(false));
 private static SimpleChannel channel;
 public static void register(SimpleChannel value){channel=value;value.messageBuilder(Gain.class,27,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeInt(p.entity)).decoder(b->new Gain(b.readInt())).consumerMainThread(SourceWitherFx::receive).add();}
 public static void heal(LivingEntity actor,float amount){
  float before=actor.getHealth();
  actor.heal(amount);
  if(!(actor.level() instanceof ServerLevel level)||(int)actor.getHealth()<=(int)before)return;
  if(actor instanceof SourceWitherBoss boss&&boss.ritual()!=null){
   level.sendParticles(ParticleTypes.SOUL,boss.getX(),boss.getY()+boss.getBbHeight(),boss.getZ(),6,.45,.1,.45,.06);
  }else channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->actor),new Gain(actor.getId()));
 }
 private static void receive(Gain value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourceWitherClientFx.gain(value.entity));context.get().setPacketHandled(true);}
 public record Gain(int entity){}
 private SourceWitherFx(){}
}
