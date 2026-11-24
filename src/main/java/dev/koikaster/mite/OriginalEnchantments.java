package dev.koikaster.mite;
import java.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.RegistryObject;

/** In-progress0.21 source registration. aau/38classes exact definitions;
 * table selection, native counterpart rules and gameplay consumers must be wired before release.
 */
public final class OriginalEnchantments {
 public record Definition(String name,int legacyId,int weight,int difficulty,int levels){
  public boolean hasLevels(){return levels>1;}
  /** aau.getLevelFromEnchantmentLevels, deliberately distinct from the declared minimum-cost method. */
  public int levelFromCost(int amount){if(!hasLevels())return amount<difficulty*2+1||amount>difficulty*3?0:1;if(amount<=difficulty-10)return 0;int level=(amount+difficulty-1)/difficulty;return level<1||level>levels?0:level;}
  public int declaredMinimum(int level){if(!hasLevels())level=3;return Math.max(difficulty-10,0)+difficulty*(level-1)+1;}
  Enchantment.Rarity rarity(){return weight==100?Enchantment.Rarity.COMMON:weight==25?Enchantment.Rarity.UNCOMMON:weight==5?Enchantment.Rarity.RARE:Enchantment.Rarity.VERY_RARE;}
 }
 public static final List<Definition> DEFINITIONS=List.of(
  new Definition("arrow_recovery",51,25,10,5),new Definition("stun",80,25,15,5),new Definition("fishing_fortune",81,100,10,5),new Definition("fertility",82,25,10,5),new Definition("tree_felling",83,25,10,5),new Definition("vampiric",84,1,20,5),new Definition("speed",85,5,10,5),new Definition("regeneration",86,5,20,5),new Definition("free_action",87,25,10,4),new Definition("quickness",88,25,10,5),new Definition("true_flight",89,100,10,5),new Definition("poison",90,5,10,5),new Definition("disarming",91,5,10,5),new Definition("piercing",92,5,10,5),new Definition("harvesting",93,25,10,5),new Definition("butchering",94,25,10,3),new Definition("endurance",95,25,10,4));
 public static final Map<String,RegistryObject<Enchantment>> NEW=new LinkedHashMap<>();
 private static EquipmentSlot slot(String name){return switch(name){case "speed"->EquipmentSlot.FEET;case "regeneration","endurance"->EquipmentSlot.CHEST;case "free_action"->EquipmentSlot.LEGS;default->EquipmentSlot.MAINHAND;};}
 public static void registerMissing(){for(var d:DEFINITIONS)if(!Set.of("regeneration","piercing").contains(d.name()))NEW.put(d.name(),Content.ENCHANTMENTS.register(d.name(),()->new SourceEnchantment(d)));}
 public static boolean eligible(String name,ItemStack stack){var tool=ClassicToolWear.profile(stack);var armor=OriginalArmorItems.profile(stack);var kind=tool==null?null:tool.kind();boolean swordFamily=kind==LegacyToolWear.Kind.SWORD||kind==LegacyToolWear.Kind.DAGGER||kind==LegacyToolWear.Kind.KNIFE;
  return switch(name){
   case "arrow_recovery","quickness","true_flight","poison"->stack.getItem() instanceof BowItem;
   case "fishing_fortune"->stack.getItem() instanceof FishingRodItem;
   case "stun"->kind==LegacyToolWear.Kind.CUDGEL||kind==LegacyToolWear.Kind.CLUB||kind==LegacyToolWear.Kind.WARHAMMER;
   case "fertility","harvesting"->kind==LegacyToolWear.Kind.HOE||kind==LegacyToolWear.Kind.MATTOCK||kind==LegacyToolWear.Kind.SCYTHE;
   case "tree_felling"->kind==LegacyToolWear.Kind.AXE||kind==LegacyToolWear.Kind.BATTLEAXE;
   case "vampiric"->tool!=null&&!Set.of("silver","mithril").contains(tool.material())&&(swordFamily||kind==LegacyToolWear.Kind.SCYTHE);
   case "speed"->armor!=null&&armor.type()==ArmorItem.Type.BOOTS;
   case "regeneration","endurance"->armor!=null&&armor.type()==ArmorItem.Type.CHESTPLATE;
   case "free_action"->armor!=null&&armor.type()==ArmorItem.Type.LEGGINGS;
   case "disarming"->kind==LegacyToolWear.Kind.SWORD;
   case "piercing"->kind==LegacyToolWear.Kind.PICKAXE||kind==LegacyToolWear.Kind.BATTLEAXE;
   case "butchering"->kind==LegacyToolWear.Kind.KNIFE||kind==LegacyToolWear.Kind.DAGGER;
   default->false;
  };
 }
 public static Definition definition(String name){return DEFINITIONS.stream().filter(d->d.name().equals(name)).findFirst().orElse(null);}
 public static final class SourceEnchantment extends Enchantment {
  final Definition definition;SourceEnchantment(Definition d){super(d.rarity(),EnchantmentCategory.BREAKABLE,new EquipmentSlot[]{slot(d.name())});definition=d;}
  public int getMaxLevel(){return definition.levels();}
  public boolean canEnchant(ItemStack stack){return eligible(definition.name(),stack);}
  public boolean canApplyAtEnchantingTable(ItemStack stack){return eligible(definition.name(),stack);}
  public int getMinCost(int level){return definition.declaredMinimum(level);}
  public int getMaxCost(int level){return Math.max(getMinCost(level),definition.difficulty()*level);}
 }
 private OriginalEnchantments(){}
}
