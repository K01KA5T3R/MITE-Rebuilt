package dev.koikaster.mite;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;

final class MythrilKnightContent {
    static final RegistryObject<EntityType<MythrilRavager>> MOUNT = Mobs.ENTITIES.register("mythril_ravager",
        () -> EntityType.Builder.of(MythrilRavager::new, MobCategory.MONSTER).sized(1.95f, 2.2f)
            .clientTrackingRange(10).updateInterval(2).build(MiteReforged.ID + ":mythril_ravager"));
    static final RegistryObject<EntityType<MythrilPillager>> RIDER = Mobs.ENTITIES.register("mythril_knight",
        () -> EntityType.Builder.of(MythrilPillager::new, MobCategory.MONSTER).sized(.6f, 1.95f)
            .clientTrackingRange(10).updateInterval(2).build(MiteReforged.ID + ":mythril_knight"));
    static final RegistryObject<Item> BANNER = Content.item("raider_banner", Banner::new);
    static final RegistryObject<RecipeSerializer<MythrilKnightBannerRecipe>> BANNER_RECIPE =
        Content.SERIALIZERS.register("raider_banner", () -> new SimpleCraftingRecipeSerializer<>(MythrilKnightBannerRecipe::new));

    private MythrilKnightContent() {}

    static void register(IEventBus bus) {
        bus.addListener(MythrilKnightContent::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(MOUNT.get(), net.minecraft.world.entity.monster.Ravager.createAttributes().build());
        event.put(RIDER.get(), net.minecraft.world.entity.monster.Pillager.createAttributes().build());
    }

    private static final class Banner extends Item {
        Banner() {
            super(new Properties().stacksTo(16));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (level.dimension() != Level.OVERWORLD || !player.isAlive() || player.isSpectator()) {
                return InteractionResultHolder.pass(stack);
            }
            if (level instanceof ServerLevel server && player instanceof ServerPlayer sender) {
                var site = MythrilKnightArena.summonSite(server, sender);
                if (site == null || !MythrilKnightEncounter.summon(server, sender, site)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (!sender.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}
