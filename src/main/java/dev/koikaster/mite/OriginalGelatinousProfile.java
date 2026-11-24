package dev.koikaster.mite;

/** New039 source profile used by the real native owners. No hard damage gate is implied here.
 * Values come from EntityJelly/Blob/Ooze/Pudding and EntityGelatinousCube. */
public enum OriginalGelatinousProfile {
 JELLY(1,2,false), BLOB(2,3,false), OOZE(3,3,true), PUDDING(4,4,true);
 private final int dropSubtype,attackMultiplier;
 private final boolean acidic;
 OriginalGelatinousProfile(int subtype,int multiplier,boolean acid){dropSubtype=subtype;attackMultiplier=multiplier;acidic=acid;}
 public int dropSubtype(){return dropSubtype;}
 public int attackMultiplier(){return attackMultiplier;}
 public boolean acidic(){return acidic;}
 public int admittedSize(int requested){return this==OOZE?Math.min(requested,2):requested;}
 public int attackStrength(int size){return size*attackMultiplier;}
 public int experience(int size){return size*(attackMultiplier+(acidic?1:0));}
 public int jumpDelay(boolean hasTarget,java.util.function.IntUnaryOperator nextInt){return hasTarget?10:nextInt.applyAsInt(81)+40;}
}
