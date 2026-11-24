package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.*;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Interactive Forging Table Container Menu (9 forging slots: 5 material slots,
 * target equipment, axe, warhammer, output) mirroring MITE-ITE ContainerForgingTable (0.4.17).
 */
public final class IteForgingMenu0417 extends AbstractContainerMenu {
    public static final RegistryObject<MenuType<IteForgingMenu0417>> TYPE = Content.MENUS.register(
        "ite_forging", () -> IForgeMenuType.create((id, inv, buf) -> new IteForgingMenu0417(id, inv)));

    // Slot indices: 0..4=materials, 5=tool/armor, 6=axe, 7=hammer, 8=output
    public final SimpleContainer table = new SimpleContainer(9);
    private final ContainerLevelAccess access;
    private final Player owner;
    private final int tableTier;
    private int progress;
    private int totalTime;
    private int active;

    public IteForgingMenu0417(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL, 0);
    }

    public IteForgingMenu0417(int id, Inventory inv, ContainerLevelAccess access, int tableTier) {
        super(TYPE.get(), id);
        this.owner = inv.player;
        this.access = access;
        this.tableTier = tableTier;
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            addSlot(new Slot(table, i, 8 + i * 18, 17) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (idx == 8) return false;
                    if (idx == 6) return isAxe(stack);
                    if (idx == 7) return isHammer(stack);
                    return true;
                }
            });
        }
        addDataSlot(new DataSlot() {
            @Override public int get() { return progress; }
            @Override public void set(int v) { progress = v; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return totalTime; }
            @Override public void set(int v) { totalTime = v; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return active; }
            @Override public void set(int v) { active = v; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    private static boolean isAxe(ItemStack stack) {
        var p = ClassicToolWear.profile(stack);
        return p != null && (p.kind() == LegacyToolWear.Kind.AXE || p.kind() == LegacyToolWear.Kind.BATTLEAXE)
            || stack.getItem() instanceof AxeItem;
    }

    private static boolean isHammer(ItemStack stack) {
        var p = ClassicToolWear.profile(stack);
        return p != null && p.kind() == LegacyToolWear.Kind.WARHAMMER;
    }

    public boolean startForging() {
        var recipe = currentRecipe();
        if (recipe == null || !hasRequiredTools(recipe) || !hasMaterials(recipe)) return false;
        if (!table.getItem(8).isEmpty()) return false;
        totalTime = recipe.timeReqTicks();
        progress = 0;
        active = 1;
        return true;
    }

    public void tickForging() {
        if (active == 0 || owner.level().isClientSide) return;
        var recipe = currentRecipe();
        if (recipe == null || !hasRequiredTools(recipe) || !hasMaterials(recipe)) {
            active = 0;
            progress = 0;
            return;
        }
        if (++progress < totalTime) return;
        active = 0;
        progress = 0;
        ItemStack target = table.getItem(5).copy();
        ItemStack axe = table.getItem(6);
        ItemStack hammer = table.getItem(7);
        int hammerLv = HarvestRules.harvestLevel(hammer);
        int axeLv = HarvestRules.harvestLevel(axe);

        consumeMaterials(recipe);
        damageTool(hammer, recipe.hammerCost());
        damageTool(axe, recipe.axeCost());
        var outcome = IteForgingRecipes0417.executeForge(target, recipe, hammerLv, axeLv, owner.getRandom());
        table.setItem(5, ItemStack.EMPTY);
        table.setItem(8, target);
        if (owner instanceof ServerPlayer sp) {
            owner.level().playSound(null, owner.blockPosition(),
                outcome.succeeded() ? SoundEvents.ANVIL_USE : SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS, 0.8F, outcome.succeeded() ? 1.0F : 0.75F);
            sp.displayClientMessage(Component.translatable(
                outcome.succeeded() ? "gui.forgingTable.succeed" : "gui.forgingTable.failed"), true);
        }
    }

    private IteForgingRecipes0417.ForgingRecipe currentRecipe() {
        return IteForgingRecipes0417.recipeFor(table.getItem(5), tableTier);
    }

    private boolean hasRequiredTools(IteForgingRecipes0417.ForgingRecipe recipe) {
        ItemStack axe = table.getItem(6);
        ItemStack hammer = table.getItem(7);
        return !axe.isEmpty() && isAxe(axe) && !hammer.isEmpty() && isHammer(hammer);
    }

    private boolean hasMaterials(IteForgingRecipes0417.ForgingRecipe recipe) {
        for (var req : recipe.materials()) {
            Item needed = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(req.itemPath()));
            if (needed == null) return false;
            int found = 0;
            for (int i = 0; i < 5; i++) {
                ItemStack s = table.getItem(i);
                if (s.is(needed)) found += s.getCount();
            }
            if (found < req.count()) return false;
        }
        return true;
    }

    private void consumeMaterials(IteForgingRecipes0417.ForgingRecipe recipe) {
        for (var req : recipe.materials()) {
            Item needed = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(req.itemPath()));
            if (needed == null) continue;
            int rem = req.count();
            for (int i = 0; i < 5 && rem > 0; i++) {
                ItemStack s = table.getItem(i);
                if (s.is(needed)) {
                    int take = Math.min(rem, s.getCount());
                    s.shrink(take);
                    if (s.isEmpty()) table.setItem(i, ItemStack.EMPTY);
                    rem -= take;
                }
            }
        }
    }

    private static void damageTool(ItemStack tool, int cost) {
        if (tool.isEmpty() || !tool.isDamageableItem()) return;
        int next = tool.getDamageValue() + cost;
        if (next >= tool.getMaxDamage()) tool.setCount(0);
        else tool.setDamageValue(next);
    }

    public int progress() { return progress; }
    public int totalTime() { return totalTime; }
    public boolean active() { return active != 0; }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack raw = slot.getItem();
        ItemStack copy = raw.copy();
        if (index < 9) {
            if (!this.moveItemStackTo(raw, 9, 45, true)) return ItemStack.EMPTY;
        } else if (isAxe(raw)) {
            if (!this.moveItemStackTo(raw, 6, 7, false)) return ItemStack.EMPTY;
        } else if (isHammer(raw)) {
            if (!this.moveItemStackTo(raw, 7, 8, false)) return ItemStack.EMPTY;
        } else if ((raw.isDamageableItem() || raw.getMaxDamage() > 0) && (raw.getItem() instanceof TieredItem || raw.getItem() instanceof ArmorItem)) {
            if (!this.moveItemStackTo(raw, 5, 6, false)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(raw, 0, 5, false)) return ItemStack.EMPTY;
        }
        if (raw.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (raw.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, raw);
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return access.evaluate((l, pos) -> p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!p.level().isClientSide) clearContainer(p, table);
    }
}
