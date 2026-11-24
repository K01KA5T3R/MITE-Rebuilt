package dev.koikaster.mite;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Human N018: mineral supply cannot originate from a renewable mob/loot source.
 * Initial mob equipment is distinguished from actual carried stock. Finite
 * structure equipment and player inputs keep normal use/recovery/recycling.
 * This fixed policy has no player setting. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class MineralSupply {
 static final String SUPPLY="mite_reforged:mineral_supply",INITIALIZED="MiteMineralSupply044",REASON="MiteMineralSpawnReason044";
 static final int GENERATED=1,STOCK=2;
 private static final Set<String> FAMILIES=Set.of("coal","copper","silver","gold","golden","tin","bronze","iron","steel","mithril","adamantium","netherite","diamond","emerald","lapis","quartz","amethyst","redstone","glowstone","prismarine","chainmail");
 private static final Set<String> COMPOSITES=Set.of("coal","coal_block","blazing_coal","ancient_debris","gilded_blackstone","tinted_glass","spectral_arrow","crossbow","shield","shears","spyglass","compass","recovery_compass","clock","brush","lightning_rod","bucket","anvil","chipped_anvil","damaged_anvil","hopper","cauldron","chain","bell","lantern","soul_lantern","rail","powered_rail","detector_rail","activator_rail","tripwire_hook","dispenser","dropper","piston","sticky_piston","observer","repeater","comparator","daylight_detector","note_block","jukebox","enchanting_table","smithing_table","stonecutter","blast_furnace","budding_amethyst","small_amethyst_bud","medium_amethyst_bud","large_amethyst_bud");
 static boolean metal(String material){return FAMILIES.contains(material)||material.equals("rusted_iron")||material.equals("ancient_metal")||material.equals("holy_steel");}
 static String vesselMaterial(ItemStack stack){if(ClassicVessels.handled(stack))return ClassicVessels.material(stack);var id=ForgeRegistries.ITEMS.getKey(stack.getItem());return id!=null&&id.getNamespace().equals("minecraft")&&(id.getPath().equals("bucket")||id.getPath().endsWith("_bucket"))?"iron":null;}
 public static boolean mineral(ItemStack stack){
  if(stack.isEmpty())return false;var item=stack.getItem();var id=ForgeRegistries.ITEMS.getKey(item);if(id==null)return false;
  var armor=OriginalArmorItems.profile(stack);if(armor!=null&&metal(armor.material()))return true;
  var tool=ClassicToolWear.profile(stack);if(tool!=null&&metal(tool.material()))return true;
  if(SourceCarrotSticks.handles(stack)||item instanceof BowItem||item instanceof ArrowItem||item instanceof FishingRodItem)if(metal(ClassicToolCatalogue.material(stack)))return true;
  var vessel=vesselMaterial(stack);if(vessel!=null&&metal(vessel))return true;
  if(item instanceof OriginalArmorItems.HorseArmor horse&&metal(horse.material))return true;
  if(!Set.of("minecraft",MiteReforged.ID).contains(id.getNamespace()))return false;
  String name=id.getPath();if(COMPOSITES.contains(name)||name.endsWith("_minecart")||name.equals("minecart")||name.contains("ancient_metal")||name.contains("rusted_iron"))return true;
  for(var part:name.split("_"))if(FAMILIES.contains(part))return true;return false;
 }
 static boolean bearing(ItemStack stack){return stack.is(Items.GRAVEL)||stack.is(Content.NETHER_GRAVEL.get().asItem());}
 static int source(ItemStack stack){return stack.hasTag()?stack.getTag().getInt(SUPPLY):0;}
 static void generated(ItemStack stack){if(mineral(stack)&&source(stack)!=STOCK)stack.getOrCreateTag().putInt(SUPPLY,GENERATED);}
 public static void stock(ItemStack stack){if(mineral(stack)&&source(stack)!=GENERATED)stack.getOrCreateTag().putInt(SUPPLY,STOCK);}
 static void release(ItemStack stack){if(stack.hasTag()){stack.getTag().remove(SUPPLY);if(stack.getTag().isEmpty())stack.setTag(null);}}
 private static void carried(Mob mob,Consumer<ItemStack> action){
  for(var stack:mob.getAllSlots())action.accept(stack);
  if(mob instanceof SourceLongdeadGuardian guardian)action.accept(guardian.stowedItem());
  if(mob instanceof InventoryCarrier carrier){var inventory=carrier.getInventory();for(int i=0;i<inventory.getContainerSize();i++)action.accept(inventory.getItem(i));}
  if(mob instanceof AbstractHorse horse){for(int slot:new int[]{400,401})action.accept(horse.getSlot(slot).get());for(int slot=500;slot<554;slot++){var access=horse.getSlot(slot);if(access==SlotAccess.NULL)break;action.accept(access.get());}}
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event){if(WorldContentMode.rebuilt(event.getEntity().level()))event.getEntity().getPersistentData().putString(REASON,event.getSpawnType().name());}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void joined(EntityJoinLevelEvent event){
  if(event.getLevel().isClientSide||!WorldContentMode.rebuilt(event.getLevel()))return;
  if(event.getEntity() instanceof ItemEntity item){if(mineral(item.getItem())){if(source(item.getItem())==GENERATED)event.setCanceled(true);else if(source(item.getItem())==STOCK)release(item.getItem());}return;}
  if(!(event.getEntity() instanceof Mob mob))return;var state=mob.getPersistentData();if(state.getBoolean(INITIALIZED))return;
  boolean finite=mob.getSpawnType()==MobSpawnType.STRUCTURE||state.getString(REASON).equals(MobSpawnType.STRUCTURE.name());
  if(!finite&&event.loadedFromDisk())restoreLegacyEquipmentStock(mob);
  carried(mob,finite?MineralSupply::stock:MineralSupply::generated);
  state.putBoolean(INITIALIZED,true);
 }
 private static void restoreLegacyEquipmentStock(Mob mob) {
  if (!mob.isPersistenceRequired() || !mob.canPickUpLoot() || !auditedEquipmentPickup(mob)) return;
  CompoundTag saved = null;
  for (var slot : EquipmentSlot.values()) {
   // These slots can acquire equipment or a guaranteed chance without a pickup.
   if ((slot == EquipmentSlot.MAINHAND && mob instanceof SourceLongdeadGuardian)
       || (slot == EquipmentSlot.OFFHAND && mob.getType() == EntityType.DROWNED)) continue;
   var stack = mob.getItemBySlot(slot);
   if (source(stack) != 0 || !mineral(stack)) continue;
   if (saved == null) {
    saved = new CompoundTag();
    mob.addAdditionalSaveData(saved);
   }
   String key = slot.getType() == EquipmentSlot.Type.HAND ? "HandDropChances" : "ArmorDropChances";
   var chances = saved.getList(key, Tag.TAG_FLOAT);
   // The native equipment pickup sets both persistence and exactly 2.0 in this slot.
   if (slot.getIndex() < chances.size() && chances.getFloat(slot.getIndex()) == 2.0f) stock(stack);
  }
 }

 private static boolean auditedEquipmentPickup(Mob mob) {
  if (mob instanceof SourceZombieOwner || mob instanceof SourcePigman || mob instanceof SourceRevenant
      || mob instanceof SourceSkeleton || mob instanceof SourceWitherSkeleton || mob instanceof SourceLongdead) return true;
  var type = mob.getType();
  return type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.ZOMBIE_VILLAGER
      || type == EntityType.DROWNED || type == EntityType.ZOMBIFIED_PIGLIN || type == EntityType.SKELETON
      || type == EntityType.STRAY || type == EntityType.WITHER_SKELETON;
 }

 @SubscribeEvent(priority=EventPriority.LOWEST) public static void beforeConversion(LivingConversionEvent.Pre event){if(!event.getEntity().level().isClientSide&&WorldContentMode.rebuilt(event.getEntity().level())&&event.getEntity() instanceof Mob mob)carried(mob,MineralSupply::stock);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void beforeDeath(LivingDeathEvent event){if(!event.getEntity().level().isClientSide&&WorldContentMode.rebuilt(event.getEntity().level())&&event.getEntity() instanceof Mob mob)carried(mob,MineralSupply::stock);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void drops(LivingDropsEvent event){
  if(event.getEntity().level().isClientSide||!WorldContentMode.rebuilt(event.getEntity().level())||!(event.getEntity() instanceof Mob mob))return;
  int[] builtIron={mob instanceof IronGolem golem&&golem.isPlayerCreated()?36*9:0};
  event.getDrops().removeIf(entity->{var stack=entity.getItem();if(!mineral(stack))return false;if(source(stack)==STOCK){release(stack);return false;}
   if(guardianRitualMaterials(mob,stack))return false;
   int unit=stack.is(Items.IRON_INGOT)?9:stack.is(Items.IRON_NUGGET)?1:0;if(unit>0&&builtIron[0]>=unit){int count=Math.min(stack.getCount(),builtIron[0]/unit);stack.setCount(count);builtIron[0]-=count*unit;release(stack);return false;}return true;});
 }
 private static boolean guardianRitualMaterials(Mob mob,ItemStack stack){
  // E02 and E12 use guardian drops as the monument ritual's material supply.
  return mob instanceof Guardian&&(stack.is(Items.PRISMARINE_SHARD)||stack.is(Items.PRISMARINE_CRYSTALS));
 }
 static boolean renewableTable(net.minecraft.resources.ResourceLocation id){if(!id.getNamespace().equals("minecraft"))return false;var path=id.getPath();return path.startsWith("gameplay/fishing")||path.equals("gameplay/piglin_bartering")||path.startsWith("gameplay/hero_of_the_village/");}
 public static void loot(List<ItemStack> loot,LootContext context){if(WorldContentMode.rebuilt(context.getLevel())&&renewableTable(context.getQueriedLootTableId()))loot.removeIf(stack->mineral(stack)||bearing(stack));}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void fishing(ItemFishedEvent event){if(!event.getHookEntity().level().isClientSide&&WorldContentMode.rebuilt(event.getHookEntity().level()))event.getDrops().removeIf(stack->mineral(stack)||bearing(stack));}
 private MineralSupply(){}
}
