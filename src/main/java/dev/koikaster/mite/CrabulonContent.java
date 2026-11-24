package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

final class CrabulonContent {
    static final RegistryObject<EntityType<SourceCrabulon>> CRABULON = Mobs.ENTITIES.register("crabulon", () ->
        EntityType.Builder.of(SourceCrabulon::new, MobCategory.MONSTER).sized(CrabulonArena.WIDTH, CrabulonArena.HEIGHT)
            .clientTrackingRange(10).updateInterval(2).build(MiteReforged.ID + ":crabulon"));
    static final RegistryObject<EntityType<CrabulonSpore>> SPORE = Mobs.ENTITIES.register("crabulon_spore", () ->
        EntityType.Builder.<CrabulonSpore>of(CrabulonSpore::new, MobCategory.MISC).sized(.27f, .27f)
            .clientTrackingRange(8).updateInterval(2).noSave().build(MiteReforged.ID + ":crabulon_spore"));
    static final RegistryObject<EntityType<CrabulonBrood>> BROOD = Mobs.ENTITIES.register("crabulon_brood", () ->
        EntityType.Builder.of(CrabulonBrood::new, MobCategory.MISC).sized(.55f, .51f)
            .clientTrackingRange(8).updateInterval(2).noSave().build(MiteReforged.ID + ":crabulon_brood"));

    static final RegistryObject<Item> SPROUT = Content.item("fungal_sprout", Sprout::new);
    static final RegistryObject<Item> INGOT = Content.item("fungal_ingot", () -> new Item(new Item.Properties()));
    static final RegistryObject<Item> PICKAXE = Content.item("fungal_pickaxe", FungalPickaxe::new);
    static final RegistryObject<Item> SPEAR = Content.item("fungal_spear", FungalSpear::new);
    static final float PICKAXE_SPEED = 1.20f;
    static final float SPEAR_REACH = 1.0f;

    private CrabulonContent() {}

    static void register(IEventBus bus) {
        bus.addListener(CrabulonContent::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CRABULON.get(), SourceCrabulon.attributes().build());
        event.put(BROOD.get(), CrabulonBrood.attributes().build());
    }

    static boolean fungalPickaxe(ItemStack stack) { return stack.getItem() instanceof FungalPickaxe; }
    static boolean fungalSpear(ItemStack stack) { return stack.getItem() instanceof FungalSpear; }

    static ItemStack mithrilReference(ItemStack stack) {
        return fungalPickaxe(stack) ? new ItemStack(Content.get("mithril_pickaxe")) : stack;
    }

    static void dropRewards(ServerLevel level, SourceCrabulon crab) {
        crab.spawnAtLocation(new ItemStack(Items.BROWN_MUSHROOM, 4 + crab.getRandom().nextInt(3)));
        crab.spawnAtLocation(new ItemStack(Items.RED_MUSHROOM, 3 + crab.getRandom().nextInt(3)));
        var rewards = level.getServer().overworld().getDataStorage().computeIfAbsent(FirstVictory::load,
            FirstVictory::new, "mite_crabulon_first_victory");
        if (!rewards.awarded && crab.spawnAtLocation(new ItemStack(INGOT.get(), 5)) != null) {
            rewards.awarded = true;
            rewards.setDirty();
        }
    }

    private static final class Sprout extends Item {
        private Sprout() { super(new Properties().stacksTo(16)); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.dimension() != WorldEvents.UNDERWORLD || player.isSpectator()) return InteractionResultHolder.pass(stack);
            if (level instanceof ServerLevel server) {
                if (!(player instanceof ServerPlayer owner) || !SourceCrabulon.summon(server, owner)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (!owner.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    private static final class FungalPickaxe extends PickaxeItem {
        private FungalPickaxe() {
            super(Content.TIERS.get("mithril"), 1, -2.2f, new Properties());
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            float speed = super.getDestroySpeed(stack, state);
            return speed > 1 ? speed * PICKAXE_SPEED : speed;
        }
    }

    private static final class FungalSpear extends SwordItem {
        private FungalSpear() {
            super(Content.TIERS.get("mithril"), 3, -1.7f, new Properties());
        }
    }

    private static final class FirstVictory extends SavedData {
        private boolean awarded;

        private static FirstVictory load(CompoundTag tag) {
            var state = new FirstVictory();
            state.awarded = tag.getBoolean("Awarded");
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Awarded", awarded);
            return tag;
        }
    }
}
