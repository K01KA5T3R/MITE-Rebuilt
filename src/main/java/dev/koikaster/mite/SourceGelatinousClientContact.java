package dev.koikaster.mite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class SourceGelatinousClientContact {
 @SubscribeEvent public static void movement(MovementInputUpdateEvent event){if(SourceGelatinousContact.slowed(event.getEntity())){event.getInput().leftImpulse*=.4f;event.getInput().forwardImpulse*=.4f;}}
 private SourceGelatinousClientContact(){}
}
