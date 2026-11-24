package dev.koikaster.mite;
import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.client.gui.screens.inventory.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.network.Connection;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.*;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
/** Silent source cache. No toast, chat line, tutorial or modern advancement page is opened. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientSourceAchievements570 {
 private static UUID player;private static Set<Integer> own=Set.of();private static Map<Integer,SourceAchievementNetwork570.First> world=Map.of();
 public static void receive(SourceAchievementNetwork570.Snapshot snapshot,Connection sender){var m=Minecraft.getInstance();if(m.player==null||m.getConnection()==null||m.getConnection().getConnection()!=sender||!m.player.getUUID().equals(snapshot.player()))return;player=snapshot.player();var values=new LinkedHashSet<Integer>();for(int id:snapshot.own())values.add(id);own=Collections.unmodifiableSet(values);var records=new LinkedHashMap<Integer,SourceAchievementNetwork570.First>();for(var r:snapshot.world())records.put(r.id(),r);world=Collections.unmodifiableMap(records);var state=new CompoundTag();var root=new CompoundTag();for(int id:own)root.putByte(Integer.toString(SourceAchievementCatalog561.node(id).statId()),(byte)1);state.put(SourceAchievementState561.KEY,root);PlayerState.data(m.player).put(SourceAchievementState561.KEY,root);}
 public static Set<Integer> own(){return own;}public static Map<Integer,SourceAchievementNetwork570.First> world(){return world;}
 public static boolean ready(){var p=Minecraft.getInstance().player;return p!=null&&p.getUUID().equals(player);}
 @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event){if(event.getScreen() instanceof InventoryScreen||event.getScreen() instanceof CreativeModeInventoryScreen)if(Minecraft.getInstance().player!=null)SourceAchievementNetwork570.request(true);}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){player=null;own=Set.of();world=Map.of();}
 private ClientSourceAchievements570(){}
}
