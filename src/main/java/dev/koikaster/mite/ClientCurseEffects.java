package dev.koikaster.mite;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** bcw/avj/nn notices:100ticks, pause countdown in screens, native spell particles, no narration. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientCurseEffects {
 private static int notification;
 public static void notice(int entityId,int kind,int id){var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;var actor=mc.level.getEntity(entityId);if(actor==mc.player){var data=PlayerState.data(mc.player);if(kind==1){data.putInt("CurseId",id);notification=100;}else if(kind==2){data.putInt("CurseId",0);data.putBoolean("CurseKnown",false);notification=100;}else if(kind==3)data.putBoolean("CurseKnown",true);}if(kind==3&&actor!=null){int count=actor==mc.player&&mc.options.getCameraType().isFirstPerson()?20:10;var random=mc.level.random;for(int i=0;i<count;i++)mc.level.addParticle(ParticleTypes.ENTITY_EFFECT,actor.getX()+random.nextFloat()*actor.getBbWidth()*2-actor.getBbWidth(),actor.getY()+random.nextFloat()*actor.getBbHeight(),actor.getZ()+random.nextFloat()*actor.getBbWidth()*2-actor.getBbWidth(),0,0,0);}}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase==TickEvent.Phase.END&&Minecraft.getInstance().screen==null&&notification>0)notification--;}
 @SubscribeEvent public static void render(RenderGuiEvent.Post event){var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui||notification<=0||OriginalCurseEffects.id(mc.player)<=0)return;int alpha=Math.min(255,(int)(notification*256f/10));if(alpha<=0)return;var text=Component.translatable("curse.mite_reforged.notify").withStyle(ChatFormatting.DARK_PURPLE);int y=mc.getWindow().getGuiScaledHeight()-59;if(mc.gameMode!=null&&!mc.gameMode.hasExperience())y+=14;event.getGuiGraphics().drawString(mc.font,text,(mc.getWindow().getGuiScaledWidth()-mc.font.width(text))/2,y,(alpha<<24)|0xffffff);}
 @SubscribeEvent public static void air(RenderGuiOverlayEvent.Pre event){var player=Minecraft.getInstance().player;if(player!=null&&event.getOverlay().id().equals(VanillaGuiOverlay.AIR_LEVEL.id())&&OriginalCurseEffects.has(player,2,false)&&!player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER))event.setCanceled(true);}
 // avj's original draw condition requires an active curse; its lift counter does not display text.
 private ClientCurseEffects(){}
}
