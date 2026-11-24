package dev.koikaster.mite;

import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Vanilla 1.20.1 ordinary music added to the existing C418 pools. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class ModernMusicClient {
    private static final Map<ResourceLocation, ResourceLocation> POOLS = Map.ofEntries(
        Map.entry(new ResourceLocation("minecraft:music.creative"), new ResourceLocation("mite_reforged:music.modern.game")),
        Map.entry(new ResourceLocation("minecraft:music.game"), new ResourceLocation("mite_reforged:music.modern.game")),
        Map.entry(new ResourceLocation("minecraft:music.nether.basalt_deltas"), new ResourceLocation("mite_reforged:music.modern.nether.basalt_deltas")),
        Map.entry(new ResourceLocation("minecraft:music.nether.crimson_forest"), new ResourceLocation("mite_reforged:music.modern.nether.crimson_forest")),
        Map.entry(new ResourceLocation("minecraft:music.nether.nether_wastes"), new ResourceLocation("mite_reforged:music.modern.nether.nether_wastes")),
        Map.entry(new ResourceLocation("minecraft:music.nether.soul_sand_valley"), new ResourceLocation("mite_reforged:music.modern.nether.soul_sand_valley")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.badlands"), new ResourceLocation("mite_reforged:music.modern.overworld.badlands")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.bamboo_jungle"), new ResourceLocation("mite_reforged:music.modern.overworld.bamboo_jungle")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.cherry_grove"), new ResourceLocation("mite_reforged:music.modern.overworld.cherry_grove")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.deep_dark"), new ResourceLocation("mite_reforged:music.modern.overworld.deep_dark")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.desert"), new ResourceLocation("mite_reforged:music.modern.overworld.desert")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.dripstone_caves"), new ResourceLocation("mite_reforged:music.modern.overworld.dripstone_caves")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.flower_forest"), new ResourceLocation("mite_reforged:music.modern.overworld.flower_forest")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.forest"), new ResourceLocation("mite_reforged:music.modern.overworld.forest")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.frozen_peaks"), new ResourceLocation("mite_reforged:music.modern.overworld.frozen_peaks")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.grove"), new ResourceLocation("mite_reforged:music.modern.overworld.grove")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.jagged_peaks"), new ResourceLocation("mite_reforged:music.modern.overworld.jagged_peaks")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.jungle"), new ResourceLocation("mite_reforged:music.modern.overworld.jungle")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.lush_caves"), new ResourceLocation("mite_reforged:music.modern.overworld.lush_caves")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.meadow"), new ResourceLocation("mite_reforged:music.modern.overworld.meadow")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.old_growth_taiga"), new ResourceLocation("mite_reforged:music.modern.overworld.old_growth_taiga")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.snowy_slopes"), new ResourceLocation("mite_reforged:music.modern.overworld.snowy_slopes")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.sparse_jungle"), new ResourceLocation("mite_reforged:music.modern.overworld.sparse_jungle")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.stony_peaks"), new ResourceLocation("mite_reforged:music.modern.overworld.stony_peaks")),
        Map.entry(new ResourceLocation("minecraft:music.overworld.swamp"), new ResourceLocation("mite_reforged:music.modern.overworld.swamp")),
        Map.entry(new ResourceLocation("minecraft:music.under_water"), new ResourceLocation("mite_reforged:music.modern.under_water"))
    );
    private static final Set<String> NEW_TRACKS = Set.of(
        "music/game/a_familiar_room",
        "music/game/an_ordinary_day",
        "music/game/ancestry",
        "music/game/bromeliad",
        "music/game/comforting_memories",
        "music/game/crescent_dunes",
        "music/game/echo_in_the_wind",
        "music/game/floating_dream",
        "music/game/infinite_amethyst",
        "music/game/left_to_bloom",
        "music/game/nether/crimson_forest/chrysopoeia",
        "music/game/nether/nether_wastes/rubedo",
        "music/game/nether/soulsand_valley/so_below",
        "music/game/one_more_day",
        "music/game/stand_tall",
        "music/game/swamp/aerie",
        "music/game/swamp/firebugs",
        "music/game/swamp/labyrinthine",
        "music/game/water/axolotl",
        "music/game/water/dragon_fish",
        "music/game/water/shuniji",
        "music/game/wending"
    );
    // Sound lookup can occur outside a client tick. Only the client thread reads the world.
    private static volatile boolean enabled;

    static boolean refresh(Minecraft client) {
        boolean next = client.level != null && client.player != null && client.getConnection() != null
            && WorldContentMode.modernMusic(client.level);
        if (enabled != next) {
            enabled = next;
            stopOrdinary(client);
        }
        return next;
    }

    private static void stopOrdinary(Minecraft client) {
        if (!ClassicMusicClient.classicCreditsPlaying() && !(client.screen instanceof WinScreen)) {
            client.getMusicManager().stopPlaying();
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) refresh(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        boolean previous = enabled;
        enabled = false;
        if (previous) stopOrdinary(Minecraft.getInstance());
    }

    /** Survival and creative use the same ordinary biome/water selection. */
    static Music sharedSurvivalMusic(Minecraft client, Music selected) {
        if (selected != Musics.CREATIVE || client.level == null || client.player == null) return selected;
        var biome = client.level.getBiome(client.player.blockPosition());
        if (client.player.isUnderWater() && biome.is(BiomeTags.PLAYS_UNDERWATER_MUSIC)) return Musics.UNDER_WATER;
        return biome.value().getBackgroundMusic().orElse(Musics.GAME);
    }

    /** Route the sound lookup, retaining native Music identities and MusicManager queries. */
    public static ResourceLocation poolKey(ResourceLocation original) {
        return enabled && original != null ? POOLS.getOrDefault(original, original) : original;
    }

    static boolean allowsResolved(SoundInstance instance, Sound sample) {
        return enabled && instance.getSource() == SoundSource.MUSIC && POOLS.containsKey(instance.getLocation())
            && sample.getLocation().getNamespace().equals("minecraft")
            && NEW_TRACKS.contains(sample.getLocation().getPath());
    }

    private ModernMusicClient() {}
}
