/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.electronwill.nightconfig.core.CommentedConfig
 *  com.electronwill.nightconfig.core.file.CommentedFileConfig
 *  com.electronwill.nightconfig.core.io.WritingMode
 *  net.minecraftforge.common.ForgeConfigSpec
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$Builder
 *  net.minecraftforge.common.ForgeConfigSpec$ConfigValue
 *  net.minecraftforge.common.ForgeConfigSpec$DoubleValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 *  net.minecraftforge.fml.loading.FMLPaths
 */
package com.deathregression.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.deathregression.DeathRegression;
import java.nio.file.Path;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SPEC;
    // ---- 死亡回归总开关（默认开启）----
    public static ForgeConfigSpec.BooleanValue DEATH_REGRESSION_ENABLED;
    public static ForgeConfigSpec.ConfigValue<String> SAVE_MODE;
    public static ForgeConfigSpec.IntValue DAYS_INTERVAL;
    public static ForgeConfigSpec.DoubleValue DAYS_PROBABILITY;
    public static ForgeConfigSpec.IntValue SAVE_COOLDOWN_MINUTES;
    public static ForgeConfigSpec.BooleanValue REQUIRE_FULL_HEALTH;
    public static ForgeConfigSpec.IntValue MAX_BACKUPS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_DIMENSIONS;
    public static ForgeConfigSpec.ConfigValue<String> MANUAL_SAVE_ITEM;
    public static ForgeConfigSpec.BooleanValue EASTER_EGG_ENABLED;
    // ---- 死亡后保留人物数据（总开关默认关闭，分项默认开启）----
    public static ForgeConfigSpec.BooleanValue KEEP_PLAYER_DATA;
    public static ForgeConfigSpec.BooleanValue KEEP_INVENTORY;
    public static ForgeConfigSpec.BooleanValue KEEP_ARMOR;
    public static ForgeConfigSpec.BooleanValue KEEP_ENDER_CHEST;
    public static ForgeConfigSpec.BooleanValue KEEP_XP;
    public static ForgeConfigSpec.BooleanValue KEEP_HUNGER;
    public static ForgeConfigSpec.BooleanValue KEEP_EFFECTS;
    public static ForgeConfigSpec.BooleanValue KEEP_ABSORPTION;
    public static ForgeConfigSpec.BooleanValue KEEP_AIR;
    public static ForgeConfigSpec.BooleanValue KEEP_SCORE;
    public static ForgeConfigSpec.BooleanValue KEEP_SPAWN_POINT;
    public static ForgeConfigSpec.BooleanValue KEEP_MOD_DATA;

    /**
     * 配置由 Forge 按「服务器型」管理，每个世界一份：
     * {@code saves/<世界>/serverconfig/deathregression-server.toml}。
     * 所以这里不再手动读写文件。
     */
    public static void load() {
        // 由 Forge 在服务器启动时加载，这里什么都不用做
    }

    public static void save() {
        try {
            SPEC.save();
            DeathRegression.LOGGER.info("\u914d\u7f6e\u5df2\u5199\u5165\u672c\u4e16\u754c\u7684 serverconfig");
        } catch (Exception e) {
            DeathRegression.LOGGER.warn("\u914d\u7f6e\u4fdd\u5b58\u5931\u8d25\uff1a{}", e.getMessage());
        }
    }

    public static boolean isValidSaveMode(String mode) {
        return "SLEEP".equalsIgnoreCase(mode) || "DAYS".equalsIgnoreCase(mode) || "MIXED".equalsIgnoreCase(mode);
    }

    /** 死亡回归总开关。读不到（旧配置文件里没这一项 / 还没加载）时按「开启」处理，跟默认值一致。 */
    public static boolean isDeathRegressionEnabled() {
        try {
            Boolean value = DEATH_REGRESSION_ENABLED.get();
            return value == null || value;
        } catch (Exception e) {
            return true;
        }
    }

    public static void setDeathRegression(boolean enabled) {
        DEATH_REGRESSION_ENABLED.set(enabled);
    }

    /** 死亡后是否保留人物数据（背包/经验等）。默认关闭；读不到也按关闭处理。 */
    public static boolean isKeepPlayerDataEnabled() {
        try {
            Boolean value = KEEP_PLAYER_DATA.get();
            return value != null && value;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setKeepPlayerData(boolean enabled) {
        KEEP_PLAYER_DATA.set(enabled);
    }

    /** 分项开关：config 里读不到时用 fallback（多数是 true，风险项是 false）。 */
    private static boolean sub(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            Boolean v = value.get();
            return v == null ? fallback : v;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean isKeepInventoryEnabled() { return sub(KEEP_INVENTORY, true); }
    public static boolean isKeepArmorEnabled() { return sub(KEEP_ARMOR, true); }
    public static boolean isKeepEnderChestEnabled() { return sub(KEEP_ENDER_CHEST, true); }
    public static boolean isKeepXpEnabled() { return sub(KEEP_XP, true); }
    public static boolean isKeepHungerEnabled() { return sub(KEEP_HUNGER, true); }
    public static boolean isKeepEffectsEnabled() { return sub(KEEP_EFFECTS, true); }
    public static boolean isKeepAbsorptionEnabled() { return sub(KEEP_ABSORPTION, true); }
    public static boolean isKeepAirEnabled() { return sub(KEEP_AIR, true); }
    public static boolean isKeepScoreEnabled() { return sub(KEEP_SCORE, true); }
    public static boolean isKeepSpawnPointEnabled() { return sub(KEEP_SPAWN_POINT, false); }
    public static boolean isKeepModDataEnabled() { return sub(KEEP_MOD_DATA, false); }

    static {
        BUILDER.push("general");
        DEATH_REGRESSION_ENABLED = BUILDER.comment(new String[]{
                "\u6b7b\u4ea1\u56de\u5f52\u603b\u5f00\u5173",
                "\u9ed8\u8ba4 true\uff1a\u6b7b\u4ea1\u65f6\u4e16\u754c\u56de\u6eaf\u5230\u5b58\u6863\u70b9",
                "\u8bbe\u4e3a false\uff1a\u6574\u4e2a\u56de\u6e52\u529f\u80fd\u505c\u7528\uff0c\u6b7b\u4ea1\u5c31\u662f\u666e\u901a\u6b7b\u4ea1\uff08\u4e0d\u56de\u6eaf\u3001\u4e0d\u65ad\u5f00\u3001\u4e0d\u4fdd\u7559\u6570\u636e\uff09",
                "\u5df2\u6709\u7684\u5907\u4efd\u4e0d\u4f1a\u88ab\u5220\uff0c\u91cd\u65b0\u5f00\u542f\u540e\u53ef\u4ee5\u7ee7\u7eed\u7528",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config deathRegression <true|false>"}).define("deathRegression", true);
        SAVE_MODE = BUILDER.comment(new String[]{"\u5b58\u6863\u89e6\u53d1\u6a21\u5f0f", "\u53ef\u9009\u503c\uff1a", "  SLEEP  - \u4ec5\u73a9\u5bb6\u5728\u5e8a\u4e0a\u7761\u89c9\u65f6\u89e6\u53d1\u5b58\u6863", "  DAYS   - \u4ec5\u6e38\u620f\u5929\u6570\u53d8\u5316\u65f6\u89e6\u53d1\u5b58\u6863", "  MIXED  - \u6df7\u5408\u6a21\u5f0f\uff0c\u7761\u89c9\u548c\u5929\u6570\u53d8\u5316\u5747\u53ef\u89e6\u53d1\u5b58\u6863\uff08\u63a8\u8350\uff09"}).define("saveMode", "MIXED");
        DAYS_INTERVAL = BUILDER.comment(new String[]{"\u5929\u6570\u5b58\u6863\u95f4\u9694\uff08\u6e38\u620f\u65e5\uff09", "\u6bcf\u7ecf\u8fc7\u6b64\u6570\u91cf\u7684\u6e38\u620f\u65e5\uff0c\u6709\u6982\u7387\u89e6\u53d1\u4e00\u6b21\u81ea\u52a8\u5b58\u6863", "\u9ed8\u8ba4 10 \u4e2a\u6e38\u620f\u65e5 \u2248 \u73b0\u5b9e\u65f6\u95f4 200 \u5206\u949f"}).defineInRange("daysInterval", 10, 1, 365);
        DAYS_PROBABILITY = BUILDER.comment(new String[]{"\u5929\u6570\u5b58\u6863\u89e6\u53d1\u6982\u7387\uff080.0 ~ 1.0\uff09", "\u8fbe\u5230\u5929\u6570\u95f4\u9694\u540e\uff0c\u4ee5\u6b64\u6982\u7387\u51b3\u5b9a\u662f\u5426\u89e6\u53d1\u81ea\u52a8\u5b58\u6863", "\u4f8b\u5982\uff1a0.5 \u8868\u793a 50% \u6982\u7387\u89e6\u53d1"}).defineInRange("daysProbability", 0.5, 0.0, 1.0);
        SAVE_COOLDOWN_MINUTES = BUILDER.comment(new String[]{"\u5b58\u6863\u51b7\u5374\u65f6\u95f4\uff08\u73b0\u5b9e\u5206\u949f\uff09", "\u4e24\u6b21\u81ea\u52a8\u5b58\u6863\u4e4b\u95f4\u81f3\u5c11\u95f4\u9694\u6b64\u65f6\u957f", "\u65e0\u8bba\u662f\u7761\u89c9\u89e6\u53d1\u8fd8\u662f\u5929\u6570\u89e6\u53d1\uff0c\u90fd\u5fc5\u987b\u9075\u5b88\u6b64\u51b7\u5374", "\u9ed8\u8ba4 100 \u5206\u949f\uff0c\u786e\u4fdd\u6b7b\u4ea1\u60e9\u7f5a\u6c89\u91cd"}).defineInRange("saveCooldownMinutes", 100, 1, 1440);
        REQUIRE_FULL_HEALTH = BUILDER.comment(new String[]{"\u662f\u5426\u8981\u6c42\u73a9\u5bb6\u6ee1\u8840\u624d\u80fd\u89e6\u53d1\u81ea\u52a8\u5b58\u6863", "\u8bbe\u4e3a true \u65f6\uff0c\u73a9\u5bb6\u5fc5\u987b\u5728\u6ee1\u751f\u547d\u503c\u72b6\u6001\u4e0b\u624d\u80fd\u5b58\u6863"}).define("requireFullHealth", true);
        MAX_BACKUPS = BUILDER.comment(new String[]{"\u6bcf\u4e2a\u7ef4\u5ea6\u6700\u5927\u5907\u4efd\u6570\u91cf", "\u8d85\u8fc7\u6b64\u6570\u91cf\u540e\uff0c\u6700\u65e7\u7684\u5907\u4efd\u4f1a\u88ab\u5faa\u73af\u8986\u76d6", "\u9ed8\u8ba4\u4fdd\u7559\u6700\u65b0\u7684 3 \u4e2a\u5907\u4efd"}).defineInRange("maxBackups", 3, 1, 20);
        BLACKLISTED_DIMENSIONS = BUILDER.comment(new String[]{"\u7ef4\u5ea6\u9ed1\u540d\u5355", "\u5728\u6b64\u5217\u8868\u4e2d\u7684\u7ef4\u5ea6 ID \u4e0d\u4f1a\u89e6\u53d1\u81ea\u52a8\u5b58\u6863\uff0c\u73a9\u5bb6\u6b7b\u4ea1\u65f6\u4e5f\u4e0d\u4f1a\u56de\u6eaf", "\u7559\u7a7a\u8868\u793a\u65e0\u9650\u5236\uff08\u6240\u6709\u7ef4\u5ea6\u5747\u5141\u8bb8\uff09", "\u793a\u4f8b\uff1a[\"minecraft:the_nether\"] \u8868\u793a\u5728\u4e0b\u754c\u7981\u7528\u5b58\u6863\u548c\u56de\u6eaf\u529f\u80fd"}).defineList("blacklistedDimensions", List.<String>of(), obj -> obj instanceof String);
        MANUAL_SAVE_ITEM = BUILDER.comment(new String[]{"\u624b\u52a8\u5b58\u6863\u7269\u54c1 ID", "\u624b\u6301\u8be5\u7269\u54c1\u53f3\u952e\u5373\u53ef\u89e6\u53d1\u4e00\u6b21\u5b58\u6863\uff08\u53d7\u51b7\u5374\u4e0e\u5b89\u5168\u6761\u4ef6\u9650\u5236\uff09", "\u7559\u7a7a\u8868\u793a\u4e0d\u542f\u7528\u7269\u54c1\u5b58\u6863", "\u793a\u4f8b\uff1aminecraft:nether_star", "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config manualSaveItem <\u7269\u54c1ID | none>"}).define("manualSaveItem", "");
        EASTER_EGG_ENABLED = BUILDER.comment(new String[]{"\u804a\u5929\u5f69\u86cb\uff1a\u8bf4\u51fa\u300c\u6b7b\u4ea1\u56de\u5f52\u300d\u5f53\u573a\u6b7b\u4ea1", "\u4e2d\u82f1\u65e5\u4e09\u79cd\u8bed\u8a00\u7684\u5b8c\u6574\u8bcd\u624d\u4f1a\u89e6\u53d1", "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config easterEgg <true|false>"}).define("easterEggEnabled", true);
        KEEP_PLAYER_DATA = BUILDER.comment(new String[]{
                "\u6b7b\u4ea1\u540e\u4fdd\u7559\u4eba\u7269\u6570\u636e",
                "\u9ed8\u8ba4 false\uff1a\u4eba\u548c\u80cc\u5305\u4e00\u8d77\u56de\u5230\u5b58\u6863\u70b9\uff08\u6b7b\u4ea1\u60e9\u7f5a\u6a21\u5f0f\uff09",
                "\u8bbe\u4e3a true\uff1a\u4eba\u56de\u5230\u5b58\u6863\u70b9\uff0c\u4f46\u80cc\u5305\u3001\u672b\u5f71\u7bb1\u3001\u7ecf\u9a8c\u3001\u9965\u997f\u3001",
                "              \u836f\u6c34\u6548\u679c\u4ee5\u53ca\u5404\u6a21\u7ec4\u5b58\u5728\u73a9\u5bb6\u8eab\u4e0a\u7684\u6570\u636e\u4fdd\u7559\u6b7b\u4ea1\u65f6\u7684\u6837\u5b50",
                "\u4e24\u79cd\u60c5\u51b5\u90fd\u4e00\u6837\uff1a\u4f4d\u7f6e\u56de\u5b58\u6863\u70b9\u3001\u8840\u91cf\u7528\u5b58\u6863\u70b9\u7684\u3001\u4e16\u754c\u65b9\u5757\u4e00\u8d77\u56de\u9000",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepPlayerData <true|false>"}).define("keepPlayerData", false);
        // ---- 分项开关（总开关打开后才生效，默认全开）----
        KEEP_INVENTORY = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u80cc\u5305\uff08\u5feb\u6377\u680f 9 \u683c + \u4e3b\u80cc\u5305 27 \u683c\uff0c\u5171 36 \u683c\uff09",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepInventory <true|false>"}).define("keepInventory", true);
        KEEP_ARMOR = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u88c5\u5907\uff08\u5934\u3001\u80f8\u3001\u817f\u3001\u9774 + \u526f\u624b\uff09",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepArmor <true|false>"}).define("keepArmor", true);
        KEEP_ENDER_CHEST = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u672b\u5f71\u7bb1\uff0827 \u683c\uff09",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepEnderChest <true|false>"}).define("keepEnderChest", true);
        KEEP_XP = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u7ecf\u9a8c\uff08\u7b49\u7ea7\u3001\u8fdb\u5ea6\u3001\u603b\u91cf\u3001\u9644\u9b54\u968f\u673a\u79cd\u5b50\uff09",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepXp <true|false>"}).define("keepXp", true);
        KEEP_HUNGER = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u9965\u997f\u5ea6\u3001\u9971\u548c\u5ea6\u3001\u6d88\u8017\u5ea6",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepHunger <true|false>"}).define("keepHunger", true);
        KEEP_EFFECTS = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u836f\u6c34\u6548\u679c\uff08\u5305\u62ec\u526f\u4f5c\u7528\uff09",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepEffects <true|false>"}).define("keepEffects", true);
        KEEP_ABSORPTION = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u4f24\u5bb3\u5438\u6536\uff08\u91d1\u82f9\u679c\u7684\u9ec4\u5fc3\uff09",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepAbsorption <true|false>"}).define("keepAbsorption", true);
        KEEP_AIR = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u6c27\u6c14\uff08\u6c34\u4e0b\u618b\u6c14\u5269\u4f59\uff09",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepAir <true|false>"}).define("keepAir", true);
        KEEP_SCORE = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u8ba1\u5206\u677f\u5206\u6570",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepScore <true|false>"}).define("keepScore", true);
        KEEP_SPAWN_POINT = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u91cd\u751f\u70b9\uff08\u5e8a\u3001\u5c65\u9b54\u5e8a\u3001\u5c01\u5370\u951a\uff09",
                "\u9ed8\u8ba4 false\uff1a\u91cd\u751f\u70b9\u8ddf\u4e16\u754c\u4e00\u8d77\u56de\u5230\u5b58\u6863\u70b9\u3002",
                "\u6ce8\u610f\uff1a\u5e8a\u672c\u8eab\u4e5f\u4f1a\u8ddf\u7740\u56de\u9000\uff0c\u5f00\u542f\u540e\u53ef\u80fd\u6307\u5411\u4e00\u5f20\u4e0d\u5b58\u5728\u7684\u5e8a",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepSpawnPoint <true|false>"}).define("keepSpawnPoint", false);
        KEEP_MOD_DATA = BUILDER.comment(new String[]{
                "\u4fdd\u7559\u6a21\u7ec4\u5b58\u5728\u73a9\u5bb6\u8eab\u4e0a\u7684\u6570\u636e\uff08ForgeCaps\uff09",
                "\u9970\u54c1\u680f\u3001\u6280\u80fd\u6811\u7b49\u6a21\u7ec4\u6570\u636e\u90fd\u5728\u8fd9\u4e00\u5757",
                "\u9ed8\u8ba4 false\uff1a\u6a21\u7ec4\u6570\u636e\u53ef\u80fd\u6307\u5411\u5df2\u7ecf\u88ab\u56de\u9000\u6389\u7684\u4e1c\u897f\uff0c\u5bb9\u6613\u51fa\u5947\u602a\u7684 bug",
                "\u6ca1\u88c5\u76f8\u5173\u6a21\u7ec4\u65f6\u81ea\u52a8\u8df3\u8fc7",
                "\u6e38\u620f\u5185\u4fee\u6539\uff1a/regression config keepModData <true|false>"}).define("keepModData", false);
        BUILDER.pop();


        SPEC = BUILDER.build();
    }
}

