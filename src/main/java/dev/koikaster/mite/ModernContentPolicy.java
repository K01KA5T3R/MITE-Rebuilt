package dev.koikaster.mite;

import java.util.HashMap;
import java.util.Set;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Fixed human choices V028/V036..038/V045..046/V052/V061/V068/V087/V109..126.
 * Keeping native registry identities is necessary for Minecraft's own references;
 * generation and all ordinary acquisition paths are handled separately.
 */
public final class ModernContentPolicy {
 private static final Set<String> ENTITIES=Set.of("polar_bear","dolphin","turtle","wandering_trader","trader_llama","allay","frog","tadpole","sniffer");
 private static final Set<String> ITEMS=Set.of(
  "shield","observer","stonecutter","respawn_anchor","conduit","heart_of_the_sea","nautilus_shell",
  "scute","turtle_helmet","turtle_egg","tadpole_bucket","sniffer_egg","phantom_membrane",
  "echo_shard","recovery_compass","ochre_froglight","verdant_froglight","pearlescent_froglight",
  "mud","packed_mud","mud_bricks","mud_brick_slab","mud_brick_stairs","mud_brick_wall","muddy_mangrove_roots",
  "torchflower","torchflower_seeds","pitcher_plant","pitcher_pod","brush","suspicious_sand","suspicious_gravel",
  "decorated_pot","chiseled_bookshelf","sculk","sculk_vein","sculk_catalyst","sculk_sensor","sculk_shrieker","calibrated_sculk_sensor",
  "potted_torchflower","pitcher_crop","torchflower_crop","frogspawn");
 private static final Set<String> ENCHANTMENTS=Set.of("binding_curse","vanishing_curse","sweeping","swift_sneak");
 private static final Set<String> REMOVED_GEAR_KINDS=Set.of("sword","axe","pickaxe","shovel","hoe","dagger","hatchet","battleaxe","knife","warhammer","mattock","scythe","cudgel","club","spear","helmet","chestplate","leggings","boots","horse_armor","shears","bow","fishing_rod","carrot_on_a_stick");
 private static boolean removedEquipment(ResourceLocation id){
  if(id==null||!Set.of("minecraft",MiteReforged.ID).contains(id.getNamespace()))return false;
  String path=id.getPath();if(id.equals(Content.id("wood_club"))||id.equals(Content.id("wood_cudgel"))||id.equals(new ResourceLocation("minecraft","wooden_shovel")))return false;
  for(String prefix:List.of("wooden_","wood_","stone_","diamond_"))if(path.startsWith(prefix)&&REMOVED_GEAR_KINDS.contains(path.substring(prefix.length())))return true;
  return false;
 }
 public static boolean removedId(ResourceLocation id){
  if(DeepslateRemoval.removed(id))return true;
  if(removedEquipment(id))return true;
  if(id==null||!id.getNamespace().equals("minecraft"))return false;
  String path=id.getPath();
  return ITEMS.contains(path)||path.startsWith("mangrove_")||path.equals("potted_mangrove_propagule")
   ||path.endsWith("_pottery_sherd")||path.endsWith("_smithing_template")
   ||path.endsWith("_spawn_egg")&&ENTITIES.contains(path.substring(0,path.length()-10));
 }
 public static boolean removed(ItemStack stack){return !stack.isEmpty()&&removedId(ForgeRegistries.ITEMS.getKey(stack.getItem()));}
 public static boolean removed(BlockState state){return removedId(ForgeRegistries.BLOCKS.getKey(state.getBlock()));}
 public static boolean removed(EntityType<?> type){var id=ForgeRegistries.ENTITY_TYPES.getKey(type);return id!=null&&id.getNamespace().equals("minecraft")&&ENTITIES.contains(id.getPath());}
 public static boolean removed(Enchantment enchantment){var id=ForgeRegistries.ENCHANTMENTS.getKey(enchantment);return id!=null&&id.getNamespace().equals("minecraft")&&ENCHANTMENTS.contains(id.getPath());}
 public static boolean onlyRemovedEnchantments(ItemStack stack){
  if(!stack.is(Items.ENCHANTED_BOOK))return false;
  var values=EnchantmentHelper.getEnchantments(stack);
  return !values.isEmpty()&&values.keySet().stream().allMatch(ModernContentPolicy::removed);
 }
 /** Strip only the four explicitly deleted modern enchantments, including mixed books. */
 public static ItemStack cleanEnchantments(ItemStack stack){
  if(stack.isEmpty()||!stack.hasTag())return stack;
  var values=EnchantmentHelper.getEnchantments(stack);
  if(values.keySet().stream().noneMatch(ModernContentPolicy::removed))return stack;
  stack=stack.copy();
  var remaining=new HashMap<>(values);remaining.keySet().removeIf(ModernContentPolicy::removed);
  if(stack.is(Items.ENCHANTED_BOOK)&&remaining.isEmpty()){
   var book=new ItemStack(Items.BOOK,stack.getCount());book.setTag(stack.getTag().copy());book.removeTagKey("StoredEnchantments");return book;
  }
  // Native EnchantmentHelper adds stored entries to books, so remove that old list first.
  if(stack.is(Items.ENCHANTED_BOOK))stack.removeTagKey("StoredEnchantments");
  EnchantmentHelper.setEnchantments(remaining,stack);return stack;
 }
 private ModernContentPolicy(){}
}
