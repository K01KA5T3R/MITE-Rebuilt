package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** R195 ul fishing checks and yield, with the modern hook carrying the visible bite. */
public final class ClassicFishingMode {
    public interface BiteAccess {
        int mite$classicNibble();
        void mite$classicNibble(int ticks);
        void mite$classicBiting(boolean biting);
    }

    public static boolean tick(FishingHook hook, RandomSource random) {
        if (!(hook.level() instanceof ServerLevel world) || WorldContentMode.rebuilt(world)) return false;
        var bite = (BiteAccess) (Object) hook;
        int duration = bite.mite$classicNibble();
        if (duration > 0) {
            bite.mite$classicNibble(duration - 1);
            if (duration == 1) bite.mite$classicBiting(false);
            return true;
        }
        var player = hook.getPlayerOwner();
        if (player == null) return true;
        var water = hookWater(hook);
        if (!fullWater(world, water) || !world.isEmptyBlock(water.above())) return true;

        var fish = water.offset(random.nextInt(7) - 3, -random.nextInt(4), random.nextInt(7) - 3);
        if (!inhabitedWater(world, fish)
            || !OriginalRaycast.physicalLine(hook, Vec3.atCenterOf(water), Vec3.atCenterOf(fish))) return true;
        int time = (int) Math.floorMod(world.getDayTime() + 6000L, 24000L);
        boolean blue = SourceMoonCycle.blue(world.getDayTime(), true);
        boolean rain = world.isRainingAt(water.above());
        boolean learning = SourceSkills1685.enabled(world) && !SourceSkills1685.has(player, SourceSkillRules1685.FISHING);
        var rod = player.getMainHandItem();
        if (!(rod.getItem() instanceof FishingRodItem)) rod = player.getOffhandItem();
        int fortune = EnchantmentEffects.level("fishing_fortune", rod);
        if (random.nextInt(biteChance(time, blue, rain, learning, fortune, bait(player) >= 0)) != 0) return true;

        bite.mite$classicNibble(random.nextInt(30) + 30);
        hook.setDeltaMovement(hook.getDeltaMovement().add(0, -.2, 0));
        bite.mite$classicBiting(true);
        hook.playSound(SoundEvents.FISHING_BOBBER_SPLASH, .25f, 1 + (random.nextFloat() - random.nextFloat()) * .4f);
        double y = water.getY() + 1;
        int particles = (int) (1 + hook.getBbWidth() * 20);
        world.sendParticles(ParticleTypes.BUBBLE, hook.getX(), y, hook.getZ(), particles, hook.getBbWidth(), 0, hook.getBbWidth(), .2);
        world.sendParticles(ParticleTypes.SPLASH, hook.getX(), y, hook.getZ(), particles, hook.getBbWidth(), 0, hook.getBbWidth(), .2);
        return true;
    }

    static int biteChance(int time, boolean blue, boolean rain, boolean learning, int fortune, boolean bait) {
        int chance = blue ? 600 : net.minecraft.util.Mth.clamp((int) (600f * (Math.min(Math.abs(time - 5500), Math.abs(time - 17500)) / 600f)), 600, 2400);
        if (rain) chance /= 2;
        if (learning) chance *= 2;
        for (int i = 0; i < fortune; i++) chance = chance * 9 / 10;
        if (bait) chance /= 2;
        return Math.max(chance, 1);
    }

    static void catchFish(List<ItemStack> loot, ServerLevel world, FishingHook hook, RandomSource random) {
        loot.clear();
        var pos = hookWater(hook);
        boolean large = random.nextFloat() >= .8f && world.getBiome(pos).is(Biomes.OCEAN)
            && !nearShore(world, pos);
        loot.add(new ItemStack(large ? Items.SALMON : Items.COD));
    }

    public static void consumeBait(FishingHook hook) {
        if (!(hook.level() instanceof ServerLevel world) || WorldContentMode.rebuilt(world)) return;
        var player = hook.getPlayerOwner();
        if (player == null) return;
        var bite = (BiteAccess) (Object) hook;
        if (bite.mite$classicNibble() <= 0) return;
        bite.mite$classicNibble(0);
        int slot = bait(player);
        if (slot >= 0) player.getInventory().removeItem(slot, 1);
    }

    private static int bait(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Content.RAW_WORM.get())) return slot;
        }
        return -1;
    }

    private static boolean nearShore(ServerLevel world, BlockPos pos) {
        for (var at : BlockPos.betweenClosed(pos.offset(-16, -3, -16), pos.offset(16, 0, 16))) {
            if (!world.hasChunkAt(at)) return true;
            var block = world.getBlockState(at).getBlock();
            if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.SAND) return true;
        }
        return false;
    }

    private static BlockPos hookWater(FishingHook hook) {
        return BlockPos.containing(hook.getX(), hook.getY() - (double) .2f, hook.getZ());
    }

    private static boolean fullWater(ServerLevel world, BlockPos pos) {
        if (!world.hasChunkAt(pos)) return false;
        var state = world.getBlockState(pos);
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static boolean inhabitedWater(ServerLevel world, BlockPos pos) {
        if (!world.hasChunkAt(pos)) return false;
        var state = world.getBlockState(pos);
        return state.is(Blocks.WATER) && (state.getFluidState().isSource()
            || state.getFluidState().getOptionalValue(net.minecraft.world.level.material.FlowingFluid.FALLING).orElse(false));
    }

    private ClassicFishingMode() {}
}
