package dev.koikaster.mite;
import com.mojang.blaze3d.systems.RenderSystem;import net.minecraft.client.gui.*;import net.minecraft.client.renderer.RenderType;import net.minecraft.world.item.*;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.RegisterItemDecorationsEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;import net.minecraftforge.registries.ForgeRegistries;
/** Keep non-durability custom bars and every other mod's items on their own path. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientEquipmentBars1659 {
 @SubscribeEvent public static void decorators(RegisterItemDecorationsEvent event){for(var item:ForgeRegistries.ITEMS){var id=ForgeRegistries.ITEMS.getKey(item);if(!(item instanceof BlockItem)&&id!=null&&(id.getNamespace().equals("minecraft")||id.getNamespace().equals(MiteReforged.ID)))event.register(item,ClientEquipmentBars1659::render);}}
 private static boolean render(GuiGraphics graphics,Font font,ItemStack stack,int x,int y){if(!stack.isBarVisible())return false;int width=SourceEquipmentBars1659.value(stack,1);if(width<0)return false;graphics.flush();RenderSystem.disableDepthTest();graphics.fill(RenderType.guiOverlay(),x+2,y+13,x+15,y+15,0xff000000);graphics.fill(RenderType.guiOverlay(),x+2,y+13,x+14,y+14,0xff000000|SourceEquipmentBars1659.value(stack,3));if(width>0)graphics.fill(RenderType.guiOverlay(),x+2,y+13,x+2+width,y+14,0xff000000|SourceEquipmentBars1659.value(stack,2));graphics.flush();RenderSystem.enableDepthTest();return true;}
 private ClientEquipmentBars1659(){}
}
