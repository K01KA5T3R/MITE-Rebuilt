package dev.koikaster.mite;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

/** Origins of native Cat instances admitted to the classic experience. */
public final class ClassicCats {
    private static final String ORIGINAL = "R195OcelotTamed";
    private static final String CAT_TYPE = "R195CatType";
    private static final String STORED = "MiteLegacyStoredCat";

    public static boolean skipWildSpawner(ServerLevel level) {
        return WorldContentMode.settings(level).classicVisuals();
    }

    static boolean allows(Cat cat) {
        return cat.getPersistentData().getBoolean(STORED) || originalType(cat) != 0;
    }

    static boolean retainStored(Cat cat) {
        // Forge copies this field with the old entity when it travels to another dimension.
        cat.getPersistentData().putBoolean(STORED, true);
        return true;
    }

    static void markOriginal(Cat cat, int type) {
        cat.getPersistentData().putBoolean(ORIGINAL, true);
        cat.getPersistentData().putInt(CAT_TYPE, type);
    }

    static ResourceKey<CatVariant> variant(int type) {
        return switch (type) {
            case 1 -> CatVariant.BLACK;
            case 2 -> CatVariant.RED;
            case 3 -> CatVariant.SIAMESE;
            default -> throw new IllegalArgumentException("Invalid original CatType: " + type);
        };
    }

    private static boolean eligibleOwner(Cat cat) {
        return cat.getType() == EntityType.CAT && cat.isTame() && cat.getOwnerUUID() != null;
    }

    private static ResourceKey<CatVariant> currentVariant(Cat cat) {
        return cat.level().registryAccess().registryOrThrow(Registries.CAT_VARIANT)
            .getResourceKey(cat.getVariant()).orElse(null);
    }

    private static int originalType(Cat cat) {
        if (!eligibleOwner(cat) || !cat.getPersistentData().getBoolean(ORIGINAL)) return 0;
        int type = cat.getPersistentData().getInt(CAT_TYPE);
        return type >= 1 && type <= 3 && variant(type).equals(currentVariant(cat)) ? type : 0;
    }

    private static int breedingType(Cat cat) {
        int original = originalType(cat);
        if (original != 0 || !eligibleOwner(cat) || !cat.getPersistentData().getBoolean(STORED)) return original;
        var actual = currentVariant(cat);
        for (int type = 1; type <= 3; type++) {
            if (variant(type).equals(actual)) return type;
        }
        return 0;
    }

    static void breed(BabyEntitySpawnEvent event) {
        if (!(event.getChild() instanceof Cat kitten) || kitten.getType() != EntityType.CAT
                || !(kitten.level() instanceof ServerLevel level) || !skipWildSpawner(level)) return;
        if (!(event.getParentA() instanceof Cat first) || !(event.getParentB() instanceof Cat other)
                || first == other || first.level() != level || other.level() != level) {
            event.setCanceled(true);
            return;
        }
        int type = breedingType(first);
        if (type == 0 || breedingType(other) == 0) {
            event.setCanceled(true);
            return;
        }
        kitten.setOwnerUUID(first.getOwnerUUID());
        kitten.setTame(true);
        kitten.setVariant(first.getVariant());
        markOriginal(kitten, type);
    }

    private ClassicCats() {}
}
