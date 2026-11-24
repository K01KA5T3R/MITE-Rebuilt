package dev.koikaster.mite;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;

final class IteBeaconCrafting {
    private static final Map<ResourceLocation, Item> RECIPES = Map.of(
        new ResourceLocation("minecraft", "torch"), Items.TORCH,
        new ResourceLocation("minecraft", "stick"), Items.STICK,
        new ResourceLocation("minecraft", "ladder"), Items.LADDER,
        new ResourceLocation("minecraft", "rail"), Items.RAIL
    );
    private static final int MAX_LEVELS = 4;
    // The vanilla effect box includes the beacon block's full width.
    private static final double CHUNK_MARGIN = radius(MAX_LEVELS) + 1;

    static int period(int ordinaryPeriod) {
        return ordinaryPeriod - ordinaryPeriod / 10;
    }

    static boolean eligible(Player player, CraftingRecipe recipe, ItemStack result) {
        if (!(player instanceof ServerPlayer) || player.isCreative() || player.isSpectator()
                || recipe == null || recipe.isSpecial() || result.isEmpty()) {
            return false;
        }
        return result.getItem() == RECIPES.get(recipe.getId()) && WorldContentMode.ite(player.level());
    }

    static boolean starts(Player player, ContainerLevelAccess access, CraftingRecipe recipe, ItemStack result) {
        if (!eligible(player, recipe, result) || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return access.evaluate((tableLevel, tablePos) -> {
            if (tableLevel != level || player.distanceToSqr(
                    tablePos.getX() + 0.5, tablePos.getY() + 0.5, tablePos.getZ() + 0.5) > 64.0) {
                return false;
            }
            var chunk = level.getChunkSource().getChunkNow(tablePos.getX() >> 4, tablePos.getZ() >> 4);
            return chunk != null && chunk.getBlockState(tablePos).is(Blocks.CRAFTING_TABLE)
                && isCovered(level, player.getBoundingBox());
        }, false);
    }

    private static boolean isCovered(ServerLevel level, AABB playerBounds) {
        int minChunkX = Mth.floor(playerBounds.minX - CHUNK_MARGIN) >> 4;
        int maxChunkX = Mth.floor(playerBounds.maxX + CHUNK_MARGIN) >> 4;
        int minChunkZ = Mth.floor(playerBounds.minZ - CHUNK_MARGIN) >> 4;
        int maxChunkZ = Mth.floor(playerBounds.maxZ + CHUNK_MARGIN) >> 4;
        var chunks = level.getChunkSource();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = chunks.getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof BeaconBlockEntity beacon)
                            || beacon.isRemoved() || beacon.getLevel() != level
                            || beacon.getBeamSections().isEmpty()) {
                        continue;
                    }
                    BlockPos pos = beacon.getBlockPos();
                    if (!effectBox(level, pos, MAX_LEVELS).intersects(playerBounds)
                            || !chunk.getBlockState(pos).is(Blocks.BEACON)) {
                        continue;
                    }
                    // These are the live BeaconBlockEntity update fields in Java 1.20.1.
                    var beaconState = beacon.getUpdateTag();
                    int levels = beaconState.getInt("Levels");
                    if (levels >= 1 && levels <= MAX_LEVELS
                            && effectBox(level, pos, levels).intersects(playerBounds)
                            && hasPrimaryEffect(beaconState)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasPrimaryEffect(CompoundTag state) {
        if (!state.contains("Primary", Tag.TAG_INT)) return false;
        MobEffect selected = MobEffect.byId(state.getInt("Primary"));
        if (selected == null) return false;
        for (MobEffect[] tier : BeaconBlockEntity.BEACON_EFFECTS) {
            for (MobEffect effect : tier) {
                if (effect == selected) return true;
            }
        }
        return false;
    }

    private static AABB effectBox(ServerLevel level, BlockPos pos, int levels) {
        return new AABB(pos).inflate(radius(levels)).expandTowards(0, level.getHeight(), 0);
    }

    private static double radius(int levels) {
        return levels * 10.0 + 10.0;
    }

    private IteBeaconCrafting() {}
}
