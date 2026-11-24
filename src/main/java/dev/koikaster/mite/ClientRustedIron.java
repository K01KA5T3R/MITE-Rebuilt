package dev.koikaster.mite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Original native metal silhouettes, brown rust palette; no edited PNG asset. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientRustedIron {
 @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){e.register((s,layer)->0xffad7650,RustedIronContent.NUGGET.get(),RustedIronContent.INGOT.get());}
 private ClientRustedIron(){}
}
