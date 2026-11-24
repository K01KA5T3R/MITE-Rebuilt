package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

final class QueenBeeContent {
    static final RegistryObject<EntityType<SourceQueenBee>> QUEEN = Mobs.ENTITIES.register("queen_bee", () ->
        EntityType.Builder.of(SourceQueenBee::new, MobCategory.MONSTER).sized(QueenBeeArena.WIDTH, QueenBeeArena.HEIGHT)
            .clientTrackingRange(10).updateInterval(2).build(MiteReforged.ID + ":queen_bee"));
    static final RegistryObject<EntityType<QueenBeeGuard>> GUARD = Mobs.ENTITIES.register("queen_bee_guard", () ->
        EntityType.Builder.of(QueenBeeGuard::new, MobCategory.MISC).sized(.52f, .48f).clientTrackingRange(8)
            .updateInterval(2).noSave().build(MiteReforged.ID + ":queen_bee_guard"));
    static final RegistryObject<EntityType<QueenBeeShot>> SHOT = Mobs.ENTITIES.register("queen_bee_stinger", () ->
        EntityType.Builder.<QueenBeeShot>of(QueenBeeShot::new, MobCategory.MISC).sized(.24f, .24f).clientTrackingRange(8)
            .updateInterval(2).noSave().build(MiteReforged.ID + ":queen_bee_stinger"));
    static final RegistryObject<EntityType<StingerBoomerang>> THROWN_BOOMERANG = Mobs.ENTITIES.register("thrown_stinger_boomerang", () ->
        EntityType.Builder.<StingerBoomerang>of(StingerBoomerang::new, MobCategory.MISC).sized(.34f, .25f).clientTrackingRange(8)
            .updateInterval(2).build(MiteReforged.ID + ":thrown_stinger_boomerang"));

    static final RegistryObject<Item> BAIT = Content.item("honey_bait", Bait::new);
    static final RegistryObject<Item> BOOMERANG = Content.item("stinger_boomerang", Boomerang::new);
    static final float BOOMERANG_DAMAGE = 7f;
    static final int BOOMERANG_POISON = 200;

    private QueenBeeContent() {}

    static void register(IEventBus bus) { bus.addListener(QueenBeeContent::attributes); }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(QUEEN.get(), SourceQueenBee.attributes().build());
        event.put(GUARD.get(), QueenBeeGuard.attributes().build());
    }

    static void dropRewards(ServerLevel level, SourceQueenBee bee) {
        bee.spawnAtLocation(new ItemStack(Items.HONEYCOMB, 4 + bee.getRandom().nextInt(3)));
        bee.spawnAtLocation(new ItemStack(Items.HONEY_BOTTLE, 1 + bee.getRandom().nextInt(2)));
        var data = level.getServer().overworld().getDataStorage().computeIfAbsent(Victory::load, Victory::new, "mite_queen_bee_first_victory");
        if (!data.awarded && bee.spawnAtLocation(new ItemStack(BOOMERANG.get())) != null) {
            data.awarded = true;
            data.setDirty();
        }
    }

    private static final class Bait extends Item {
        Bait() { super(new Properties().stacksTo(16)); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.dimension() != Level.OVERWORLD || player.isSpectator()) return InteractionResultHolder.pass(stack);
            if (level instanceof ServerLevel server) {
                if (!(player instanceof ServerPlayer owner) || !SourceQueenBee.summon(server, owner)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (!owner.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    private static final class Boomerang extends Item {
        Boomerang() { super(new Properties().stacksTo(1)); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isSpectator() || player instanceof ServerPlayer server && Incapacitation2123.incapacitated(server)) {
                return InteractionResultHolder.fail(stack);
            }
            if (level instanceof ServerLevel server) {
                StingerBoomerang thrown = THROWN_BOOMERANG.get().create(server);
                if (thrown == null) return InteractionResultHolder.fail(stack);
                boolean infinite = player.getAbilities().instabuild;
                thrown.launch(player, hand, infinite ? ItemStack.EMPTY : stack.copyWithCount(1));
                if (!server.addFreshEntity(thrown)) return InteractionResultHolder.fail(stack);
                if (!infinite) stack.shrink(1);
                server.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, .75f, 1.30f);
            }
            player.getCooldowns().addCooldown(this, 14);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    private static final class Victory extends SavedData {
        private boolean awarded;

        private static Victory load(CompoundTag tag) {
            var state = new Victory();
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
