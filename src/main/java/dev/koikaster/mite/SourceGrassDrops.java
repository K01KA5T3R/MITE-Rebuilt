package dev.koikaster.mite;

import java.util.function.IntUnaryOperator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** aon's nonsilk player lottery replaces dirt with one raw worm. */
public final class SourceGrassDrops {
 static boolean worm(boolean player,boolean freezing,boolean rain,int fortune,IntUnaryOperator random){if(!player||freezing)return false;fortune=Math.min(3,Math.max(0,fortune));if(rain)fortune+=12;fortune=Math.min(fortune,14);return random.applyAsInt(16-fortune)==0;}
 public static void apply(ObjectArrayList<ItemStack> loot,LootContext context){
  var tool=context.getParamOrNull(LootContextParams.TOOL);if(tool==null)tool=ItemStack.EMPTY;if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0)return;
  var who=context.getParamOrNull(LootContextParams.THIS_ENTITY);if(!(who instanceof Player))return;
  var origin=context.getParamOrNull(LootContextParams.ORIGIN);if(origin==null)return;var pos=BlockPos.containing(origin);var level=context.getLevel();var biome=level.getBiome(pos).value();boolean freezing=biome.getBaseTemperature()<=.15f;
  var up=pos.above();boolean blood=OriginalWeather.blood(level);
  boolean rain=(double)level.getRainLevel(1)>0.2&&(biome.hasPrecipitation()||blood)&&!freezing&&SourceFlora.sky(level,up)&&level.getHeight(Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ())<=up.getY();
  boolean worm=worm(true,freezing,rain,EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,tool),level.random::nextInt);loot.clear();loot.add(new ItemStack(worm?Content.RAW_WORM.get():Items.DIRT));
 }
 private SourceGrassDrops(){}
}
