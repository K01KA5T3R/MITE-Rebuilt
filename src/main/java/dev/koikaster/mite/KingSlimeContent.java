package dev.koikaster.mite;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

final class KingSlimeContent {
    static final RegistryObject<EntityType<SourceKingSlime>> KING = Mobs.ENTITIES.register("king_slime",
        () -> EntityType.Builder.of(SourceKingSlime::new, MobCategory.MONSTER).sized(2.04f, 2.04f)
            .clientTrackingRange(10).updateInterval(2).build(MiteReforged.ID + ":king_slime"));
    static final RegistryObject<EntityType<KingSlimeJewel>> JEWEL = Mobs.ENTITIES.register("king_slime_jewel",
        () -> EntityType.Builder.of(KingSlimeJewel::new, MobCategory.MISC).sized(.48f, .48f)
            .clientTrackingRange(8).updateInterval(2).noSave().build(MiteReforged.ID + ":king_slime_jewel"));
    static final RegistryObject<EntityType<KingSlimeJewelBolt>> JEWEL_BOLT = Mobs.ENTITIES.register("king_slime_jewel_bolt",
        () -> EntityType.Builder.<KingSlimeJewelBolt>of(KingSlimeJewelBolt::new, MobCategory.MISC).sized(.23f, .23f)
            .clientTrackingRange(8).updateInterval(2).noSave().build(MiteReforged.ID + ":king_slime_jewel_bolt"));

    static final RegistryObject<EntityType<KingSlimeMinion>> MINION = Mobs.ENTITIES.register("king_slime_minion",
        () -> EntityType.Builder.of(KingSlimeMinion::new, MobCategory.MONSTER).sized(2.04f, 2.04f)
            .clientTrackingRange(8).updateInterval(3).noSave().build(MiteReforged.ID + ":king_slime_minion"));

    static final RegistryObject<Item> CROWN = Content.item("slime_crown", Crown::new);

    private KingSlimeContent() {}

    static void register(IEventBus bus) {
        KingSlimeRewards.register();
        bus.addListener(KingSlimeContent::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(KING.get(), SourceKingSlime.attributes().build());
        event.put(JEWEL.get(), KingSlimeJewel.attributes().build());
        event.put(MINION.get(), KingSlimeMinion.attributes().build());
    }

    private static final class Crown extends Item {
        private Crown() {
            super(new Properties().stacksTo(16));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.dimension() != Level.OVERWORLD || player.isSpectator()) {
                return InteractionResultHolder.pass(stack);
            }
            if (level instanceof ServerLevel server) {
                if (!(player instanceof ServerPlayer owner) || !SourceKingSlime.summon(server, owner)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (!owner.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}
