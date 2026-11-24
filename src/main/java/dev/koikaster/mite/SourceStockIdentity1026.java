package dev.koikaster.mite;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Native representations of source stock inputs. Whole native stacks remain the move payload.
 * Unresolved flattened catalogue aliases and brewing metadata are tracked outside this adapter.
 */
public final class SourceStockIdentity1026 {
 public record Value(SourceStockRules1023.Item item,int subtype,boolean damageable){}
 private static final Map<LegacyToolWear.Kind,String> TOOLS=Map.ofEntries(
  Map.entry(LegacyToolWear.Kind.SWORD,"zl"),Map.entry(LegacyToolWear.Kind.AXE,"ya"),
  Map.entry(LegacyToolWear.Kind.PICKAXE,"yn"),Map.entry(LegacyToolWear.Kind.HOE,"yb"),
  Map.entry(LegacyToolWear.Kind.SHOVEL,"ItemShovel"),Map.entry(LegacyToolWear.Kind.BATTLEAXE,"ItemBattleAxe"),
  Map.entry(LegacyToolWear.Kind.HATCHET,"ItemHatchet"),Map.entry(LegacyToolWear.Kind.WARHAMMER,"ItemWarHammer"),
  Map.entry(LegacyToolWear.Kind.MATTOCK,"ItemMattock"),Map.entry(LegacyToolWear.Kind.SCYTHE,"ItemScythe"),
  Map.entry(LegacyToolWear.Kind.DAGGER,"ItemDagger"),Map.entry(LegacyToolWear.Kind.KNIFE,"ItemKnife"),
  Map.entry(LegacyToolWear.Kind.CLUB,"ItemClub"),Map.entry(LegacyToolWear.Kind.CUDGEL,"ItemCudgel"),
  Map.entry(LegacyToolWear.Kind.SHEARS,"yx"));
 private static final Map<String,String> BOWLS=Map.ofEntries(
  Map.entry("water_bowl","water"),Map.entry("milk_bowl","milk"),Map.entry("meat_stew","beef_stew"),
  Map.entry("chicken_soup","chicken_soup"),Map.entry("vegetable_stew","vegetable_soup"),
  Map.entry("ice_cream","ice_cream"),Map.entry("salad","salad"),
  Map.entry("cream_of_mushroom_soup","cream_of_mushroom_soup"),
  Map.entry("cream_of_vegetable_soup","cream_of_vegetable_soup"),Map.entry("pumpkin_soup","pumpkin_soup"),
  Map.entry("mashed_potato","mashed_potato"),Map.entry("sorbet","sorbet"),
  Map.entry("porridge","porridge"),Map.entry("cereal","cereal"));
 private static Value value(String id,String type,boolean subtypes,int subtype,String material,String contents,String empty,SourceStockRules1023.Special special,SourceStockRules1023.Block block){
  return new Value(new SourceStockRules1023.Item(id,type,subtypes,material,contents,empty,special,block),subtype,SourceStockTypes1023.profile(type).damageable());
 }
 private static Value simple(String id,String type){return value(id,type,false,0,null,null,null,SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);}
 public static Value view(ItemStack stack){
  if(stack.isEmpty())return null;
  var item=stack.getItem();var id=ForgeRegistries.ITEMS.getKey(item);
  if(id==null)throw new IllegalArgumentException("Unregistered stock item");
  String nativeId=id.toString();boolean own=id.getNamespace().equals(MiteReforged.ID),nativeItem=id.getNamespace().equals("minecraft");
  if(!own&&!nativeItem){var result=simple("native:"+nativeId,"yc");return new Value(result.item(),0,item.canBeDepleted());}
  if(ClassicVessels.handled(stack)){
   String material=ClassicVessels.material(stack),fluid=ClassicVessels.fluid(stack),contents=fluid.isEmpty()?null:fluid;
   return value("bucket:"+material+":"+(contents==null?"empty":contents),"milk".equals(contents)?"yi":"wr",false,0,material,contents,"bucket:"+material+":empty",SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);
  }
  if(stack.is(Items.BOWL)||OriginalBowls.full(stack)){
   String contents=stack.is(Items.BOWL)?null:stack.is(Items.MUSHROOM_STEW)?"mushroom_stew":BOWLS.get(id.getPath());
   if(!stack.is(Items.BOWL)&&contents==null)throw new IllegalStateException("Missing source bowl contents: "+nativeId);
   return value("bowl:"+(contents==null?"empty":contents),"ItemBowl",false,0,"wood",contents,"bowl:empty",SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);
  }
  var tool=ClassicToolWear.profile(stack);
  if(tool!=null){String type=TOOLS.get(tool.kind());if(type==null)throw new IllegalStateException("Missing source tool type "+tool.kind());return simple("tool:"+tool.material()+":"+tool.kind(),type);}
  if(SourceCarrotSticks.handles(stack))return simple("carrot-stick:"+SourceCarrotSticks.material(stack),"ws");
  if(item instanceof BowItem)return simple(item instanceof TorchBow?"native:"+nativeId:"bow:"+ClassicToolCatalogue.material(stack),"wp");
  if(item instanceof FishingRodItem)return simple("rod:"+ClassicToolCatalogue.material(stack),"xv");
  if(stack.is(Items.FLINT_AND_STEEL))return simple("source:flint-and-steel","xw");
  var armor=OriginalArmorItems.profile(stack);
  if(armor!=null){String type=switch(armor.type()){case HELMET->"ItemHelmet";case CHESTPLATE->"ItemCuirass";case LEGGINGS->"ItemLeggings";case BOOTS->"ItemBoots";};return simple("armor:"+armor.material()+":"+armor.chain()+":"+armor.type(),type);}
  if(item instanceof SourceRunestoneItem)return value("native:"+nativeId,"ItemRunestone",true,SourceRunestoneItem.rune(stack),null,null,null,SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);
  if(item instanceof BlockItem block){
   var worldBlock=block.getBlock();
   if(worldBlock instanceof MetalAnvilBlock anvil){var tag=stack.getTagElement("BlockEntityTag");int subtype=tag==null?0:SourceAnvil.stage(anvil.material,Math.max(0,tag.getInt("damage")));return value("anvil:"+anvil.material,"wg",true,subtype,null,null,null,SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);}
   if(worldBlock instanceof AnvilBlock){int subtype=worldBlock==Blocks.DAMAGED_ANVIL?2:worldBlock==Blocks.CHIPPED_ANVIL?1:0;return value("anvil:iron","wg",true,subtype,null,null,null,SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);}
   return value("native:"+nativeId,"zh",false,0,null,null,null,SourceStockRules1023.Special.NONE,worldBlock==Blocks.GRAVEL?SourceStockRules1023.Block.GRAVEL:SourceStockRules1023.Block.NONE);
  }
  if(stack.is(Items.FILLED_MAP)){var map=MapItem.getMapId(stack);return value("source:filled-map","yh",true,map==null?0:map,null,null,null,SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);}
  if(item instanceof PotionItem){
   var potion=PotionUtils.getPotion(stack);int subtype;var original=SourceLegacyPotions1406.subtype(stack);
   if(original!=null)subtype=original;
   else subtype=0x10000+BuiltInRegistries.POTION.getId(potion)+(stack.is(Items.SPLASH_POTION)?0x20000:stack.is(Items.LINGERING_POTION)?0x40000:0);
   return value("source:potion","yp",true,subtype,null,null,null,SourceStockRules1023.Special.NONE,SourceStockRules1023.Block.NONE);
  }
  var special=stack.is(Items.STICK)?SourceStockRules1023.Special.STICK:stack.is(Items.BONE)?SourceStockRules1023.Special.BONE:stack.is(Items.GLASS_BOTTLE)?SourceStockRules1023.Special.GLASS_BOTTLE:SourceStockRules1023.Special.NONE;
  var fallback=value("native:"+nativeId,"yc",false,0,null,null,null,special,SourceStockRules1023.Block.NONE);
  return new Value(fallback.item(),fallback.subtype(),item.canBeDepleted());
 }
 public static int score(ItemStack previous,ItemStack candidate){var p=view(previous);if(p==null)throw new IllegalArgumentException("Missing previous stock item");var c=view(candidate);return SourceStockRules1023.score(p.item(),p.subtype(),c==null?null:c.item(),candidate.getCount(),c==null?0:c.subtype());}
 public static boolean sameItem(ItemStack first,ItemStack second){var a=view(first);var b=view(second);return a!=null&&b!=null&&a.item().identity().equals(b.item().identity());}
 public static boolean damageable(ItemStack stack){var v=view(stack);return v!=null&&v.damageable();}
 private SourceStockIdentity1026(){}
}
