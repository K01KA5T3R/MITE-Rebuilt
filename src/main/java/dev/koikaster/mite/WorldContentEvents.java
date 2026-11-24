package dev.koikaster.mite;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class WorldContentEvents {
    @SubscribeEvent
    public static void beforeStart(ServerAboutToStartEvent event) {
        restrictRecipes(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void reloadBeforeRecipesAreSent(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) restrictRecipes(event.getPlayerList().getServer());
        else WorldContentTransport.sync(event.getPlayer());
    }

    private static void restrictRecipes(MinecraftServer server) {
        var settings = WorldContentMode.settings(server.getWorldData());
        var manager = server.getRecipeManager();
        var registries = server.registryAccess();
        manager.replaceRecipes(manager.getRecipes().stream()
            .filter(recipe -> RebuiltContent.allows(recipe, registries, settings))
            .<Recipe<?>>map(recipe -> {
                if (!settings.extras() && recipe instanceof ShapelessRecipe original && original.getId().equals(Content.id("salad"))) {
                    var flower = Ingredient.of(Items.DANDELION);
                    var ingredients = NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.BOWL), flower, flower, flower);
                    return new ShapelessRecipe(original.getId(), original.getGroup(), original.category(),
                        original.getResultItem(registries).copy(), ingredients);
                }
                return IteGoldenAppleRecipes.select(recipe, registries, settings.ite());
            }).toList());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (!RebuiltContent.allows(event.getItemStack(), WorldContentMode.settings(event.getLevel()))) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var settings = WorldContentMode.settings(level);
        if (!RebuiltContent.allows(level.getBlockState(event.getPos()), settings)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        } else if (!RebuiltContent.allows(event.getItemStack(), settings)) {
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    static boolean itemActionAllowed(Level level, ItemStack stack) {
        return stack.isEmpty() || !ModernContentPolicy.removed(stack) && RebuiltContent.allows(stack, WorldContentMode.settings(level));
    }

    static boolean ingredientsAllowed(Level level, Container input) {
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            if (!itemActionAllowed(level, input.getItem(slot))) return false;
        }
        return true;
    }

    static boolean effectAllowed(LivingEntity actor, ItemStack stack) {
        return !(actor instanceof Player) || itemActionAllowed(actor.level(), stack);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntity(PlayerInteractEvent.EntityInteract event) {
        if (itemActionAllowed(event.getLevel(), event.getItemStack())) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntityAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (itemActionAllowed(event.getLevel(), event.getItemStack())) return;
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        var player = event.getEntity();
        if (!itemActionAllowed(player.level(), player.getMainHandItem())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void startMining(PlayerInteractEvent.LeftClickBlock event) {
        if (itemActionAllowed(event.getLevel(), event.getItemStack())) return;
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START
                || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        var player = event.getPlayer();
        if (!itemActionAllowed(player.level(), player.getMainHandItem())) event.setCanceled(true);
    }

    public static boolean craftingAllowed(Object rawType, Object rawInput, Object rawLevel) {
        return rawType != net.minecraft.world.item.crafting.RecipeType.CRAFTING
            || ingredientsAllowed((Level) rawLevel, (Container) rawInput);
    }

    public static ItemStack activeEquipment(ItemStack stack, LivingEntity actor) {
        return effectAllowed(actor, stack) && (!(actor instanceof Player player)
            || stack != player.getOffhandItem() || ModernOffhand.allowed(player, stack)) ? stack : ItemStack.EMPTY;
    }

    public static java.util.function.Predicate<ItemStack> projectilePredicate(
            java.util.function.Predicate<ItemStack> predicate, Player player) {
        return stack -> predicate.test(stack) && itemActionAllowed(player.level(), stack)
            && (stack != player.getOffhandItem() || ModernOffhand.allowed(player, stack));
    }

    public static boolean canGlide(ItemStack stack, LivingEntity actor) {
        return effectAllowed(actor, stack) && stack.canElytraFly(actor);
    }

    public static com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            net.minecraft.world.entity.ai.attributes.AttributeModifier> equipmentModifiers(
            ItemStack stack, EquipmentSlot slot, LivingEntity actor) {
        boolean permitted = effectAllowed(actor, stack) && (slot != EquipmentSlot.OFFHAND
            || !(actor instanceof Player player) || ModernOffhand.allowed(player, stack));
        return permitted ? stack.getAttributeModifiers(slot) : com.google.common.collect.ImmutableMultimap.of();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useTotem(LivingUseTotemEvent event) {
        var actor = event.getEntity();
        if (!effectAllowed(actor, event.getTotem())
                || event.getHandHolding() == InteractionHand.OFF_HAND && actor instanceof Player player
                && !ModernOffhand.allowed(player, event.getTotem())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level
                && !RebuiltContent.allows(event.getPlacedBlock(), WorldContentMode.settings(level))) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityJoins(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var settings = WorldContentMode.settings(level);
        var entity = event.getEntity();
        boolean stored = event.loadedFromDisk() || LegacyStoredVehicle.restoring(entity);
        if (stored && LegacyBossArchive.archiveIfInactive(entity, level, settings)) {
            event.setCanceled(true);
            return;
        }
        if (!RebuiltContent.allows(entity, settings) && !(stored && keepsLegacyResident(event, level, settings))
                && !LegacyShoulderParrot.keeps(event, level, settings)) {
            event.setCanceled(true);
        }
    }

    private static boolean keepsLegacyResident(EntityJoinLevelEvent event, ServerLevel level, WorldExtraSettings settings) {
        if (!settings.classicVisuals()) return false;
        var entity = event.getEntity();
        var type = entity.getType();
        var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null || !"minecraft".equals(id.getNamespace()) || ModernContentPolicy.removed(type)) return false;
        if (event.loadedFromDisk() && type == EntityType.CAT && entity instanceof Cat cat) return ClassicCats.retainStored(cat);
        if (entity instanceof SourceElderGuardianBoss elder && elder.ritual()
                || entity instanceof SourceGiantZombieBoss giant && giant.ritual()) return false;
        // The former classic catalogue did not exclude these types.
        if (type == EntityType.PARROT || type == EntityType.ENDERMITE || type == EntityType.CAMEL) return true;
        if (!settings.extras()) {
            // Legacy group bits admitted only these ordinary residents before effective group gating.
            return type == EntityType.GUARDIAN && settings.rebuiltBosses()
                || type == EntityType.BEE && settings.calamityBosses();
        }
        // Classic worlds with the historical modern terrain retain prior residents.
        return !WorldContentMode.classicTerrain(level.getServer().getWorldData())
            && ClassicModernContent.entity(entity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void catsBreed(BabyEntitySpawnEvent event) {
        ClassicCats.breed(event);
    }

    public static boolean creativeStackAllowed(ServerboundSetCreativeModeSlotPacket packet, ServerGamePacketListenerImpl listener) {
        var player = listener.player;
        var stack = packet.getItem();
        if (!RebuiltContent.allows(stack, WorldContentMode.settings(player.level()))) return false;
        if (packet.getSlotNum() != 45 || ModernOffhand.allowed(player, stack)) return true;
        if (player.isCreative()) player.inventoryMenu.broadcastFullState();
        return false;
    }

    private WorldContentEvents() {}
}
