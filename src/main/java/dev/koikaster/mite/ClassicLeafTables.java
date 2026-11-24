package dev.koikaster.mite;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Fix the base leaf table before generation: modern pools must not consume the source roll RNG. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicLeafTables {
 private static final Set<String> NAMES=Set.of("oak","spruce","birch","jungle","acacia","dark_oak","azalea","flowering_azalea","mangrove","cherry");
 @SubscribeEvent public static void load(LootTableLoadEvent event){var id=event.getName();if(!id.getNamespace().equals("minecraft"))return;String path=id.getPath();if(!path.startsWith("blocks/")||!path.endsWith("_leaves")||!NAMES.contains(path.substring(7,path.length()-7)))return;var table=LootTable.lootTable().setParamSet(LootContextParamSets.BLOCK).build();table.setLootTableId(id);event.setTable(table);}
 private ClassicLeafTables(){}
}
