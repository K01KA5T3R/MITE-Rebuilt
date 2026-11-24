package dev.koikaster.mite;
/** aqw.a permits End-portal construction outside dimension0. It is unrelated
 * to falling blocks. Scope is per construction thread and cannot leak on error. */
public final class SourcePortalPlacementRules {
 private static final ThreadLocal<Integer> DEPTH=new ThreadLocal<>();
 public static boolean mayExist(boolean constructing,boolean overworld){return constructing||overworld;}
 public static boolean constructing(){var count=DEPTH.get();return count!=null&&count>0;}
 public static Scope allow(){Integer depth=DEPTH.get();DEPTH.set(depth==null?1:depth+1);return new Scope();}
 public static final class Scope implements AutoCloseable {
  private boolean closed;
  private Scope(){}
  @Override public void close(){if(closed)return;Integer count=DEPTH.get();if(count==null||count<=0)throw new IllegalStateException("Unbalanced portal construction scope");if(count==1)DEPTH.remove();else DEPTH.set(count-1);closed=true;}
 }
 private SourcePortalPlacementRules(){}
}
