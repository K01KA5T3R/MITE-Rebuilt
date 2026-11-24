package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

/** va/amv/vc adapters. Quality is disabled by38D; global source skills remain a separate port. */
public final class ClassicRepair {
 public record Profile(String material,int components,int cost,boolean chain,int wearMultiplier){}
 public record Result(ItemStack output,int consumed,int anvilDamage,String operation){static Result empty(){return new Result(ItemStack.EMPTY,0,0,"");}}
 public static Profile profile(ItemStack stack){
  if(stack.is(Items.ELYTRA))return new Profile("elytra",1,16,false,200); // V051: fixed slow leather repair, 1/16 per piece.
  if(!ClassicToolCatalogue.sourceItem(stack))return null;
  var tool=ClassicToolWear.profile(stack);if(tool!=null)return new Profile(tool.material(),LegacyToolWear.components(tool.kind()),LegacyProduction.toolRepairCost(LegacyToolWear.components(tool.kind())),false,1);
  String material=ClassicToolCatalogue.material(stack);
  if(stack.getItem() instanceof BowItem)return new Profile(material,material.equals("wood")?0:1,material.equals("wood")?0:2,false,200);
  if(stack.getItem() instanceof FishingRodItem)return new Profile(material,1,1,false,22);
  if(stack.getItem() instanceof ArmorItem){var armor=OriginalArmorItems.profile(stack);if(armor==null)return null;int components=armor.components();return new Profile(armor.material(),components,components*2/(armor.material().equals("leather")?2:1),armor.chain(),200);
  }return null;
 }
 public static int armorMaxDamage(int original,ItemStack stack){return OriginalArmorItems.maxDamage(original,stack);}
 public static Item repairItem(Profile p){if(p==null||p.cost()<=0)return Items.AIR;if(p.material().equals("elytra"))return Items.LEATHER;String m=p.material().equals("rusted_iron")?"iron":p.material();return switch(m){case "copper","silver","gold","iron","ancient_metal","mithril","adamantium","bronze","steel","holy_steel","netherite"->Content.nugget(m);default->Items.AIR;};}
 /** Working grade is independent of an alloy's greater equipment durability. */
 private static float workingStrength(String material){return LegacyToolWear.durability(switch(material){case "bronze"->"copper";case "steel"->"iron";default->material;});}
 public static boolean consumable(ItemStack stack,String anvil){return consumable(stack,anvil,false);}
 static boolean consumable(ItemStack stack,String anvil,boolean r196){
  if(stack.is(Items.LEATHER)||stack.is(Items.ENCHANTED_BOOK)||stack.is(Content.BOTTLE_OF_DISENCHANTING.get()))return true;
  if(stack.isDamageableItem()&&!stack.isEnchanted())return true;
  for(String metal:new String[]{"copper","silver","gold","iron","ancient_metal","mithril","adamantium","bronze","steel","holy_steel","netherite"}){
   if(stack.is(Content.nugget(metal)))return r196||workingStrength(metal)<=LegacyToolWear.durability(anvil);
  }
  return false;
 }
 public static boolean repairFits(Profile profile,ItemStack material,String anvil){return repairFits(profile,material,anvil,false);}
 private static boolean repairFits(Profile profile,ItemStack material,String anvil,boolean r196){
  if(profile==null||repairItem(profile)==Items.AIR||!material.is(repairItem(profile)))return false;
  String nugget=profile.material().equals("rusted_iron")?"iron":profile.material();
  return r196||Math.max(workingStrength(profile.material()),workingStrength(nugget))<=LegacyToolWear.durability(anvil);
 }
 static boolean requiresStrongerAnvil(Result preview,ItemStack first,String anvil){
  if(!(preview.operation().equals("repair")||preview.operation().equals("combine")))return false;
  var item=profile(first);
  if(item==null)return false;
  String material=item.material();
  // R196 compares the consumed material for nugget repairs and the first item's repair material for combinations.
  if(preview.operation().equals("repair")&&material.equals("rusted_iron"))material="iron";
  return r196Strength(material)>r196Strength(anvil);
 }
 private static float r196Strength(String material){return material.equals("gold")?4f:workingStrength(material);}
 public static boolean canRename(ItemStack stack){return !stack.isEmpty()&&!(stack.getItem() instanceof SourceRunestoneItem)&&!(stack.getItem() instanceof SourceReferencedBooks.Book)&&!stack.is(Items.BOOK)&&!stack.is(Items.WRITABLE_BOOK)&&!stack.is(Items.WRITTEN_BOOK);}
 public static Result result(ItemStack first,ItemStack second,String anvil,String requestedName){
  return result(first,second,anvil,requestedName,false);
 }
 static Result result(ItemStack first,ItemStack second,String anvil,String requestedName,boolean r196){
  if(first.isEmpty())return Result.empty();
  var profile=profile(first);boolean combine=!first.is(Content.BANDAGE.get())&&!second.isEmpty()&&first.is(second.getItem())&&first.isDamaged()&&!second.isEnchanted()&&(first.isEnchanted()||second.isDamaged());
  boolean repair=first.isDamaged()&&repairFits(profile,second,anvil,r196);boolean enchanting=false;
  Map<Enchantment,Integer> book=second.is(Items.ENCHANTED_BOOK)?new HashMap<>(EnchantmentHelper.getEnchantments(second)):new HashMap<>();book.keySet().removeIf(ModernContentPolicy::removed);
  if(first.isEnchantable()&&!first.isEnchanted())enchanting=book.keySet().stream().anyMatch(e->e.canEnchant(first));
  boolean disenchant=first.isEnchanted()&&second.is(Content.BOTTLE_OF_DISENCHANTING.get());
  String name=requestedName==null?(first.hasCustomHoverName()?first.getHoverName().getString():""):requestedName.trim();
  boolean rename=name.isBlank()?first.hasCustomHoverName():!name.equals(first.getHoverName().getString());
  if(!canRename(first)||!second.isEmpty()&&!combine&&!repair&&!enchanting&&!disenchant)rename=false;
  if(!combine&&!repair&&!enchanting&&!disenchant&&!rename)return Result.empty();
  var output=first.copy();int used=0,wear=0;String operation;
  if(combine){output.setDamageValue(LegacyProduction.combinedDamage(first.getMaxDamage(),first.getDamageValue(),second.getDamageValue()));operation="combine";}
  else if(repair){var change=LegacyProduction.repair(first.getDamageValue(),first.getMaxDamage(),profile.cost(),second.getCount(),profile.chain());if(change.consumed()<=0)return Result.empty();output.setDamageValue(change.remainingDamage());used=change.consumed();wear=(first.getDamageValue()-change.remainingDamage())*profile.wearMultiplier();operation="repair";}
  else if(enchanting){var values=new HashMap<>(EnchantmentHelper.getEnchantments(output));for(var entry:book.entrySet())if(entry.getKey().canEnchant(first)){int old=values.getOrDefault(entry.getKey(),0),next=old==entry.getValue()?entry.getValue()+1:Math.max(old,entry.getValue());values.put(entry.getKey(),Math.min(next,entry.getKey().getMaxLevel()));}EnchantmentHelper.setEnchantments(values,output);operation="enchant";}
  else if(disenchant){output.removeTagKey("Enchantments");operation="disenchant";}else operation="rename";
  if(canRename(first)){if(name.isBlank()){if(first.hasCustomHoverName())output.resetHoverName();}else if(!name.equals(first.getHoverName().getString()))output.setHoverName(Component.literal(name));}
  return new Result(output,used,wear,operation);
 }
 public static boolean anvil(BlockState state){return state.getBlock() instanceof MetalAnvilBlock||state.is(Blocks.ANVIL)||state.is(Blocks.CHIPPED_ANVIL)||state.is(Blocks.DAMAGED_ANVIL);}
 public static boolean accessible(Level level,BlockPos pos){return !level.getBlockState(pos.above()).blocksMotion();}
 public static void open(ServerPlayer player,BlockPos pos){var level=player.serverLevel();var state=level.getBlockState(pos);if(!anvil(state)||!accessible(level,pos))return;
  if(!(state.getBlock() instanceof MetalAnvilBlock)){int stage=state.is(Blocks.DAMAGED_ANVIL)?2:state.is(Blocks.CHIPPED_ANVIL)?1:0;var facing=state.getValue(AnvilBlock.FACING);level.setBlock(pos,Content.ALL_BLOCKS.get("iron_anvil").get().defaultBlockState().setValue(MetalAnvilBlock.FACING,facing).setValue(MetalAnvilBlock.DAMAGE,stage),3);if(level.getBlockEntity(pos) instanceof SourceAnvil entity)entity.setDamage(stage==2?SourceAnvil.minimumStage("iron",2):stage==1?SourceAnvil.minimumStage("iron",1):0);}
  if(level.getBlockEntity(pos) instanceof SourceAnvil entity)NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new SourceRepairMenu(id,inv,entity),entity.name==null?Component.translatable("container.repair"):entity.name),buffer->{buffer.writeBlockPos(pos);buffer.writeUtf(entity.material());});
 }
 private ClassicRepair(){}
}
