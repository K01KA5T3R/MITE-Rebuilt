package dev.koikaster.mite;
/** alf.b(atc): source avoid-task path success compares X/Z only and uses Java
 * truncation. Y equality is deliberately absent in the original method. */
public final class SourceVillagerPathRules {
 public static boolean endFits(boolean present,int endX,int endZ,double x,double z){return present&&endX==(int)x&&endZ==(int)z;}
 private SourceVillagerPathRules(){}
}
