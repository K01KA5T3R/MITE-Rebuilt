package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;

/** R195 nn wet-age and og living sunlight checks. */
public final class OriginalMobEnvironment {
    private final Mobs.Undead mob;
    public int wetTicks;

    public OriginalMobEnvironment(Mobs.Undead mob) {
        this.mob = mob;
    }

    public void wetTick() {
        wetTicks = nextWetTicks(mob, wetTicks);
    }

    public void sunlight() {
        if (mob.getMobType() == MobType.UNDEAD) sunlight(mob, wetTicks);
    }

    static int nextWetTicks(Mob mob, int previousTicks) {
        var world = mob.level();
        if (mob.isInWater() || OriginalWeather.rain(world, mob.blockPosition())
                || OriginalWeather.rain(world, crown(mob))) return 0;
        return previousTicks + 1;
    }

    static void sunlight(Mob mob, int wetTicks) {
        var world = mob.level();
        if (world.isClientSide || !SourceSpecialSpawns.sourceDay(world) || mob.isBaby()
                || !mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || !mob.isAlive()) return;

        var head = crown(mob);
        if (OriginalWeather.rain(world, head) || OriginalWeather.overcast(world, mob.blockPosition())) return;

        float light = OriginalWeather.brightness(mob);
        if (light <= .5f || mob.getRandom().nextFloat() * 30 >= (light - .4f) * 2
                || !world.canSeeSky(head)) return;

        if (wetTicks < 3) Network.environmentSteam(mob);
        else mob.setSecondsOnFire(8);
    }

    private static BlockPos crown(Mob mob) {
        return BlockPos.containing(mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ());
    }
}
