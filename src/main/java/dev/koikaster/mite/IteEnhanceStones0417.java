package dev.koikaster.mite;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers MITE-ITE 2.0.30 four Enhance Stones (iron, mithril, adamantium, universal)
 * with upstream failure rates (35/55/65/50) for 0.4.17.
 */
public final class IteEnhanceStones0417 {
    public static final RegistryObject<Item> IRON = Content.item("iron_enhance_stone",
        () -> new StoneItem(IteForgingRecipes0417.StoneType.IRON));
    public static final RegistryObject<Item> MITHRIL = Content.item("mithril_enhance_stone",
        () -> new StoneItem(IteForgingRecipes0417.StoneType.MITHRIL));
    public static final RegistryObject<Item> ADAMANTIUM = Content.item("adamantium_enhance_stone",
        () -> new StoneItem(IteForgingRecipes0417.StoneType.ADAMANTIUM));
    public static final RegistryObject<Item> UNIVERSAL = Content.item("universal_enhance_stone",
        () -> new StoneItem(IteForgingRecipes0417.StoneType.UNIVERSAL));

    public static final class StoneItem extends Item {
        public final IteForgingRecipes0417.StoneType stoneType;

        public StoneItem(IteForgingRecipes0417.StoneType type) {
            super(new Item.Properties().stacksTo(16));
            this.stoneType = type;
        }
    }

    public static void init() {
        // Trigger registration during Content bootstrap.
    }

    private IteEnhanceStones0417() {}
}
