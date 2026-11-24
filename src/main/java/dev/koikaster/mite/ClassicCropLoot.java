package dev.koikaster.mite;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** anr/ani/apt/BlockOnion/aqz: source yields, not Fortune multiplied native crop loot. */
public final class ClassicCropLoot {
 public static void apply(ObjectArrayList<ItemStack> loot,LootContext context,BlockState state){
  if(!Agriculture.supports(state))return;loot.clear();
  if(SourceCropBlight.blighted(state)&&!WorldContentMode.rebuilt(context.getLevel()))return;
  var crop=(CropBlock)state.getBlock();int age=crop.getAge(state);boolean mature=crop.isMaxAge(state);
  // apt's immature poisonous-potato branch precedes anr's drop-cause filter.
  if(state.is(Blocks.POTATOES)&&age>0&&!mature){loot.add(new ItemStack(Items.POISONOUS_POTATO));return;}
  boolean trampled=SourcePlantTrampling619.trampled(context,state);var entity=context.getParamOrNull(LootContextParams.THIS_ENTITY);boolean harvested=!trampled&&entity instanceof Player;
  boolean snowed=!trampled&&SourceSnowfallDrops.active(context.getLevel());
  boolean selfDropped=!trampled&&entity==null&&!context.hasParam(LootContextParams.EXPLOSION_RADIUS)&&!snowed;
  if(!harvested&&!selfDropped&&!snowed)return;
  Item item;int quantity;float chance=1;
  if(age==0){item=state.is(Blocks.WHEAT)?Items.WHEAT_SEEDS:produce(state);quantity=1;}
  else if(mature&&!selfDropped){item=produce(state);quantity=state.is(Blocks.WHEAT)?1:Math.random()<.25?3:2;var tool=context.getParamOrNull(LootContextParams.TOOL);if(tool!=null&&effective(tool,state))chance+=EnchantmentEffects.fraction("harvesting",tool)*.5f;}
  else return;
  int count=0;var random=context.getRandom();for(int i=0;i<quantity;i++)if(random.nextFloat()<chance){count++;if(chance>1&&random.nextFloat()<chance-1)count++;}if(count>0)loot.add(new ItemStack(item,count));
 }
 private static Item produce(BlockState state){return state.is(Blocks.WHEAT)?Items.WHEAT:state.is(Blocks.CARROTS)?Items.CARROT:state.is(Blocks.POTATOES)?Items.POTATO:Content.ONION.get();}
 private static boolean effective(ItemStack tool,BlockState state){var profile=ClassicToolWear.profile(tool);if(profile==null)return false;return state.is(Blocks.WHEAT)?profile.kind()==LegacyToolWear.Kind.SCYTHE:profile.kind()==LegacyToolWear.Kind.HOE||profile.kind()==LegacyToolWear.Kind.MATTOCK||profile.kind()==LegacyToolWear.Kind.SHOVEL;}
 private ClassicCropLoot(){}
}
