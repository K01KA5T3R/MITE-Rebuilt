package dev.koikaster.mite;
import java.util.*;
import dev.koikaster.mite.visual.ClassicPackResources;
import dev.koikaster.mite.visual.ClassicVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClassicResourcePacks {
 public static final String PROGRAMMER_ID="mite_reforged:programmer_art_base",COMPAT_ID="mite_reforged:classic_compat";
 public static final String R195_ID="mite_reforged:r195_original",VANILLA164_ID="mite_reforged:vanilla164_visuals";
 public static final String REDRAW_ID="mite_reforged:classic_redraw",IMPORT_ID="mite_reforged:classic_import";
 private static String applied;
 private static boolean discovered;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;
  var mc=Minecraft.getInstance();
  if(!canReload(mc))return;
  String signature=ClassicVisuals.signature();
  var repository=mc.getResourcePackRepository();boolean discovery=!discovered;if(discovery){repository.reload();discovered=true;}var available=repository.getAvailableIds();if(available.isEmpty())return;
  var selected=new ArrayList<>(repository.getSelectedIds());
  var ids=List.of(PROGRAMMER_ID,VANILLA164_ID,R195_ID,COMPAT_ID);
  var desired=ids.stream().filter(available::contains).toList();
  var previous=List.copyOf(selected);
  selected.removeAll(ids);
  selected.removeAll(List.of(REDRAW_ID,IMPORT_ID,"programmer_art"));
  // ResourceManager layers later packs over earlier packs. Keep user overlays above the built-in classic stack.
  int baseEnd=Math.max(selected.indexOf("vanilla"),selected.indexOf("mod_resources"))+1;
  selected.addAll(baseEnd,desired);
  boolean changed=!selected.equals(previous);
  if(signature.equals(applied)&&!changed&&!discovery)return;
  boolean categoryChanged=applied!=null&&!signature.equals(applied);applied=signature;
  if(changed||categoryChanged||discovery){
   repository.setSelected(selected);
   if(categoryChanged&&((!ClassicVisuals.gui()&&mc.screen instanceof ClassicTitleScreen)||(ClassicVisuals.gui()&&mc.screen!=null&&mc.screen.getClass()==net.minecraft.client.gui.screens.TitleScreen.class)))mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
   mc.reloadResourcePacks().exceptionally(error->{com.mojang.logging.LogUtils.getLogger().error("Classic visual resource reload failed",error);return null;});
  }
 }
 private static boolean canReload(Minecraft minecraft){
  if(!minecraft.isRunning()||minecraft.getOverlay()!=null)return false;
  if(minecraft.screen instanceof ConnectScreen||minecraft.screen instanceof ReceivingLevelScreen)return false;
  var listener=minecraft.getConnection();
  // Forge restores its frozen registries as the disconnected world is cleared.
  // Wait for the title screen before switching the pack filter back to the menu profile.
  if(minecraft.level==null)return listener==null&&minecraft.screen instanceof TitleScreen;
  return listener!=null&&listener.getConnection().isConnected();
 }
 public static void addImports(AddPackFindersEvent event){
  if(event.getPackType()!=PackType.CLIENT_RESOURCES)return;
  var file=net.minecraftforge.fml.ModList.get().getModFileById(MiteReforged.ID).getFile();
  event.addRepositorySource(consumer->{
   var programmer=Pack.readMetaAndCreate(PROGRAMMER_ID,Component.literal("Programmer Art"),true,id->new ClassicPackResources(id,file.findResource("retro_programmer_art")),PackType.CLIENT_RESOURCES,Pack.Position.TOP,PackSource.BUILT_IN);if(programmer!=null)consumer.accept(programmer);
   var compat=Pack.readMetaAndCreate(COMPAT_ID,Component.literal("Classic Visuals"),true,id->new ClassicPackResources(id,file.findResource("retro_compat")),PackType.CLIENT_RESOURCES,Pack.Position.TOP,PackSource.BUILT_IN);if(compat!=null)consumer.accept(compat);
   var base=Pack.readMetaAndCreate(VANILLA164_ID,Component.literal("Minecraft 1.6.4"),true,id->new ClassicPackResources(id,file.findResource("retro_164")),PackType.CLIENT_RESOURCES,Pack.Position.TOP,PackSource.BUILT_IN);if(base!=null)consumer.accept(base);
   var r195=Pack.readMetaAndCreate(R195_ID,Component.translatable("gui.mite_reforged.r195_original"),true,id->new ClassicPackResources(id,file.findResource("retro_r195")),PackType.CLIENT_RESOURCES,Pack.Position.TOP,PackSource.BUILT_IN);if(r195!=null)consumer.accept(r195);
  });
 }
 private ClassicResourcePacks(){}
}
