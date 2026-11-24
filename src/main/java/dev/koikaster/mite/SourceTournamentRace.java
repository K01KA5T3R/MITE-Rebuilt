package dev.koikaster.mite;

import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Records the first committed completion of each R195 race objective. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceTournamentRace extends SavedData {
    private UUID winner;
    private long tick;

    static void crafted(ServerPlayer player, ItemStack output) {
        var server = player.getServer();
        if (SourceTournament.mode(server) != SourceTournament.Mode.PICKAXE) return;
        if (!output.is(Content.get("gold_pickaxe")) && !output.is(Content.get("copper_pickaxe"))
                && !output.is(Content.get("silver_pickaxe"))) return;
        record(player, SourceTournament.Mode.PICKAXE);
    }

    @SubscribeEvent
    public static void picked(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SourceTournament.mode(player.getServer()) == SourceTournament.Mode.DIAMOND
                && event.getStack().is(Items.DIAMOND)) {
            record(player, SourceTournament.Mode.DIAMOND);
        }
    }

    @SubscribeEvent
    public static void entered(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SourceTournament.mode(player.getServer()) == SourceTournament.Mode.NETHER
                && event.getFrom() != Level.NETHER && event.getTo() == Level.NETHER
                && player.serverLevel().dimension() == Level.NETHER) {
            record(player, SourceTournament.Mode.NETHER);
        }
    }

    private static void record(ServerPlayer player, SourceTournament.Mode mode) {
        var server = player.getServer();
        String objective = mode.name().toLowerCase(Locale.ROOT);
        var storage = server.overworld().getDataStorage();
        var result = storage.computeIfAbsent(SourceTournamentRace::load, SourceTournamentRace::new,
                "mite_r195_" + objective + "_tournament");
        if (result.winner != null) return;
        result.winner = player.getUUID();
        result.tick = server.overworld().getGameTime();
        result.setDirty();
        storage.save();
        SourceTournament.victory(server, mode,
                Component.translatable("message.mite_reforged.tournament." + objective + "_won", player.getDisplayName()));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (winner != null) {
            tag.putUUID("Winner", winner);
            tag.putLong("WorldTick", tick);
        }
        return tag;
    }

    private static SourceTournamentRace load(CompoundTag tag) {
        var result = new SourceTournamentRace();
        if (tag.hasUUID("Winner")) {
            result.winner = tag.getUUID("Winner");
            result.tick = tag.getLong("WorldTick");
        }
        return result;
    }
}

