package dev.koikaster.mite;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;

/** R195 aau/aaw/wn/EnumEquipmentMaterial. Source selection is separate from native offers and level costs. */
public final class ClassicEnchanting {
 public record Entry(OriginalEnchantments.Definition definition,Enchantment enchantment){
  int minimum(int level){return definition.legacyId()>=256?enchantment.getMinCost(level):definition.declaredMinimum(level);}
 }
 private static final Map<String,OriginalEnchantments.Definition> NATIVE=new LinkedHashMap<>();
 static {nativeEntry("protection",0,100,10,4);nativeEntry("fire_protection",1,25,10,4);nativeEntry("feather_falling",2,25,10,4);nativeEntry("blast_protection",3,25,10,4);nativeEntry("projectile_protection",4,25,10,4);nativeEntry("respiration",5,5,10,3);nativeEntry("aqua_affinity",6,5,10,1);nativeEntry("thorns",7,5,20,3);nativeEntry("sharpness",16,100,10,5);nativeEntry("smite",17,25,10,5);nativeEntry("bane_of_arthropods",18,25,10,5);nativeEntry("knockback",19,25,10,2);nativeEntry("fire_aspect",20,5,20,2);nativeEntry("looting",21,25,10,3);nativeEntry("efficiency",32,100,10,5);nativeEntry("silk_touch",33,5,10,1);nativeEntry("unbreaking",34,25,10,5);nativeEntry("fortune",35,5,10,3);nativeEntry("power",48,100,10,5);nativeEntry("punch",49,25,10,2);nativeEntry("flame",50,5,20,1);}
 private static void nativeEntry(String name,int id,int weight,int difficulty,int levels){NATIVE.put(name,new OriginalEnchantments.Definition(name,id,weight,difficulty,levels));}
 public static OriginalEnchantments.Definition definition(Enchantment enchantment){var key=ForgeRegistries.ENCHANTMENTS.getKey(enchantment);if(key==null)return null;if(key.getNamespace().equals("minecraft"))return NATIVE.get(key.getPath());return key.getNamespace().equals(MiteReforged.ID)?OriginalEnchantments.definition(key.getPath()):null;}
 public static List<Entry> entries(boolean treasure){var result=new ArrayList<Entry>();for(var d:NATIVE.values()){var e=ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft",d.name()));if(e!=null)result.add(new Entry(d,e));}for(var d:OriginalEnchantments.DEFINITIONS){var e=ForgeRegistries.ENCHANTMENTS.getValue(Content.id(d.name()));if(e!=null)result.add(new Entry(d,e));}if(treasure)result.add(new Entry(new OriginalEnchantments.Definition("slow_mending",256,5,25,1),Content.SLOW_MENDING.get()));result.sort(Comparator.comparingInt(e->e.definition().legacyId()));return result;}
 public static boolean special(ItemStack stack){return stack.is(Items.GOLDEN_APPLE)||stack.is(Items.POTION)&&PotionUtils.getPotion(stack)==Potions.WATER;}
 public static boolean handles(ItemStack stack){return ClassicToolCatalogue.sourceItem(stack)&&(stack.is(Items.BOOK)||special(stack)||ClassicToolWear.profile(stack)!=null||OriginalArmorItems.profile(stack)!=null||stack.getItem() instanceof BowItem||stack.getItem() instanceof FishingRodItem);}
 public static boolean handles(Object stack){return handles((ItemStack)stack);}
 private static int materialEnchantability(String material,int fallback){return switch(material){case "leather","wood"->10;case "flint","obsidian","rusted_iron"->0;case "copper","silver","iron"->30;case "gold"->50;case "ancient_metal","adamantium","holy_steel"->40;case "mithril","diamond"->100;default->fallback;};}
 public static int enchantability(int original,Object rawStack){var stack=(ItemStack)rawStack;if(!ClassicToolCatalogue.sourceItem(stack))return original;if(stack.is(Items.BOOK))return 30;var tool=ClassicToolWear.profile(stack);if(tool!=null)return materialEnchantability(tool.material(),original);if(stack.getItem() instanceof BowItem||stack.getItem() instanceof FishingRodItem)return Math.max(10,materialEnchantability(ClassicToolCatalogue.material(stack),original));return original;}
 public static int stackEnchantability(Object rawStack){var stack=(ItemStack)rawStack;return enchantability(stack.getItem().getEnchantmentValue(stack),stack);}
 public static boolean accepts(Object rawStack){var stack=(ItemStack)rawStack;if(stack.isEmpty()||LargeBonePickaxe.matches(stack))return false;if(stack.is(Items.BOOK)||special(stack))return true;return stack.getMaxStackSize()==1&&stack.isDamageableItem()&&stack.getEnchantmentValue()>0&&!stack.isEnchanted();}
 public static boolean eligible(Enchantment enchantment,ItemStack stack){if(ModernContentPolicy.removed(enchantment))return false;var d=definition(enchantment);if(d==null)return enchantment.canEnchant(stack);if(d.legacyId()>=51)return OriginalEnchantments.eligible(d.name(),stack);var tool=ClassicToolWear.profile(stack);var armor=OriginalArmorItems.profile(stack);var kind=tool==null?null:tool.kind();boolean sword=kind==LegacyToolWear.Kind.SWORD||kind==LegacyToolWear.Kind.DAGGER||kind==LegacyToolWear.Kind.KNIFE;return switch(d.name()){
  case "protection","fire_protection","feather_falling","blast_protection","projectile_protection"->OriginalArmorItems.protectionEligible(enchantment,stack);
  case "respiration","aqua_affinity"->armor!=null&&armor.type()==ArmorItem.Type.HELMET;
  case "thorns"->armor!=null&&armor.type()==ArmorItem.Type.CHESTPLATE;
  case "sharpness"->sword||kind==LegacyToolWear.Kind.BATTLEAXE||kind==LegacyToolWear.Kind.SCYTHE;
  case "smite"->kind==LegacyToolWear.Kind.WARHAMMER;
  case "bane_of_arthropods","fire_aspect"->sword;
  case "knockback"->kind==LegacyToolWear.Kind.CUDGEL||kind==LegacyToolWear.Kind.CLUB||kind==LegacyToolWear.Kind.WARHAMMER;
  case "looting"->sword||kind==LegacyToolWear.Kind.CUDGEL||kind==LegacyToolWear.Kind.CLUB;
  case "efficiency"->kind==LegacyToolWear.Kind.PICKAXE||kind==LegacyToolWear.Kind.AXE||kind==LegacyToolWear.Kind.HATCHET||kind==LegacyToolWear.Kind.BATTLEAXE||kind==LegacyToolWear.Kind.SHOVEL||kind==LegacyToolWear.Kind.MATTOCK||kind==LegacyToolWear.Kind.HOE;
  case "silk_touch"->kind==LegacyToolWear.Kind.PICKAXE||kind==LegacyToolWear.Kind.SHOVEL||kind==LegacyToolWear.Kind.SHEARS||kind==LegacyToolWear.Kind.KNIFE||kind==LegacyToolWear.Kind.DAGGER;
  case "fortune"->kind==LegacyToolWear.Kind.PICKAXE||kind==LegacyToolWear.Kind.SHOVEL;
  case "unbreaking"->ClassicToolWear.canUnbreak(stack)>0;
  case "power","punch","flame"->stack.getItem() instanceof BowItem;
  default->false;};}
 /** Negative preserves unrelated modern/foreign enchantments and the frozen0.20 protection adapter. */
 public static int eligibilityDecision(Object rawEnchant,Object rawStack){var enchantment=(Enchantment)rawEnchant;if(ModernContentPolicy.removed(enchantment))return 0;var stack=(ItemStack)rawStack;var d=definition(enchantment);if(d==null||d.legacyId()<=4||d.legacyId()==34||!ClassicToolCatalogue.sourceItem(stack))return -1;return eligible(enchantment,stack)?1:0;}
 public static int applyingDecision(Object rawEnchant,Object rawStack){var enchantment=(Enchantment)rawEnchant;if(ModernContentPolicy.removed(enchantment))return 0;var stack=(ItemStack)rawStack;return definition(enchantment)==null||!ClassicToolCatalogue.sourceItem(stack)?-1:eligible(enchantment,stack)?1:0;}
 public static boolean sourceDamageEnchantment(Object enchantment){return definition((Enchantment)enchantment)!=null;}
 public static float damageBonus(Object rawEnchant,int level,Object rawType){var enchantment=(DamageEnchantment)rawEnchant;var type=(net.minecraft.world.entity.MobType)rawType;boolean source=definition(enchantment)!=null;if(enchantment.type==0)return source?level:1f+Math.max(0,level-1)*.5f;if(enchantment.type==1&&type==net.minecraft.world.entity.MobType.UNDEAD||enchantment.type==2&&type==net.minecraft.world.entity.MobType.ARTHROPOD)return level*(source?2f:2.5f);return 0;}
 public static int alterLevels(int levels,int enchantability){if(enchantability<1)return 0;if(levels<=enchantability)return levels;float altered=enchantability;for(int i=enchantability+1;i<=levels;i++){if(i<=enchantability*2)altered+=.5f;else if(i<=enchantability*3)altered+=.25f;else break;}return Math.round(altered);}
 public static List<EnchantmentInstance> select(RandomSource random,ItemStack stack,int levels,boolean treasure){if(stack.getEnchantmentValue()<=0)return new ArrayList<>();int power=Math.max((int)(levels*(1+(random.nextFloat()-.5f)*.5f)),1);var selected=new ArrayList<EnchantmentInstance>();var definitions=entries(treasure);while(power>0){var candidates=new HashMap<Integer,EnchantmentInstance>();for(var entry:definitions){if(!stack.is(Items.BOOK)&&!eligible(entry.enchantment(),stack))continue;for(int level=entry.definition().levels();level>=1;level--)if(entry.minimum(level)<=power){candidates.put(entry.definition().legacyId(),new EnchantmentInstance(entry.enchantment(),level));break;}}for(var previous:selected)candidates.values().removeIf(candidate->!previous.enchantment.isCompatibleWith(candidate.enchantment));if(candidates.isEmpty())break;int total=0;for(var id:candidates.keySet())total+=entry(definitions,id).definition().weight();int chosen=random.nextInt(total);EnchantmentInstance pick=null;Entry pickEntry=null;for(var id:candidates.keySet()){var entry=entry(definitions,id);if((chosen-=entry.definition().weight())<0){pick=candidates.get(id);pickEntry=entry;break;}}if(pick==null)break;if(selected.size()<2&&candidates.size()>1&&pickEntry.definition().hasLevels()&&random.nextInt(2)==0)pick=new EnchantmentInstance(pick.enchantment,random.nextInt(pick.level)+1);selected.add(pick);power-=pickEntry.minimum(pick.level);power-=5;if(power<5||selected.size()>2)break;}var shuffled=new ArrayList<EnchantmentInstance>();int remaining=selected.size();while(remaining>0){int i=random.nextInt(selected.size());var value=selected.get(i);if(value==null)continue;shuffled.add(value);selected.set(i,null);remaining--;}return shuffled;}
 private static Entry entry(List<Entry> entries,int legacyId){return entries.stream().filter(e->e.definition().legacyId()==legacyId).findFirst().orElseThrow();}
 public static List<EnchantmentInstance> selectNative(Object rawRandom,Object rawStack,int levels,boolean treasure){var stack=(ItemStack)rawStack;return select((RandomSource)rawRandom,stack,alterLevels(levels,stack.getEnchantmentValue()),treasure);}
 private ClassicEnchanting(){}
}
