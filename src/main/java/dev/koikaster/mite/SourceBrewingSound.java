package dev.koikaster.mite;
import java.util.*;import java.lang.ref.WeakReference;
import net.minecraft.world.inventory.BrewingStandMenu;import net.minecraft.world.level.block.entity.BlockEntity;import net.minecraft.sounds.SoundSource;
/** vf's per-menu cached brew time and listener loop, without modifying brewing state. */
public final class SourceBrewingSound {
 private static final class State{int time;final List<WeakReference<Object>> listeners=new ArrayList<>();}
 private static final Map<Object,State> STATES=new WeakHashMap<>();
 public static void watch(Object menu,Object container,Object listener,boolean add){if(!(container instanceof BlockEntity block)||block.getLevel()==null||block.getLevel().isClientSide)return;synchronized(STATES){var state=STATES.computeIfAbsent(menu,k->new State());state.listeners.removeIf(w->{var target=w.get();return target==null||!add&&target.equals(listener);});if(add&&state.listeners.stream().noneMatch(w->{var target=w.get();return target!=null&&target.equals(listener);}))state.listeners.add(new WeakReference<>(listener));}}
 public static void changed(Object raw,Object container,int current){
  if((!(raw instanceof BrewingStandMenu)&&!(raw instanceof SourceBrewingMenu1412))||!(container instanceof BlockEntity block)||block.getLevel()==null||block.getLevel().isClientSide)return;
  int listeners;synchronized(STATES){var state=STATES.computeIfAbsent(raw,k->new State());boolean start=SourceContextSoundRules.boil(state.time,current);state.time=current;state.listeners.removeIf(w->w.get()==null);listeners=start?state.listeners.size():0;}
  var level=block.getLevel();var pos=block.getBlockPos();for(int i=0;i<listeners;i++)level.playSound(null,(double)((float)pos.getX()+.5f),(double)((float)pos.getY()+.5f),(double)((float)pos.getZ()+.5f),SourceCreatureSounds.BOIL.get(),SoundSource.BLOCKS,.2f,.9f+(level.random.nextFloat()+level.random.nextFloat())/10f);
 }
 private SourceBrewingSound(){}
}
