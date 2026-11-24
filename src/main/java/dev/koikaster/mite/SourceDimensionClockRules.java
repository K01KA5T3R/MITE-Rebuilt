package dev.koikaster.mite;

/** js.shouldTimeProgress/shouldRandomBlockTicksBePerformed and the human90B
 * empty-server override. Native time storage and transport are handled by SourceDimensionClock. */
public final class SourceDimensionClockRules {
 public interface Host {
  boolean dedicated();
  boolean players(int dimension);
  boolean anyConnected();
  boolean onlyGhosts();
  boolean nonGhostConnected();
  boolean underworldVisited();
  boolean netherVisited();
 }
 public static boolean sourceProgress(Host host,int dimension){
  boolean regional=host.players(-1)||host.players(0)||host.players(-2);
  if(host.dedicated()){
   if(dimension==-2)return host.underworldVisited()&&regional;
   if(dimension==-1)return host.netherVisited()&&regional;
   if(dimension==1)return host.players(1);
   return true;
  }
  if(host.onlyGhosts())return false;
  if(dimension==-2)return host.underworldVisited()&&regional;
  if(dimension==-1)return host.netherVisited()&&regional;
  if(dimension==1)return host.players(1);
  return !host.anyConnected()||regional;
 }
 public static boolean sourceRandomTicks(Host host,int dimension){return sourceProgress(host,dimension)&&host.nonGhostConnected();}
 /** User90B takes precedence over R195's dedicated Overworld no-player branch. */
 public static boolean progress(Host host,int dimension){return host.anyConnected()&&sourceProgress(host,dimension);}
 public static boolean randomTicks(Host host,int dimension){return progress(host,dimension)&&host.nonGhostConnected();}
 public static int index(int dimension){return switch(dimension){case 0->0;case -1->1;case 1->2;case -2->3;default->throw new IllegalArgumentException("Unmapped source dimension: "+dimension);};}
 private SourceDimensionClockRules(){}
}
