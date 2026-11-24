package dev.koikaster.mite;

import java.util.function.IntUnaryOperator;

/** R195 aoo: one mutually exclusive result, with Fortune capped at three. */
public final class LegacyGravel {
 public enum Drop {GRAVEL,FLINT_SHARD,FLINT,COPPER,SILVER,GOLD,OBSIDIAN,EMERALD,DIAMOND,MITHRIL,ADAMANTIUM,QUARTZ}
 private static final Drop[] MINERALS={Drop.COPPER,Drop.SILVER,Drop.GOLD,Drop.OBSIDIAN,Drop.EMERALD,Drop.DIAMOND,Drop.MITHRIL,Drop.ADAMANTIUM};
 public static Drop roll(int fortune,boolean nether,IntUnaryOperator random){
  if(random.applyAsInt(12-Math.min(3,Math.max(0,fortune))*2)>2)return Drop.GRAVEL;
  if(random.applyAsInt(3)>0)return random.applyAsInt(16)==0?Drop.FLINT:Drop.FLINT_SHARD;
  int i=0;while(i<MINERALS.length-1&&random.applyAsInt(3)==0)i++;
  var drop=MINERALS[i];
  if(!nether)return drop;
  return switch(drop){case COPPER,SILVER,MITHRIL,ADAMANTIUM->Drop.GOLD;case OBSIDIAN,EMERALD,DIAMOND->Drop.QUARTZ;default->drop;};
 }
 private LegacyGravel(){}
}
