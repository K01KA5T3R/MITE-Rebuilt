package dev.koikaster.mite;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** New-world UI only. Re-pins selection after data-pack/experiment state changes. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientFixedWorldType {
 private static final Map<WorldCreationUiState,Boolean> WATCHED=new WeakHashMap<>();
 private static final Set<WorldCreationUiState> UPDATING=Collections.newSetFromMap(new WeakHashMap<>());
 @SubscribeEvent public static void initialize(ScreenEvent.Init.Pre event){
  if(!(event.getScreen() instanceof CreateWorldScreen screen))return;
  var state=screen.getUiState();if(WATCHED.put(state,true)==null)state.addListener(ClientFixedWorldType::pin);pin(state);
 }
 private static void restrict(List<WorldCreationUiState.WorldTypeEntry> entries,WorldCreationUiState.WorldTypeEntry large){
  if(entries.size()==1&&entries.get(0).equals(large))return;
  entries.clear();entries.add(large);
 }
 private static void pin(WorldCreationUiState state){
  if(!UPDATING.add(state))return;
  try{
   var holder=state.getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.LARGE_BIOMES);
   var large=new WorldCreationUiState.WorldTypeEntry(holder);
   if(!state.getWorldType().equals(large))state.setWorldType(large);
   restrict(state.getNormalPresetList(),large);restrict(state.getAltPresetList(),large);
  }finally{UPDATING.remove(state);}
 }
 private ClientFixedWorldType(){}
}
