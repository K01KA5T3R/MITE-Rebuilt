package dev.koikaster.mite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Official ITE 2.0.30 Configs model with upstream ConfigEntry read/reset semantics and client snapshot sync. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class IteConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String SYNC_TAG = "MiteIteConfigSnapshot";

    public record Snapshot(
        int diamondExp,
        int emeraldExp,
        int quartzExp,
        int lapisExp,
        int quartzMaxExpLevel,
        int goldenAppleEatTime,
        int enchantedGoldenAppleEatTime,
        boolean firstDayLongerDayTime,
        int fishingXpScale,
        int inRainDebuffTime,
        double inWallDamageForPlayer,
        double steppedMobDamageBase,
        int steppedMobDamageIncreaseDay,
        double steppedMobDamageMax,
        int ironToMithrilCookTime,
        int mithrilToAdamantiumCookTime,
        int enhanceLimit,
        boolean mobSpawnerSpeedUpWithPlayers,
        boolean overworldAdamantiteOre
    ) {
        public static final Snapshot DEFAULTS = new Snapshot(
            400, 300, 35, 40, 55,
            20, 10,
            true, 10, 3600, 10.0,
            1.0, 24, 4.0,
            6000, 9000, 512, true, false
        );

        public CompoundTag toTag() {
            var tag = new CompoundTag();
            tag.putInt("diamondExp", diamondExp);
            tag.putInt("emeraldExp", emeraldExp);
            tag.putInt("quartzExp", quartzExp);
            tag.putInt("lapisExp", lapisExp);
            tag.putInt("quartzMaxExpLevel", quartzMaxExpLevel);
            tag.putInt("goldenAppleEatTime", goldenAppleEatTime);
            tag.putInt("enchantedGoldenAppleEatTime", enchantedGoldenAppleEatTime);
            tag.putBoolean("firstDayLongerDayTime", firstDayLongerDayTime);
            tag.putInt("fishingXpScale", fishingXpScale);
            tag.putInt("inRainDebuffTime", inRainDebuffTime);
            tag.putDouble("inWallDamageForPlayer", inWallDamageForPlayer);
            tag.putDouble("steppedMobDamageBase", steppedMobDamageBase);
            tag.putInt("steppedMobDamageIncreaseDay", steppedMobDamageIncreaseDay);
            tag.putDouble("steppedMobDamageMax", steppedMobDamageMax);
            tag.putInt("ironToMithrilCookTime", ironToMithrilCookTime);
            tag.putInt("mithrilToAdamantiumCookTime", mithrilToAdamantiumCookTime);
            tag.putInt("enhanceLimit", enhanceLimit);
            tag.putBoolean("mobSpawnerSpeedUpWithPlayers", mobSpawnerSpeedUpWithPlayers);
            tag.putBoolean("overworldAdamantiteOre", overworldAdamantiteOre);
            return tag;
        }

        public static Snapshot fromTag(CompoundTag tag) {
            var d = DEFAULTS;
            return new Snapshot(
                tag.contains("diamondExp") ? tag.getInt("diamondExp") : d.diamondExp,
                tag.contains("emeraldExp") ? tag.getInt("emeraldExp") : d.emeraldExp,
                tag.contains("quartzExp") ? tag.getInt("quartzExp") : d.quartzExp,
                tag.contains("lapisExp") ? tag.getInt("lapisExp") : d.lapisExp,
                tag.contains("quartzMaxExpLevel") ? tag.getInt("quartzMaxExpLevel") : d.quartzMaxExpLevel,
                tag.contains("goldenAppleEatTime") ? tag.getInt("goldenAppleEatTime") : d.goldenAppleEatTime,
                tag.contains("enchantedGoldenAppleEatTime") ? tag.getInt("enchantedGoldenAppleEatTime") : d.enchantedGoldenAppleEatTime,
                tag.contains("firstDayLongerDayTime") ? tag.getBoolean("firstDayLongerDayTime") : d.firstDayLongerDayTime,
                tag.contains("fishingXpScale") ? tag.getInt("fishingXpScale") : d.fishingXpScale,
                tag.contains("inRainDebuffTime") ? tag.getInt("inRainDebuffTime") : d.inRainDebuffTime,
                tag.contains("inWallDamageForPlayer") ? tag.getDouble("inWallDamageForPlayer") : d.inWallDamageForPlayer,
                tag.contains("steppedMobDamageBase") ? tag.getDouble("steppedMobDamageBase") : d.steppedMobDamageBase,
                tag.contains("steppedMobDamageIncreaseDay") ? tag.getInt("steppedMobDamageIncreaseDay") : d.steppedMobDamageIncreaseDay,
                tag.contains("steppedMobDamageMax") ? tag.getDouble("steppedMobDamageMax") : d.steppedMobDamageMax,
                tag.contains("ironToMithrilCookTime") ? tag.getInt("ironToMithrilCookTime") : d.ironToMithrilCookTime,
                tag.contains("mithrilToAdamantiumCookTime") ? tag.getInt("mithrilToAdamantiumCookTime") : d.mithrilToAdamantiumCookTime,
                tag.contains("enhanceLimit") ? tag.getInt("enhanceLimit") : d.enhanceLimit,
                tag.contains("mobSpawnerSpeedUpWithPlayers") ? tag.getBoolean("mobSpawnerSpeedUpWithPlayers") : d.mobSpawnerSpeedUpWithPlayers,
                tag.contains("overworldAdamantiteOre") ? tag.getBoolean("overworldAdamantiteOre") : d.overworldAdamantiteOre
            );
        }
    }

    private static volatile Snapshot serverSnapshot = Snapshot.DEFAULTS;
    private static volatile Snapshot clientRemote;

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        serverSnapshot = loadForDirectory(event.getServer().getServerDirectory().toPath());
        ClassicQuality.setDurabilityScalingSuppressed(
            WorldContentMode.settings(event.getServer().getWorldData()).ite());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        serverSnapshot = Snapshot.DEFAULTS;
        ClassicQuality.setDurabilityScalingSuppressed(false);
    }

    public static Snapshot loadForDirectory(Path baseDir) {
        Path preferred = baseDir.resolve("config/mite-ite-cfg.json");
        Path legacy = baseDir.resolve("mite-ite-cfg.json");
        Path target = Files.isRegularFile(preferred) ? preferred : Files.isRegularFile(legacy) ? legacy : preferred;
        try {
            JsonObject root;
            if (Files.isRegularFile(target)) {
                var parsed = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8));
                root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            } else {
                root = new JsonObject();
            }
            var loaded = parseAndNormalize(root);
            Files.createDirectories(target.getParent());
            Files.writeString(target, GSON.toJson(loaded.normalized()) + "\n", StandardCharsets.UTF_8);
            return loaded.snapshot();
        } catch (Exception e) {
            LOGGER.warn("Failed to read {}; resetting to ITE 2.0.30 defaults", target, e);
            return Snapshot.DEFAULTS;
        }
    }

    public record ParseOutcome(Snapshot snapshot, JsonObject normalized) {}

    /** Implements ConfigEntry#read: accepts {_comment, value} or raw scalar; missing _comment/null/invalid resets to default. */
    public static ParseOutcome parseAndNormalize(JsonObject root) {
        var d = Snapshot.DEFAULTS;
        JsonObject out = new JsonObject();
        out.addProperty("config_version", 1);

        JsonObject gm = obj(root, "GameMechanics");
        JsonObject outGm = new JsonObject();
        double inWall = readDouble(gm, "in_wall_damage_for_player", "玩家墙内窒息伤害(小数)", d.inWallDamageForPlayer, outGm);
        double stepBase = readDouble(gm, "stepped_mob_damage_factor", "渐进伤害每次增幅点数基础值(小数)", d.steppedMobDamageBase, outGm);
        double stepMax = readDouble(gm, "stepped_mob_damage_progress_max", "渐进伤害增幅最大值(小数)", d.steppedMobDamageMax, outGm);
        int stepDay = readInt(gm, "stepped_mob_damage_progress_increase_day", "渐进伤害每次增幅每增加一点所需天数(整数)", d.steppedMobDamageIncreaseDay, outGm);
        int fishXp = readInt(gm, "fishing_xp", "钓鱼经验(整数)", d.fishingXpScale, outGm);
        int rainTime = readInt(gm, "in_rain_debuff_time", "雨中获得负面效果所需时长(整数)", d.inRainDebuffTime, outGm);
        boolean firstDay = readBool(gm, "first_day_longer_day_time", "加长第一天时间(开关)", d.firstDayLongerDayTime, outGm);
        out.add("GameMechanics", outGm);

        JsonObject item = obj(root, "Item");
        JsonObject outItem = new JsonObject();
        int diamondExp = readInt(item, "diamond_exp", "钻石经验", d.diamondExp, outItem);
        int emeraldExp = readInt(item, "emerald_exp", "绿宝石经验", d.emeraldExp, outItem);
        int quartzExp = readInt(item, "quartz_exp", "石英经验", d.quartzExp, outItem);
        int lapisExp = readInt(item, "lapis_exp", "青金石经验", d.lapisExp, outItem);
        int quartzCap = readInt(item, "quartz_max_exp_level", "石英可提供经验最大等级", d.quartzMaxExpLevel, outItem);
        int appleTime = readInt(item, "golden_apple_eat_time", "金苹果食用时间(刻)", d.goldenAppleEatTime, outItem);
        int encAppleTime = readInt(item, "enchanted_golden_apple_eat_time", "附魔金苹果食用时间(刻)", d.enchantedGoldenAppleEatTime, outItem);

        JsonObject recipes = obj(item, "Recipes");
        JsonObject outRecipes = new JsonObject();
        int ironMithrilTime = readInt(recipes, "iron_to_mithril_cook_time", "铁块烧制秘银时间", d.ironToMithrilCookTime, outRecipes);
        int mithrilAdamTime = readInt(recipes, "mithril_to_adamantium_cook_time", "秘银块烧制艾德曼时间", d.mithrilToAdamantiumCookTime, outRecipes);
        outItem.add("Recipes", outRecipes);
        out.add("Item", outItem);

        JsonObject block = obj(root, "Block");
        JsonObject outBlock = new JsonObject();
        boolean spawnerSpeedup = readBool(block, "mob_spawner_speed_up_with_players", "刷怪笼在旁边有玩家时加速生怪", d.mobSpawnerSpeedUpWithPlayers, outBlock);
        out.add("Block", outBlock);

        JsonObject entities = obj(root, "Entities");
        JsonObject outEntities = new JsonObject();
        int enhanceLimit = readInt(entities, "enhance_limit", "生物基础属性/装备增强天数上限", d.enhanceLimit, outEntities);
        out.add("Entities", outEntities);

        JsonObject worldGen = obj(root, "WorldGen");
        JsonObject overworld = obj(worldGen, "Overworld");
        JsonObject outWorldGen = new JsonObject();
        JsonObject outOverworld = new JsonObject();
        boolean overworldAdam = readBool(overworld, "overworld_adamantite_ore", "主世界生成艾德曼", d.overworldAdamantiteOre, outOverworld);
        outWorldGen.add("Overworld", outOverworld);
        out.add("WorldGen", outWorldGen);

        var snap = new Snapshot(
            diamondExp, emeraldExp, quartzExp, lapisExp, quartzCap,
            appleTime, encAppleTime,
            firstDay, fishXp, rainTime, inWall,
            stepBase, stepDay, stepMax,
            ironMithrilTime, mithrilAdamTime,
            enhanceLimit, spawnerSpeedup, overworldAdam
        );
        return new ParseOutcome(snap, out);
    }

    private static JsonObject obj(JsonObject parent, String key) {
        if (parent == null || !parent.has(key)) return null;
        JsonElement el = parent.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private static int readInt(JsonObject parent, String key, String comment, int def, JsonObject out) {
        int val = def;
        JsonElement el = parent != null ? parent.get(key) : null;
        if (el != null && !el.isJsonNull()) {
            try {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                    val = el.getAsInt();
                } else if (el.isJsonObject()) {
                    JsonObject entry = el.getAsJsonObject();
                    if (!entry.has("_comment") || !entry.get("_comment").isJsonPrimitive() || !entry.has("value") || !entry.get("value").isJsonPrimitive() || !entry.get("value").getAsJsonPrimitive().isNumber()) {
                        throw new IllegalArgumentException("Missing _comment or numeric value for " + key);
                    }
                    val = entry.get("value").getAsInt();
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("Resetting invalid ITE config key {} to default {}", key, def);
                val = def;
            }
        }
        out.add(key, commented(comment, val));
        return val;
    }

    private static double readDouble(JsonObject parent, String key, String comment, double def, JsonObject out) {
        double val = def;
        JsonElement el = parent != null ? parent.get(key) : null;
        if (el != null && !el.isJsonNull()) {
            try {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                    val = el.getAsDouble();
                } else if (el.isJsonObject()) {
                    JsonObject entry = el.getAsJsonObject();
                    if (!entry.has("_comment") || !entry.get("_comment").isJsonPrimitive() || !entry.has("value") || !entry.get("value").isJsonPrimitive() || !entry.get("value").getAsJsonPrimitive().isNumber()) {
                        throw new IllegalArgumentException("Missing _comment or numeric value for " + key);
                    }
                    val = entry.get("value").getAsDouble();
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("Resetting invalid ITE config key {} to default {}", key, def);
                val = def;
            }
        }
        out.add(key, commented(comment, val));
        return val;
    }

    private static boolean readBool(JsonObject parent, String key, String comment, boolean def, JsonObject out) {
        boolean val = def;
        JsonElement el = parent != null ? parent.get(key) : null;
        if (el != null && !el.isJsonNull()) {
            try {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
                    val = el.getAsBoolean();
                } else if (el.isJsonObject()) {
                    JsonObject entry = el.getAsJsonObject();
                    if (!entry.has("_comment") || !entry.get("_comment").isJsonPrimitive() || !entry.has("value") || !entry.get("value").isJsonPrimitive() || !entry.get("value").getAsJsonPrimitive().isBoolean()) {
                        throw new IllegalArgumentException("Missing _comment or boolean value for " + key);
                    }
                    val = entry.get("value").getAsBoolean();
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("Resetting invalid ITE config key {} to default {}", key, def);
                val = def;
            }
        }
        out.add(key, commented(comment, val));
        return val;
    }

    private static JsonObject commented(String comment, Object value) {
        var o = new JsonObject();
        o.addProperty("_comment", comment);
        if (value instanceof Boolean b) o.addProperty("value", b);
        else if (value instanceof Integer i) o.addProperty("value", i);
        else if (value instanceof Number n) o.addProperty("value", n);
        return o;
    }

    public static void writeSync(CompoundTag data) {
        data.put(SYNC_TAG, serverSnapshot.toTag());
    }

    public static void readSync(CompoundTag data) {
        if (data.contains(SYNC_TAG, CompoundTag.TAG_COMPOUND)) {
            clientRemote = Snapshot.fromTag(data.getCompound(SYNC_TAG));
        }
    }

    public static void clearClient() {
        clientRemote = null;
    }

    public static Snapshot effective(Level level) {
        if (level != null && level.isClientSide) {
            Snapshot synced = clientRemote;
            return synced != null ? synced : Snapshot.DEFAULTS;
        }
        return serverSnapshot;
    }

    private IteConfig() {}
}
