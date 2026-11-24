package dev.koikaster.mite;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

enum BossMusicTrack {
    ENDER_DRAGON("ender_dragon", MiteReforged.ID, "music/boss/aphelion"),
    WITHER("wither", MiteReforged.ID, "music/boss/eyes_in_the_void"),
    GIANT_ZOMBIE("giant_zombie", "minecraft", "music/game/nuance2"),
    ELDER_GUARDIAN("elder_guardian", MiteReforged.ID, "music/boss/signal_to_noise"),
    KING_SLIME("king_slime", MiteReforged.ID, "music/boss/gelatinous_monarch"),
    CRABULON("crabulon", MiteReforged.ID, "music/boss/infestation"),
    QUEEN_BEE("queen_bee", MiteReforged.ID, "music/boss/royal_retaliation"),
    MYTHRIL_KNIGHT("mythril_knight", MiteReforged.ID, "music/boss/asgore"),
    REDSTONE_GOLEM("redstone_iron_golem", MiteReforged.ID, "music/boss/golem"),
    UNDERWORLD_GIANT_SKELETON("underworld_giant_skeleton", MiteReforged.ID, "music/boss/heartache");

    final SoundEvent event;
    final ResourceLocation file;

    BossMusicTrack(String id, String namespace, String path) {
        event = SoundEvent.createVariableRangeEvent(new ResourceLocation(MiteReforged.ID, "music.boss." + id));
        file = new ResourceLocation(namespace, path);
    }
}
