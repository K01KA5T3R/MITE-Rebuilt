package dev.koikaster.mite;
import java.util.List;

/** Human V063's repeated banner encounter, independent of villages and native
 * raid waves. Fixed limits use ordinary loaded-world/player/mob facts. */
public final class BloodBannerRules {
 public static final int PERIOD=100,LOCAL_LIMIT=12,PLAYER_LIMIT=24,WORLD_LIMIT=96;
 public enum Kind {PILLAGER,VINDICATOR,EVOKER,RAVAGER}
 public record Anchor(int x,int y,int z){}
 public static final class State {public long lastPulse=Long.MIN_VALUE;public int cursor;}
 public interface Host {
  long time();boolean allowed();List<Anchor> anchors();boolean loaded(Anchor a);boolean ominous(Anchor a);void forget(Anchor a);
  boolean playerNear(Anchor a);int nearby(Anchor a);int globalCount();int players();int random(int bound);boolean spawn(Anchor a,Kind kind);
 }
 public static Kind select(int draw){if(draw<0||draw>=14)throw new IllegalArgumentException("Banner draw outside14");return draw<8?Kind.PILLAGER:draw<12?Kind.VINDICATOR:draw==12?Kind.EVOKER:Kind.RAVAGER;}
 public static int pulse(State state,Host host){
  long time=host.time();if(Math.floorMod(time,PERIOD)!=0||state.lastPulse==time)return 0;state.lastPulse=time;
  var sites=host.anchors();if(sites.isEmpty())return 0;int start=Math.floorMod(state.cursor,sites.size());state.cursor=(start+1)%sites.size();
  // Loaded stale entries are pruned even outside a blood night. Unloaded sites
  // remain saved, and never acquire chunk tickets merely to inspect a banner.
  boolean enabled=host.allowed();int count=enabled?host.globalCount():0,cap=enabled?Math.min(WORLD_LIMIT,Math.min(4,Math.max(0,host.players()))*PLAYER_LIMIT):0,total=0;
  for(int i=0;i<sites.size();i++){var anchor=sites.get((start+i)%sites.size());if(!host.loaded(anchor))continue;if(!host.ominous(anchor)){host.forget(anchor);continue;}if(!enabled||count>=cap||!host.playerNear(anchor)||host.nearby(anchor)>=LOCAL_LIMIT)continue;if(host.spawn(anchor,select(host.random(14)))){count++;total++;}}
  return total;
 }
 private BloodBannerRules(){}
}
