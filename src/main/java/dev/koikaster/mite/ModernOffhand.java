package dev.koikaster.mite;

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;

/** Uses the player's current world for native offhand admission and V016. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class ModernOffhand {
    public static boolean allowed(Player player, ItemStack stack) {
        if (stack.isEmpty()) return true;
        var settings = WorldContentMode.settings(player.level());
        return !settings.classicVisuals() && (!settings.extras() || stack.is(Items.TOTEM_OF_UNDYING)
            || stack.is(Items.TIPPED_ARROW) || stack.is(Items.FIREWORK_ROCKET));
    }

    public static boolean canUseHand(LivingEntity actor, InteractionHand hand) {
        return hand != InteractionHand.OFF_HAND || !(actor instanceof Player player)
            || allowed(player, player.getOffhandItem());
    }

    public static boolean rejectSwap(Object rawPlayer, Object rawPacket) {
        var player = (ServerPlayer) rawPlayer;
        var packet = (ServerboundPlayerActionPacket) rawPacket;
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
                || allowed(player, player.getMainHandItem())) return false;
        player.inventoryMenu.broadcastFullState();
        return true;
    }

    private static void rejectUse(PlayerInteractEvent event) {
        if (event.getHand() != InteractionHand.OFF_HAND || allowed(event.getEntity(), event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void block(PlayerInteractEvent.RightClickBlock event) {
        rejectUse(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void item(PlayerInteractEvent.RightClickItem event) {
        rejectUse(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entity(PlayerInteractEvent.EntityInteract event) {
        rejectUse(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void specific(PlayerInteractEvent.EntityInteractSpecific event) {
        rejectUse(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void reconcile(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)) return;
        var original = player.getOffhandItem();
        if (allowed(player, original)) return;

        var inventory = player.getInventory();
        var main = new RangedWrapper(new InvWrapper(inventory), 0, inventory.items.size());
        var remaining = ItemHandlerHelper.insertItemStacked(main, original.copy(), false);
        if (!remaining.isEmpty()) {
            var level = player.serverLevel();
            var drop = new ItemEntity(level, player.getX(), player.getY() + .35, player.getZ(), remaining.copy());
            drop.setThrower(player.getUUID());
            drop.setDefaultPickUpDelay();
            if (level.addFreshEntity(drop)) remaining = ItemStack.EMPTY;
        }
        if (remaining.getCount() == original.getCount()) return;
        player.setItemInHand(InteractionHand.OFF_HAND, remaining);
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
    }

    private ModernOffhand() {}
}

