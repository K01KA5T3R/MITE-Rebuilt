package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

final class GiantSkeletonWavePlan {
    static final int LAST_WAVE = 9;
    static final String ROLE_TAG = "MiteGiantSkeletonWaveRole";

    enum Role {
        SUPPORT, FRONT, FLANK, CAPTAIN, FILL;

        static Role read(String name) {
            for (Role role : values()) if (role.name().equals(name)) return role;
            return FILL;
        }
    }

    record Recruit(EntityType<? extends Mob> type, Role role) {
        boolean ranged() {
            return type == EntityType.SKELETON || type == EntityType.STRAY;
        }
    }

    private record Weighted(EntityType<? extends Mob> type, int opening, int middle, int finalWaves) {
        int weight(int wave) {
            return wave <= 3 ? opening : wave <= 6 ? middle : finalWaves;
        }
    }

    private static final List<Weighted> POOL = List.of(
        new Weighted(EntityType.SKELETON, 7, 6, 3),
        new Weighted(EntityType.ZOMBIE, 15, 6, 2),
        new Weighted(EntityType.HUSK, 5, 12, 5),
        new Weighted(EntityType.STRAY, 0, 8, 12),
        new Weighted(EntityType.WITHER_SKELETON, 0, 3, 15));

    private static EntityType<Mobs.Undead> nativeUndead(String name) {
        return Mobs.UNDEAD.get(name).get();
    }

    private GiantSkeletonWavePlan() {}

    static int units(int wave, int players) {
        if (wave < 1 || wave > LAST_WAVE) throw new IllegalArgumentException("Invalid wave " + wave);
        int solo = switch (wave) {
            case 1, 2 -> 3;
            case 3, 4 -> 4;
            case 5, 6 -> 5;
            case 7, 8 -> 6;
            default -> 8;
        };
        int previous = (int)Math.min(13, solo + Math.max(0L, (long)players - 1));
        return (previous * 8 + 4) / 5;
    }

    static List<Recruit> roster(int wave, int count, RandomSource random) {
        if (wave < 1 || wave > LAST_WAVE || count < 3) throw new IllegalArgumentException("Invalid wave roster");
        var result = new ArrayList<Recruit>(count);
        result.add(new Recruit(wave < 3 ? EntityType.ZOMBIE : wave < 7 ? EntityType.HUSK : EntityType.WITHER_SKELETON,
            wave == LAST_WAVE ? Role.CAPTAIN : Role.FRONT));
        if (wave >= 2) result.add(new Recruit(wave < 4 ? EntityType.SKELETON : EntityType.STRAY, Role.SUPPORT));
        if (wave >= 6) result.add(new Recruit(wave < 8 ? EntityType.HUSK : EntityType.WITHER_SKELETON, Role.FLANK));
        if (wave >= 8 && count >= 5) result.add(new Recruit(EntityType.STRAY, Role.SUPPORT));
        var nativeFront = wave < 3 ? nativeUndead("ghoul")
            : wave < 6 ? nativeUndead("wight") : nativeUndead("revenant");
        if (result.size() < count) result.add(new Recruit(nativeFront, Role.FRONT));
        if (wave >= 7 && result.size() < count) result.add(new Recruit(nativeUndead("wight"), Role.FLANK));
        if (wave >= 8 && result.size() < count) result.add(new Recruit(nativeUndead("shadow"), Role.FLANK));
        while (result.size() < count) result.add(new Recruit(draw(wave, random), Role.FILL));
        return result;
    }

    static EntityType<? extends Mob> savedType(ResourceLocation id) {
        if (id == null) return null;
        for (var entry : POOL) {
            if (EntityType.getKey(entry.type()).equals(id)) return entry.type();
        }
        if (id.getNamespace().equals(MiteReforged.ID)) {
            return switch (id.getPath()) {
                case "ghoul", "wight", "revenant", "shadow" -> nativeUndead(id.getPath());
                default -> null;
            };
        }
        return null;
    }

    static EntityType<? extends Mob> draw(int wave, RandomSource random) {
        if (wave < 1 || wave > LAST_WAVE) throw new IllegalArgumentException("Invalid wave " + wave);
        int total = POOL.stream().mapToInt(entry -> entry.weight(wave)).sum();
        int choice = random.nextInt(total);
        for (var entry : POOL) {
            choice -= entry.weight(wave);
            if (choice < 0) return entry.type();
        }
        throw new IllegalStateException("Undead weight table did not cover its draw");
    }
}
