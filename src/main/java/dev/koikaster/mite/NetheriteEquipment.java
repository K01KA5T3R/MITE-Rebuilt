package dev.koikaster.mite;

import java.util.Set;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

/** Completes the existing steel-to-netherite equipment route without duplicate vanilla gear. */
public final class NetheriteEquipment {
 public static final Set<String> EXTRA=Set.of("warhammer","mattock","battleaxe","scythe","dagger","knife","hatchet","shears");
 public static void register(){
  Content.item("netherite_nugget",()->new Item(new Item.Properties().fireResistant()));
  var tier=new ForgeTier(LegacyHarvest.material("netherite"),LegacyToolWear.maxDamage(LegacyToolWear.Kind.PICKAXE,"netherite"),LegacyMining.materialEfficiency("netherite"),ClassicToolCatalogue.materialDamage("netherite"),Tiers.NETHERITE.getEnchantmentValue(),BlockTags.NEEDS_DIAMOND_TOOL,()->Ingredient.of(Content.nugget("netherite")));
  Content.TIERS.put("netherite",tier);
  for(String kind:new String[]{"warhammer","mattock","battleaxe","scythe","dagger","knife","hatchet","shears"})Content.item("netherite_"+kind,()->{
   var p=new Item.Properties().fireResistant();return switch(kind){
    case "warhammer"->new PickaxeItem(tier,0,-2.4f,p);
    case "mattock"->new Tools.Mattock(tier,0,-2.4f,p);
    case "battleaxe","hatchet"->new Tools.Axe(tier,0,-2.4f,p);
    case "scythe"->new Tools.Hoe(tier,0,-2.4f,p);
    case "dagger","knife"->new Tools.Sword(tier,0,-2.4f,p);
    case "shears"->new Shears(p.durability(LegacyToolWear.maxDamage(LegacyToolWear.Kind.SHEARS,"netherite")));
    default->throw new IllegalStateException(kind);
   };
  });
 }
 public static final class Shears extends ShearsItem implements AdvancedEnchanting.MaterialEquipment {
  public Shears(Properties properties){super(properties);}
  @Override public int getEnchantmentValue(){return Tiers.NETHERITE.getEnchantmentValue();}
  @Override public String advancedEnchantingMaterial(ItemStack stack){return "netherite";}
 }
 private NetheriteEquipment(){}
}
