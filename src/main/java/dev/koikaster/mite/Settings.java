package dev.koikaster.mite;
/** Fixed gameplay rules. No Forge config, file loader, setters or difficulty presets. */
public final class Settings {
 public static final int START_HEALTH=6;
 /** R195 avb new-world default is false and its professions button is hidden. */
 public static final boolean SOURCE_PROFESSIONS=false;
 /** ux.global_hunger_rate is fixed1.0 in the supplied R195; no setter changes it. */
 public static final double EXHAUSTION=1,CRAFT_TIME=1,GROWTH=.18,MENDING=.1;
 public static final boolean COLLAPSE=true,GRIEFING=true,MOONS=true,EMPTY_PAUSE=true,STRICT_HARVEST=true,CLASSIC_OCCLUSION=true;
 private Settings(){}
}
