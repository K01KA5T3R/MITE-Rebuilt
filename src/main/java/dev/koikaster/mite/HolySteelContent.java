package dev.koikaster.mite;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.registries.*;

/** Questionnaire N004: finite outer-End metal, one material grade above adamantium. */
public final class HolySteelContent {
 public static final String MATERIAL="holy_steel";
 public static final String[] TOOLS={"pickaxe","axe","hatchet","shovel","hoe","sword","dagger","knife","mattock","warhammer","battleaxe","scythe","shears"};
 public static final ForgeTier TIER=new ForgeTier(6,LegacyToolWear.maxDamage(LegacyToolWear.Kind.PICKAXE,MATERIAL),3.5f,7,40,BlockTags.create(Content.id("harvest_level_6")),()->Ingredient.of(Content.get("holy_steel_nugget")));
 public static final RegistryObject<Block> ORE=block("holy_steel_ore",()->new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(8,1200).requiresCorrectToolForDrops()),4);
 public static final RegistryObject<Block> BLOCK=block("holy_steel_block",()->new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6,1200).requiresCorrectToolForDrops()),4);
 public static final RegistryObject<Block> ANVIL=block("holy_steel_anvil",()->new MetalAnvilBlock(MATERIAL,BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6,1200).requiresCorrectToolForDrops()),1);
 public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,MiteReforged.ID);
 public static final RegistryObject<OuterOre> FEATURE=FEATURES.register("holy_steel_ore",OuterOre::new);
 static {
  Content.TIERS.put(MATERIAL,TIER);
  Content.item("holy_steel_ingot",()->new Item(properties().stacksTo(8)));
  Content.item("holy_steel_nugget",()->new Item(properties().stacksTo(64)));
  Content.item("holy_steel_chain",()->new Item(properties().stacksTo(8)));
  for(String kind:TOOLS)Content.item(MATERIAL+"_"+kind,()->tool(kind));
  Content.item("holy_steel_arrow",()->new Tools.MetalArrow(MATERIAL,properties()));
  Content.item("holy_steel_fishing_rod",()->new FishingRodItem(properties().durability(ClassicToolCatalogue.rodDurability(MATERIAL))));
  Content.item("holy_steel_bucket",()->new SpecialItems.MetalBucket(MATERIAL,properties().stacksTo(8)));
  for(boolean chain:new boolean[]{false,true})for(var type:ArmorItem.Type.values())Content.item(MATERIAL+(chain?"_chainmail_":"_")+type.getName(),()->new Armor(chain,type));
 }
 private static Item.Properties properties(){return new Item.Properties().fireResistant().rarity(Rarity.RARE);}
 private static RegistryObject<Block> block(String name,Supplier<Block> supplier,int cap){var b=Content.BLOCKS.register(name,supplier);Content.ALL_BLOCKS.put(name,b);Content.item(name,()->new BlockItem(b.get(),properties().stacksTo(cap)));return b;}
 private static Item tool(String kind){var p=properties();return switch(kind){
  case "pickaxe","warhammer"->new PickaxeItem(TIER,0,-2.4f,p);
  case "axe","hatchet","battleaxe"->new Tools.Axe(TIER,0,-2.4f,p);
  case "shovel"->new ShovelItem(TIER,0,-2.4f,p);
  case "hoe","scythe"->new Tools.Hoe(TIER,0,-2.4f,p);
  case "mattock"->new Tools.Mattock(TIER,0,-2.4f,p);
  case "sword","dagger","knife"->new Tools.Sword(TIER,0,-2.4f,p);
  case "shears"->new ShearsItem(p.durability(LegacyToolWear.maxDamage(LegacyToolWear.Kind.SHEARS,MATERIAL)));
  default->throw new IllegalArgumentException(kind);
 };}
 public static final class Armor extends ArmorItem {
  public Armor(boolean chain,Type type){super(new OriginalArmorItems.Material(MATERIAL,chain),type,properties());}
  @Override public String getArmorTexture(ItemStack stack,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.EquipmentSlot slot,String type){return MiteReforged.ID+":textures/models/armor/holy_native_052/"+(((OriginalArmorItems.Material)getMaterial()).chain()?"chainmail":"iron")+"_layer_"+(slot==net.minecraft.world.entity.EquipmentSlot.LEGS?2:1)+".png";}
  @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(ClientHolySteel.ARMOR);}
 }
 public static boolean outer(Level level,BlockPos pos){return level.dimension()==Level.END&&(long)pos.getX()*pos.getX()+(long)pos.getZ()*pos.getZ()>1024L*1024L;}
 public static final class OuterOre extends Feature<NoneFeatureConfiguration> {
  public OuterOre(){super(NoneFeatureConfiguration.CODEC);}
  @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
   var world=c.level();var p=c.origin();if(!WorldContentMode.rebuilt(world.getLevel())||!outer(world.getLevel(),p))return false;
   int top=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,p.getX(),p.getZ());if(top<24)return false;
   int y=top-4-c.random().nextInt(17);var at=new BlockPos(p.getX(),y,p.getZ());
   if(!world.getBlockState(at).is(Blocks.END_STONE))return false;
   return Feature.ORE.place(new OreConfiguration(new BlockMatchTest(Blocks.END_STONE),HolySteelContent.ORE.get().defaultBlockState(),6),world,c.chunkGenerator(),c.random(),at);
  }
 }
 /** Preserve old saves without retaining the mistaken raw item in the registry. */
 public static void missingMappings(MissingMappingsEvent event){
  for(var mapping:event.getMappings(ForgeRegistries.Keys.ITEMS,MiteReforged.ID))
   if(mapping.getKey().equals(Content.id("raw_holy_steel")))mapping.remap(ORE.get().asItem());
 }
 public static void register(){net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(HolySteelContent::missingMappings);}
 private HolySteelContent(){}
}
