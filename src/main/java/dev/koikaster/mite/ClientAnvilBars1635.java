package dev.koikaster.mite;
import com.mojang.blaze3d.systems.RenderSystem;import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.gui.Font;import net.minecraft.world.item.ItemStack;import net.minecraft.world.level.block.Blocks;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.RegisterItemDecorationsEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
/** Original three rectangles, after native decoration, for the carried anvil only. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientAnvilBars1635 {
 @SubscribeEvent public static void decorators(RegisterItemDecorationsEvent event){for(var value:Content.ALL_BLOCKS.values())if(value.get() instanceof MetalAnvilBlock)event.register(value.get(),ClientAnvilBars1635::render);for(var block:new net.minecraft.world.level.block.Block[]{Blocks.ANVIL,Blocks.CHIPPED_ANVIL,Blocks.DAMAGED_ANVIL})event.register(block,ClientAnvilBars1635::render);}
 private static boolean render(GuiGraphics graphics,Font font,ItemStack stack,int x,int y){if(SourceAnvilBars1635.value(stack,0)!=1)return false;int width=SourceAnvilBars1635.value(stack,1);graphics.flush();RenderSystem.disableDepthTest();graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(),x+2,y+13,x+15,y+15,0xff000000);graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(),x+2,y+13,x+14,y+14,0xff000000|SourceAnvilBars1635.value(stack,3));if(width>0)graphics.fill(net.minecraft.client.renderer.RenderType.guiOverlay(),x+2,y+13,x+2+width,y+14,0xff000000|SourceAnvilBars1635.value(stack,2));graphics.flush();RenderSystem.enableDepthTest();return true;}
 private ClientAnvilBars1635(){}
}
