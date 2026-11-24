package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class SourcePaintings2050 {
    private record Motif(String id, String title, int width, int height, int rarity) {}

    // R195 om enum order, per-art dimensions and rarity.
    private static final List<String> VANILLA = List.of(
        "kebab", "aztec", "alban", "aztec2", "bomb",
        "plant", "wasteland", "pool", "courbet", "sea",
        "sunset", "creebet", "wanderer", "graham", "match",
        "bust", "stage", "void", "skull_and_roses", "wither",
        "fighters", "pointer", "pigscene", "burning_skull", "skeleton",
        "donkey_kong"
    );
    private static final List<Motif> MOTIFS = List.of(
        new Motif("abyss", "Abyss", 48, 32, 32),
        new Motif("baron_almric", "BaronAlmric", 48, 32, 64),
        new Motif("boat", "Boat", 48, 32, 16),
        new Motif("castle", "Castle", 32, 32, 8),
        new Motif("castle_britannia", "CastleBritannia", 64, 32, 8),
        new Motif("darklands", "Darklands", 32, 32, 8),
        new Motif("deathtrap_dungeon", "DeathtrapDungeon", 48, 48, 64),
        new Motif("dnd_basic", "DnDBasic", 32, 32, 64),
        new Motif("draracle", "Draracle", 48, 32, 16),
        new Motif("elden_grove", "EldenGrove", 64, 32, 8),
        new Motif("fair_day", "FairDay", 64, 32, 8),
        new Motif("fallen_bridge", "FallenBridge", 48, 32, 16),
        new Motif("gate_closing", "GateClosing", 64, 32, 16),
        new Motif("ghoul", "Ghoul", 32, 48, 16),
        new Motif("gladstone_keep", "GladstoneKeep", 48, 32, 16),
        new Motif("graves", "Graves", 32, 32, 8),
        new Motif("king_richard", "KingRichard", 32, 32, 64),
        new Motif("messenger", "Messenger", 48, 32, 16),
        new Motif("mountains", "Mountains", 32, 32, 8),
        new Motif("rolands_manor", "RolandsManor", 48, 32, 8),
        new Motif("scotia", "Scotia", 32, 32, 64),
        new Motif("ship", "Ship", 48, 32, 8),
        new Motif("sunlight", "Sunlight", 32, 32, 8),
        new Motif("titan", "Titan", 48, 32, 8),
        new Motif("wolves", "Wolves", 32, 32, 8)
    );

    private static final DeferredRegister<PaintingVariant> REGISTRY =
        DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, MiteReforged.ID);
    private static final Map<ResourceLocation, Motif> BY_ID = new LinkedHashMap<>();
    static {
        for (Motif motif : MOTIFS) {
            REGISTRY.register(motif.id(), () -> new PaintingVariant(motif.width(), motif.height()));
            BY_ID.put(new ResourceLocation(MiteReforged.ID, motif.id()), motif);
        }
    }

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }

    // Implemented on Painting to reuse the RNG vanilla uses for this entity.
    public interface EntityRandom {
        RandomSource mite$paintingRandom2050();
    }

    private static int rarity(Holder<PaintingVariant> holder) {
        Motif motif = holder.unwrapKey().map(key -> BY_ID.get(key.location())).orElse(null);
        return motif == null ? 0 : motif.rarity();
    }

    static <T> Optional<T> choose(List<T> fits, ToIntFunction<T> rarity, IntUnaryOperator draw) {
        if (fits.isEmpty()) return Optional.empty();
        T selected;
        int rarityBound;
        do {
            selected = fits.get(draw.applyAsInt(fits.size()));
            rarityBound = rarity.applyAsInt(selected);
        } while (fits.size() != 1 && rarityBound > 1 && draw.applyAsInt(rarityBound) != 0);
        return Optional.of(selected);
    }

    public static Optional<Painting> createForItem(Level level, BlockPos anchor, Direction side) {
        if (side.getAxis().isVertical() || !level.isEmptyBlock(anchor)) return Optional.empty();
        Map<ResourceLocation, Holder<PaintingVariant>> available = new LinkedHashMap<>();
        for (Holder<PaintingVariant> holder : BuiltInRegistries.PAINTING_VARIANT.getTagOrEmpty(PaintingVariantTags.PLACEABLE)) {
            holder.unwrapKey().ifPresent(key -> available.put(key.location(), holder));
        }
        if (available.isEmpty()) return Optional.empty();

        List<Holder<PaintingVariant>> ordered = new ArrayList<>(available.size());
        for (String id : VANILLA) addAvailable(available, ordered, new ResourceLocation("minecraft", id));
        for (ResourceLocation id : BY_ID.keySet()) addAvailable(available, ordered, id);
        ordered.addAll(available.values());

        Painting painting = new Painting(level, anchor, side, ordered.get(0));
        List<Holder<PaintingVariant>> fits = new ArrayList<>();
        for (Holder<PaintingVariant> holder : ordered) {
            painting.setVariant(holder);
            if (painting.survives()) fits.add(holder);
        }
        RandomSource random = ((EntityRandom) painting).mite$paintingRandom2050();
        return choose(fits, SourcePaintings2050::rarity, random::nextInt).map(holder -> {
            painting.setVariant(holder);
            return painting;
        });
    }

    private static void addAvailable(Map<ResourceLocation, Holder<PaintingVariant>> available,
                                     List<Holder<PaintingVariant>> ordered, ResourceLocation id) {
        Holder<PaintingVariant> holder = available.remove(id);
        if (holder != null) ordered.add(holder);
    }

    public static boolean hasOriginalBacking(HangingEntity hanging) {
        if (!(hanging instanceof Painting painting) || hanging.getType() != EntityType.PAINTING) return true;
        Direction side = painting.getDirection();
        if (side == null) return false;
        BlockPos behind = painting.getPos().relative(side.getOpposite());
        Direction across = side.getCounterClockWise();
        int width = painting.getWidth()/16;
        int height = painting.getHeight()/16;
        Level level = painting.level();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pos.set(behind).move(across, x-(width-1)/2).move(Direction.UP, y-(height-1)/2);
                var state = level.getBlockState(pos);
                // R195 doors do not provide a horizontal flat solid face.
                if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock
                    || !state.blocksMotion() || !state.isFaceSturdy(level, pos, side, SupportType.FULL)) return false;
            }
        }
        return true;
    }

    public static boolean appendTaggedTooltip(ItemStack stack, List<Component> tooltip) {
        if (!stack.is(Items.PAINTING)) return false;
        var tag = stack.getTagElement("EntityTag");
        if (tag == null) return false;
        Motif motif = Painting.loadVariant(tag).flatMap(Holder::unwrapKey)
            .map(key -> BY_ID.get(key.location())).orElse(null);
        if (motif == null) return false;
        tooltip.add(Component.literal(motif.title()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("painting.dimensions", motif.width()/16, motif.height()/16));
        return true;
    }

    private SourcePaintings2050() {}
}
