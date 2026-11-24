package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class Incapacitation2123 {
    private static final String TAG = "Incapacitated2123";
    private static final int DOWN_TICKS = 2000;
    static final int RESCUE_TICKS = 200;
    private static final int RESCUE_HUD_INTERVAL = 4;
    private static final double RESCUE_DISTANCE_SQUARED = 4.0;
    private static final Map<UUID, Rescue> RESCUES = new HashMap<>();
    private static final Set<UUID> FINISHING = new HashSet<>();
    private static SimpleChannel channel;
    private static final Set<UUID> ACTIVE_HELPER_HUD = new HashSet<>();

    private record Rescue(UUID helper, int ticks) {}

    public static boolean incapacitated(ServerPlayer player) {
        return WorldContentMode.rebuilt(player.level()) && PlayerState.data(player).contains(TAG, Tag.TAG_COMPOUND);
    }

    private static CompoundTag data(ServerPlayer player) {
        return PlayerState.data(player).getCompound(TAG);
    }

    private static long now(ServerPlayer player) {
        return player.getServer().overworld().getGameTime();
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void dying(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !WorldContentMode.rebuilt(player.level()) || player instanceof FakePlayer
            || player.isSpectator() || player.isCreative() || player.getServer()==null || FINISHING.contains(player.getUUID())) return;
        if (incapacitated(player)) {
            event.setCanceled(true);
            player.setHealth(1);
            return;
        }
        long online = player.getServer().getPlayerList().getPlayers().stream().filter(p -> !(p instanceof FakePlayer)).limit(2).count();
        if (online < 2) return;

        var stored = new CompoundTag();
        stored.putLong("Deadline", now(player)+DOWN_TICKS);
        stored.putDouble("X",player.getX());
        stored.putDouble("Y",player.getY());
        stored.putDouble("Z",player.getZ());
        stored.putBoolean("NoGravity",player.isNoGravity());
        if (player.getForcedPose()!=null) stored.putString("Pose",player.getForcedPose().name());
        storeDamage(stored,event.getSource());
        PlayerState.data(player).put(TAG,stored);

        event.setCanceled(true);
        player.setHealth(1);
        player.closeContainer();
        player.stopRiding();
        player.stopUsingItem();
        if (player.isSleeping()) player.stopSleepInBed(true,true);
        hold(player);
        sync(player,DOWN_TICKS,0);
        player.getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable("chat.mite_reforged.player_downed", player.getDisplayName()), false);
    }

    private static void hold(ServerPlayer player) {
        player.setNoGravity(true);
        player.setForcedPose(Pose.SWIMMING);
        player.setPose(Pose.SWIMMING);
        player.setSprinting(false);
        player.setShiftKeyDown(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance=0;
    }

    private static void storeDamage(CompoundTag stored, DamageSource cause) {
        cause.typeHolder().unwrapKey().ifPresent(type -> stored.putString("Damage",type.location().toString()));
        if (cause.getDirectEntity()!=null) stored.putUUID("Direct",cause.getDirectEntity().getUUID());
        if (cause.getEntity()!=null) stored.putUUID("Causing",cause.getEntity().getUUID());
        var origin=cause.getSourcePosition();
        if (origin!=null) {
            stored.putDouble("DamageX",origin.x);
            stored.putDouble("DamageY",origin.y);
            stored.putDouble("DamageZ",origin.z);
        }
    }

    private static DamageSource damage(ServerPlayer player, CompoundTag stored) {
        var id=ResourceLocation.tryParse(stored.getString("Damage"));
        if (id==null) return player.damageSources().generic();
        var level=player.serverLevel();
        var key=ResourceKey.create(Registries.DAMAGE_TYPE,id);
        var type=level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(key).orElse(null);
        if (type==null) return player.damageSources().generic();
        var direct=stored.hasUUID("Direct")?level.getEntity(stored.getUUID("Direct")):null;
        var causing=stored.hasUUID("Causing")?level.getEntity(stored.getUUID("Causing")):null;
        var position=stored.contains("DamageX")?new Vec3(stored.getDouble("DamageX"),stored.getDouble("DamageY"),stored.getDouble("DamageZ")):null;
        return new DamageSource(type,direct,causing,position);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var playerList=event.getServer().getPlayerList();
        var players=playerList.getPlayers();
        var activeHud = new HashSet<UUID>();
        var helpers=new HashSet<UUID>();
        for (var player:players) {
            if (!incapacitated(player)) continue;
            var stored=data(player);
            long left=stored.getLong("Deadline")-now(player);
            if (left<=0) { die(player); continue; }

            var anchor=new Vec3(stored.getDouble("X"),stored.getDouble("Y"),stored.getDouble("Z"));
            if (player.position().distanceToSqr(anchor)>.04) {
                player.teleportTo(player.serverLevel(),anchor.x,anchor.y,anchor.z,player.getYRot(),player.getXRot());
            }
            hold(player);
            var previous=RESCUES.get(player.getUUID());
            var helper=players.stream().filter(p -> !helpers.contains(p.getUUID()) && canRescue(player,p))
                .min(Comparator.comparingDouble(p -> player.distanceToSqr(p))).orElse(null);
            int progress=0;
            if (helper!=null) {
                helpers.add(helper.getUUID());
                progress=previous!=null&&previous.helper().equals(helper.getUUID())?previous.ticks()+1:1;
                if (progress>=RESCUE_TICKS) { revive(player,stored); continue; }
                RESCUES.put(player.getUUID(),new Rescue(helper.getUUID(),progress));
                activeHud.add(helper.getUUID());
                if (progress == 1 || progress % RESCUE_HUD_INTERVAL == 0) syncHelper(helper, progress);
            } else RESCUES.remove(player.getUUID());
            if (player.tickCount%20==0 || progress==1 || previous!=null&&progress==0) sync(player,(int)Math.min(DOWN_TICKS,left),progress);
        }
        for (UUID id : ACTIVE_HELPER_HUD) {
            if (activeHud.contains(id)) continue;
            var helper = playerList.getPlayer(id);
            if (helper != null) syncHelper(helper, 0);
        }
        ACTIVE_HELPER_HUD.clear();
        ACTIVE_HELPER_HUD.addAll(activeHud);
    }

    private static boolean canRescue(ServerPlayer down, ServerPlayer helper) {
        return helper!=down && !(helper instanceof FakePlayer) && helper.level()==down.level() && helper.isAlive()
            && !helper.isSpectator() && !incapacitated(helper) && helper.isShiftKeyDown() && !helper.isPassenger()
            && helper.distanceToSqr(down)<=RESCUE_DISTANCE_SQUARED && helper.hasLineOfSight(down)
            && ((down.getTeam()==null&&helper.getTeam()==null)||down.isAlliedTo(helper));
    }

    private static void revive(ServerPlayer player, CompoundTag stored) {
        clear(player,stored);
        player.setHealth(1);
        player.getFoodData().setSaturation(1);
        Network.sync(player);
    }

    private static void die(ServerPlayer player) {
        if (!incapacitated(player)) return;
        finishDeath(player,damage(player,data(player)));
    }

    static void finishDeath(ServerPlayer player, DamageSource cause) {
        if (incapacitated(player)) clear(player,data(player));
        var id=player.getUUID();
        FINISHING.add(id);
        try {
            player.setHealth(0);
            player.die(cause);
        } finally { FINISHING.remove(id); }
    }

    private static void clear(ServerPlayer player, CompoundTag stored) {
        PlayerState.data(player).remove(TAG);
        RESCUES.remove(player.getUUID());
        player.setNoGravity(stored.getBoolean("NoGravity"));
        Pose prior=null;
        if (stored.contains("Pose")) {
            try { prior=Pose.valueOf(stored.getString("Pose")); } catch (IllegalArgumentException ignored) {}
        }
        player.setForcedPose(prior);
        sync(player,0,0);
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && incapacitated(player)) {
            hold(player);
            sync(player,(int)Math.max(1,Math.min(DOWN_TICKS,data(player).getLong("Deadline")-now(player))),0);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        RESCUES.remove(event.getEntity().getUUID());
        ACTIVE_HELPER_HUD.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hurt(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && incapacitated(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void heal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && incapacitated(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void travel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && incapacitated(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && incapacitated(player)) event.setCanceled(true);
    }

    public static void register(SimpleChannel value) {
        channel=value;
        value.messageBuilder(State.class,43,NetworkDirection.PLAY_TO_CLIENT).encoder(State::write).decoder(State::read).consumerMainThread(State::handle).add();
        value.messageBuilder(Surrender.class,44,NetworkDirection.PLAY_TO_SERVER).encoder(Surrender::write).decoder(Surrender::read).consumerMainThread(Surrender::handle).add();
        value.messageBuilder(HelperState.class,57,NetworkDirection.PLAY_TO_CLIENT)
            .encoder(HelperState::write).decoder(HelperState::read).consumerMainThread(HelperState::handle).add();
    }

    private static void sync(ServerPlayer player,int left,int rescue) {
        if (player.connection!=null && channel!=null) channel.send(PacketDistributor.PLAYER.with(() -> player),new State(left,rescue));
    }

    private static void syncHelper(ServerPlayer helper, int progress) {
        if (helper.connection != null && channel != null) {
            channel.send(PacketDistributor.PLAYER.with(() -> helper), new HelperState(progress));
        }
    }

    private record HelperState(int progress) {
        static void write(HelperState state, FriendlyByteBuf out) {
            out.writeVarInt(state.progress());
        }

        static HelperState read(FriendlyByteBuf in) {
            return new HelperState(in.readVarInt());
        }

        static void handle(HelperState state, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientIncapacitation2123.updateHelper(state.progress()));
            ctx.get().setPacketHandled(true);
        }
    }

    static void requestSurrender() {
        channel.sendToServer(new Surrender());
    }

    private record State(int left,int rescue) {
        static void write(State state,FriendlyByteBuf out) { out.writeVarInt(state.left);out.writeVarInt(state.rescue); }
        static State read(FriendlyByteBuf in) { return new State(in.readVarInt(),in.readVarInt()); }
        static void handle(State state,Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,() -> () -> ClientIncapacitation2123.update(state.left,state.rescue));
            ctx.get().setPacketHandled(true);
        }
    }

    private record Surrender() {
        static void write(Surrender ignored,FriendlyByteBuf out) {}
        static Surrender read(FriendlyByteBuf in) { return new Surrender(); }
        static void handle(Surrender ignored,Supplier<NetworkEvent.Context> ctx) {
            var player=ctx.get().getSender();
            if (player!=null) die(player);
            ctx.get().setPacketHandled(true);
        }
    }

    private Incapacitation2123() {}
}
