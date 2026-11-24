package dev.koikaster.mite;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Native entry points only; the arithmetic and branch values are ported from R195, plus 0.4.16 ClassicQuality durability scaling. */
public final class ClassicToolWear {
 public record Profile(LegacyToolWear.Kind kind,String material){}
 public static Profile profile(ItemStack stack){
  if(stack.isEmpty()||!(stack.getItem() instanceof TieredItem||stack.getItem() instanceof ShearsItem)||ForgeRegistries.ITEMS==null)return null;
  if(CrabulonContent.fungalPickaxe(stack))return new Profile(LegacyToolWear.Kind.PICKAXE,"mithril");
  if(CrabulonContent.fungalSpear(stack))return new Profile(LegacyToolWear.Kind.SWORD,"mithril");
  var id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null||!id.getNamespace().equals("minecraft")&&!id.getNamespace().equals(MiteReforged.ID))return null;
  String path=id.getPath();if(path.equals("shears"))return new Profile(LegacyToolWear.Kind.SHEARS,"iron");int split=path.lastIndexOf('_');if(split<0)return null;String material=path.substring(0,split);material=switch(material){case "wooden"->"wood";case "golden"->"gold";default->material;};
  if(LegacyToolWear.durability(material)<=0)return null;
  try{return new Profile(LegacyToolWear.Kind.valueOf(path.substring(split+1).toUpperCase(java.util.Locale.ROOT)),material);}catch(IllegalArgumentException ignored){return null;}
 }
 private static int baseMaxDamage(int vanilla,ItemStack stack){
  if(LegacyFlintDurability749.handles(stack))return LegacyFlintDurability749.MAX;
  var p=profile(stack);if(p!=null)return LegacyToolWear.maxDamage(p.kind(),p.material(),R196Gold.equipment(p.material()));
  if(!ClassicToolCatalogue.sourceItem(stack))return vanilla;
  if(stack.getItem() instanceof BowItem)return ClassicToolCatalogue.bowDurability(ClassicToolCatalogue.material(stack));
  if(stack.getItem() instanceof FishingRodItem){var material=ClassicToolCatalogue.material(stack);return ClassicToolCatalogue.rodDurability(material,R196Gold.equipment(material));}
  return ClassicRepair.armorMaxDamage(vanilla,stack);
 }
 public static int maxDamage(int vanilla,Object rawStack){
  var stack=(ItemStack)rawStack;
  return ClassicQuality.applyDurabilityModifier(baseMaxDamage(vanilla,stack),stack);
 }
 private static boolean plantClothVine(BlockState state){return state.is(BlockTags.WOOL)||state.is(BlockTags.WOOL_CARPETS)||state.getBlock() instanceof BushBlock||state.getBlock() instanceof VineBlock;}
 public static int blockDamage(ItemStack stack,Player player,BlockState state,Level level,BlockPos pos){
  var p=profile(stack);if(p==null||player.isCreative()||!stack.isDamageableItem()||(state.getBlock() instanceof SourceMetalBuildings.Strongbox?SourceMetalBuildings.portable(state,level,pos,player):HarvestRules.portable(state)))return 0;
  float h=ClassicMining.hardness(state,level,pos);if(h<=0||ClassicMining.baseStrength(player,state)<=1f)return 0;
  boolean sandstone=state.is(Blocks.SANDSTONE)||state.is(Blocks.RED_SANDSTONE);
  boolean grass=state.is(Blocks.WHEAT)||state.is(Blocks.GRASS)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.FERN)||state.is(Blocks.LARGE_FERN);
  return LegacyToolWear.blockDamage(h,LegacyToolWear.blockDecay(p.kind(),sandstone,grass,plantClothVine(state)));
 }
 public static boolean mineBlock(Object rawStack,Object rawLevel,Object rawState,Object rawPos,Object rawActor){
  var stack=(ItemStack)rawStack;var level=(Level)rawLevel;var actor=(LivingEntity)rawActor;if(profile(stack)==null||!(actor instanceof Player player))return false;
  if(!level.isClientSide){int amount=DirectionalMining1765.wear(blockDamage(stack,player,(BlockState)rawState,level,(BlockPos)rawPos),stack,player,(BlockState)rawState,level,(BlockPos)rawPos);if(amount>0)stack.hurtAndBreak(amount,actor,p->p.broadcastBreakEvent(EquipmentSlot.MAINHAND));}
  return true;
 }
 public static boolean hurtEnemy(Object rawStack,Object rawTarget,Object rawActor){
  var stack=(ItemStack)rawStack;var profile=profile(stack);if(profile==null)return false;var actor=(LivingEntity)rawActor;
  if(!actor.level().isClientSide){boolean sharp=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS,stack)>0,vampiric=EnchantmentEffects.level("vampiric",stack)>0;stack.hurtAndBreak(LegacyToolWear.attackDamage(profile.kind(),vampiric,sharp),actor,p->p.broadcastBreakEvent(EquipmentSlot.MAINHAND));}
  return true;
 }
 /** R195 ye applies this per point to every enchanted item; there is no modern armor exception. */
 public static boolean ignoreDamage(Object rawStack,int level,Object rawRandom){return LegacyToolWear.ignoreDamage(level,((RandomSource)rawRandom).nextFloat());}
 /** R195 aas excludes blades and scythes; native modern-only equipment keeps its own eligibility. */
 public static int canUnbreak(Object rawStack){
  var stack=(ItemStack)rawStack;var p=profile(stack);if(p!=null)return switch(p.kind()){case SWORD,DAGGER,KNIFE,SCYTHE,SHEARS->0;default->1;};
  if(stack.getItem() instanceof ArmorItem){var armor=OriginalArmorItems.profile(stack);return armor!=null&&armor.solidMetal()?1:0;}
  if(stack.getItem() instanceof ShearsItem)return 0;
  return -1;
 }
 @SuppressWarnings("unchecked") public static void hoeWear(Object rawStack,int original,Object rawActor,Object rawConsumer){var stack=(ItemStack)rawStack;var p=profile(stack);stack.hurtAndBreak(p!=null&&p.kind()==LegacyToolWear.Kind.HOE?50:original,(LivingEntity)rawActor,(Consumer<LivingEntity>)rawConsumer);}
 public static boolean scythe(ItemStack stack){var p=profile(stack);return p!=null&&p.kind()==LegacyToolWear.Kind.SCYTHE;}
 private ClassicToolWear(){}
}
