package com.deathregression.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.deathregression.backup.BackupManager;
import com.deathregression.DeathRegression;
import com.deathregression.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** /regression config <项> <值>：全部配置项都能在游戏里改，改完自动写回 toml。 */
public final class RegressionConfigCommand {

    private RegressionConfigCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("regression")
                .then(Commands.literal("config")
                        .requires(s -> s.hasPermission(2))
                        // 查看全部
                        .executes(RegressionConfigCommand::showAll)
                        // 布尔项
                        .then(Commands.literal("deathRegression")
                                .executes(RegressionConfigCommand::showDeathRegression)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> setBool(c, "deathRegression", BoolArgumentType.getBool(c, "value")))))
                        .then(Commands.literal("requireFullHealth")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> setBool(c, "requireFullHealth", BoolArgumentType.getBool(c, "value")))))
                        .then(Commands.literal("easterEgg")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> setBool(c, "easterEgg", BoolArgumentType.getBool(c, "value")))))
                        .then(Commands.literal("keepPlayerData")
                                .executes(RegressionConfigCommand::showKeepPlayerData)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> setBool(c, "keepPlayerData", BoolArgumentType.getBool(c, "value")))))
                        // ---- 人物数据分项 ----
                        .then(keepToggle("keepInventory"))
                        .then(keepToggle("keepArmor"))
                        .then(keepToggle("keepEnderChest"))
                        .then(keepToggle("keepXp"))
                        .then(keepToggle("keepHunger"))
                        .then(keepToggle("keepEffects"))
                        .then(keepToggle("keepAbsorption"))
                        .then(keepToggle("keepAir"))
                        .then(keepToggle("keepScore"))
                        .then(keepToggle("keepSpawnPoint"))
                        .then(keepToggle("keepModData"))
                        // 人物数据菜单
                        .then(Commands.literal("playerdata")
                                .executes(RegressionConfigCommand::showPlayerDataMenu))
                        // 存档模式
                        .then(Commands.literal("saveMode")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .executes(c -> setSaveMode(c, StringArgumentType.getString(c, "value")))))
                        // 整数项
                        .then(Commands.literal("saveCooldownMinutes")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 1440))
                                        .executes(c -> setInt(c, "saveCooldownMinutes", IntegerArgumentType.getInteger(c, "value")))))
                        .then(Commands.literal("daysInterval")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 365))
                                        .executes(c -> setInt(c, "daysInterval", IntegerArgumentType.getInteger(c, "value")))))
                        .then(Commands.literal("maxBackups")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 20))
                                        .executes(c -> setInt(c, "maxBackups", IntegerArgumentType.getInteger(c, "value")))))
                        // 小数项
                        .then(Commands.literal("daysProbability")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .executes(c -> setDouble(c, "daysProbability", DoubleArgumentType.getDouble(c, "value")))))
                        // 列表项
                        .then(Commands.literal("blacklistedDimensions")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("dim", StringArgumentType.string())
                                                .executes(c -> addDimension(c, StringArgumentType.getString(c, "dim")))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("dim", StringArgumentType.string())
                                                .executes(c -> removeDimension(c, StringArgumentType.getString(c, "dim")))))
                                .then(Commands.literal("clear")
                                        .executes(RegressionConfigCommand::clearDimensions)))
                        // 手动存档物品（空 = 关闭）
                        .then(Commands.literal("manualSaveItem")
                                .then(Commands.argument("itemId", StringArgumentType.string())
                                        .executes(c -> setManualItem(c, StringArgumentType.getString(c, "itemId")))))
                        // 备份相关
                        .then(Commands.literal("backups")
                                .executes(RegressionConfigCommand::listBackups))));
    }

    // ---------------------------------------------------------------- 查看

    /**
     * 一条消息一个组件。之前把 {@code \n} 塞在一个组件里，聊天栏经常只显示第一行，
     * 看不到"当前：开启/关闭"。
     */
    private static void sendLines(CommandContext<CommandSourceStack> ctx, ChatFormatting color, java.util.List<String> lines) {
        for (String line : lines) {
            ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(color), false);
        }
    }

    private static String onOff(boolean value) {
        return value ? "开启" : "关闭";
    }

    private static int showAll(CommandContext<CommandSourceStack> ctx) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("===== DeathRegression 配置（本世界）=====");
        lines.add("死亡回归总开关 = " + onOff(ModConfig.isDeathRegressionEnabled())
                + "    /regression config deathRegression true|false");
        lines.add("保留人物数据   = " + onOff(ModConfig.isKeepPlayerDataEnabled())
                + "    /regression config keepPlayerData true|false");
        lines.add("saveMode            = " + ModConfig.SAVE_MODE.get()
                + "    /regression config saveMode SLEEP|DAYS|MIXED");
        lines.add("saveCooldownMinutes = " + ModConfig.SAVE_COOLDOWN_MINUTES.get()
                + "    /regression config saveCooldownMinutes 1~1440");
        lines.add("daysInterval        = " + ModConfig.DAYS_INTERVAL.get()
                + "    /regression config daysInterval 1~365");
        lines.add("daysProbability     = " + ModConfig.DAYS_PROBABILITY.get()
                + "    /regression config daysProbability 0.0~1.0");
        lines.add("requireFullHealth   = " + ModConfig.REQUIRE_FULL_HEALTH.get()
                + "    /regression config requireFullHealth true|false");
        lines.add("maxBackups          = " + ModConfig.MAX_BACKUPS.get()
                + "    /regression config maxBackups 1~20");
        lines.add("easterEggEnabled    = " + ModConfig.EASTER_EGG_ENABLED.get()
                + "    /regression config easterEgg true|false");
        lines.add("blacklistedDims     = " + ModConfig.BLACKLISTED_DIMENSIONS.get()
                + "    /regression config blacklistedDimensions add|remove|clear");
        lines.add("manualSaveItem      = " + (String.valueOf(ModConfig.MANUAL_SAVE_ITEM.get()).isEmpty()
                ? "（关闭）" : ModConfig.MANUAL_SAVE_ITEM.get())
                + "    /regression config manualSaveItem <物品ID|none>");
        lines.add("配置文件：saves/<世界>/serverconfig/deathregression-server.toml（每个世界自己一份）");
        sendLines(ctx, ChatFormatting.AQUA, lines);
        return 1;
    }

    /** /regression config deathRegression —— 只看总开关状态。 */
    private static int showDeathRegression(CommandContext<CommandSourceStack> ctx) {
        boolean on = ModConfig.isDeathRegressionEnabled();
        sendLines(ctx, ChatFormatting.AQUA, java.util.List.of(
                "===== 死亡回归总开关 =====",
                "当前：" + (on ? "开启" : "关闭"),
                "开启（默认）：死亡时世界回溯到最近的存档点",
                "关闭：死亡就是普通死亡（不回退、不断开、不保留数据），已有备份不删",
                "/regression config deathRegression true   → 开启",
                "/regression config deathRegression false  → 关闭"));
        return 1;
    }

    /** /regression config keepPlayerData —— 只看这一项的状态。 */
    private static int showKeepPlayerData(CommandContext<CommandSourceStack> ctx) {
        boolean on = ModConfig.isKeepPlayerDataEnabled();
        sendLines(ctx, ChatFormatting.AQUA, java.util.List.of(
                "===== 死亡后保留人物数据 =====",
                "当前：" + (on ? "开启" : "关闭（默认）"),
                "关闭（默认）：人和背包一起回到存档点",
                "开启：人回到存档点，具体保留哪几样由下面的分项决定",
                "两种情况都一样：位置回存档点、血量用存档点的、世界方块一起回退",
                "/regression config keepPlayerData true   → 开启",
                "/regression config keepPlayerData false  → 关闭",
                "/regression config playerdata            → 打开分项菜单"));
        return 1;
    }

    // ---------------------------------------------------------------- 人物数据分项

    /** 分项的显示名。 */
    private static String keepLabel(String key) {
        return switch (key) {
            case "keepInventory" -> "背包（36 格）";
            case "keepArmor" -> "装备（护甲+副手）";
            case "keepEnderChest" -> "末影箱";
            case "keepXp" -> "经验";
            case "keepHunger" -> "饥饿度";
            case "keepEffects" -> "药水效果";
            case "keepAbsorption" -> "伤害吸收";
            case "keepAir" -> "氧气";
            case "keepScore" -> "计分板分数";
            case "keepSpawnPoint" -> "重生点";
            case "keepModData" -> "模组数据（饰品等）";
            default -> key;
        };
    }

    private static boolean keepValue(String key) {
        return switch (key) {
            case "keepInventory" -> ModConfig.isKeepInventoryEnabled();
            case "keepArmor" -> ModConfig.isKeepArmorEnabled();
            case "keepEnderChest" -> ModConfig.isKeepEnderChestEnabled();
            case "keepXp" -> ModConfig.isKeepXpEnabled();
            case "keepHunger" -> ModConfig.isKeepHungerEnabled();
            case "keepEffects" -> ModConfig.isKeepEffectsEnabled();
            case "keepAbsorption" -> ModConfig.isKeepAbsorptionEnabled();
            case "keepAir" -> ModConfig.isKeepAirEnabled();
            case "keepScore" -> ModConfig.isKeepScoreEnabled();
            case "keepSpawnPoint" -> ModConfig.isKeepSpawnPointEnabled();
            case "keepModData" -> ModConfig.isKeepModDataEnabled();
            default -> false;
        };
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> keepToggle(String key) {
        return Commands.literal(key)
                .executes(c -> showKeepFlag(c, key))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(c -> setBool(c, key, BoolArgumentType.getBool(c, "value"))));
    }

    private static int showKeepFlag(CommandContext<CommandSourceStack> ctx, String key) {
        boolean on = keepValue(key);
        boolean master = ModConfig.isKeepPlayerDataEnabled();
        sendLines(ctx, ChatFormatting.AQUA, java.util.List.of(
                keepLabel(key) + "：当前 " + (master ? "" : "（总开关关着，不生效）") + onOff(on),
                "/regression config " + key + " true   → 保留",
                "/regression config " + key + " false  → 不保留",
                "总开关：" + onOff(master) + "    /regression config keepPlayerData true|false"));
        return 1;
    }

    /** 可点击的开关行：右边 [开]/[关] 点一下就切换。 */
    private static Component keepButton(String key, boolean includeLabel) {
        boolean on = keepValue(key);
        Component button = Component.literal(on ? " [开]" : " [关]")
                .withStyle(Style.EMPTY
                        .withColor(on ? ChatFormatting.GREEN : ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/regression config " + key + " " + (!on)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(on ? "点击关闭" : "点击打开"))));
        if (!includeLabel) {
            return button;
        }
        return Component.literal(keepLabel(key)).withStyle(ChatFormatting.WHITE).append(button);
    }

    /** /regression config playerdata —— 人物数据分项菜单（可点击开关）。 */
    private static int showPlayerDataMenu(CommandContext<CommandSourceStack> ctx) {
        boolean master = ModConfig.isKeepPlayerDataEnabled();
        ctx.getSource().sendSuccess(() -> Component.literal("===== 人物数据保留设置 =====")
                .withStyle(ChatFormatting.AQUA), false);
        ctx.getSource().sendSuccess(() -> Component.literal("(点右边的 [开]/[关] 直接切换)")
                .withStyle(ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(() -> Component.literal("总开关 " + onOff(master) + "  ")
                .withStyle(master ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(Component.literal(master ? "[已开]" : "[已关]")
                        .withStyle(Style.EMPTY
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/regression config keepPlayerData " + (!master)))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("点击切换总开关"))))), false);
        String[] keys = {"keepInventory", "keepArmor", "keepEnderChest", "keepXp", "keepHunger",
                "keepEffects", "keepAbsorption", "keepAir", "keepScore", "keepSpawnPoint", "keepModData"};
        for (String key : keys) {
            ctx.getSource().sendSuccess(() -> keepButton(key, true), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                        "固定用存档点：位置、血量、着火、摔落、死亡状态")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    // ---------------------------------------------------------------- 写入

    private static int setBool(CommandContext<CommandSourceStack> ctx, String key, boolean value) {
        switch (key) {
            case "deathRegression" -> ModConfig.setDeathRegression(value);
            case "requireFullHealth" -> ModConfig.REQUIRE_FULL_HEALTH.set(value);
            case "easterEgg" -> ModConfig.EASTER_EGG_ENABLED.set(value);
            case "keepPlayerData" -> ModConfig.setKeepPlayerData(value);
            case "keepInventory" -> ModConfig.KEEP_INVENTORY.set(value);
            case "keepArmor" -> ModConfig.KEEP_ARMOR.set(value);
            case "keepEnderChest" -> ModConfig.KEEP_ENDER_CHEST.set(value);
            case "keepXp" -> ModConfig.KEEP_XP.set(value);
            case "keepHunger" -> ModConfig.KEEP_HUNGER.set(value);
            case "keepEffects" -> ModConfig.KEEP_EFFECTS.set(value);
            case "keepAbsorption" -> ModConfig.KEEP_ABSORPTION.set(value);
            case "keepAir" -> ModConfig.KEEP_AIR.set(value);
            case "keepScore" -> ModConfig.KEEP_SCORE.set(value);
            case "keepSpawnPoint" -> ModConfig.KEEP_SPAWN_POINT.set(value);
            case "keepModData" -> ModConfig.KEEP_MOD_DATA.set(value);
            default -> {
                return 0;
            }
        }
        return done(ctx, key, String.valueOf(value));
    }

    private static int setInt(CommandContext<CommandSourceStack> ctx, String key, int value) {
        switch (key) {
            case "saveCooldownMinutes" -> ModConfig.SAVE_COOLDOWN_MINUTES.set(value);
            case "daysInterval" -> ModConfig.DAYS_INTERVAL.set(value);
            case "maxBackups" -> ModConfig.MAX_BACKUPS.set(value);
            default -> {
                return 0;
            }
        }
        return done(ctx, key, String.valueOf(value));
    }

    private static int setDouble(CommandContext<CommandSourceStack> ctx, String key, double value) {
        if ("daysProbability".equals(key)) {
            ModConfig.DAYS_PROBABILITY.set(value);
        } else {
            return 0;
        }
        return done(ctx, key, String.valueOf(value));
    }

    private static int setSaveMode(CommandContext<CommandSourceStack> ctx, String raw) {
        String mode = raw.toUpperCase();
        if (!ModConfig.isValidSaveMode(mode)) {
            ctx.getSource().sendFailure(Component.literal("无效的存档模式：" + raw + "（可选 SLEEP / DAYS / MIXED）")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        ModConfig.SAVE_MODE.set(mode);
        return done(ctx, "saveMode", mode);
    }

    @SuppressWarnings("unchecked")
    private static int addDimension(CommandContext<CommandSourceStack> ctx, String dim) {
        List<String> list = new ArrayList<>((List<String>) ModConfig.BLACKLISTED_DIMENSIONS.get());
        if (list.contains(dim)) {
            ctx.getSource().sendFailure(Component.literal("已经在黑名单里：" + dim).withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        list.add(dim);
        ModConfig.BLACKLISTED_DIMENSIONS.set(list);
        return done(ctx, "blacklistedDimensions", list.toString());
    }

    @SuppressWarnings("unchecked")
    private static int removeDimension(CommandContext<CommandSourceStack> ctx, String dim) {
        List<String> list = new ArrayList<>((List<String>) ModConfig.BLACKLISTED_DIMENSIONS.get());
        if (!list.remove(dim)) {
            ctx.getSource().sendFailure(Component.literal("黑名单里没有：" + dim).withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        ModConfig.BLACKLISTED_DIMENSIONS.set(list);
        return done(ctx, "blacklistedDimensions", list.toString());
    }

    private static int clearDimensions(CommandContext<CommandSourceStack> ctx) {
        ModConfig.BLACKLISTED_DIMENSIONS.set(List.of());
        return done(ctx, "blacklistedDimensions", "[]");
    }

    /** /regression config manualSaveItem <物品ID|none> —— 设置或关闭手动存档物品。 */
    private static int setManualItem(CommandContext<CommandSourceStack> ctx, String raw) {
        if (raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("clear")) {
            ModConfig.MANUAL_SAVE_ITEM.set("");
            return done(ctx, "manualSaveItem", "（已关闭）");
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            ctx.getSource().sendFailure(Component.literal("物品 ID 格式不正确：" + raw).withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            ctx.getSource().sendFailure(Component.literal("找不到这个物品：" + raw).withStyle(ChatFormatting.RED));
            return 0;
        }
        ModConfig.MANUAL_SAVE_ITEM.set(id.toString());
        return done(ctx, "manualSaveItem", id.toString() + "（手持右键即可存档）");
    }

    // ---------------------------------------------------------------- 备份列表

    private static int listBackups(CommandContext<CommandSourceStack> ctx) {
        BackupManager bm = DeathRegression.getBackupManager();
        if (bm == null || bm.getBackupRootDir() == null) {
            ctx.getSource().sendFailure(Component.literal("备份管理器未初始化").withStyle(ChatFormatting.RED));
            return 0;
        }
        Path root = bm.getBackupRootDir();
        if (!Files.exists(root)) {
            ctx.getSource().sendSuccess(() -> Component.literal("还没有任何备份").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        final StringBuilder sb = new StringBuilder("===== 备份列表 =====\n");
        final int[] count = {0};
        try (Stream<Path> dirs = Files.list(root)) {
            dirs.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .sorted((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .forEach(p -> {
                        long ago = (System.currentTimeMillis() - p.toFile().lastModified()) / 60000L;
                        sb.append("  ").append(p.getFileName()).append("  （").append(ago).append(" 分钟前）\n");
                        count[0]++;
                    });
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("读取备份目录失败：" + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
        if (count[0] == 0) {
            sb.append("  （空）\n");
        }
        sb.append("===================");
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    // ---------------------------------------------------------------- 工具

    private static int done(CommandContext<CommandSourceStack> ctx, String key, String value) {
        ModConfig.SPEC.save();
        ctx.getSource().sendSuccess(() -> Component.literal("已设置 " + key + " = " + value + "（已写入配置）")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
