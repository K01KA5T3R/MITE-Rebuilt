package dev.koikaster.mite;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** jv destination was empty before transfer -> ael transient heal flag. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceEndEntry {
 private static final Set<ServerLevel> pending=Collections.newSetFromMap(new WeakHashMap<>());
 @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity().level() instanceof ServerLevel level&&level.dimension()==Level.END&&level.players().size()==1)pending.add(level);}
 public static boolean consume(ServerLevel level){return pending.remove(level);}
 private SourceEndEntry(){}
}
