package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.util.RandomSource;

/** The encounter decides eligibility; this only favors variety among eligible moves. */
final class BossAttackOrder<T extends Enum<T>> {
    private record Choice<T>(T move, double weight) {}

    private final T[] moves;
    private final EnumMap<T, Integer> lastUsed;
    private int turn;

    BossAttackOrder(Class<T> type) {
        moves = type.getEnumConstants();
        lastUsed = new EnumMap<>(type);
    }

    List<T> candidates(RandomSource random) {
        List<Choice<T>> order = new ArrayList<>(moves.length);
        for (T move : moves) {
            int age = Math.min(8, turn - lastUsed.getOrDefault(move, -8));
            double weight = random.nextDouble() + age * .15;
            if (age < 2) weight -= 1.2;
            order.add(new Choice<>(move, weight));
        }
        order.sort(Comparator.comparingDouble((Choice<T> choice) -> choice.weight()).reversed());
        return order.stream().map(Choice::move).toList();
    }

    void used(T move) {
        lastUsed.put(move, ++turn);
    }
}
