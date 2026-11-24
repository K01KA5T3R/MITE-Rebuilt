package dev.koikaster.mite;
/** qm/rh specialization for rx. No zombie rain branch is applicable to Ocelot. */
public final class OriginalOcelotWanderDecision {
 public interface Host {
  long lastCactusTick();long totalTime();boolean navigationDone();int nextInt(int bound);
  int x();int y();int z();boolean hasHome();double homeDistanceSquared();float homeRadius();boolean withinHome(int x,int y,int z);float weight(int x,int y,int z);
 }
 public record Point(int x,int y,int z){}
 public static Point select(Host host){
  long last=host.lastCactusTick(),elapsed=last<1?-1:host.totalTime()-last;boolean cactus=elapsed>=0&&elapsed<10&&host.navigationDone();
  if(!cactus&&host.nextInt(120)!=0)return null;
  double extent=host.homeRadius()+10;boolean restricted=host.hasHome()&&host.homeDistanceSquared()+4<extent*extent;float best=-100;Point selected=null;
  for(int i=0;i<10;i++){int x=host.nextInt(21)-10,y=host.nextInt(15)-7,z=host.nextInt(21)-10;if(restricted){x+=host.x();y+=host.y();z+=host.z();if(!host.withinHome(x,y,z))continue;}float weight=host.weight(x,y,z);if(weight>best){best=weight;selected=new Point(x,y,z);}}
  return selected;
 }
 private OriginalOcelotWanderDecision(){}
}
