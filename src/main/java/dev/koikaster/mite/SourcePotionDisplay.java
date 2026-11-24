package dev.koikaster.mite;
import java.util.*;import java.util.function.Supplier;
import net.minecraft.core.BlockPos;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;import net.minecraft.world.effect.MobEffectInstance;import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.entity.player.PlayerEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;import net.minecraftforge.network.simple.SimpleChannel;

/** Additional display colors only. Native metadata, effects, event data and saved NBT stay unchanged. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourcePotionDisplay {
 private static SimpleChannel channel;
 private record Pair(int nativeColor,int classicColor){}
 private static final Map<Entity,Pair> COLORS=new WeakHashMap<>();
 public static void register(SimpleChannel value){channel=value;value.messageBuilder(EntityColor.class,31,NetworkDirection.PLAY_TO_CLIENT).encoder(EntityColor::write).decoder(EntityColor::read).consumerMainThread(EntityColor::handle).add();value.messageBuilder(SplashColor.class,32,NetworkDirection.PLAY_TO_CLIENT).encoder(SplashColor::write).decoder(SplashColor::read).consumerMainThread(SplashColor::handle).add();}
 private static EntityColor packet(Entity entity,Pair pair){return new EntityColor(entity.level().dimension().location(),entity.getId(),entity.getUUID(),pair.nativeColor,pair.classicColor);}
 private static void capture(Entity entity,int nativeColor,int classicColor){
  if(!(entity.level() instanceof ServerLevel)||channel==null)return;var pair=new Pair(nativeColor,classicColor);var before=COLORS.put(entity,pair);if(!pair.equals(before))channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entity),packet(entity,pair));
 }
 /** Replaces only display-color calculations; preserves the native result on both sides. */
 public static int captureMix(Collection<MobEffectInstance> effects,Object raw){int nativeColor=PotionUtils.getColor(effects);var entity=(Entity)raw;if(!entity.level().isClientSide)capture(entity,nativeColor,SourcePotionPalette.mix(effects));return nativeColor;}
 public static void fixed(Object raw,int color){var entity=(Entity)raw;if(!entity.level().isClientSide)capture(entity,color,color);}
 /** Invoked where LivingEntity/Arrow/AreaEffectCloud consume an already synchronized color. */
 public static int entityColor(int nativeColor,Object raw){var entity=(Entity)raw;return entity.level().isClientSide?dev.koikaster.mite.visual.ClientPotionDisplay.entityColor(entity,nativeColor):nativeColor;}
 public static int splash(Object rawStack,Object rawEntity){
  var stack=(ItemStack)rawStack;var entity=(Entity)rawEntity;int nativeColor=PotionUtils.getColor(stack);
  if(entity.level() instanceof ServerLevel&&channel!=null){var tag=stack.getTag();int classicColor=tag!=null&&tag.contains(PotionUtils.TAG_CUSTOM_POTION_COLOR,99)?nativeColor:SourcePotionPalette.mix(PotionUtils.getMobEffects(stack));
   // Native EMPTY is magenta, even if custom effects exist without a base potion.
   if(PotionUtils.getPotion(stack)==net.minecraft.world.item.alchemy.Potions.EMPTY)classicColor=nativeColor;
   if(classicColor!=nativeColor){int event=PotionUtils.getPotion(stack).hasInstantEffects()?2007:2002;var message=new SplashColor(entity.level().dimension().location(),entity.blockPosition(),event,nativeColor,classicColor);channel.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(entity.blockPosition().getX(),entity.blockPosition().getY(),entity.blockPosition().getZ(),64,entity.level().dimension())),message);}
  }return nativeColor;
 }
 @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event){var entity=event.getTarget();var pair=COLORS.get(entity);if(channel!=null&&pair!=null&&event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)channel.send(PacketDistributor.PLAYER.with(()->player),packet(entity,pair));}
 public record EntityColor(ResourceLocation dimension,int entity,UUID uuid,int nativeColor,int classicColor){
  static void write(EntityColor v,FriendlyByteBuf b){b.writeResourceLocation(v.dimension);b.writeVarInt(v.entity);b.writeUUID(v.uuid);b.writeInt(v.nativeColor);b.writeInt(v.classicColor);}
  static EntityColor read(FriendlyByteBuf b){return new EntityColor(b.readResourceLocation(),b.readVarInt(),b.readUUID(),b.readInt(),b.readInt());}
  static void handle(EntityColor v,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->dev.koikaster.mite.visual.ClientPotionDisplay.receive(v));c.get().setPacketHandled(true);}
 }
 public record SplashColor(ResourceLocation dimension,BlockPos position,int event,int nativeColor,int classicColor){
  public SplashColor{position=position.immutable();if(event!=2002&&event!=2007)throw new IllegalArgumentException("Potion event");}
  static void write(SplashColor v,FriendlyByteBuf b){b.writeResourceLocation(v.dimension);b.writeBlockPos(v.position);b.writeVarInt(v.event);b.writeInt(v.nativeColor);b.writeInt(v.classicColor);}
  static SplashColor read(FriendlyByteBuf b){return new SplashColor(b.readResourceLocation(),b.readBlockPos(),b.readVarInt(),b.readInt(),b.readInt());}
  static void handle(SplashColor v,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->dev.koikaster.mite.visual.ClientPotionDisplay.receive(v));c.get().setPacketHandled(true);}
 }
 private SourcePotionDisplay(){}
}
