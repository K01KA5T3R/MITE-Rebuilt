package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class UnderworldExplorationRewards {
    static final ResourceLocation MINESHAFT_CART = Content.id("chests/underworld_mineshaft");
    private static final ResourceLocation MINESHAFT =
        Content.id("chests/underworld_mineshaft_bonus");
    private static final ResourceLocation DUNGEON =
        Content.id("chests/underworld_dungeon_bonus");

    public static void setMineshaftLoot(MinecartChest cart, ResourceLocation table, long seed, WorldGenLevel world) {
        if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(table)
                && UnderworldExplorationRules.enabled(world.getLevel())) {
            table = MINESHAFT_CART;
        }
        cart.setLootTable(table, seed);
    }

    static void mineshaft(List<ItemStack> loot, LootContext original) {
        var level = original.getLevel();
        if (!MINESHAFT_CART.equals(original.getQueriedLootTableId())
                || !UnderworldExplorationRules.enabled(level)) return;

        level.getServer().getLootData().getLootTable(MINESHAFT).getRandomItemsRaw(original, loot::add);
    }

    static void dungeon(ServerLevel level, BlockPos pos, Container inventory) {
        if (!UnderworldExplorationRules.enabled(level)) return;

        var slots = new ArrayList<Integer>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) slots.add(i);
        }
        if (slots.isEmpty()) return;

        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .create(LootContextParamSets.CHEST);
        long seed = level.getSeed() ^ pos.asLong() * 0x9e3779b97f4a7c15L ^ 0x636176655f727569L;
        var context = new LootContext.Builder(params).withOptionalRandomSeed(seed == 0 ? 1 : seed).create(null);
        var placement = RandomSource.create(seed ^ 0x4d495445L);
        level.getServer().getLootData().getLootTable(DUNGEON).getRandomItemsRaw(context, result -> {
            int remaining = result.getCount();
            int limit = Math.min(result.getMaxStackSize(), inventory.getMaxStackSize());
            while (remaining > 0 && limit > 0 && !slots.isEmpty()) {
                int index = placement.nextInt(slots.size());
                int count = Math.min(remaining, limit);
                inventory.setItem(slots.remove(index), result.copyWithCount(count));
                remaining -= count;
            }
        });
        inventory.setChanged();
    }

    private UnderworldExplorationRewards() {}
}
