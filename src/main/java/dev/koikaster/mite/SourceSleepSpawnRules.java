package dev.koikaster.mite;

/** The complete no-light parent chain used by R195 sleep candidates. Geometry,
 * liquids and path weights stay lazy to preserve the source's short circuits.
 * Selection/depth and spawn-surface checks belong to js/aci before this call. */
public final class SourceSleepSpawnRules {
 public enum Family {MONSTER,EARTH,CLAY,HELLHOUND,SILVERFISH,PIGMAN}
 public record Profile(Family family,boolean leaves,boolean shallowWater){}
 public interface Host {
  int difficulty();boolean validEarthSupport();boolean validClaySupport();
  boolean unobstructedByEntities();boolean noCollidingBoxes();boolean leavesBelow();
  boolean abovePresent();boolean waterAbove();boolean waterAtFeet();boolean solidAbove();boolean cactusAbove();
  boolean lavaInBody();boolean anyLiquidInBody();float pathWeight();boolean livingPlayerWithinFive();
 }
 public static boolean accept(Profile profile,Host world){
  if(profile.family==Family.EARTH&&!world.validEarthSupport()||profile.family==Family.CLAY&&!world.validClaySupport())return false;
  // tn bypasses on/og's leaf/path checks. rp's Hellhound branch has no tm
  // difficulty gate; do not silently introduce one from modern Monster rules.
  if(profile.family!=Family.HELLHOUND&&world.difficulty()<=0)return false;
  if(!world.unobstructedByEntities()||!world.noCollidingBoxes())return false;
  if(profile.family==Family.PIGMAN)return !world.anyLiquidInBody();
  if(!profile.leaves&&world.leavesBelow())return false;
  if(profile.shallowWater){
   if(world.abovePresent()){
    if(world.waterAbove()||world.waterAtFeet()&&world.solidAbove())return false;
   }
   if(world.cactusAbove()||world.lavaInBody())return false;
  }else if(world.anyLiquidInBody())return false;
  // on calls this only after the entire og body check. NaN is rejected by
  // the literal >= comparison, rather than an inverted < comparison.
  if(!(world.pathWeight()>=0))return false;
  return profile.family!=Family.SILVERFISH||!world.livingPlayerWithinFive();
 }
 private SourceSleepSpawnRules(){}
}
