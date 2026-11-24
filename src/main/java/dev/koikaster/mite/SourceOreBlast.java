package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** R195 apr's explosion branch at the actual native explosion loot call. */
public final class SourceOreBlast {
    private enum Ore { COAL, DIAMOND, EMERALD, QUARTZ, LAPIS, METAL }

    private static Ore ore(BlockState state) {
        if (state.is(Blocks.COAL_ORE)) return Ore.COAL;
        if (state.is(Blocks.DIAMOND_ORE)) return Ore.DIAMOND;
        if (state.is(Blocks.EMERALD_ORE)) return Ore.EMERALD;
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return Ore.QUARTZ;
        if (state.is(Blocks.LAPIS_ORE)) return Ore.LAPIS;
        if (state.getBlock() instanceof SourceTournamentOre
                || state.is(Blocks.COPPER_ORE) || state.is(Blocks.IRON_ORE) || state.is(Blocks.GOLD_ORE)) {
            return Ore.METAL;
        }
        return null;
    }

    public static void afterBreak(BlockState state, ServerLevel level, BlockPos at, ItemStack tool, boolean player) {
        state.spawnAfterBreak(level, at, tool, player && ore(state) == null);
    }

    public static List<ItemStack> drops(BlockState state, LootParams.Builder params) {
        var nativeDrops = state.getDrops(params);
        var mineral = ore(state);
        if (mineral == null) return nativeDrops;

        var level = params.getLevel();
        var replacement = switch (mineral) {
            case COAL -> ItemStack.EMPTY;
            case DIAMOND -> new ItemStack(Content.DIAMOND_SHARD.get());
            case EMERALD -> new ItemStack(Content.EMERALD_SHARD.get());
            case QUARTZ -> new ItemStack(Content.QUARTZ_SHARD.get());
            case LAPIS -> new ItemStack(Items.LAPIS_LAZULI, 3 + level.getRandom().nextInt(3));
            case METAL -> new ItemStack(state.getBlock().asItem());
        };
        var vanillaItem = switch (mineral) {
            case COAL -> Items.COAL;
            case DIAMOND -> Items.DIAMOND;
            case EMERALD -> Items.EMERALD;
            case QUARTZ -> Items.QUARTZ;
            case LAPIS -> Items.LAPIS_LAZULI;
            case METAL -> state.getBlock().asItem();
        };
        var drops = new ArrayList<>(nativeDrops);
        // Leave unrelated stacks contributed by Forge loot modifiers in place.
        drops.removeIf(stack -> stack.is(vanillaItem) || stack.is(state.getBlock().asItem())
            || !replacement.isEmpty() && stack.is(replacement.getItem()));
        if (!replacement.isEmpty()) drops.add(replacement);
        if (mineral == Ore.LAPIS && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ExperienceOrb.award(level, params.getParameter(LootContextParams.ORIGIN), replacement.getCount() * 5);
        }
        return drops;
    }

    private SourceOreBlast() {}
}
