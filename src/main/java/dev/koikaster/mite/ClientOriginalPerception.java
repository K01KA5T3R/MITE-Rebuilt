package dev.koikaster.mite;

import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** bfl's active Packet87 producer: actual rendered living mobs, one batch/second, at most64 entries. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientOriginalPerception {
 private static final LinkedHashMap<Integer,Short> pending=new LinkedHashMap<>();
 private static long nextPacket;
 @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event){var mc=Minecraft.getInstance();if(System.currentTimeMillis()<nextPacket||mc.player==null||!(event.getEntity() instanceof Mob mob)||!OriginalDespawn.handled(mob)||!OriginalDespawn.inPlayerView(mob,mc.player))return;if(pending.size()<64)pending.put(mob.getId(),(short)OriginalDespawn.renderedCap(mob));}
 @SubscribeEvent public static void stage(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||System.currentTimeMillis()<nextPacket)return;nextPacket=System.currentTimeMillis()+1000;if(!pending.isEmpty()){int[] ids=new int[pending.size()];short[] caps=new short[ids.length];int i=0;for(var entry:pending.entrySet()){ids[i]=entry.getKey();caps[i++]=entry.getValue();}Network.despawnCaps(ids,caps);pending.clear();}}
 private ClientOriginalPerception(){}
}
