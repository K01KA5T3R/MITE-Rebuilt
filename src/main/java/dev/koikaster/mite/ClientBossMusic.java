package dev.koikaster.mite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
final class ClientBossMusic {
    private static final Map<UUID, Session> sessions = new LinkedHashMap<>();
    private static Session current;
    private static BossLoop loop;
    private static int retry;

    static void receive(BossMusicPacket packet) {
        switch (packet.action()) {
            case START -> sessions.computeIfAbsent(packet.encounter(), id -> new Session(packet.track(), packet.dimension()));
            case STOP -> sessions.remove(packet.encounter());
            case DUCK -> {
                Session session = sessions.get(packet.encounter());
                if (session != null) {
                    session.duckGain = Mth.clamp(packet.gain() / 100f, 0, 1);
                    session.duckTicks = Mth.clamp(packet.ticks(), 0, 20 * 60);
                }
            }
        }
    }

    static boolean takesMusicChannel() {
        return current != null || target(Minecraft.getInstance()) != null;
    }

    static boolean permits(SoundInstance instance, Sound sample) {
        return instance instanceof BossLoop boss && boss.track.file.equals(sample.getLocation());
    }

    static boolean isBossLoop(SoundInstance instance) {
        return instance instanceof BossLoop;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            reset(client);
            return;
        }

        for (Session session : sessions.values()) session.tickMix();
        Session target = ClassicMusicClient.classicCreditsPlaying() ? null : target(client);
        if (current != null && current != target) {
            current.fade = Math.max(0, current.fade - .05f);
            if (current.fade > 0) {
                if (loop != null) loop.gain(current.fade * current.mix);
                return;
            }
            stopNative(client);
            current = null;
        }
        if (target == null) return;
        if (current == null) {
            current = target;
            current.fade = 0;
            client.getMusicManager().stopPlaying();
            retry = 0;
        }
        current.fade = Math.min(1, current.fade + .10f);
        updateNative(client, current.fade * current.mix);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset(Minecraft.getInstance());
    }

    private static Session target(Minecraft client) {
        if (client.level == null || client.player == null) return null;
        var dimension = client.level.dimension().location();
        Session latest = null;
        for (Session session : sessions.values()) {
            if (session.dimension.equals(dimension) && client.getResourceManager().getResource(session.stream.getPath()).isPresent()) {
                latest = session;
            }
        }
        return latest;
    }

    private static void updateNative(Minecraft client, float gain) {
        if (client.options.getSoundSourceOptionInstance(SoundSource.MASTER).get() == 0
                || client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).get() == 0) {
            stopNative(client);
            retry = 0;
            return;
        }
        if (loop != null) loop.gain(gain);
        if (loop != null && client.getSoundManager().isActive(loop)) return;
        if (retry-- > 0) return;
        loop = new BossLoop(current, gain);
        client.getSoundManager().play(loop);
        retry = 12;
    }

    private static void stopNative(Minecraft client) {
        if (loop != null) client.getSoundManager().stop(loop);
        loop = null;
    }

    private static void reset(Minecraft client) {
        stopNative(client);
        sessions.clear();
        current = null;
        retry = 0;
    }

    private static final class Session {
        final BossMusicTrack track;
        final ResourceLocation dimension;
        final Sound stream;
        float fade;
        float mix = 1;
        float duckGain = 1;
        int duckTicks;

        Session(BossMusicTrack track, ResourceLocation dimension) {
            this.track = track;
            this.dimension = dimension;
            stream = new Sound(track.file.toString(), ConstantFloat.of(1), ConstantFloat.of(1), 1,
                Sound.Type.FILE, true, false, 16);
        }

        void tickMix() {
            float target = duckTicks > 0 ? duckGain : 1;
            mix = Mth.approach(mix, target, target < mix ? .23f : .07f);
            if (duckTicks > 0) --duckTicks;
        }
    }

    private static final class BossLoop extends AbstractTickableSoundInstance {
        final BossMusicTrack track;
        private final Sound stream;

        BossLoop(Session session, float gain) {
            super(session.track.event, SoundSource.MUSIC, RandomSource.create());
            track = session.track;
            stream = session.stream;
            looping = true;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            gain(gain);
        }

        void gain(float value) {
            volume = value;
        }

        @Override public WeighedSoundEvents resolve(SoundManager manager) {
            var registered = super.resolve(manager);
            if (registered != null && sound != null && stream.getLocation().equals(sound.getLocation()) && sound.shouldStream()) {
                return registered;
            }
            sound = stream;
            var declared = new WeighedSoundEvents(track.event.getLocation(), null);
            declared.addSound(stream);
            return declared;
        }

        @Override public void tick() { }
        @Override public boolean canStartSilent() { return true; }
    }

    private ClientBossMusic() {}
}
