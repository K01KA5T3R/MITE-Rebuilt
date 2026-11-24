package dev.koikaster.mite;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** yc's68 armor declarations, ItemChain and ItemHorseArmor. Source recipes/assets are fixed bundled data. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class OriginalArmorItems {
 public static final JsonObject ROSTER=load();
 private static JsonObject load(){try(var in=OriginalArmorItems.class.getResourceAsStream("/data/mite_reforged/classic_armor_roster.json")){if(in==null)throw new IOException("missing original armor roster");return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException error){throw new ExceptionInInitializerError(error);}}
 public static void register(){for(var element:ROSTER.getAsJsonArray("armor")){var row=element.getAsJsonObject();if(!row.get("new").getAsBoolean())continue;String material=row.get("material").getAsString();boolean chain=row.get("chain").getAsBoolean();var type=ArmorItem.Type.valueOf(row.get("kind").getAsString().toUpperCase(Locale.ROOT));Content.item(row.get("name").getAsString(),()->new ArmorItem(new Material(material,chain),type,properties(material)));}
  for(var element:ROSTER.getAsJsonArray("chains")){var row=element.getAsJsonObject();Content.item(row.get("name").getAsString(),()->new Item(properties(row.get("material").getAsString()).stacksTo(8)));}
  for(var element:ROSTER.getAsJsonArray("horses")){var row=element.getAsJsonObject();Content.item(row.get("name").getAsString(),()->new HorseArmor(row.get("material").getAsString(),row.get("protection").getAsInt(),net.minecraft.resources.ResourceLocation.tryParse(row.get("texture").getAsString())));}
 }
 private static Item.Properties properties(String material){var properties=new Item.Properties();return (material.equals("adamantium")||material.equals("holy_steel"))?properties.fireResistant():properties;}
 public record Profile(String material,boolean chain,ArmorItem.Type type,int maxDamage,float materialProtection){
  public int components(){return OriginalArmorItems.components(type);}
  public boolean solidMetal(){return !chain&&!Set.of("leather","diamond","turtle","other").contains(material);}
 }
 public static int components(ArmorItem.Type type){return switch(type){case HELMET->5;case CHESTPLATE->8;case LEGGINGS->7;case BOOTS->4;};}
 public static float materialProtection(String m){return switch(m){case "leather"->2;case "rusted_iron","gold"->6;case "copper","silver"->7;case "iron","ancient_metal","diamond"->8;case "mithril"->9;case "adamantium"->10;case "holy_steel"->11;case "vibranium"->15;case "bronze"->7.5f;case "steel"->8.5f;case "netherite"->9.5f;default->0;};}
 public static float materialDurability(String m){return materialDurability(m,false);}
 private static float materialDurability(String m,boolean r196){return m.equals("leather")?1:m.equals("netherite")?128:LegacyToolWear.durability(m,r196);}
 public static Profile profile(ItemStack stack){if(!(stack.getItem() instanceof ArmorItem armor))return null;var material=armor.getMaterial();String name;boolean chain=false;if(material instanceof Material source){name=source.material();chain=source.chain();}else if(material instanceof Content.MetalArmor metal)name=metal.metal();else if(material==ArmorMaterials.LEATHER)name="leather";else if(material==ArmorMaterials.CHAIN){name="iron";chain=true;}else if(material==ArmorMaterials.IRON)name="iron";else if(material==ArmorMaterials.GOLD)name="gold";else if(material==ArmorMaterials.DIAMOND)name="diamond";else if(material==ArmorMaterials.NETHERITE)name="netherite";else name=material==ArmorMaterials.TURTLE?"turtle":"other";
  float durability=materialDurability(name,R196Gold.equipment(name));int maximum=durability>0?(int)(components(armor.getType())*durability*(chain?1:2)):material.getDurabilityForType(armor.getType());float protection=materialProtection(name);if(protection<=0)protection=armor.getDefense()*24f/components(armor.getType());return new Profile(name,chain,armor.getType(),maximum,protection-(chain?2:0));
 }
 public static int maxDamage(int original,ItemStack stack){var p=profile(stack);return p==null?original:p.maxDamage();}
 public static float factor(ItemStack stack,LivingEntity owner){if(owner!=null&&!(owner instanceof net.minecraft.world.entity.player.Player))return .5f;return (float)Rules.armorFraction(stack.getDamageValue(),stack.getMaxDamage());}
 public static float protection(ItemStack stack,LivingEntity owner){var p=profile(stack);if(p==null)return 0;float base=p.components()*p.materialProtection()/24f;int level=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION,stack);if(level>0)base+=base*(level/4f)*.5f;return base*factor(stack,owner);}
 public static int horseProtection(int original,Object item){if(item==Items.GOLDEN_HORSE_ARMOR)return 3;return original;}
 public static int enchantability(int original,Object item){if(!(item instanceof ArmorItem armor))return original;var material=armor.getMaterial();String name=material instanceof Material m?m.material():material instanceof Content.MetalArmor m?m.metal():material==ArmorMaterials.LEATHER?"leather":material==ArmorMaterials.CHAIN||material==ArmorMaterials.IRON?"iron":material==ArmorMaterials.GOLD?"gold":"";return switch(name){case "leather"->10;case "rusted_iron"->0;case "gold"->50;case "mithril","vibranium"->100;case "ancient_metal","adamantium","holy_steel"->40;case "copper","silver","iron","bronze","steel"->30;default->original;};}
 public static boolean protectionEligible(Object enchant,Object rawStack){var stack=(ItemStack)rawStack;var p=profile(stack);if(p==null)return false;if(enchant==Enchantments.FALL_PROTECTION)return p.type()==ArmorItem.Type.BOOTS;if(enchant==Enchantments.BLAST_PROTECTION||enchant==Enchantments.PROJECTILE_PROTECTION)return p.solidMetal()&&(p.type()==ArmorItem.Type.CHESTPLATE||p.type()==ArmorItem.Type.LEGGINGS);return true;}
 public static int protectionDecision(Object enchant,Object stack){return enchant==Enchantments.ALL_DAMAGE_PROTECTION||enchant==Enchantments.FIRE_PROTECTION||enchant==Enchantments.FALL_PROTECTION||enchant==Enchantments.BLAST_PROTECTION||enchant==Enchantments.PROJECTILE_PROTECTION?(protectionEligible(enchant,stack)?1:0):-1;}
 @SubscribeEvent public static void attributes(ItemAttributeModifierEvent event){var stack=event.getItemStack();var p=profile(stack);if(p==null||((ArmorItem)stack.getItem()).getEquipmentSlot()!=event.getSlotType())return;event.removeAttribute(Attributes.ARMOR);event.removeAttribute(Attributes.ARMOR_TOUGHNESS);event.removeAttribute(Attributes.KNOCKBACK_RESISTANCE);var id=UUID.nameUUIDFromBytes(("mite-source-armor-"+event.getSlotType().getName()).getBytes(StandardCharsets.UTF_8));event.addModifier(Attributes.ARMOR,new AttributeModifier(id,"R195 armor protection",protection(stack,null),AttributeModifier.Operation.ADDITION));}
 @SubscribeEvent public static void piercingName(ItemTooltipEvent event){var p=ClassicToolWear.profile(event.getItemStack());if(p==null||!(p.kind()==LegacyToolWear.Kind.AXE||p.kind()==LegacyToolWear.Kind.HATCHET||p.kind()==LegacyToolWear.Kind.BATTLEAXE))return;int level=EnchantmentHelper.getItemEnchantmentLevel(Content.PIERCING.get(),event.getItemStack());if(level<=0)return;String ordinary=Content.PIERCING.get().getFullname(level).getString();for(int i=0;i<event.getToolTip().size();i++)if(!SourceTooltipLore1669.authored(event.getItemStack(),event.getToolTip().get(i))&&event.getToolTip().get(i).getString().equals(ordinary))event.getToolTip().set(i,Component.translatable("enchantment.mite_reforged.cleaving").append(" ").append(Component.translatable("enchantment.level."+level)).withStyle(net.minecraft.ChatFormatting.GRAY));}
 public record Material(String material,boolean chain) implements ArmorMaterial {
  public int getDurabilityForType(ArmorItem.Type type){return (int)(components(type)*materialDurability(material)*(chain?1:2));}
  public int getDefenseForType(ArmorItem.Type type){return (int)(components(type)*(materialProtection(material)-(chain?2:0))/24f);}
  public int getEnchantmentValue(){return switch(material){case "rusted_iron"->0;case "gold"->50;case "ancient_metal","adamantium","holy_steel"->40;case "mithril"->100;case "leather"->10;default->30;};}
  public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_IRON;}
  public Ingredient getRepairIngredient(){return Ingredient.of(Content.nugget(material.equals("rusted_iron")?"iron":material));}
  public String getName(){return MiteReforged.ID+":"+material+(chain?"_chainmail":"");}
  public float getToughness(){return 0;}
  public float getKnockbackResistance(){return 0;}
 }
 public static final class HorseArmor extends HorseArmorItem {public final String material;public HorseArmor(String material,int protection,net.minecraft.resources.ResourceLocation texture){super(protection,texture,properties(material).stacksTo(1));this.material=material;}}
 private OriginalArmorItems(){}
}
