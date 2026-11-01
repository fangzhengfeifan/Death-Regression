package com.deathregression.regression;

import com.deathregression.DeathRegression;
import com.deathregression.config.ModConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 死亡后保留人物数据（分项可配置）。
 *
 * <p>思路：
 * <ol>
 *   <li>死亡瞬间：把玩家数据从<b>内存</b>导出（磁盘那份可能是旧的），存一份到临时目录，同时留内存</li>
 *   <li>世界换回存档点之后：以「存档点那份」为底，按开关把「死亡那份」的对应条目盖上去，
 *       得到一份<b>合并结果</b>，写回 {@code playerdata/<uuid>.dat}</li>
 *   <li>玩家登录时再按同一份合并结果套到活着的玩家身上（双保险，文件被覆盖也不会丢）</li>
 * </ol>
 *
 * <p>合并结果里的位置、血量、着火、摔落、死亡状态一律来自存档点 ——
 * 位置不回存档点就不叫回溯，血量 0 会一进世界当场再死一次。
 */
public final class PlayerDataKeeper {

    /** 事件订阅要用实例，所以构造不能是私有。 */
    public PlayerDataKeeper() {}

    /** 等待登录时套用的数据（进程内有效）。 */
    private static Path pendingDir;
    private static UUID pendingUuid;
    /** 合并好的最终数据，直接放内存里带着走，不依赖文件。 */
    private static CompoundTag pendingTag;

    /** 临时目录名（在 saves 下面）。 */
    static final String KEEP_DIR = "regression_keep";

    // ================================================================ 死亡瞬间

    /** 死亡瞬间调用：记下这个玩家现在的数据。总开关关着就返回 null。 */
    public static Path stash(Path worldDir, UUID uuid, CompoundTag livePlayer) {
        if (!ModConfig.isKeepPlayerDataEnabled() || livePlayer == null || worldDir == null) {
            return null;
        }
        Path keepDir = worldDir.getParent().resolve(KEEP_DIR).resolve(uuid.toString());
        pendingTag = null; // 此时还没合并，等世界替换完再算
        pendingUuid = uuid;
        try {
            Files.createDirectories(keepDir);
            NbtIo.writeCompressed(livePlayer, keepDir.resolve("playerdata.dat").toFile());
            DeathRegression.LOGGER.info("人物数据：已记下死亡时的数据（背包 {} 格、经验 {} 级）",
                    countStacks(livePlayer.getList("Inventory", 10)), livePlayer.getInt("XpLevel"));
            return keepDir;
        } catch (Exception e) {
            DeathRegression.LOGGER.error("人物数据：暂存失败，本次回溯后全部回档", e);
            return null;
        }
    }

    // ================================================================ 合并

    /**
     * 以存档点为底、把死亡那份里「开关打开」的条目盖上去。
     *
     * @param death      死亡瞬间导出的数据（不会被修改）
     * @param checkpoint 存档点里的数据（不会被修改）
     * @return 合并结果（新标签）
     */
    public static CompoundTag buildMerged(CompoundTag death, CompoundTag checkpoint) {
        CompoundTag out = checkpoint.copy();
        mergeInventory(death, out);
        if (ModConfig.isKeepEnderChestEnabled()) {
            out.put("EnderItems", death.getList("EnderItems", 10).copy());
        }
        if (ModConfig.isKeepXpEnabled()) {
            copyInt(death, out, "XpLevel");
            copyInt(death, out, "XpTotal");
            copyInt(death, out, "XpSeed");
            if (death.contains("XpP")) {
                out.putFloat("XpP", death.getFloat("XpP"));
            }
        }
        if (ModConfig.isKeepHungerEnabled()) {
            copyInt(death, out, "foodLevel");
            copyInt(death, out, "foodTickTimer");
            copyFloat(death, out, "foodSaturationLevel");
            copyFloat(death, out, "foodExhaustionLevel");
        }
        if (ModConfig.isKeepEffectsEnabled()) {
            if (death.contains("active_effects")) {
                out.put("active_effects", death.getList("active_effects", 10).copy());
            } else {
                out.remove("active_effects");
            }
        }
        if (ModConfig.isKeepAbsorptionEnabled() && death.contains("AbsorptionAmount")) {
            out.putDouble("AbsorptionAmount", death.getDouble("AbsorptionAmount"));
        }
        if (ModConfig.isKeepAirEnabled()) {
            copyInt(death, out, "Air");
        }
        if (ModConfig.isKeepScoreEnabled()) {
            copyInt(death, out, "Score");
        }
        if (ModConfig.isKeepSpawnPointEnabled() && death.contains("SpawnX")) {
            copyInt(death, out, "SpawnX");
            copyInt(death, out, "SpawnY");
            copyInt(death, out, "SpawnZ");
            copyFloat(death, out, "SpawnAngle");
            copyInt(death, out, "SpawnForced");
            if (death.contains("SpawnDimension")) {
                out.put("SpawnDimension", death.get("SpawnDimension").copy());
            }
        }
        if (ModConfig.isKeepModDataEnabled() && death.contains("ForgeCaps")) {
            out.put("ForgeCaps", death.getCompound("ForgeCaps").copy());
        }
        return out;
    }

    /** 背包按槽位合并：0–35 归「背包」，36–40 归「装备」，各自按开关取。 */
    private static void mergeInventory(CompoundTag death, CompoundTag out) {
        boolean keepInventory = ModConfig.isKeepInventoryEnabled();
        boolean keepArmor = ModConfig.isKeepArmorEnabled();
        if (!keepInventory && !keepArmor) {
            return; // 全不保留：out 里就是存档点原样
        }
        Map<Integer, CompoundTag> bySlot = new TreeMap<>();
        for (Tag t : out.getList("Inventory", 10)) {
            CompoundTag entry = (CompoundTag) t;
            bySlot.put(entry.getInt("Slot"), entry);
        }
        for (Tag t : death.getList("Inventory", 10)) {
            CompoundTag entry = (CompoundTag) t;
            int slot = entry.getInt("Slot");
            if ((slot >= 0 && slot <= 35 && keepInventory) || (slot >= 36 && slot <= 40 && keepArmor)) {
                bySlot.put(slot, entry.copy());
            }
        }
        ListTag merged = new ListTag();
        for (CompoundTag entry : bySlot.values()) {
            merged.add(entry);
        }
        out.put("Inventory", merged);
        if (keepInventory) {
            out.putInt("SelectedItemSlot", death.getInt("SelectedItemSlot"));
        }
    }

    private static void copyInt(CompoundTag from, CompoundTag to, String key) {
        if (from.contains(key)) {
            to.putInt(key, from.getInt(key));
        }
    }

    private static void copyFloat(CompoundTag from, CompoundTag to, String key) {
        if (from.contains(key)) {
            to.putFloat(key, from.getFloat(key));
        }
    }

    // ================================================================ 回溯之后

    /**
     * 世界已经换回存档点之后调用：合并出最终人物数据，写进 {@code playerdata/<uuid>.dat}，
     * 同时留在内存里等登录时套用。
     *
     * @param keepDir 死亡瞬间暂存的目录；为 null 表示不保留（直接用存档点的）
     */
    public static void apply(Path worldDir, Path keepDir, UUID uuid) {
        try {
            if (keepDir == null || uuid == null) {
                return;
            }
            Path stashFile = keepDir.resolve("playerdata.dat");
            if (!Files.exists(stashFile, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            if (worldDir == null || !Files.isDirectory(worldDir)) {
                return;
            }
            CompoundTag death = NbtIo.readCompressed(stashFile.toFile());
            Path target = worldDir.resolve("playerdata").resolve(uuid + ".dat");
            CompoundTag checkpoint = new CompoundTag();
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                checkpoint = NbtIo.readCompressed(target.toFile());
            } else {
                DeathRegression.LOGGER.warn("人物数据：存档点里没有这个玩家，只写保留项");
            }
            CompoundTag merged = buildMerged(death, checkpoint);
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            NbtIo.writeCompressed(merged, tmp.toFile());
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            pendingTag = merged;
            pendingUuid = uuid;
            DeathRegression.LOGGER.info("人物数据：合并完成（背包 {} 格、经验 {} 级），已写入 playerdata",
                    countStacks(merged.getList("Inventory", 10)), merged.getInt("XpLevel"));
            DeathRegression.LOGGER.info("人物数据：保留项 —— 背包={} 装备={} 末影箱={} 经验={} 饥饿={} 药水={} 吸收={} 氧气={} 计分板={} 重生点={} 模组={}",
                    onOff(ModConfig.isKeepInventoryEnabled()), onOff(ModConfig.isKeepArmorEnabled()),
                    onOff(ModConfig.isKeepEnderChestEnabled()), onOff(ModConfig.isKeepXpEnabled()),
                    onOff(ModConfig.isKeepHungerEnabled()), onOff(ModConfig.isKeepEffectsEnabled()),
                    onOff(ModConfig.isKeepAbsorptionEnabled()), onOff(ModConfig.isKeepAirEnabled()),
                    onOff(ModConfig.isKeepScoreEnabled()), onOff(ModConfig.isKeepSpawnPointEnabled()),
                    onOff(ModConfig.isKeepModDataEnabled()));
        } catch (Exception e) {
            DeathRegression.LOGGER.error("人物数据：合并/写回失败，本次全部回档（不影响进游戏）", e);
        }
    }

    private static String onOff(boolean value) {
        return value ? "留" : "丢";
    }

    // ================================================================ 登录时套用

    /**
     * 玩家登录时调用：把合并结果灌到玩家身上。
     *
     * <p>文件已经在 {@link #apply} 里写好了，理论上游戏读档就够了；
     * 这里再套一次是防文件被覆盖，两边值相同，重复套没有副作用。
     */
    public static boolean applyToPlayer(net.minecraft.server.level.ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (pendingUuid == null || !pendingUuid.equals(uuid)) {
            return false;
        }
        try {
            CompoundTag tag = pendingTag;
            String source = "内存";
            if (tag == null && pendingDir != null) {
                Path file = pendingDir.resolve("playerdata.dat");
                if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                    tag = NbtIo.readCompressed(file.toFile());
                    source = "文件";
                }
            }
            if (tag == null) {
                return false;
            }
            int slots = countStacks(tag.getList("Inventory", 10));
            int xp = tag.getInt("XpLevel");

            // 物品：合并结果里已经按开关挑好了（未保留的槽位是存档点的）
            player.getInventory().load(tag.getList("Inventory", 10));
            player.getInventory().selected = tag.getInt("SelectedItemSlot");
            player.getEnderChestInventory().fromTag(tag.getList("EnderItems", 10));

            // 成长
            player.experienceLevel = xp;
            player.experienceProgress = tag.getFloat("XpP");
            player.totalExperience = tag.getInt("XpTotal");
            // XpSeed 没有公开的 setter，靠写入 playerdata 文件由游戏自己读

            // 状态
            player.getFoodData().setFoodLevel(tag.getInt("foodLevel"));
            player.getFoodData().setSaturation(tag.getFloat("foodSaturationLevel"));
            player.setAbsorptionAmount(tag.getFloat("AbsorptionAmount"));
            player.setAirSupply(tag.getInt("Air"));
            player.setScore(tag.getInt("Score"));

            // 药水效果
            player.getActiveEffects().clear();
            for (Tag t : tag.getList("active_effects", 10)) {
                net.minecraft.world.effect.MobEffectInstance effect =
                        net.minecraft.world.effect.MobEffectInstance.load((CompoundTag) t);
                if (effect != null) {
                    player.addEffect(effect);
                }
            }

            // 重生点
            applySpawnPoint(player, tag);

            // 同步给客户端
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(
                    player.getInventory().selected));

            DeathRegression.LOGGER.info("人物数据：从{}套用 —— 合并数据里有 {} 格、套用后玩家身上 {} 格、经验 {} 级",
                    source, slots, countItems(player), player.experienceLevel);
            return true;
        } catch (Exception e) {
            DeathRegression.LOGGER.error("人物数据：套用失败（保持存档点的状态）", e);
            return false;
        } finally {
            deleteQuietly(pendingDir);
            pendingDir = null;
            pendingUuid = null;
            pendingTag = null;
        }
    }

    private static void applySpawnPoint(net.minecraft.server.level.ServerPlayer player, CompoundTag tag) {
        if (!tag.contains("SpawnX")) {
            return;
        }
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension =
                net.minecraft.world.level.Level.OVERWORLD;
        if (tag.contains("SpawnDimension")) {
            try {
                dimension = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new net.minecraft.resources.ResourceLocation(tag.getString("SpawnDimension")));
            } catch (Exception ignored) {
                // 维度名坏了就用主世界
            }
        }
        player.setRespawnPosition(dimension,
                new net.minecraft.core.BlockPos(tag.getInt("SpawnX"), tag.getInt("SpawnY"), tag.getInt("SpawnZ")),
                tag.getFloat("SpawnAngle"), tag.getBoolean("SpawnForced"), false);
    }

    private static int countStacks(ListTag list) {
        return list.size();
    }

    private static int countItems(net.minecraft.server.level.ServerPlayer player) {
        int n = 0;
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** 记下"等这个玩家登录时要套用哪份数据"。 */
    public static void markPending(Path keepDir, UUID uuid) {
        pendingDir = keepDir;
        pendingUuid = uuid;
    }

    public static boolean hasPending() {
        return pendingUuid != null;
    }

    /** 玩家登录时套用合并结果。 */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            // 等一 tick 再套，确保玩家已经完全初始化
            player.server.execute(() -> applyToPlayer(player));
        }
    }

    /** 启动时清掉上次崩溃留下的临时目录。 */
    public static void cleanupStale(Path worldDir) {
        if (hasPending()) {
            return; // 有一份正等着登录时套用，别删
        }
        if (worldDir == null || worldDir.getParent() == null) {
            return;
        }
        Path root = worldDir.getParent().resolve(KEEP_DIR);
        if (!Files.isDirectory(root)) {
            return;
        }
        deleteQuietly(root);
        DeathRegression.LOGGER.info("人物数据：清理了上次留下的临时目录");
    }

    public static void deleteQuietly(Path dir) {
        if (dir == null || !Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 单个删不掉就算了
                }
            }
        } catch (IOException ignored) {
            // 忽略
        }
    }
}
