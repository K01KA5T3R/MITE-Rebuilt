package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

public final class DirectionalSorting2113 {
    public static final int ROWS = 21;
    public static final int COLUMNS = 22;

    public static boolean click(AbstractContainerMenu menu, Player actor, int button) {
        if (!(actor instanceof ServerPlayer player) || !WorldContentMode.rebuilt(actor.level())
            || player.containerMenu != menu || player.isSpectator()
            || (button != ROWS && button != COLUMNS) || !menu.getCarried().isEmpty() || !menu.stillValid(player)) {
            return false;
        }
        var slots = slots(menu, player.getInventory());
        if (slots.isEmpty() || slots.stream().anyMatch(slot -> !slot.mayPickup(player))) return false;
        slots.sort(button == ROWS ? Comparator.comparingInt((Slot slot) -> slot.y).thenComparingInt(slot -> slot.x)
            : Comparator.comparingInt((Slot slot) -> slot.x).thenComparingInt(slot -> slot.y));

        var inputs = new ArrayList<ItemStack>();
        for (var slot : slots) {
            var stack = slot.getItem();
            if (!stack.isEmpty()) inputs.add(stack);
        }
        inputs.sort(Comparator.comparing(stack -> String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()))));
        var grouped = compact(inputs);
        if (grouped.size() > slots.size()) return false;
        int lineLength = button == ROWS ? 9 : slots.size() / 9;
        var layout = separated(grouped, slots.size(), lineLength, ItemStack::getItem);
        if (!fits(slots, layout)) {
            var byCategory = new ArrayList<>(grouped);
            byCategory.sort(Comparator.comparing(DirectionalSorting2113::category));
            layout = separated(byCategory, slots.size(), lineLength, DirectionalSorting2113::category);
        }
        if (!fits(slots, layout)) {
            layout = grouped;
            if (!fits(slots, layout)) return false;
        }
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            var result = i < layout.size() ? layout.get(i) : ItemStack.EMPTY;
            if (!ItemStack.matches(slot.getItem(), result)) slot.set(result);
        }
        menu.broadcastChanges();
        return true;
    }

    private static List<Slot> slots(AbstractContainerMenu menu, Inventory inventory) {
        var target = new ArrayList<Slot>();
        if (menu.getClass() == InventoryMenu.class) {
            for (var slot : menu.slots) {
                int index = slot.getContainerSlot();
                if (slot.container == inventory && index >= 9 && index < Inventory.INVENTORY_SIZE) target.add(slot);
            }
        } else if (menu.getClass() == ChestMenu.class && menu instanceof ChestMenu chest) {
            for (int index = 0; index < chest.getRowCount() * 9; index++) {
                var slot = chest.getSlot(index);
                if (slot.container == chest.getContainer()) target.add(slot);
            }
        }
        return target;
    }

    private static boolean fits(List<Slot> slots, List<ItemStack> layout) {
        if (layout == null || layout.size() > slots.size()) return false;
        for (int i = 0; i < layout.size(); i++) {
            var stack = layout.get(i);
            if (stack.isEmpty()) continue;
            var slot = slots.get(i);
            if (!slot.mayPlace(stack) || stack.getCount() > slot.getMaxStackSize(stack)) return false;
        }
        return true;
    }

    private static List<ItemStack> separated(List<ItemStack> stacks, int size, int lineLength, Function<ItemStack, ?> groupBy) {
        var result = new ArrayList<ItemStack>(Collections.nCopies(size, ItemStack.EMPTY));
        Object previous = null;
        int next = 0;
        for (var stack : stacks) {
            var group = groupBy.apply(stack);
            if (group != previous) {
                int used = next % lineLength;
                if (used != 0) next += lineLength - used;
                previous = group;
            }
            if (next >= size) return null;
            result.set(next++, stack);
        }
        return result;
    }

    private enum Category { EQUIPMENT, FOOD, FARMING, MATERIALS, DEVICES, BUILDING }

    private static Category category(ItemStack stack) {
        var item = stack.getItem();
        if (stack.getFoodProperties(null) != null || item instanceof PotionItem) return Category.FOOD;
        if (item instanceof TieredItem || item instanceof ArmorItem || item instanceof ProjectileWeaponItem
            || item instanceof FishingRodItem || item instanceof ShearsItem || item instanceof TridentItem
            || item instanceof ShieldItem || item instanceof ElytraItem || item instanceof HorseArmorItem
            || item instanceof FlintAndSteelItem || item instanceof BucketItem || ClassicVessels.handled(stack)
            || stack.is(ItemTags.ARROWS)) return Category.EQUIPMENT;
        if (item instanceof SpawnEggItem || item instanceof ItemNameBlockItem || stack.is(Tags.Items.SEEDS)
            || stack.is(Tags.Items.CROPS) || stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.FLOWERS)
            || stack.is(ItemTags.LEAVES) || stack.is(Items.BONE_MEAL)) return Category.FARMING;
        if (stack.is(Items.REDSTONE) || stack.is(Items.COMPARATOR) || stack.is(Items.REPEATER)
            || stack.is(Items.REDSTONE_BLOCK) || stack.is(Items.REDSTONE_TORCH)) return Category.DEVICES;
        if (item instanceof BlockItem blockItem) {
            var block = blockItem.getBlock();
            if (block instanceof BaseRailBlock || block instanceof DiodeBlock || block instanceof PistonBaseBlock
                || block instanceof DispenserBlock || block instanceof HopperBlock || block instanceof AbstractFurnaceBlock
                || block instanceof ThermalFurnace.BlockImpl || block instanceof AnvilBlock || block instanceof MetalAnvilBlock
                || block instanceof BrewingStandBlock || block instanceof EnchantmentTableBlock
                || block instanceof CraftingTableBlock || block instanceof MithrilAutoWorkbench.BlockImpl
                || block instanceof FoodPreservation1905.Rack) return Category.DEVICES;
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (!stack.is(Tags.Items.ORES) && !stack.is(Tags.Items.STORAGE_BLOCKS)
                && (id == null || !id.getPath().endsWith("_ore"))) return Category.BUILDING;
        }
        return Category.MATERIALS;
    }

    private static List<ItemStack> compact(List<ItemStack> inputs) {
        var result = new ArrayList<ItemStack>();
        for (var input : inputs) {
            int left = input.getCount();
            int limit = input.getMaxStackSize();
            for (var existing : result) {
                if (!ItemStack.isSameItemSameTags(existing, input)) continue;
                int move = Math.min(left, limit - existing.getCount());
                existing.grow(move);
                left -= move;
                if (left == 0) break;
            }
            while (left > 0) {
                int count = Math.min(left, limit);
                result.add(input.copyWithCount(count));
                left -= count;
            }
        }
        return result;
    }

    public static java.util.stream.Stream<?> shortcutActions(Object[] actions) {
        return java.util.Arrays.stream(actions).filter(action -> !legacySortAction(action));
    }

    public static boolean legacySortAction(Object action) {
        return action instanceof Enum<?> value && value.name().equals("SORT");
    }

    private DirectionalSorting2113() {}
}
