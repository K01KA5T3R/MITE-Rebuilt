package dev.koikaster.mite.visual;
import java.util.*;
import dev.koikaster.mite.MiteReforged;import dev.koikaster.mite.SourcePotionDisplay;
import net.minecraft.client.Minecraft;import net.minecraft.client.multiplayer.ClientLevel;import net.minecraft.core.BlockPos;import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.event.TickEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;

/** Per-client-world cosmetic side data; native entity state is never rewritten. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientPotionDisplay {
 private record Pair(int nativeColor,int classicColor){}
 private record Pending(SourcePotionDisplay.EntityColor value,long expires){}
 private record Key(int event,BlockPos position,int nativeColor){}
 private record Splash(int color,long expires){}
 private static ClientLevel level;private static long ticks;
 private static final Map<Entity,Pair> COLORS=new WeakHashMap<>();
 private static final LinkedHashMap<UUID,Pending> PENDING=new LinkedHashMap<>();
 private static final LinkedHashMap<Key,ArrayDeque<Splash>> SPLASHES=new LinkedHashMap<>();
 private static int splashCount;
 private static ClientLevel context(){var current=Minecraft.getInstance().level;if(current!=level){level=current;COLORS.clear();PENDING.clear();SPLASHES.clear();splashCount=0;}return current;}
 public static void receive(SourcePotionDisplay.EntityColor value){var world=context();if(world==null||!world.dimension().location().equals(value.dimension()))return;var entity=world.getEntity(value.entity());if(entity!=null&&entity.getUUID().equals(value.uuid()))COLORS.put(entity,new Pair(value.nativeColor(),value.classicColor()));else{PENDING.put(value.uuid(),new Pending(value,ticks+200));while(PENDING.size()>1024)PENDING.remove(PENDING.keySet().iterator().next());}}
 public static void receive(SourcePotionDisplay.SplashColor value){var world=context();if(world==null||!world.dimension().location().equals(value.dimension()))return;var key=new Key(value.event(),value.position(),value.nativeColor());SPLASHES.computeIfAbsent(key,k->new ArrayDeque<>()).addLast(new Splash(value.classicColor(),ticks+100));splashCount++;while(splashCount>256){var it=SPLASHES.entrySet().iterator();var first=it.next();first.getValue().removeFirst();splashCount--;if(first.getValue().isEmpty())it.remove();}}
 public static int entityColor(Entity entity,int nativeColor){var world=context();if(world!=entity.level())return nativeColor;var pending=PENDING.get(entity.getUUID());if(pending!=null&&(pending.expires<ticks||pending.value.entity()==entity.getId())){PENDING.remove(entity.getUUID());if(pending.expires>=ticks)COLORS.put(entity,new Pair(pending.value.nativeColor(),pending.value.classicColor()));}var pair=COLORS.get(entity);return ClassicVisualState.ready&&ClassicVisuals.particles()&&pair!=null&&pair.nativeColor==nativeColor?pair.classicColor:nativeColor;}
 /** Consume FIFO even with classic display disabled, so a later event cannot inherit stale color. */
 public static int splashColor(int event,BlockPos pos,int nativeColor){context();if(event!=2002&&event!=2007)return nativeColor;var key=new Key(event,pos,nativeColor);var queue=SPLASHES.get(key);if(queue==null)return nativeColor;while(!queue.isEmpty()){var entry=queue.removeFirst();splashCount--;if(queue.isEmpty())SPLASHES.remove(key);if(entry.expires>=ticks)return ClassicVisualState.ready&&ClassicVisuals.particles()?entry.color:nativeColor;}SPLASHES.remove(key);return nativeColor;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;ticks++;context();PENDING.values().removeIf(p->p.expires<ticks);var it=SPLASHES.values().iterator();while(it.hasNext()){var queue=it.next();while(!queue.isEmpty()&&queue.peekFirst().expires<ticks){queue.removeFirst();splashCount--;}if(queue.isEmpty())it.remove();}}
 private ClientPotionDisplay(){}
}
