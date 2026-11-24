package dev.koikaster.mite;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraftforge.common.loot.*;
public final class SurvivalLoot extends LootModifier {
 public static final Codec<SurvivalLoot> CODEC=RecordCodecBuilder.create(i->codecStart(i).apply(i,SurvivalLoot::new));
 public SurvivalLoot(LootItemCondition[] c){super(c);}
 protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext c){
  var settings=WorldContentMode.settings(c.getLevel());
  boolean rebuilt=settings.extras();
  var b=c.getParamOrNull(LootContextParams.BLOCK_STATE);var rand=c.getRandom();
  if(b!=null){
   SourceSnowMaterials.loot(loot,c,b);
   if(!rebuilt&&b.is(Blocks.NETHER_GOLD_ORE)&&!loot.isEmpty()){
    loot.clear();
    loot.add(new ItemStack(Items.NETHER_GOLD_ORE));
   }
   SourceSkillWorld1820.oreLoot(loot,c,b);
   if(b.is(Blocks.GRASS_BLOCK))SourceGrassDrops.apply(loot,c);
   if(Agriculture.supports(b))ClassicCropLoot.apply(loot,c,b);
   if(ClassicGravel.isGravel(b))ClassicGravel.apply(loot,c,b);
   if(b.getBlock() instanceof LeavesBlock)ClassicLeaves.apply(loot,c,b);
   if(b.is(Blocks.GRASS)||b.is(Blocks.TALL_GRASS)||b.is(Blocks.FERN)){
    var tool=c.getParamOrNull(LootContextParams.TOOL);
    if(rebuilt&&tool!=null&&tool.is(HarvestRules.KNIVES)&&rand.nextFloat()<.5f)loot.add(new ItemStack(Content.FIBRE.get()));
    if(rand.nextFloat()<.008f)loot.add(new ItemStack(Content.ONION_SEEDS.get()));
    if(rand.nextFloat()<.025f)loot.add(new ItemStack(Content.RAW_WORM.get()));
   }
   if(rebuilt&&(b.is(Blocks.GRASS)||b.is(Blocks.TALL_GRASS)||b.is(Blocks.FERN)||b.is(Blocks.LARGE_FERN))
      &&c.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof net.minecraft.world.entity.player.Player){
    var tool=c.getParamOrNull(LootContextParams.TOOL);
    if((tool==null||!(tool.getItem() instanceof ShearsItem)&&net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
       net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,tool)==0)&&rand.nextInt(64)==0){
     loot.add(new ItemStack(FruitForaging.berry(rand)));
    }
   }
  }else if(c.getQueriedLootTableId().getPath().startsWith("entities/"))ClassicButchering.apply(loot,c);
  else if(c.getQueriedLootTableId().getPath().startsWith("chests/")){
   for(int i=0;i<loot.size();i++){
    var stack=loot.get(i);var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
    if(key!=null&&key.getNamespace().equals("minecraft")){
     String n=key.getPath();
     if(stack.getItem() instanceof TieredItem||stack.getItem() instanceof ArmorItem)loot.set(i,new ItemStack(Items.IRON_NUGGET,1));
     else if(n.contains("diamond")||n.contains("netherite")&&!n.endsWith("smithing_template")||n.equals("obsidian")||n.equals("enchanted_golden_apple"))loot.set(i,new ItemStack(Items.FLINT,1));
     else if(n.endsWith("_ingot"))loot.set(i,new ItemStack(n.startsWith("gold")?Items.GOLD_NUGGET:Items.IRON_NUGGET,Math.min(4,stack.getCount())));
    }
   }
  }
  // Compatibility inputs are still converted by LegacyMetalOres below.
  loot.removeIf(stack->!LegacyMetalOres.hidden(stack)&&!RebuiltContent.allows(stack,settings));
  if(c.getQueriedLootTableId().equals(new net.minecraft.resources.ResourceLocation("minecraft:gameplay/fishing"))&&c.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof net.minecraft.world.entity.projectile.FishingHook hook)
   if(rebuilt)FishingRules.adjust(loot,c.getLevel(),hook.blockPosition(),rand);
   else ClassicFishingMode.catchFish(loot,c.getLevel(),hook,rand);
  LegacyMetalOres.loot(loot);RustedIronContent.loot(loot,c);UnderworldExplorationRewards.mineshaft(loot,c);MineralSupply.loot(loot,c);loot.removeIf(stack->ClassicMusic.removed(stack)||ModernContentPolicy.removed(stack));for(int i=0;i<loot.size();i++)loot.set(i,ModernContentPolicy.cleanEnchantments(loot.get(i)));
  if(b!=null)FurnaceExperience.carry(loot,b,c.getParamOrNull(LootContextParams.BLOCK_ENTITY));
  return loot;
 }
 public Codec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
