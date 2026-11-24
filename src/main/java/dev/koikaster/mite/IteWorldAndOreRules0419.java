package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * MITE-ITE 2.0.30 Phase-C closure mechanics (0.4.19):
 * weighted ore frequencies & min vein heights, biome dynamic elite spawns,
 * ite:time_back saved state, blood-moon calendar helper, beacon break boost clear,
 * manure crop-blight cure, merchant safe resync, portal mantle safety, and golden apple integration.
 * Strictly gated behind WorldContentMode.ite(level).
 */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class IteWorldAndOreRules0419 {
    private static final String TIME_BACK_KEY = "mite_ite_time_back";

    private IteWorldAndOreRules0419() {}

    public static int getWeightedFrequency(int frequency, int chancePercent) {
        if (frequency <= 0 || chancePercent <= 0) return 0;
        if (chancePercent >= 100) return frequency;
        return Math.round(frequency * (chancePercent / 100.0F));
    }

    public static int getMinVeinHeight(boolean underworld, boolean nether, String mineralId) {
        if (underworld) {
            return "adamantium".equals(mineralId) ? 130 : 140;
        }
        if (nether) {
            return 35;
        }
        return 0;
    }

    public static boolean isBloodMoonDay(long unadjustedTick) {
        long day = unadjustedTick / 24000L + 1L;
        if (day <= 0) return false;
        return day < 128L ? (day % 32L == 0L) : (day % 16L == 0L);
    }

    public static int getDayOfOverworld(Level level) {
        if (level == null) return 0;
        Level overworld = level;
        if (level instanceof ServerLevel sl && sl.getServer().overworld() != null) {
            overworld = sl.getServer().overworld();
        }
        return (int) Math.max(overworld.getDayTime() / 24000L + 1L, 1L);
    }

    public static int portalMantleSafeY(int targetY, int mantleY) {
        return Math.max(targetY, mantleY + 5);
    }

    public static void safeMerchantCloseSync(ServerPlayer player) {
        if (player != null && WorldContentMode.ite(player.level())) {
            player.containerMenu.broadcastFullState();
        }
    }

    public static final class TimeBackData extends SavedData {
        private boolean timeBack;

        public boolean isTimeBack() { return this.timeBack; }
        public void setTimeBack(boolean val) {
            if (this.timeBack != val) {
                this.timeBack = val;
                setDirty();
            }
        }

        public static TimeBackData load(CompoundTag tag) {
            var d = new TimeBackData();
            d.timeBack = tag.getBoolean("ite:time_back");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("ite:time_back", this.timeBack);
            return tag;
        }
    }

    public static boolean isTimeBack(ServerLevel level) {
        if (level == null || !WorldContentMode.ite(level)) return false;
        var overworld = level.getServer().overworld();
        if (overworld == null) return false;
        return overworld.getDataStorage().computeIfAbsent(TimeBackData::load, TimeBackData::new, TIME_BACK_KEY).isTimeBack();
    }

    public static void setTimeBack(ServerLevel level, boolean value) {
        if (level == null || !WorldContentMode.ite(level)) return;
        var overworld = level.getServer().overworld();
        if (overworld == null) return;
        overworld.getDataStorage().computeIfAbsent(TimeBackData::load, TimeBackData::new, TIME_BACK_KEY).setTimeBack(value);
    }

    public static boolean cureCropWithManure(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (level == null || !WorldContentMode.ite(level) || !stack.is(Content.MANURE.get())) return false;
        BlockState state = level.getBlockState(pos);
        if (!SourceCropBlight.blighted(state)) return false;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(SourceCropBlight.BLIGHTED, false), 3);
            if (player != null && !player.isCreative()) {
                stack.shrink(1);
            }
        }
        return true;
    }

    public static void clearNearbyBeaconBoost(ServerLevel level, BlockPos beaconPos) {
        if (level == null || !WorldContentMode.ite(level)) return;
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(beaconPos) <= 64.0D * 64.0D) {
                var data = player.getPersistentData();
                data.remove("MiteIteBeaconBoostUntil");
                data.remove("MiteIteBeaconFactor");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!WorldContentMode.ite(level)) return;
        if (cureCropWithManure(level, event.getPos(), event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel sl && WorldContentMode.ite(sl) && event.getState().is(Blocks.BEACON)) {
            clearNearbyBeaconBoost(sl, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !WorldContentMode.ite(sl)) return;
        if (event.getMobCategory() != MobCategory.MONSTER) return;
        var dim = sl.dimension();
        if (dim == Level.NETHER) {
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(IteEntities0418.ZOMBIE_PIGMAN_LORD.get(), 4, 1, 1));
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(IteEntities0418.ANNIHILATION_SKELETON.get(), 3, 1, 1));
        } else if (dim == Level.OVERWORLD || sl.dimension().location().getPath().contains("underworld")) {
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(IteEntities0418.ZOMBIE_LORD.get(), 4, 1, 1));
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(IteEntities0418.ANNIHILATION_SKELETON.get(), 3, 1, 1));
            event.addSpawnerData(new MobSpawnSettings.SpawnerData(IteEntities0418.WANDERING_WITCH.get(), 4, 1, 1));
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!stack.is(Items.GOLDEN_APPLE) && !stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return;
        if (!WorldContentMode.ite(player.level())) return;
        event.setDuration(IteGoldenApples.duration(stack, event.getDuration(), player));
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        if (!stack.is(Items.GOLDEN_APPLE) && !stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return;
        IteGoldenApples.applyEffects(stack, player.level(), player);
    }
}
