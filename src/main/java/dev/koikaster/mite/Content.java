package dev.koikaster.mite;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
public final class Content {
 public static final DeferredRegister<net.minecraft.world.item.enchantment.Enchantment> ENCHANTMENTS=DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,MiteReforged.ID);
 public static final RegistryObject<net.minecraft.world.item.enchantment.Enchantment> REGENERATION=ENCHANTMENTS.register("regeneration",()->new net.minecraft.world.item.enchantment.Enchantment(net.minecraft.world.item.enchantment.Enchantment.Rarity.RARE,net.minecraft.world.item.enchantment.EnchantmentCategory.ARMOR_CHEST,new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.CHEST}) {
  public int getMaxLevel(){return 5;}
  public int getMinCost(int level){return 11+20*(level-1);}
  public int getMaxCost(int level){return getMinCost(level)+19;}
 });
 public static final RegistryObject<net.minecraft.world.item.enchantment.Enchantment> SLOW_MENDING=ENCHANTMENTS.register("slow_mending",()->new net.minecraft.world.item.enchantment.Enchantment(net.minecraft.world.item.enchantment.Enchantment.Rarity.RARE,net.minecraft.world.item.enchantment.EnchantmentCategory.BREAKABLE,net.minecraft.world.entity.EquipmentSlot.values()) {public boolean isTreasureOnly(){return true;}public int getMinCost(int l){return 25;}public int getMaxCost(int l){return 75;}});
 public static final RegistryObject<net.minecraft.world.item.enchantment.Enchantment> PIERCING=ENCHANTMENTS.register("piercing",()->new net.minecraft.world.item.enchantment.Enchantment(net.minecraft.world.item.enchantment.Enchantment.Rarity.RARE,net.minecraft.world.item.enchantment.EnchantmentCategory.DIGGER,new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.MAINHAND}) {
  public int getMaxLevel(){return 5;}
  public boolean canEnchant(ItemStack stack){var profile=ClassicToolWear.profile(stack);return profile!=null&&(profile.kind()==LegacyToolWear.Kind.PICKAXE||profile.kind()==LegacyToolWear.Kind.BATTLEAXE);}
 });
 public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_SERIALIZERS=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,MiteReforged.ID);
 public static final RegistryObject<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> SURVIVAL_LOOT=LOOT_SERIALIZERS.register("survival",()->SurvivalLoot.CODEC);
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MiteReforged.ID);
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,MiteReforged.ID);
 public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,MiteReforged.ID);
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,MiteReforged.ID);
 public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,MiteReforged.ID);
 public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,MiteReforged.ID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MiteReforged.ID);
 public static final Map<String,RegistryObject<Item>> ALL_ITEMS=new LinkedHashMap<>();
 public static final Map<String,RegistryObject<Block>> ALL_BLOCKS=new LinkedHashMap<>();
 public static final Map<String,ForgeTier> TIERS=new LinkedHashMap<>();
 public static final String[] METALS={"copper","silver","gold","bronze","iron","steel","mithril","adamantium"};
 public static final RegistryObject<Item> FIBRE=item("plant_fibre",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> FLINT_SHARD=item("flint_shard",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> OBSIDIAN_SHARD=item("obsidian_shard",()->new Item(new Item.Properties().stacksTo(64)));
 public static final RegistryObject<Item> EMERALD_SHARD=item("emerald_shard",()->new Item(new Item.Properties().stacksTo(64)));
 public static final RegistryObject<Item> DIAMOND_SHARD=item("diamond_shard",()->new Item(new Item.Properties().stacksTo(64)));
 public static final RegistryObject<Item> QUARTZ_SHARD=item("quartz_shard",()->new Item(new Item.Properties().stacksTo(64)));
 public static final RegistryObject<Item> GLASS_SHARD=item("glass_shard",()->new Item(new Item.Properties().stacksTo(64)));
 public static final RegistryObject<Block> NETHER_GRAVEL=BLOCKS.register("nether_gravel",()->new GravelBlock(BlockBehaviour.Properties.copy(Blocks.GRAVEL)));
 static {ALL_BLOCKS.put("nether_gravel",NETHER_GRAVEL);item("nether_gravel",()->new BlockItem(NETHER_GRAVEL.get(),new Item.Properties().stacksTo(4)));}
 // N018: crushing renewable stone supplies aggregate, never new ore-bearing gravel.
 public static final RegistryObject<Block> CRUSHED_GRAVEL=BLOCKS.register("crushed_gravel",()->new GravelBlock(BlockBehaviour.Properties.copy(Blocks.GRAVEL)));
 static {ALL_BLOCKS.put("crushed_gravel",CRUSHED_GRAVEL);item("crushed_gravel",()->new BlockItem(CRUSHED_GRAVEL.get(),new Item.Properties().stacksTo(4)){
  public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){super.appendHoverText(stack,level,lines,flag);}
 });}
 public static final RegistryObject<Item> BLAZING_COAL=item("blazing_coal",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> RUNIC_KEY=item("runic_key",()->new SpecialItems.RunicKey(new Item.Properties().stacksTo(1)));
 public static final RegistryObject<Item> RUNE_STONE=item("rune_stone",()->new SpecialItems.RuneStone(new Item.Properties().stacksTo(1)));
 public static final RegistryObject<Item> BANDAGE=item("bandage",()->new SpecialItems.Bandage(new Item.Properties().durability(10).setNoRepair()));
 public static final RegistryObject<Item> FLINT_KNIFE=item("flint_knife",()->new Tools.Sword(Tiers.WOOD,1,-1.8f,new Item.Properties().durability(48)));
 public static final RegistryObject<Item> FLINT_HATCHET=item("flint_hatchet",()->new Tools.Axe(Tiers.WOOD,3,-2.4f,new Item.Properties().durability(96)));
 public static final RegistryObject<Item> FLINT_SHOVEL=item("flint_shovel",()->new ShovelItem(Tiers.WOOD,1,-2.4f,new Item.Properties().durability(64)));
 public static final RegistryObject<Item> FLOUR=item("flour",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> DOUGH=item("dough",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> BRONZE_BLEND=item("bronze_blend",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> STEEL_BLEND=item("steel_blend",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> COOKED_EGG=food("cooked_egg",3,.4f,false);
 public static final RegistryObject<Item> PORRIDGE=item("porridge",()->OriginalBowls.meal("porridge",4,2,false));
 public static final RegistryObject<Item> PUMPKIN_SOUP=item("pumpkin_soup",()->OriginalBowls.meal("pumpkin_soup",1,2,false));
 public static final RegistryObject<Item> FISH_STEW=food("fish_stew",7,.6f,true);
 public static final RegistryObject<Item> APPLE_PIE=food("apple_pie",7,.4f,false);
 public static final RegistryObject<Item> CHOCOLATE=food("chocolate",2,.2f,false);
 public static final RegistryObject<Item> ONION=item("onion",()->new ItemNameBlockItem(Content.ONIONS.get(),new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(2).saturationMod(.2f).build())));
 public static final RegistryObject<Item> ORANGE=food("orange",1,1f,false);
 public static final RegistryObject<Item> BANANA=food("banana",1,1f,false);
 public static final RegistryObject<Block> ONIONS=BLOCKS.register("onions",Agriculture.OnionCrop::new);
 public static final RegistryObject<Block> DEAD_WHEAT=deadCrop("dead_wheat");
 public static final RegistryObject<Block> DEAD_CARROTS=deadCrop("dead_carrots");
 public static final RegistryObject<Block> DEAD_POTATOES=deadCrop("dead_potatoes");
 public static final RegistryObject<Block> DEAD_ONIONS=deadCrop("dead_onions");
 public static final RegistryObject<Item> MANURE=item("manure",()->new Item(new Item.Properties().stacksTo(16)));
 public static final RegistryObject<Item> ONION_SEEDS=item("onion_seeds",()->new ItemNameBlockItem(ONIONS.get(),new Item.Properties()));
 public static final RegistryObject<Item> VEGETABLE_STEW=item("vegetable_stew",()->OriginalBowls.meal("vegetable_stew",6,6,false));
 public static final RegistryObject<Item> MEAT_STEW=item("meat_stew",()->OriginalBowls.meal("meat_stew",16,16,false));
 public static final RegistryObject<Item> SALAD=food("salad",4,.3f,true);
 public static final RegistryObject<Item> RAW_WORM=food("worm",1,.1f,false);
 public static final RegistryObject<Item> CHEESE=food("cheese",3,.5f,false);
 static {OriginalBowls.register();}
 public static final RegistryObject<Block> SACRED_SANDSTONE=BLOCKS.register("sacred_sandstone",
  ()->new SourceSacredSandstone(BlockBehaviour.Properties.copy(Blocks.SANDSTONE).strength(.8f)));
 static {ALL_BLOCKS.put("sacred_sandstone",SACRED_SANDSTONE);
  item("sacred_sandstone",()->new SourceSacredSandstone.Item(SACRED_SANDSTONE.get(),new Item.Properties().stacksTo(4)));}
 public static final RegistryObject<Block> GRAVE=block("grave",()->new Grave.BlockImpl(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(5,3600000).noOcclusion()));
 public static final Map<String,RegistryObject<ThermalFurnace.BlockImpl>> FURNACES=new LinkedHashMap<>();
 static {
  String[] fs={"clay_oven","hardened_clay_oven","sandstone_oven","stone_furnace","obsidian_furnace","netherrack_furnace"};
  int[] heat={1,1,1,2,3,4};float[] hardness={.5f,1,1,2,4,8};
  for(int i=0;i<fs.length;i++) {
   String n=fs[i]; int h=heat[i];float strength=hardness[i];boolean large=!n.equals("clay_oven");
   RegistryObject<ThermalFurnace.BlockImpl> f=BLOCKS.register(n,()->new ThermalFurnace.BlockImpl(h,large,BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(strength).requiresCorrectToolForDrops().lightLevel(s->s.getValue(BlockStateProperties.LIT)?13:0)));
   FURNACES.put(n,f); ALL_BLOCKS.put(n,(RegistryObject)f);
   item(n,()->new BlockItem(f.get(),new Item.Properties().stacksTo(1)));
  }
  for(String m:new String[]{"tin","silver","mithril","adamantium"}) {
   for(String prefix:new String[]{"","deepslate_"}) {
    String name=prefix+m+"_ore";
    block(name,()->{
     var properties=BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(prefix.isEmpty()?4:6).requiresCorrectToolForDrops();
     if(prefix.isEmpty()&&!m.equals("tin"))return new SourceTournamentOre(properties);
     return new DropExperienceBlock(properties);
    });
   }
   item(m+"_ingot",()->new Item(new Item.Properties()));
  }
  for(String m:new String[]{"bronze","steel"})item(m+"_ingot",()->new Item(new Item.Properties()));
  for(String m:METALS) {
   if(!m.equals("iron")&&!m.equals("gold"))item(m+"_nugget",()->new Item(new Item.Properties()));
   int lv=metalLevel(m),dur=metalDurability(m);
   ForgeTier t=new ForgeTier(lv,dur,2+lv*2,lv,12,BlockTags.NEEDS_IRON_TOOL,()->Ingredient.of(nugget(m)));
   TIERS.put(m,t);
   {
    item(m+"_pickaxe",()->new PickaxeItem(t,1,-2.2f,new Item.Properties()));
    item(m+"_axe",()->new Tools.Axe(t,5,-2.4f,new Item.Properties()));
    item(m+"_shovel",()->new ShovelItem(t,1,-2.2f,new Item.Properties()));
    item(m+"_hoe",()->new Tools.Hoe(t,-1,-1.5f,new Item.Properties()));
    item(m+"_sword",()->new Tools.Sword(t,3,-1.7f,new Item.Properties()));
    for(ArmorItem.Type a:ArmorItem.Type.values())item(m+"_"+a.getName(),()->new ArmorItem(new MetalArmor(m),a,m.equals("adamantium")?new Item.Properties().fireResistant():new Item.Properties()));
   }
   item(m+"_dagger",()->new Tools.Sword(t,1,-.8f,new Item.Properties()));
   item(m+"_battleaxe",()->new Tools.Axe(t,7,-2.6f,new Item.Properties()));
   item(m+"_warhammer",()->new PickaxeItem(t,8,-3,new Item.Properties()));
   item(m+"_mattock",()->new Tools.Mattock(t,2,-2.3f,new Item.Properties()));
   item(m+"_scythe",()->new Tools.Hoe(t,3,-2.4f,new Item.Properties()));
   if(m.equals("mithril"))item(m+"_bow",()->new Tools.MetalBow(lv,new Item.Properties().durability(128)));
   item(m+"_arrow",()->new Tools.MetalArrow(lv,new Item.Properties()));
   item(m+"_bucket",()->new SpecialItems.MetalBucket(m,new Item.Properties().stacksTo(8)));
   item(m+"_coin",()->new SpecialItems.Coin(m,new Item.Properties().stacksTo(64)));
   block(m+"_anvil",()->new MetalAnvilBlock(m,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5,1200).requiresCorrectToolForDrops()));
  }
  for(String m:new String[]{"silver","bronze","steel","mithril","adamantium","ancient_metal"})block(m+"_block",()->new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5,1200).requiresCorrectToolForDrops()));
 }
 static {OriginalToolItems.register();OriginalArmorItems.register();OriginalEnchantments.registerMissing();NetheriteEquipment.register();IteContent0416.init();IteEnchantments0417.init();IteEnhanceStones0417.init();IteForgingMenu0417.TYPE.getId();}
 public static final RegistryObject<Item> ANCIENT_METAL_COIN=item("ancient_metal_coin",()->new SpecialItems.Coin("ancient_metal",new Item.Properties().stacksTo(64)));
 public static final RegistryObject<Item> ANCIENT_METAL_BUCKET=item("ancient_metal_bucket",()->new SpecialItems.MetalBucket("ancient_metal",new Item.Properties().stacksTo(8)));
 public static final RegistryObject<Block> ANCIENT_METAL_ANVIL=block("ancient_metal_anvil",()->new MetalAnvilBlock("ancient_metal",BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5,1200).requiresCorrectToolForDrops()));
 public static final RegistryObject<Item> BOTTLE_OF_DISENCHANTING=item("bottle_of_disenchanting",DisenchantingBottle::new);
 public static final RegistryObject<net.minecraft.world.entity.EntityType<SourceAnvil.Falling>> ANVIL_FALLING=Mobs.ENTITIES.register("source_falling_anvil",()->net.minecraft.world.entity.EntityType.Builder.<SourceAnvil.Falling>of(SourceAnvil.Falling::new,net.minecraft.world.entity.MobCategory.MISC).sized(.98f,.98f).clientTrackingRange(10).updateInterval(20).build("mite_reforged:source_falling_anvil"));
 public static final RegistryObject<BlockEntityType<ThermalFurnace.Entity>> FURNACE_ENTITY=BLOCK_ENTITIES.register("thermal_furnace",()->BlockEntityType.Builder.of(ThermalFurnace.Entity::new,FURNACES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
 public static final RegistryObject<BlockEntityType<Grave.Entity>> GRAVE_ENTITY=BLOCK_ENTITIES.register("grave",()->BlockEntityType.Builder.of(Grave.Entity::new,GRAVE.get()).build(null));
 public static final RegistryObject<BlockEntityType<SourceAnvil>> ANVIL_ENTITY=BLOCK_ENTITIES.register("source_anvil",()->BlockEntityType.Builder.of(SourceAnvil::new,ALL_BLOCKS.entrySet().stream().filter(e->e.getKey().endsWith("_anvil")).map(e->e.getValue().get()).toArray(Block[]::new)).build(null));
 public static final RegistryObject<Block> EMERALD_ENCHANTING_TABLE=block("emerald_enchanting_table",()->new OriginalEnchantingTables.EmeraldTable(BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE).strength(2.4f,2.4f)));
 public static final RegistryObject<BlockEntityType<OriginalEnchantingTables.EmeraldEntity>> EMERALD_ENCHANTING_ENTITY=BLOCK_ENTITIES.register("emerald_enchanting_table",()->BlockEntityType.Builder.of(OriginalEnchantingTables.EmeraldEntity::new,EMERALD_ENCHANTING_TABLE.get()).build(null));
 public static final RegistryObject<MenuType<SourceEnchantMenu>> ENCHANT_MENU=MENUS.register("source_enchant",()->IForgeMenuType.create(SourceEnchantMenu::new));
 public static final RegistryObject<MenuType<SourceRepairMenu>> REPAIR_MENU=MENUS.register("source_repair",()->IForgeMenuType.create(SourceRepairMenu::new));
 public static final RegistryObject<MenuType<SourceFurnaceMenu>> FURNACE_MENU=MENUS.register("source_furnace",()->IForgeMenuType.create(SourceFurnaceMenu::new));
 public static final RegistryObject<MenuType<SourceBrewingMenu1412>> BREW_MENU=MENUS.register("source_brewing",()->IForgeMenuType.create((id,inventory,data)->new SourceBrewingMenu1412(id,inventory)));
 public static final RegistryObject<MenuType<TimedCrafting.Menu>> CRAFT_MENU=MENUS.register("timed_crafting",()->IForgeMenuType.create((id,inv,data)->new TimedCrafting.Menu(id,inv)));
 public static final RegistryObject<RecipeType<ThermalRecipe>> THERMAL=RECIPE_TYPES.register("thermal",()->new RecipeType<>() {});
 public static final RegistryObject<RecipeSerializer<ThermalRecipe>> THERMAL_SERIALIZER=SERIALIZERS.register("thermal",ThermalRecipe.Serializer::new);
 public static final RegistryObject<RecipeSerializer<ProcessingRecipe>> PROCESSING=SERIALIZERS.register("tool_processing",()->new SimpleCraftingRecipeSerializer<>(ProcessingRecipe::new));
 public static final RegistryObject<RecipeSerializer<CurrencyRecipe>> CURRENCY_SERIALIZER=SERIALIZERS.register("currency_exchange",CurrencyRecipe.Serializer::new);
 public static final RegistryObject<RecipeSerializer<VesselStoneRecipe>> VESSEL_STONE_SERIALIZER=SERIALIZERS.register("vessel_stone_dismantle",()->new SimpleCraftingRecipeSerializer<>(VesselStoneRecipe::new));
 public static final RegistryObject<RecipeSerializer<DisenchantingBottle.Recipe>> DISENCHANTING_RECIPE=SERIALIZERS.register("disenchanting_bottle",()->new SimpleCraftingRecipeSerializer<>(DisenchantingBottle.Recipe::new));
 public static final RegistryObject<RecipeSerializer<ClassicCraftRepair>> CRAFT_REPAIR_SERIALIZER=SERIALIZERS.register("source_craft_repair",()->new SimpleCraftingRecipeSerializer<>(ClassicCraftRepair::new));
 public static final RegistryObject<RecipeSerializer<SteelNetheriteRecipe>> STEEL_NETHERITE_SERIALIZER=SERIALIZERS.register("steel_netherite_upgrade",SteelNetheriteRecipe.Serializer::new);
 public static final RegistryObject<RecipeSerializer<SourceBowlFoodRecipe>> SOURCE_BOWL_FOOD=SERIALIZERS.register("source_bowl_food",SourceBowlFoodRecipe.Serializer::new);
 public static final RegistryObject<RecipeSerializer<ClassicMixedPlanksRecipe>> CLASSIC_MIXED_PLANKS_SERIALIZER=SERIALIZERS.register("classic_mixed_planks",ClassicMixedPlanksRecipe.Serializer::new);
 public static final RegistryObject<RecipeSerializer<SourceBowlTransferRecipe>> SOURCE_BOWL_TRANSFER=SERIALIZERS.register("source_bowl_transfer",()->new SimpleCraftingRecipeSerializer<>(SourceBowlTransferRecipe::new));
 public static final RegistryObject<RecipeSerializer<BandageRefillRecipe>> BANDAGE_REFILL=SERIALIZERS.register("bandage_refill",()->new SimpleCraftingRecipeSerializer<>(BandageRefillRecipe::new));
 public static final RegistryObject<CreativeModeTab> TAB=TABS.register("survival",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.mite_reforged")).icon(()->new ItemStack(FLINT_HATCHET.get())).displayItems((p,o)->ALL_ITEMS.values().stream().filter(i->ClassicCoins.enabled(i.get())&&i!=RUNE_STONE).forEach(i->{if(i.get() instanceof SourceRunestoneItem){for(int rune=0;rune<16;rune++)o.accept(SourceRunestoneItem.select(new ItemStack(i.get()),rune));}else if(i.get() instanceof SourceReferencedBooks.Book){for(int index=1;index<=9;index++)o.accept(SourceReferencedBooks.generate(index));}else o.accept(i.get());})).build());
 public static ResourceLocation id(String path) {return new ResourceLocation(MiteReforged.ID,path);}
 public static RegistryObject<Item> item(String n,Supplier<? extends Item> s) {RegistryObject<Item> o=ITEMS.register(n,()->s.get());ALL_ITEMS.put(n,o);return o;}
 public static RegistryObject<Block> block(String n,Supplier<? extends Block> s) {return block(n,s,Item.Properties::new);}
 static RegistryObject<Block> block(String name,Supplier<? extends Block> factory,Supplier<Item.Properties> itemProperties) {
  RegistryObject<Block> block=BLOCKS.register(name,factory);
  ALL_BLOCKS.put(name,block);
  item(name,()->new BlockItem(block.get(),itemProperties.get()));
  return block;
 }
 private static RegistryObject<Block> deadCrop(String n){RegistryObject<Block> block=BLOCKS.register(n,Agriculture.DeadCrop::new);ALL_BLOCKS.put(n,block);item(n,()->new BlockItem(block.get(),new Item.Properties().stacksTo(8)));return block;}
 public static RegistryObject<Item> food(String n,int hunger,float sat,boolean bowl) {return item(n,()->{var properties=new Item.Properties().stacksTo(bowl?4:16).food(new FoodProperties.Builder().nutrition(hunger).saturationMod(sat).build());return bowl?new SpecialItems.BowlMeal(properties):new Item(properties);});}
 public static Item get(String n) {return ALL_ITEMS.get(n).get();}
 public static Item nugget(String m) {return m.equals("iron")?Items.IRON_NUGGET:m.equals("gold")?Items.GOLD_NUGGET:get(m+"_nugget");}
 public static int metalLevel(String m) {return switch(m) {case "copper","silver","gold"->1;case "bronze","iron"->2;case "steel"->3;case "mithril"->4;case "adamantium"->5;case "holy_steel"->6;default->0;};}
 public static int metalDurability(String m) {return switch(m) {case "copper"->320;case "silver"->220;case "gold"->96;case "bronze"->600;case "iron"->900;case "steel"->1800;case "mithril"->4800;case "adamantium"->12000;default->64;};}
 public record MetalArmor(String metal) implements ArmorMaterial {
  public int getDurabilityForType(ArmorItem.Type t) {return metalDurability(metal)*(t==ArmorItem.Type.CHESTPLATE?2:1);}
  public int getDefenseForType(ArmorItem.Type t) {
   int index=switch(t){case BOOTS->0;case LEGGINGS->1;case CHESTPLATE->2;case HELMET->3;};
   int[] values=switch(metalLevel(metal)){case 1->new int[]{1,3,4,1};case 2->new int[]{2,4,5,2};case 3->new int[]{2,5,6,2};case 4->new int[]{3,5,7,3};default->new int[]{4,7,9,4};};return values[index];
  }
  public int getEnchantmentValue() {return 12;}
  public SoundEvent getEquipSound() {return SoundEvents.ARMOR_EQUIP_IRON;}
  public Ingredient getRepairIngredient() {return Ingredient.of(nugget(metal));}
  public String getName() {return metal.equals("iron")?"minecraft:iron":MiteReforged.ID+":"+metal;}
  public float getToughness() {return Math.max(0,metalLevel(metal)-2);}
  public float getKnockbackResistance() {return metalLevel(metal)>=4?.1f:0;}
 }
 public static final RegistryObject<Item> SNOW_SLAB=item("snow_slab",SourceSnowMaterials.SlabItem::new);
 static {R196Workbench.registerBlocks();}
 private Content() {}
}
