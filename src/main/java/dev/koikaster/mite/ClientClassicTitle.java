package dev.koikaster.mite;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientClassicTitle {
 @SubscribeEvent public static void opening(ScreenEvent.Opening event){
  boolean enabled=dev.koikaster.mite.visual.ClassicVisuals.gui();
  if(!enabled&&event.getNewScreen() instanceof ClassicTitleScreen)event.setNewScreen(new TitleScreen());
  else if(enabled&&event.getNewScreen()!=null&&event.getNewScreen().getClass()==TitleScreen.class)event.setNewScreen(new ClassicTitleScreen());
 }
 @SubscribeEvent public static void tutorial(net.minecraftforge.event.TickEvent.ClientTickEvent event){
  if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!dev.koikaster.mite.visual.ClassicVisuals.gui())return;
  var mc=net.minecraft.client.Minecraft.getInstance();
  if(mc.options.tutorialStep!=net.minecraft.client.tutorial.TutorialSteps.NONE)mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
 }
 private ClientClassicTitle(){}
}
