package dev.koikaster.mite;
import java.util.*;
import net.minecraft.world.Difficulty;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class WorldEvents {
 public static final ResourceKey<Level> UNDERWORLD=ResourceKey.create(Registries.DIMENSION,Content.id("underworld"));
 public static void tick(MinecraftServer server){
  for(ServerPlayer p:server.getPlayerList().getPlayers()){
   var l=p.serverLevel();if(p.tickCount%200!=0||p.isSpectator()||p.isCreative()||l.getDifficulty()==Difficulty.PEACEFUL)continue;
   long day=l.getDayTime()/24000;var t=PlayerState.data(p);
   if(Settings.MOONS&&l.dimension()==Level.OVERWORLD&&t.getLong("NotifiedDay")!=day){
    t.putLong("NotifiedDay",day);Rules.Moon moon=SourceMoonCycle.kind(l.getDayTime());if(moon!=Rules.Moon.NORMAL)p.sendSystemMessage(Component.translatable("message.mite_reforged.moon_"+moon.name().toLowerCase(java.util.Locale.ROOT)));
   }
  }
 }
 // Ordinary dimensional access is now owned by real source portal frames and
 // SourcePortalTravel. A visit flag and a fixed-Y key destination cannot route it.
 private WorldEvents(){}
}
