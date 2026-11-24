package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The first observed blood moon unlocks ITE encounters only after its actual dawn. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class IteBloodMoonProgress extends SavedData {
    private static final String KEY = "mite_ite_blood_moon";
    private static final long NO_SAMPLE = -1;
    static final int GROWTH_LIMIT_DAY = 366;

    private long observedCycle = NO_SAMPLE;
    private long previousTime = NO_SAMPLE;
    private int firstEligibleDay;

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        observeWorld(event.getServer().overworld());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) {
            observeWorld(level);
        }
    }

    private static void observeWorld(ServerLevel overworld) {
        if (overworld != null && Settings.MOONS && WorldContentMode.ite(overworld)) {
            state(overworld).observe(overworld.getDayTime());
        }
    }

    static int firstEligibleDay(ServerLevel level) {
        if (!WorldContentMode.ite(level)) return 0;
        var overworld = level.getServer().overworld();
        return overworld == null ? 0 : state(overworld).firstEligibleDay;
    }

    private static IteBloodMoonProgress state(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(IteBloodMoonProgress::load, IteBloodMoonProgress::new, KEY);
    }

    void observe(long time) {
        if (firstEligibleDay > 0) return;
        if (time < 0) {
            previousTime = NO_SAMPLE;
            return;
        }
        long cycle = time / 24000L;
        long last = previousTime;
        previousTime = time;
        if (SourceMoonCycle.blood(time, true)) {
            if (observedCycle != cycle) {
                observedCycle = cycle;
                setDirty();
            }
        } else if (observedCycle == cycle && last >= 0 && last == time - 1
                && SourceMoonCycle.blood(last, true) && SourceMoonCycle.daytime(time)) {
            firstEligibleDay = Math.min(GROWTH_LIMIT_DAY,
                SourceMoonCycle.dayOfWorld(Math.min(time, 24000L * GROWTH_LIMIT_DAY)));
            observedCycle = NO_SAMPLE;
            setDirty();
        }
    }

    static IteBloodMoonProgress load(CompoundTag data) {
        var progress = new IteBloodMoonProgress();
        int day = data.getInt("FirstEligibleDay");
        if (data.contains("FirstEligibleDay", Tag.TAG_INT) && day >= 1 && day <= GROWTH_LIMIT_DAY) {
            progress.firstEligibleDay = day;
        }
        if (progress.firstEligibleDay == 0 && data.contains("ObservedCycle", Tag.TAG_LONG)) {
            progress.observedCycle = Math.max(NO_SAMPLE, data.getLong("ObservedCycle"));
        }
        return progress;
    }

    @Override
    public CompoundTag save(CompoundTag data) {
        data.putInt("FirstEligibleDay", firstEligibleDay);
        data.putLong("ObservedCycle", observedCycle);
        return data;
    }
}
