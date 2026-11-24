package dev.koikaster.mite;

import java.util.List;
import net.minecraft.world.level.Level;

/** World-scoped questionnaire47D/V042 policy: Extras mode hides plantDoctor; pure R195/R196 restore it. */
public final class SourceAchievementPolicy567 {
    public static final int PLANT_DOCTOR = 123;
    static final int MAX_ACTIVE = 62;

    public static boolean active(int id) {
        return SourceAchievementCatalog561.known(id);
    }

    public static boolean active(int id, boolean r196) {
        return active(id, r196, true);
    }

    public static boolean active(int id, boolean r196, boolean extras) {
        return active(id)
            && (!extras || id != PLANT_DOCTOR)
            && (r196 || id != R196AchievementLayout.BETTER_TOOLS);
    }

    public static boolean active(int id, Level level) {
        return active(id, WorldContentMode.r196(level), WorldContentMode.rebuilt(level));
    }

    public static List<SourceAchievementCatalog561.Node> nodes() {
        return nodes(false, true);
    }

    public static List<SourceAchievementCatalog561.Node> nodes(boolean r196) {
        return nodes(r196, true);
    }

    public static List<SourceAchievementCatalog561.Node> nodes(boolean r196, boolean extras) {
        return SourceAchievementCatalog561.nodes(r196).stream()
            .filter(n -> active(n.id(), r196, extras))
            .toList();
    }

    public static boolean automaticNotification() {
        return false;
    }

    private SourceAchievementPolicy567() {}
}
