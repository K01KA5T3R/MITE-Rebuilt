package dev.koikaster.mite;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Local item.use runs before MultiPlayerGameMode returns/sends its vanilla use packet. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientVesselControl {
 public static void send(InteractionHand hand){Network.vesselControl(hand,Screen.hasControlDown());}
 @SubscribeEvent public static void tooltip(ItemTooltipEvent event){var stack=event.getItemStack();if(!SourceTooltipLayout1447.extended(event,Screen.hasShiftDown())||!ClassicVessels.handled(stack))return;String fluid=ClassicVessels.fluid(stack);if(fluid.equals("lava")&&!(stack.getItem() instanceof SpecialItems.MetalBucket))event.getToolTip().add(Component.translatable("tooltip.mite_reforged.bucket_melt_chance",Math.round(ClassicVessels.melting(stack)*100)));var player=Minecraft.getInstance().player;if(player!=null&&PlayerState.data(player).getInt("Xp")>=100&&(fluid.equals("water")||fluid.equals("lava")))event.getToolTip().add(Component.translatable("tooltip.mite_reforged.bucket_source"));}
 private ClientVesselControl(){}
}
