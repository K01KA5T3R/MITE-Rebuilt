package dev.koikaster.mite;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.registries.RegistryObject;

/**
 * MITE-ITE 2.0.30 Vibranium material tier, tools/weapons/armor/bow, Blaze Coal Powder,
 * Nether Adamantium Ore, Vibranium Furnace/Anvil/Block, and Forging Table blocks (0.4.16).
 */
public final class IteContent0416 {
    public static final String MATERIAL = "vibranium";

    public static final ForgeTier VIBRANIUM_TIER = new ForgeTier(
        6,
        LegacyToolWear.maxDamage(LegacyToolWear.Kind.PICKAXE, MATERIAL),
        3.5F,
        9.0F,
        100,
        BlockTags.create(Content.id("harvest_level_6")),
        () -> Ingredient.of(Content.get("vibranium_nugget"))
    );

    public static final RegistryObject<Item> OBSIDIAN_STICK = Content.item("obsidian_stick",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_CHUNK = Content.item("diamond_chunk",
        () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLAZE_COAL_POWDER = Content.item("blaze_coal_powder",
        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> VIBRANIUM_INGOT = Content.item("vibranium_ingot",
        () -> new Item(new Item.Properties().fireResistant().stacksTo(8)));
    public static final RegistryObject<Item> VIBRANIUM_NUGGET = Content.item("vibranium_nugget",
        () -> new Item(new Item.Properties().fireResistant().stacksTo(64)));

    public static final RegistryObject<Block> VIBRANIUM_BLOCK = Content.block("vibranium_block",
        () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6.0F, 1200.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()),
        () -> new Item.Properties().fireResistant().stacksTo(4));

    public static final RegistryObject<Block> NETHER_ADAMANTIUM_ORE = Content.block("nether_adamantium_ore",
        () -> new NetherAdamantiumOreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).strength(4.0F, 12.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()),
        () -> new Item.Properties().fireResistant().stacksTo(4));

    public static final RegistryObject<Block> VIBRANIUM_ANVIL = Content.block("vibranium_anvil",
        () -> new MetalAnvilBlock(MATERIAL, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6.0F, 1200.0F).sound(SoundType.ANVIL).requiresCorrectToolForDrops()),
        () -> new Item.Properties().fireResistant().stacksTo(1));

    public static final RegistryObject<ThermalFurnace.BlockImpl> VIBRANIUM_FURNACE;

    public static final Map<String, RegistryObject<Block>> FORGING_TABLES = new LinkedHashMap<>();

    public static final String[] TOOL_KINDS = {
        "pickaxe", "axe", "hatchet", "shovel", "hoe",
        "sword", "dagger", "knife", "mattock", "warhammer", "battleaxe", "scythe"
    };

    static {
        Content.TIERS.put(MATERIAL, VIBRANIUM_TIER);
        for (String kind : TOOL_KINDS) {
            Content.item(MATERIAL + "_" + kind, () -> createTool(kind));
        }
        Content.item("vibranium_bow", () -> new Tools.MetalBow(6, new Item.Properties().fireResistant().durability(ClassicToolCatalogue.bowDurability(MATERIAL))));
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            Content.item(MATERIAL + "_" + type.getName(), () -> new ArmorItem(
                new OriginalArmorItems.Material(MATERIAL, false), type, new Item.Properties().fireResistant()));
        }

        RegistryObject<ThermalFurnace.BlockImpl> furnace = Content.BLOCKS.register("vibranium_furnace",
            () -> new ThermalFurnace.BlockImpl(5, true, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(8.0F, 3200.0F).requiresCorrectToolForDrops()
                .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0)));
        VIBRANIUM_FURNACE = furnace;
        Content.FURNACES.put("vibranium_furnace", furnace);
        Content.ALL_BLOCKS.put("vibranium_furnace", (RegistryObject<Block>) (RegistryObject<?>) furnace);
        Content.item("vibranium_furnace", () -> new BlockItem(furnace.get(), new Item.Properties().fireResistant().stacksTo(1)));

        for (String tier : new String[]{"iron", "mithril", "adamantium", "vibranium"}) {
            String name = tier + "_forging_table";
            FORGING_TABLES.put(tier, Content.block(name,
                () -> new ForgingTableBlock(tier), () -> new Item.Properties().fireResistant().stacksTo(1)));
        }
    }

    public static final RegistryObject<CreativeModeTab> ITE_TAB = Content.TABS.register("ite",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mite_reforged.ite"))
            .icon(() -> new ItemStack(VIBRANIUM_INGOT.get()))
            .displayItems((params, out) -> {
                for (var entry : Content.ALL_ITEMS.entrySet()) {
                    String path = entry.getKey();
                    if (isIteItem(path)) {
                        out.accept(entry.getValue().get());
                    }
                }
            }).build());

    private static Item createTool(String kind) {
        Item.Properties p = new Item.Properties().fireResistant();
        return switch (kind) {
            case "pickaxe", "warhammer" -> new PickaxeItem(VIBRANIUM_TIER, 0, -2.4F, p);
            case "axe", "hatchet", "battleaxe" -> new Tools.Axe(VIBRANIUM_TIER, 0, -2.4F, p);
            case "shovel" -> new ShovelItem(VIBRANIUM_TIER, 0, -2.4F, p);
            case "hoe", "scythe" -> new Tools.Hoe(VIBRANIUM_TIER, 0, -2.4F, p);
            case "mattock" -> new Tools.Mattock(VIBRANIUM_TIER, 0, -2.4F, p);
            case "sword", "dagger", "knife" -> new Tools.Sword(VIBRANIUM_TIER, 0, -2.4F, p);
            default -> throw new IllegalArgumentException("Unknown Vibranium tool kind: " + kind);
        };
    }

    public static boolean isIteItem(String path) {
        return path.equals("obsidian_stick")
            || path.equals("diamond_chunk")
            || path.equals("blaze_coal_powder")
            || path.startsWith("vibranium_")
            || path.equals("nether_adamantium_ore")
            || path.equals("r196_vibranium_workbench")
            || path.endsWith("_forging_table")
            || path.endsWith("_enhance_stone");
    }

    public static final class NetherAdamantiumOreBlock extends DropExperienceBlock {
        public NetherAdamantiumOreBlock(Properties props) {
            super(props);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
            for (int i = 0; i < 2; i++) {
                int sx = rand.nextInt(2) * 2 - 1;
                int sz = rand.nextInt(2) * 2 - 1;
                double x = pos.getX() + 0.5D + 0.25D * sx;
                double y = pos.getY() + rand.nextFloat();
                double z = pos.getZ() + 0.5D + 0.25D * sz;
                double vy = (rand.nextFloat() - 0.5D) * 0.125D;
                double vx = rand.nextFloat() * sx * 0.125D;
                double vz = rand.nextFloat() * sz * 0.125D;
                level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, vx, vy, vz);
            }
        }
    }

    public static final class ForgingTableBlock extends Block {
        public final String tier;

        public ForgingTableBlock(String tier) {
            super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(8.0F, 8.75F).sound(SoundType.STONE).requiresCorrectToolForDrops());
            this.tier = tier;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!WorldContentMode.ite(level)) return InteractionResult.FAIL;
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                int tIdx = switch (tier) { case "mithril" -> 1; case "adamantium" -> 2; case "vibranium" -> 3; default -> 0; };
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (wid, inv, p) -> new IteForgingMenu0417(wid, inv, net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos), tIdx),
                    Component.translatable(this.getDescriptionId())));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static void init() {
        // Trigger static initialization of ITE 0.4.16 content during Content bootstrap.
    }

    private IteContent0416() {}
}
