package dev.koikaster.mite;
/** Transient state belongs to its actual native Mob, with no static owner map or save tag. */
public interface SourceRepairOwner {
 Object mite$repairOwner();
 void mite$repairOwner(Object state);
}
