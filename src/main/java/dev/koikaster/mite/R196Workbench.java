package dev.koikaster.mite;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.RegistryObject;

/** The four flint, seven metal, and four obsidian workbenches in R196. */
public final class R196Workbench extends CraftingTableBlock {
    private record Wood(String name, Block log) {}

    private static final List<Wood> OLD_WOODS = List.of(
        new Wood("oak", Blocks.OAK_LOG), new Wood("spruce", Blocks.SPRUCE_LOG),
        new Wood("birch", Blocks.BIRCH_LOG), new Wood("jungle", Blocks.JUNGLE_LOG));

    static final RegistryObject<RecipeSerializer<R196BenchRecipe>> RECIPE =
        Content.SERIALIZERS.register("r196_workbench", R196BenchRecipe.Serializer::new);

    enum Tier {
        FLINT("flint", 1, 0, .20f),
        COPPER("copper", 4, 1, .30f),
        SILVER("silver", 4, 1, .30f),
        GOLD("gold", 4, 1, .30f),
        IRON("iron", 8, 4, .40f),
        ANCIENT_METAL("ancient_metal", 16, 8, .50f),
        MITHRIL("mithril", 64, 16, .60f),
        ADAMANTIUM("adamantium", 256, 64, .70f),
        VIBRANIUM("vibranium", 256, 256, .55f),
        OBSIDIAN("obsidian", 2, 0, .20f);

        final String name;
        final int hardness;
        final int predecessorHardness;
        final float modifier;

        Tier(String name, int hardness, int predecessorHardness, float modifier) {
            this.name = name;
            this.hardness = hardness;
            this.predecessorHardness = predecessorHardness;
            this.modifier = modifier;
        }

        static Tier fromNetwork(int index) {
            var choices = values();
            return index >= 0 && index < choices.length ? choices[index] : FLINT;
        }
    }

    public static Optional<Component> requiredWorkbenchHint(CraftingContainer grid, ItemStack output, Recipe<?> recipe) {
        int required = SourceR196Crafting.requiredHardness(grid, output, recipe);
        return switch (required) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(Component.translatable("container.crafting.mite.workbench_required"));
            case 2 -> Optional.of(Component.translatable("jei.mite_reforged.workbench_minimum",
                Component.translatable("container.crafting.mite.obsidian_workbench")));
            case 4 -> Optional.of(Component.translatable("jei.mite_reforged.workbench_copper_tier"));
            default -> {
                yield Optional.of(java.util.Arrays.stream(Tier.values()).filter(candidate -> candidate.hardness == required)
                    .findFirst().map(tier -> Component.translatable("jei.mite_reforged.workbench_minimum",
                        Component.translatable("block.mite_reforged.r196_" + tier.name + "_workbench")))
                    .orElseGet(() -> Component.translatable("container.crafting.mite.workbench_tier")));
            }
        };
    }

    private final Tier tier;
    private final Block dismantledLog;

    private R196Workbench(Tier tier, Block dismantledLog) {
        super(Properties.copy(Blocks.CRAFTING_TABLE));
        this.tier = tier;
        this.dismantledLog = dismantledLog;
    }

    static void registerBlocks() {
        for (var tier : Tier.values()) {
            if (tier == Tier.FLINT || tier == Tier.OBSIDIAN) {
                for (var wood : OLD_WOODS) register(tier, wood);
            } else if (tier == Tier.VIBRANIUM) {
                String id = "r196_vibranium_workbench";
                Content.block(id, () -> new R196Workbench(tier, null), () -> new Item.Properties().fireResistant().stacksTo(4));
            } else {
                register(tier, null);
            }
        }
    }

    private static void register(Tier tier, Wood wood) {
        String id = "r196_" + tier.name + (wood == null ? "" : "_" + wood.name()) + "_workbench";
        Content.block(id, () -> new R196Workbench(tier, wood == null ? null : wood.log()),
            () -> new Item.Properties().stacksTo(4));
    }

    static boolean canOpen(BlockState state, Level level) {
        return state.is(Blocks.CRAFTING_TABLE)
            || state.getBlock() instanceof R196Workbench bench && (bench.tier == Tier.VIBRANIUM ? WorldContentMode.ite(level) : WorldContentMode.r196(level));
    }

    static Tier tier(BlockState state) {
        return state.getBlock() instanceof R196Workbench bench ? bench.tier : Tier.FLINT;
    }

    Tier tier() {
        return tier;
    }

    Block dismantledLog() {
        return dismantledLog;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(this.tier == Tier.VIBRANIUM ? WorldContentMode.ite(level) : WorldContentMode.r196(level))) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) TimedCrafting.open(serverPlayer, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
