package dev.koikaster.mite;

/** The server stores this choice in the world's primary data. */
public record WorldExtraSettings(boolean classicVisuals, boolean extras, boolean rebuiltBosses, boolean calamityBosses,
        boolean ite, boolean r196, boolean modernTextures, boolean modernMusic) {
    static final int LEGACY_MASK = 15;
    private static final int ITE_BIT = 16;
    static final int V2_MASK = LEGACY_MASK | ITE_BIT;
    private static final int R196_BIT = 32;
    static final int V3_MASK = V2_MASK | R196_BIT;
    private static final int MODERN_TEXTURES_BIT = 64;
    private static final int MODERN_MUSIC_BIT = 128;
    static final int SETTINGS_MASK = V3_MASK | MODERN_TEXTURES_BIT | MODERN_MUSIC_BIT;
    static final WorldExtraSettings DEFAULT = legacy(true);

    public WorldExtraSettings(boolean classicVisuals, boolean extras, boolean rebuiltBosses, boolean calamityBosses,
            boolean ite, boolean r196) {
        this(classicVisuals, extras, rebuiltBosses, calamityBosses, ite, r196, false, false);
    }

    static WorldExtraSettings legacy(boolean rebuilt) {
        return new WorldExtraSettings(true, rebuilt, rebuilt, rebuilt, false, false);
    }

    static WorldExtraSettings decode(int flags, WorldExtraSettings fallback) {
        if (flags < 0 || flags > SETTINGS_MASK) return fallback;
        return new WorldExtraSettings((flags & 8) == 0, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0,
            (flags & ITE_BIT) != 0, (flags & R196_BIT) != 0,
            (flags & MODERN_TEXTURES_BIT) != 0, (flags & MODERN_MUSIC_BIT) != 0);
    }

    static WorldExtraSettings decodeLegacy(int flags, WorldExtraSettings fallback) {
        return flags >= 0 && flags <= LEGACY_MASK ? decode(flags, fallback) : fallback;
    }

    static WorldExtraSettings decodeV2(int flags, WorldExtraSettings fallback) {
        return flags >= 0 && flags <= V2_MASK ? decode(flags, fallback) : fallback;
    }

    static WorldExtraSettings decodeV3(int flags, WorldExtraSettings fallback) {
        return flags >= 0 && flags <= V3_MASK ? decode(flags, fallback) : fallback;
    }

    int encode() {
        return (classicVisuals ? 0 : 8) | (extras ? 1 : 0) | (rebuiltBosses ? 2 : 0) | (calamityBosses ? 4 : 0)
            | (ite ? ITE_BIT : 0) | (r196 ? R196_BIT : 0)
            | (modernTextures ? MODERN_TEXTURES_BIT : 0) | (modernMusic ? MODERN_MUSIC_BIT : 0);
    }

    boolean rebuiltBossesEnabled() {
        return extras && rebuiltBosses;
    }

    boolean calamityBossesEnabled() {
        return extras && calamityBosses;
    }

    boolean modernTexturesEnabled() {
        return !classicVisuals && modernTextures;
    }

    boolean modernMusicEnabled() {
        return !classicVisuals && modernMusic;
    }

    WorldExtraSettings withVisuals(boolean classic) {
        return new WorldExtraSettings(classic, extras, rebuiltBosses, calamityBosses, ite, r196,
            !classic && modernTexturesEnabled(), !classic && modernMusicEnabled());
    }

    WorldExtraSettings withModernTextures(boolean enabled) {
        return new WorldExtraSettings(classicVisuals, extras, rebuiltBosses, calamityBosses, ite, r196,
            enabled && !classicVisuals, modernMusic);
    }

    WorldExtraSettings withModernMusic(boolean enabled) {
        return new WorldExtraSettings(classicVisuals, extras, rebuiltBosses, calamityBosses, ite, r196,
            modernTextures, enabled && !classicVisuals);
    }

    WorldExtraSettings withExtras(boolean enabled) {
        return new WorldExtraSettings(classicVisuals, enabled, rebuiltBosses, calamityBosses, ite, r196, modernTextures, modernMusic);
    }

    WorldExtraSettings withRebuiltBosses(boolean enabled) {
        return new WorldExtraSettings(classicVisuals, extras, enabled, calamityBosses, ite, r196, modernTextures, modernMusic);
    }

    WorldExtraSettings withCalamityBosses(boolean enabled) {
        return new WorldExtraSettings(classicVisuals, extras, rebuiltBosses, enabled, ite, r196, modernTextures, modernMusic);
    }

    WorldExtraSettings withIte(boolean enabled) {
        return new WorldExtraSettings(classicVisuals, extras, rebuiltBosses, calamityBosses, enabled, r196, modernTextures, modernMusic);
    }

    WorldExtraSettings withR196(boolean enabled) {
        return new WorldExtraSettings(classicVisuals, extras, rebuiltBosses, calamityBosses, ite, enabled, modernTextures, modernMusic);
    }
}
