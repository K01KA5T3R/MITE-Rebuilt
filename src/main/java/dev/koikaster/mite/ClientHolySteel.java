package dev.koikaster.mite;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Literal old iron PNG edits. Native item/armor models, no tint or added gold geometry. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientHolySteel {
 @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
  ItemProperties.register(Content.get("holy_steel_bucket"),Content.id("fluid"),(stack,level,entity,seed)->switch(ClassicVessels.fluid(stack)){case "water"->1;case "lava"->2;case "milk"->3;case "stone"->4;default->0;});
  ItemProperties.register(Content.get("holy_steel_anvil"),Content.id("wear"),(stack,level,entity,seed)->SourceAnvil.stage("holy_steel",stack.getTagElement("BlockEntityTag")==null?0:stack.getTagElement("BlockEntityTag").getInt("damage")));
  ItemProperties.register(Content.get("holy_steel_fishing_rod"),new ResourceLocation("cast"),(stack,level,entity,seed)->entity instanceof net.minecraft.world.entity.player.Player p&&p.fishing!=null&&(p.getMainHandItem()==stack||p.getOffhandItem()==stack)?1:0);
 });}
 public static final IClientItemExtensions ARMOR=new IClientItemExtensions(){};
 private ClientHolySteel(){}
}
