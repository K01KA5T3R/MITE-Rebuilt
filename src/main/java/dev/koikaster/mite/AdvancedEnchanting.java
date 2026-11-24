package dev.koikaster.mite;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Legality reuses existing MITE definitions. Material admission never guesses a name prefix. */
public final class AdvancedEnchanting {
 public enum Reason {READY,EMPTY,MATERIAL,UNSUPPORTED,CONFLICT,MAXIMUM,UPGRADES_DISABLED,CAPACITY,EXPERIENCE,INVALID}
 public record Row(ResourceLocation id,int minimum,int maximum,int current,Reason reason){}
 public record Quote(int levels,int points,int ordinaryFloor,Reason reason){}
 public interface MaterialEquipment {String advancedEnchantingMaterial(ItemStack stack);}
 public static String material(ItemStack stack){
  var item=stack.getItem();
  if(item instanceof MaterialEquipment explicit)return explicit.advancedEnchantingMaterial(stack);
  var armor=OriginalArmorItems.profile(stack);if(armor!=null&&!armor.material().equals("other"))return armor.material();
  if(item instanceof TieredItem tool){for(var entry:Content.TIERS.entrySet())if(tool.getTier()==entry.getValue())return entry.getKey();if(tool.getTier()==Tiers.NETHERITE)return "netherite";}
  // Explicit original registration records cover non-Tiered shears, rods and bows.
  for(var entry:OriginalToolItems.ENTRIES.entrySet())if(Content.get(entry.getKey())==item)return entry.getValue().get("material").getAsString();
  if(item==Content.get("mithril_bow"))return "mithril";
  if(item==Content.get("holy_steel_shears")||item==Content.get("holy_steel_fishing_rod"))return "holy_steel";
  if(stack.is(ItemTags.create(Content.id("advanced_enchanting_mithril"))))return "mithril";
  if(stack.is(ItemTags.create(Content.id("advanced_enchanting_adamantium"))))return "adamantium";
  if(stack.is(ItemTags.create(Content.id("advanced_enchanting_holy_steel"))))return "holy_steel";
  return "";
 }
 public static boolean accepts(ItemStack stack,AdvancedEnchantConfig.Policy policy){
  if(stack.isEmpty()||LargeBonePickaxe.matches(stack)||stack.getCount()!=1||ModernContentPolicy.removed(stack)||!stack.isDamageableItem()||stack.getMaxStackSize()!=1||stack.getEnchantmentValue()<=0)return false;
  int required=switch(policy.minimum()){case MITHRIL->4;case ADAMANTIUM->5;case HOLY_STEEL->6;};
  return LegacyHarvest.material(material(stack))>=required;
 }
 public static int maximum(Enchantment enchantment){var d=ClassicEnchanting.definition(enchantment);return d==null?enchantment.getMaxLevel():Math.min(d.levels(),enchantment.getMaxLevel());}
 public static int minimumPower(Enchantment enchantment,int rank){var d=ClassicEnchanting.definition(enchantment);return Math.max(1,d==null?enchantment.getMinCost(rank):d.declaredMinimum(rank));}
 public static boolean available(Enchantment enchantment,ItemStack stack){return !ModernContentPolicy.removed(enchantment)&&!enchantment.isCurse()&&ClassicEnchanting.eligible(enchantment,stack)&&(ClassicEnchanting.definition(enchantment)!=null||enchantment.canApplyAtEnchantingTable(stack));}
 public static List<Row> rows(ItemStack stack,AdvancedEnchantConfig.Policy policy){
  if(!accepts(stack,policy))return List.of();var existing=EnchantmentHelper.getEnchantments(stack);var rows=new ArrayList<Row>();
  for(var enchantment:ForgeRegistries.ENCHANTMENTS){var id=ForgeRegistries.ENCHANTMENTS.getKey(enchantment);if(id==null||!available(enchantment,stack))continue;int min=Math.max(1,enchantment.getMinLevel()),max=maximum(enchantment),current=existing.getOrDefault(enchantment,0);if(max<min||max>255)continue;
   Reason reason=Reason.READY;if(current>=max)reason=Reason.MAXIMUM;else if(current>0&&!policy.upgrades())reason=Reason.UPGRADES_DISABLED;else for(var other:existing.keySet())if(other!=enchantment&&(!other.isCompatibleWith(enchantment)||!enchantment.isCompatibleWith(other))){reason=Reason.CONFLICT;break;}
   rows.add(new Row(id,Math.max(min,current+1),max,current,reason));
  }
  rows.sort(Comparator.comparing(row->row.id().toString()));return List.copyOf(rows);
 }
 public static int shelfTier(int count,AdvancedEnchantConfig.Policy policy){return Math.min(policy.maximumTier(),20+2*Math.min(Math.max(count,0),policy.maximumShelves()));}
 private static int rarity(Enchantment e,AdvancedEnchantConfig.Policy p){var d=ClassicEnchanting.definition(e);if(d!=null)return d.weight()>=100?0:d.weight()>=25?p.uncommon():d.weight()>=5?p.rare():p.veryRare();return switch(e.getRarity()){case COMMON->0;case UNCOMMON->p.uncommon();case RARE->p.rare();case VERY_RARE->p.veryRare();};}
 private static int cumulative(Enchantment e,int rank,AdvancedEnchantConfig.Policy p){return rank<=0?0:p.base()+p.perRank()*rank+rarity(e,p)+(rank==maximum(e)?p.maximumRank():0);}
 /** Preserve the player's fraction of the next level using the existing R195 curve. */
 public static int remainingXp(int xp,int levels){int level=Rules.level(xp);if(levels<=0||levels>level)return -1;int target=level-levels;long oldSpan=level>=Rules.MAX_LEVEL?1:Rules.xpForLevel(level+1)-Rules.xpForLevel(level);long remainder=level>=Rules.MAX_LEVEL?0:Math.max(0,xp-Rules.xpForLevel(level));long newSpan=Rules.xpForLevel(target+1)-Rules.xpForLevel(target);return Rules.xpForLevel(target)+(int)Math.min(newSpan-1,remainder*newSpan/oldSpan);}
 public static Quote quote(ItemStack stack,ResourceLocation id,int rank,int shelves,int xp,AdvancedEnchantConfig.Policy policy){
  if(!accepts(stack,policy))return new Quote(0,0,0,stack.isEmpty()?Reason.EMPTY:Reason.MATERIAL);
  Enchantment e=ForgeRegistries.ENCHANTMENTS.getValue(id);if(e==null||!id.equals(ForgeRegistries.ENCHANTMENTS.getKey(e)))return new Quote(0,0,0,Reason.INVALID);
  var row=rows(stack,policy).stream().filter(r->r.id().equals(id)).findFirst().orElse(null);if(row==null)return new Quote(0,0,0,Reason.UNSUPPORTED);
  if(row.reason()!=Reason.READY)return new Quote(0,0,0,row.reason());if(rank<row.minimum()||rank>row.maximum())return new Quote(0,0,0,Reason.INVALID);
  int current=row.current(),other=EnchantmentHelper.getEnchantments(stack).size()-(current>0?1:0);
  int nominal=Math.max(1,cumulative(e,rank,policy)-cumulative(e,current,policy)+policy.perOther()*other);
  long ordinary=100L*Math.max(0,minimumPower(e,rank)-(current>0?minimumPower(e,current):0));long floor=(ordinary*(100+policy.premium())+99)/100;
  if(floor>Integer.MAX_VALUE)return new Quote(nominal,0,Integer.MAX_VALUE,Reason.CAPACITY);
  int level=Rules.level(xp),cost=nominal,points=0;
  for(;cost<=50;cost++){int virtual=Math.max(level,cost);int virtualXp=virtual==level?xp:Rules.xpForLevel(virtual);int remaining=remainingXp(virtualXp,cost);points=virtualXp-remaining;if(points>=floor)break;}
  if(cost>50||cost>shelfTier(shelves,policy))return new Quote(cost,points,(int)floor,Reason.CAPACITY);
  if(cost>level)return new Quote(cost,points,(int)floor,Reason.EXPERIENCE);
  int after=remainingXp(xp,cost);points=xp-after;return new Quote(cost,points,(int)floor,points>=floor?Reason.READY:Reason.INVALID);
 }
 private AdvancedEnchanting(){}
}
