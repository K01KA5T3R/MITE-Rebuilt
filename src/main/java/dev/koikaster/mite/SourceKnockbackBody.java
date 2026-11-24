package dev.koikaster.mite;

/** Runtime-only of fields. These clocks are deliberately not saved to NBT. */
public interface SourceKnockbackBody {
 int mite$knockbackTicks();void mite$knockbackTicks(int value);
 float mite$attackedYaw();void mite$attackedYaw(float value);
}
