package dev.koikaster.mite;
import java.util.*;import net.minecraft.server.level.ServerPlayer;import net.minecraft.tags.ItemTags;import net.minecraft.world.entity.EquipmentSlot;import net.minecraft.world.item.*;import net.minecraft.world.level.block.*;import net.minecraftforge.registries.ForgeRegistries;
/** ss pickup, wd's ordered result classification, vq output classification and uf armor checks. */
public final class SourceAchievementItems570 {
 private static String id(ItemStack s){var id=ForgeRegistries.ITEMS.getKey(s.getItem());return id==null?"":id.toString();}
 private static void award(ServerPlayer p,String name){SourceAchievementService570.grant(p,name);}
 public static void picked(ServerPlayer p,ItemStack s){if(s.isEmpty()||ModernContentPolicy.removed(s))return;if(s.is(ItemTags.LOGS))award(p,"mineWood");if(s.is(Items.LEATHER))award(p,"killCow");if(s.is(Items.DIAMOND))award(p,"diamonds");if(s.is(Items.EMERALD))award(p,"emeralds");if(s.is(Items.BLAZE_ROD))award(p,"blazeRod");String id=id(s);if(s.is(Items.WHEAT_SEEDS)||Set.of("mite_reforged:blueberries","mite_reforged:worm").contains(id))award(p,"seeds");if(s.is(Items.STICK))award(p,"stickPicker");if(Set.of("mite_reforged:copper_nugget","mite_reforged:silver_nugget","minecraft:gold_nugget","minecraft:iron_nugget","mite_reforged:mithril_nugget","mite_reforged:adamantium_nugget").contains(id))award(p,"nuggets");}
 private static boolean rockyTool(ClassicToolWear.Profile profile){return profile!=null&&(profile.kind()==LegacyToolWear.Kind.PICKAXE||profile.kind()==LegacyToolWear.Kind.WARHAMMER);}
 private static boolean dining(ItemStack s){return s.is(Items.MUSHROOM_STEW)||s.is(Items.RABBIT_STEW)||s.is(Items.BEETROOT_SOUP)||s.is(Items.SUSPICIOUS_STEW)||Set.of("mite_reforged:salad","mite_reforged:meat_stew","mite_reforged:chicken_soup","mite_reforged:vegetable_stew","mite_reforged:cream_of_mushroom_soup","mite_reforged:cream_of_vegetable_soup","mite_reforged:pumpkin_soup").contains(id(s));}
 public static void crafted(ServerPlayer p,ItemStack s){
  if(s.isEmpty()||ModernContentPolicy.removed(s))return;SourceTournamentRace.crafted(p,s);var block=s.getItem() instanceof BlockItem i?i.getBlock():null;String id=id(s);var tool=ClassicToolWear.profile(s);var kind=tool==null?null:tool.kind();
  if(block instanceof ThermalFurnace.BlockImpl f&&f.capacity==1)award(p,"buildOven");
  else if(block instanceof R196Workbench bench&&WorldContentMode.r196(p.level()))
   award(p,bench.dismantledLog()==null?"betterTools":"buildWorkBench");
  else if(block==Blocks.CRAFTING_TABLE)award(p,"buildWorkBench");
  else if(block==Blocks.TORCH)award(p,"buildTorches");
  else if(Set.of("mite_reforged:copper_pickaxe","mite_reforged:silver_pickaxe","minecraft:golden_pickaxe","mite_reforged:bronze_pickaxe").contains(id))award(p,"buildPickaxe");
  else if(block==Blocks.FURNACE||id.equals("mite_reforged:stone_furnace"))award(p,"buildFurnace");
  else if(id.equals("mite_reforged:obsidian_furnace"))award(p,"obsidianFurnace");
  else if(id.equals("mite_reforged:netherrack_furnace"))award(p,"netherrackFurnace");
  else if(kind==LegacyToolWear.Kind.HOE||kind==LegacyToolWear.Kind.MATTOCK)award(p,"buildHoe");
  else if(s.is(Items.CAKE))award(p,"bakeCake");
  else if(rockyTool(tool)&&LegacyHarvest.material(tool.material())>=3){award(p,"buildBetterPickaxe");if(LegacyHarvest.material(tool.material())>=5)award(p,"crystalBreaker");}
  else if(id.equals("mite_reforged:flint_hatchet")||id.equals("mite_reforged:flint_knife"))award(p,"cuttingEdge");
  else if(id.equals("mite_reforged:wood_club"))award(p,"buildClub");
  else if(kind==LegacyToolWear.Kind.AXE||kind==LegacyToolWear.Kind.BATTLEAXE)award(p,"buildAxe");
  else if(block==Blocks.ENCHANTING_TABLE||block==Content.EMERALD_ENCHANTING_TABLE.get())award(p,"enchantments");
  else if(block==Blocks.BOOKSHELF)award(p,"bookcase");
  else if(kind==LegacyToolWear.Kind.SHOVEL)award(p,"buildShovel");
  else if(kind==LegacyToolWear.Kind.SCYTHE)award(p,"buildScythe");
  else if(OriginalArmorItems.profile(s)!=null&&OriginalArmorItems.profile(s).chain())award(p,"buildChainMail");
  else if(s.getItem() instanceof FishingRodItem)award(p,"fishingRod");
  else if(id.equals("mite_reforged:flour"))award(p,"flour");
  else if(dining(s))award(p,"fineDining");
 }
 public static void smelted(ServerPlayer p,ItemStack s){if(s.isEmpty()||ModernContentPolicy.removed(s))return;if(s.is(Items.IRON_INGOT))award(p,"acquireIron");else if(s.is(Items.COOKED_COD)||s.is(Items.COOKED_SALMON))award(p,"cookFish");else if(s.is(Items.BREAD))award(p,"makeBread");else if(id(s).equals("mite_reforged:mithril_ingot"))award(p,"mithrilIngot");else if(id(s).equals("mite_reforged:adamantium_ingot"))award(p,"adamantiumIngot");}
 public static void armor(ServerPlayer p){boolean leather=false,plate=true,adamantium=true;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){var profile=OriginalArmorItems.profile(p.getItemBySlot(slot));if(profile==null){plate=adamantium=false;continue;}String m=profile.material();if(m.equals("leather"))leather=true;if(!Set.of("copper","silver","gold","iron","mithril","adamantium","ancient_metal","bronze","steel","holy_steel","netherite").contains(m))plate=false;if(!m.equals("adamantium"))adamantium=false;}if(leather)award(p,"wearLeather");if(plate)award(p,"wearAllPlateArmor");if(adamantium)award(p,"wearAllAdamantiumPlateArmor");}
 private SourceAchievementItems570(){}
}
