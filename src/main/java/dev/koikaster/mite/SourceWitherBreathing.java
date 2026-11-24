package dev.koikaster.mite;
import net.minecraftforge.event.entity.living.LivingDrownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 of.breathesAir excludes undead before both air use and drowning.
 * Forge otherwise checks already-negative air even when canDrown is false. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceWitherBreathing {
 @SubscribeEvent public static void drowning(LivingDrownEvent event){if(event.getEntity() instanceof SourceWitherBoss)event.setCanceled(true);}
 private SourceWitherBreathing(){}
}
