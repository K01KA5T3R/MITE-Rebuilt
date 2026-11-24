package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientWarhammer {
 @SubscribeEvent public static void tooltip(ItemTooltipEvent e){append(e,SourceTooltipLayout1447.extended(e,Screen.hasShiftDown()));}
 static void append(ItemTooltipEvent e,boolean shift){if(shift&&WarhammerArea.hammer(e.getItemStack())&&enabled(e))e.getToolTip().add(Component.translatable("tooltip.mite_reforged.warhammer_area",WarhammerArea.RADIUS,WarhammerArea.MAX_EXTRA,40,WarhammerArea.INTERVAL/20).withStyle(net.minecraft.ChatFormatting.GRAY));}
 private static boolean enabled(ItemTooltipEvent e){var level=e.getEntity()==null?Minecraft.getInstance().level:e.getEntity().level();return level!=null&&WorldContentMode.rebuilt(level);}
 private ClientWarhammer(){}
}
