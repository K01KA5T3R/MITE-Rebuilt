package dev.koikaster.mite;

import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Vanilla's anvil combines damageable items without consulting Item.setNoRepair. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class BandageRepairEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventNativeCombination(AnvilUpdateEvent event) {
        if (event.getLeft().is(Content.BANDAGE.get()) && event.getRight().is(Content.BANDAGE.get())) {
            event.setCanceled(true);
        }
    }

    private BandageRepairEvents() {}
}
