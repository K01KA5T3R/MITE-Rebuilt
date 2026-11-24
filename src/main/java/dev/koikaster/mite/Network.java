package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class Network {
 private static final String VERSION="156";
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Content.id("main"),()->VERSION,VERSION::equals,VERSION::equals);
 static { WorldContentTransport.register(CHANNEL); }
 public static void init(){SourceTournamentTransport.register(CHANNEL);BossCombatVisual.register(CHANNEL);BossSkullTrail.register(CHANNEL);BossMusicPacket.register(CHANNEL);DragonIntroPacket.register(CHANNEL);WitherIntroPacket.register(CHANNEL);GroundSeaBossIntroPacket.register(CHANNEL);Incapacitation2123.register(CHANNEL);CooperativeMining2133.register(CHANNEL);LocalWeatherTransport1981.register(CHANNEL);MountedCrossbowTransport1949.register(CHANNEL);DirectionalMining1765.register(CHANNEL);SourceRootTransport1001.register(CHANNEL,35);SourceToolBreakTransport1058.register(CHANNEL,38);SourceCaveAmbient.register(CHANNEL);SourceWolfHowl.register(CHANNEL);SourcePotionDisplay.register(CHANNEL);SourceBowNock826.register(CHANNEL,30);SourceEggUse487.register(CHANNEL,29);SourceSnowballEffects.register(CHANNEL,28);SourceWitherFx.register(CHANNEL);AdvancedEnchantNetwork.register(CHANNEL);SourceWitchCackle.register(CHANNEL,24);SourceFireballReversal058.register(CHANNEL,23);SourceCreeperFragments.register(CHANNEL,22);SourceCreatureFragments.register(CHANNEL,21);SourceExplosionImpulse.register(CHANNEL,20);SourceChemicalFx.register(CHANNEL,19);SourceSummonedFx.register(CHANNEL,18);SourcePortalTransition.register(CHANNEL,15);SourceWebEffects.register(CHANNEL,12);CHANNEL.messageBuilder(SourcePhaseFx.class,13,NetworkDirection.PLAY_TO_CLIENT).encoder(SourcePhaseFx::write).decoder(SourcePhaseFx::read).consumerMainThread(SourcePhaseFx::handle).add();CHANNEL.messageBuilder(WolfHealFx.class,14,NetworkDirection.PLAY_TO_CLIENT).encoder(WolfHealFx::write).decoder(WolfHealFx::read).consumerMainThread(WolfHealFx::handle).add();CHANNEL.messageBuilder(RepairFx.class,11,NetworkDirection.PLAY_TO_CLIENT).encoder(RepairFx::write).decoder(RepairFx::read).consumerMainThread(RepairFx::handle).add();CHANNEL.messageBuilder(EnvironmentSmoke.class,8,NetworkDirection.PLAY_TO_CLIENT).encoder(EnvironmentSmoke::write).decoder(EnvironmentSmoke::read).consumerMainThread(EnvironmentSmoke::handle).add();CHANNEL.messageBuilder(ShadowVision.class,9,NetworkDirection.PLAY_TO_CLIENT).encoder(ShadowVision::write).decoder(ShadowVision::read).consumerMainThread(ShadowVision::handle).add();CHANNEL.messageBuilder(VisionReport.class,10,NetworkDirection.PLAY_TO_SERVER).encoder(VisionReport::write).decoder(VisionReport::read).consumerMainThread(VisionReport::handle).add();CHANNEL.messageBuilder(EnvironmentSteam.class,7,NetworkDirection.PLAY_TO_CLIENT).encoder(EnvironmentSteam::write).decoder(EnvironmentSteam::read).consumerMainThread(EnvironmentSteam::handle).add();CHANNEL.messageBuilder(DespawnCaps.class,6,NetworkDirection.PLAY_TO_SERVER).encoder(DespawnCaps::write).decoder(DespawnCaps::read).consumerMainThread(DespawnCaps::handle).add();CHANNEL.messageBuilder(State.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder(State::write).decoder(State::read).consumerMainThread(State::handle).add();CHANNEL.messageBuilder(CraftingState.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(CraftingState::write).decoder(CraftingState::read).consumerMainThread(CraftingState::handle).add();CHANNEL.messageBuilder(VesselControl.class,2,NetworkDirection.PLAY_TO_SERVER).encoder(VesselControl::write).decoder(VesselControl::read).consumerMainThread(VesselControl::handle).add();CHANNEL.messageBuilder(RepairName.class,3,NetworkDirection.PLAY_TO_SERVER).encoder(RepairName::write).decoder(RepairName::read).consumerMainThread(RepairName::handle).add();CHANNEL.messageBuilder(CurseLearning.class,4,NetworkDirection.PLAY_TO_SERVER).encoder(CurseLearning::write).decoder(CurseLearning::read).consumerMainThread(CurseLearning::handle).add();CHANNEL.messageBuilder(CurseNotice.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder(CurseNotice::write).decoder(CurseNotice::read).consumerMainThread(CurseNotice::handle).add();}
 public static void curseLearn(int id){CHANNEL.sendToServer(new CurseLearning(id));}
 public static void despawnCaps(int[] ids,short[] caps){CHANNEL.sendToServer(new DespawnCaps(ids,caps));}
 public record DespawnCaps(int[] ids,short[] caps){
  public DespawnCaps{if(ids.length>64||ids.length!=caps.length)throw new IllegalArgumentException("Source despawn batch size");ids=ids.clone();caps=caps.clone();}
  static void write(DespawnCaps packet,FriendlyByteBuf buffer){buffer.writeByte(packet.ids.length);for(int i=0;i<packet.ids.length;i++){buffer.writeInt(packet.ids[i]);buffer.writeShort(packet.caps[i]);}}
  static DespawnCaps read(FriendlyByteBuf buffer){int count=buffer.readUnsignedByte();if(count>64)throw new io.netty.handler.codec.DecoderException("Source despawn batch exceeds64");int[] ids=new int[count];short[] caps=new short[count];for(int i=0;i<count;i++){ids[i]=buffer.readInt();caps[i]=buffer.readShort();}return new DespawnCaps(ids,caps);}
  static void handle(DespawnCaps packet,Supplier<NetworkEvent.Context> context){var player=context.get().getSender();if(player!=null)SourcePerceptionReports.apply(player,packet.ids);context.get().setPacketHandled(true);}
 }
 public record CurseLearning(int id){
  static void write(CurseLearning value,FriendlyByteBuf buffer){buffer.writeVarInt(value.id);}
  static CurseLearning read(FriendlyByteBuf buffer){return new CurseLearning(buffer.readVarInt());}
  static void handle(CurseLearning value,Supplier<NetworkEvent.Context> context){var player=context.get().getSender();if(player!=null&&OriginalCurses.definition(value.id)!=null)OriginalCurseEffects.has(player,value.id,true);context.get().setPacketHandled(true);}
 }
 public static void curseNotice(ServerPlayer player,int kind,int id){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->player),new CurseNotice(player.getId(),kind,id));}
 public record CurseNotice(int entity,int kind,int id){
  static void write(CurseNotice value,FriendlyByteBuf buffer){buffer.writeVarInt(value.entity);buffer.writeByte(value.kind);buffer.writeByte(value.id);}
  static CurseNotice read(FriendlyByteBuf buffer){return new CurseNotice(buffer.readVarInt(),buffer.readUnsignedByte(),buffer.readUnsignedByte());}
  static void handle(CurseNotice value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->ClientCurseEffects.notice(value.entity,value.kind,value.id));context.get().setPacketHandled(true);}
 }
 public static void repairName(int menu,String name){CHANNEL.sendToServer(new RepairName(menu,name));}
 public record RepairName(int menu,String name){
  static void write(RepairName value,FriendlyByteBuf buffer){buffer.writeVarInt(value.menu);buffer.writeUtf(value.name,40);}
  static RepairName read(FriendlyByteBuf buffer){return new RepairName(buffer.readVarInt(),buffer.readUtf(40));}
  static void handle(RepairName value,Supplier<NetworkEvent.Context> context){var player=context.get().getSender();if(player!=null&&player.containerMenu.containerId==value.menu&&player.containerMenu instanceof SourceRepairMenu menu&&menu.stillValid(player))menu.rename(value.name);context.get().setPacketHandled(true);}
 }
 public static void vesselControl(net.minecraft.world.InteractionHand hand,boolean ctrl){CHANNEL.sendToServer(new VesselControl(hand,ctrl));}
 public record VesselControl(net.minecraft.world.InteractionHand hand,boolean ctrl){
  static void write(VesselControl value,FriendlyByteBuf buffer){buffer.writeEnum(value.hand);buffer.writeBoolean(value.ctrl);}
  static VesselControl read(FriendlyByteBuf buffer){return new VesselControl(buffer.readEnum(net.minecraft.world.InteractionHand.class),buffer.readBoolean());}
  static void handle(VesselControl value,Supplier<NetworkEvent.Context> context){var player=context.get().getSender();if(player!=null&&ClassicVessels.handled(player.getItemInHand(value.hand)))ClassicVessels.control(player,value.hand,value.ctrl);context.get().setPacketHandled(true);}
 }
 public static void crafting(ServerPlayer player,String key,int progress,int total){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new CraftingState(key,progress,total));}
 public record CraftingState(String key,int progress,int total){
  static void write(CraftingState state,FriendlyByteBuf buffer){buffer.writeUtf(state.key,64);buffer.writeVarInt(state.progress);buffer.writeVarInt(state.total);}
  static CraftingState read(FriendlyByteBuf buffer){return new CraftingState(buffer.readUtf(64),buffer.readVarInt(),buffer.readVarInt());}
  static void handle(CraftingState state,Supplier<NetworkEvent.Context> context){var ctx=context.get();var sender=ctx.getNetworkManager();ctx.enqueueWork(()->ClientStateConnectionGate.crafting(state.key,state.progress,state.total,sender));ctx.setPacketHandled(true);}
 }
 public static void sync(ServerPlayer p){var data=PlayerState.data(p).copy();data.remove("CraftingCheckpoints");SourceSkills1685.snapshot(p,data);data.putLong("SourceRandomSeed925",p.serverLevel().getServer().overworld().getSeed());data.putInt("FoodNutrition",p.getFoodData().getFoodLevel());data.putFloat("FoodSatiation",p.getFoodData().getSaturationLevel());data.putLong("ClassicFogSeed",dev.koikaster.mite.visual.ClassicFogField.hashSeed(p.serverLevel().getServer().overworld().getSeed()));dev.koikaster.mite.visual.ClassicFogMetadata.append(data,p.serverLevel().getServer().overworld());IteConfig.writeSync(data);CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(data));}
 public record State(CompoundTag data){
  static void write(State s,FriendlyByteBuf b){b.writeNbt(s.data);}
  static State read(FriendlyByteBuf b){var t=b.readNbt();return new State(t==null?new CompoundTag():t);}
  static void handle(State s,Supplier<NetworkEvent.Context> context){
   var event=context.get();var sender=event.getNetworkManager();
   event.enqueueWork(()->ClientStateConnectionGate.receive(s.data,sender));event.setPacketHandled(true);
  }
 }
 public static void environmentSteam(net.minecraft.world.entity.Entity entity){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entity),new EnvironmentSteam(entity.getId()));}
 public record EnvironmentSteam(int entity){static void write(EnvironmentSteam value,FriendlyByteBuf buffer){buffer.writeInt(value.entity);}static EnvironmentSteam read(FriendlyByteBuf buffer){return new EnvironmentSteam(buffer.readInt());}static void handle(EnvironmentSteam value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->ClientEnvironmentEffects.steam(value.entity));context.get().setPacketHandled(true);}}
 public static void environmentSmoke(Mobs.Undead actor){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->actor),new EnvironmentSmoke(actor.getId()));}
 public record EnvironmentSmoke(int entity){static void write(EnvironmentSmoke value,FriendlyByteBuf buffer){buffer.writeInt(value.entity);}static EnvironmentSmoke read(FriendlyByteBuf buffer){return new EnvironmentSmoke(buffer.readInt());}static void handle(EnvironmentSmoke value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->{var level=net.minecraft.client.Minecraft.getInstance().level;if(level!=null&&level.getEntity(value.entity) instanceof Mobs.Undead actor)ClientFireElementalEffects.smoke(actor);});context.get().setPacketHandled(true);}}
 public static void shadowVision(ServerPlayer player,float value){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ShadowVision(value));}
 public record ShadowVision(float value){static void write(ShadowVision packet,FriendlyByteBuf buffer){buffer.writeFloat(packet.value);}static ShadowVision read(FriendlyByteBuf buffer){return new ShadowVision(buffer.readFloat());}static void handle(ShadowVision packet,Supplier<NetworkEvent.Context> context){var ctx=context.get();var sender=ctx.getNetworkManager();ctx.enqueueWork(()->ClientStateConnectionGate.shadowVision(packet.value,sender));ctx.setPacketHandled(true);}}
 public static void visionReport(float value){CHANNEL.sendToServer(new VisionReport(value));}
 public record VisionReport(float value){static void write(VisionReport packet,FriendlyByteBuf buffer){buffer.writeFloat(packet.value);}static VisionReport read(FriendlyByteBuf buffer){return new VisionReport(buffer.readFloat());}static void handle(VisionReport packet,Supplier<NetworkEvent.Context> context){var sender=context.get().getSender();if(sender!=null)OriginalShadowVision.report(sender,packet.value);context.get().setPacketHandled(true);}}
 public static void repairFx(net.minecraft.world.entity.Mob entity){CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),new RepairFx(entity.getId(),((net.minecraft.server.level.ServerLevel)entity.level()).getSeed()));}
 public record RepairFx(int entity,long seed){static void write(RepairFx value,FriendlyByteBuf buffer){buffer.writeInt(value.entity);buffer.writeLong(value.seed);}static RepairFx read(FriendlyByteBuf buffer){return new RepairFx(buffer.readInt(),buffer.readLong());}static void handle(RepairFx value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->{OriginalRandomTables.initialize(value.seed);ClientRepairParticles.repair(value.entity);});context.get().setPacketHandled(true);}}
 public static void wolfHealFx(net.minecraft.world.entity.Mob entity){CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),new WolfHealFx(entity.getId()));}
 public record WolfHealFx(int entity){static void write(WolfHealFx value,FriendlyByteBuf buffer){buffer.writeInt(value.entity);}static WolfHealFx read(FriendlyByteBuf buffer){return new WolfHealFx(buffer.readInt());}static void handle(WolfHealFx value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourceWolfClientEffects.heal(value.entity));context.get().setPacketHandled(true);}}
public static void phaseTrail(net.minecraft.world.entity.PathfinderMob actor,net.minecraft.core.BlockPos destination,short count,net.minecraft.core.BlockPos origin){CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(destination.getX(),destination.getY(),destination.getZ(),64,actor.level().dimension())),new SourcePhaseFx(destination,count,origin));}
public record SourcePhaseFx(net.minecraft.core.BlockPos destination,short count,net.minecraft.core.BlockPos origin){
 static void write(SourcePhaseFx value,FriendlyByteBuf buffer){buffer.writeBlockPos(value.destination);buffer.writeShort(value.count);buffer.writeBlockPos(value.origin);}
 static SourcePhaseFx read(FriendlyByteBuf buffer){return new SourcePhaseFx(buffer.readBlockPos(),buffer.readShort(),buffer.readBlockPos());}
 static void handle(SourcePhaseFx value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourcePhaseClientParticles.trail(value.destination,value.count,value.origin));context.get().setPacketHandled(true);}
}
 private Network(){}
}
