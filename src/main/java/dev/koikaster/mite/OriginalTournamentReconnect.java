package dev.koikaster.mite;

/** R195 SoonestReconnectionTime and jy's day-boundary admission rules. */
final class OriginalTournamentReconnect {
    private static final int DAY_TICKS = 24000;
    private static final int HOUR_TICKS = 1000;
    private static final int SUNRISE = 5;
    private static final int LATEST_HOUR = 20;
    private static final int GRACE_TICKS = 600;
    static final int DEPLETION_TICKS = 72000;

    record Decision(int delayTicks, long carriedTicks) {}

    private long leftAt;
    private long earliest;
    private int leaveHour;
    private long carriedTicks;

    void leave(long gameTime, int adjustedTime) {
        leftAt = gameTime;
        int hour = adjustedTime / HOUR_TICKS;
        leaveHour = hour >= SUNRISE && hour <= LATEST_HOUR ? hour : LATEST_HOUR;
        earliest = gameTime + DAY_TICKS - adjustedTime + (long) leaveHour * HOUR_TICKS;
    }

    Decision evaluate(long gameTime, int adjustedTime) {
        long elapsed = carriedTicks + Math.max(0, gameTime - leftAt);
        int hour = adjustedTime / HOUR_TICKS;
        if (hour == LATEST_HOUR) return new Decision(0, 0);
        if (elapsed <= GRACE_TICKS) return new Decision(0, elapsed);
        if (gameTime >= earliest && hour >= leaveHour && hour <= LATEST_HOUR) {
            return new Decision(0, 0);
        }
        long untilEarliest = earliest - gameTime;
        if (untilEarliest <= 0) untilEarliest = Math.floorMod(untilEarliest - 1, DAY_TICKS) + 1;
        int untilLatest = Math.floorMod(LATEST_HOUR * HOUR_TICKS - adjustedTime, DAY_TICKS);
        return new Decision((int) Math.min(untilEarliest, untilLatest), elapsed);
    }

    void joined(long gameTime, int adjustedTime) {
        carriedTicks = evaluate(gameTime, adjustedTime).carriedTicks();
    }

    void creditOnlineTick() {
        if (carriedTicks > 0) carriedTicks--;
    }

    static String format(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        int minutes = seconds / 60 % 60;
        int remainder = seconds % 60;
        return seconds / 3600 + ":" + (minutes < 10 ? "0" : "") + minutes
                + ":" + (remainder < 10 ? "0" : "") + remainder;
    }
}

