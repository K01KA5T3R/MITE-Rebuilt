package dev.koikaster.mite;
import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.client.multiplayer.ClientPacketListener;
/** Connection-owned 250ms placement use delay. No desktop input APIs are used. */
public final class SourcePlacementClient1075 {
 private static final Map<ClientPacketListener,Long> until=Collections.synchronizedMap(new WeakHashMap<>());
 public static void placed(){var connection=Minecraft.getInstance().getConnection();if(connection!=null)until.put(connection,System.currentTimeMillis()+250L);}
 public static boolean allowed(){var connection=Minecraft.getInstance().getConnection();return connection==null||System.currentTimeMillis()>=until.getOrDefault(connection,0L);}
 private SourcePlacementClient1075(){}
}
