package dev.koikaster.mite;

import java.util.Map;
import java.util.function.IntPredicate;

/** Visibility and world first-completer semantics for both achievement layouts. */
public final class SourceAchievementRules561 {
    public record First(String username, int day) {}

    public static boolean allParents(int id, boolean includeSecond, IntPredicate unlocked) {
        return allParents(id, includeSecond, unlocked, false);
    }

    static boolean allParents(int id, boolean includeSecond, IntPredicate unlocked, boolean r196) {
        var node = SourceAchievementCatalog561.node(id, r196);
        if (includeSecond && secondPathUnlocked(node, unlocked, r196)) return true;
        while (node.parent() >= 0) {
            node = SourceAchievementCatalog561.node(node.parent(), r196);
            if (!unlocked.test(node.id())) return false;
            if (includeSecond && secondPathUnlocked(node, unlocked, r196)) return true;
        }
        return true;
    }

    private static boolean secondPathUnlocked(SourceAchievementCatalog561.Node node, IntPredicate unlocked, boolean r196) {
        int parent = node.secondParent();
        return parent >= 0 && unlocked.test(parent) && allParents(parent, true, unlocked, r196);
    }

    public static boolean canShow(int id, IntPredicate unlocked) {
        return canShow(id, unlocked, false);
    }

    static boolean canShow(int id, IntPredicate unlocked, boolean r196) {
        return allParents(id, true, unlocked, r196);
    }

    public static int referTo(int id, IntPredicate unlocked) {
        return referTo(id, unlocked, false);
    }

    static int referTo(int id, IntPredicate unlocked, boolean r196) {
        var node = SourceAchievementCatalog561.node(id, r196);
        while (!allParents(node.id(), true, unlocked, r196)) {
            node = SourceAchievementCatalog561.node(node.parent(), r196);
        }
        return node.id();
    }

    public static boolean first(Map<Integer, First> records, int id, String username, int day) {
        SourceAchievementCatalog561.node(id);
        if (records.containsKey(id)) return false;
        records.put(id, new First(username, day));
        return true;
    }

    private SourceAchievementRules561() {}
}