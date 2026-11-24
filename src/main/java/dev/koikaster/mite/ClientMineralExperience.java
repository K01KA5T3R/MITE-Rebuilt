package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientMineralExperience {
 @SubscribeEvent public static void tooltip(ItemTooltipEvent e){append(e,SourceTooltipLayout1447.extended(e,Screen.hasShiftDown()));}
 static void append(ItemTooltipEvent e,boolean shift){var s=e.getItemStack();
  var level=e.getEntity()==null?Minecraft.getInstance().level:e.getEntity().level();
  if((s.is(Items.REDSTONE)||s.is(Items.AMETHYST_SHARD))&&(level==null||!WorldContentMode.rebuilt(level)))return;
  int xp=MineralExperience.value(s,level);boolean mother=s.is(Items.BUDDING_AMETHYST);if(xp<1&&!mother)return;
  if(!shift)return;
  if(xp>=1)e.getToolTip().add(Component.translatable(s.is(Items.REDSTONE)?"tooltip.mite_reforged.redstone_xp":"tooltip.mite_reforged.mineral_xp",xp).withStyle(net.minecraft.ChatFormatting.GRAY));
  if(mother)e.getToolTip().add(Component.translatable("tooltip.mite_reforged.amethyst_mother").withStyle(net.minecraft.ChatFormatting.GRAY));
 }
 private ClientMineralExperience(){}
}
