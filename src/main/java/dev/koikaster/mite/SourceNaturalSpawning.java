package dev.koikaster.mite;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One source aci owner per server world. Native natural spawning retains the
 * selected modern species; its ordinary pool excludes the source-owned species. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceNaturalSpawning {
 public static final class State {
  final SourceSpawningKernel kernel=new SourceSpawningKernel();final SourceSpawningKernel.Counters counters=new SourceSpawningKernel.Counters();
  final SourceSpawnWorld world;long passes;int deepLimit,upperLimit;
  State(ServerLevel level){world=new SourceSpawnWorld(level,this);}
 }
 private static final Map<ServerLevel,State> WORLDS=new IdentityHashMap<>();
 static State state(ServerLevel level){return WORLDS.computeIfAbsent(level,State::new);}
 static boolean sourceType(EntityType<?> type){var id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);return id!=null&&SourceBiomeSpawns.owned(id.toString())||type==EntityType.BLAZE||type==EntityType.WITHER_SKELETON||type==EntityType.SILVERFISH;}
 @SuppressWarnings("unchecked") public static WeightedRandomList<MobSpawnSettings.SpawnerData> nativePool(Object raw){var rows=(WeightedRandomList<MobSpawnSettings.SpawnerData>)raw;return WeightedRandomList.create(rows.unwrap().stream().filter(row->!sourceType(row.type)).toList());}
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level)||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;var value=state(level);value.kernel.perform(value.world);value.passes++;}
 @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)WORLDS.remove(level);}
 private SourceNaturalSpawning(){}
}
