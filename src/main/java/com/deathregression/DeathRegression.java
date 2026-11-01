/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.server.ServerStartingEvent
 *  net.minecraftforge.event.server.ServerStoppedEvent
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod
 *  net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
 *  net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.deathregression;

import com.deathregression.api.RegressionAPI;
import com.deathregression.backup.BackupManager;
import com.deathregression.client.AutoRejoinHandler;
import com.deathregression.config.ModConfig;
import com.deathregression.death.DeathHandler;
import com.deathregression.network.ModNetwork;
import com.deathregression.save.PlayerDataTracker;
import com.deathregression.save.PlayerSnapshot;
import com.deathregression.trigger.DayCounter;
import com.deathregression.trigger.FirstJoinHandler;
import com.deathregression.trigger.SaveTriggerHandler;
import java.nio.file.Path;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value="deathregression")
public class DeathRegression {
    public static final String MOD_ID = "deathregression";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"deathregression");
    private static BackupManager backupManager;
    private static DayCounter dayCounter;
    private static SaveTriggerHandler saveTriggerHandler;
    private static DeathHandler deathHandler;
    private static FirstJoinHandler firstJoinHandler;
    private static com.deathregression.easteregg.ChatDeathEasterEgg easterEgg;
    private static final com.deathregression.regression.PlayerDataKeeper playerDataKeeper = new com.deathregression.regression.PlayerDataKeeper();
    private static Path pendingBackup;
    private static PlayerSnapshot pendingSnapshot;
    private static String pendingWorldFolderName;
    // 死亡瞬间记下的玩家数据（临时目录 + 玩家 UUID）
    private static Path pendingKeepDir;
    private static java.util.UUID pendingKeepUuid;

    public DeathRegression() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ModNetwork.register();
        // 配置改成「服务器型」：每个世界一份，由 Forge 在服务器启动时加载，
        // 文件在 saves/<世界>/serverconfig/deathregression-server.toml
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER, ModConfig.SPEC, "deathregression-server.toml");
        backupManager = new BackupManager();
        dayCounter = new DayCounter();
        saveTriggerHandler = new SaveTriggerHandler();
        deathHandler = new DeathHandler();
        firstJoinHandler = new FirstJoinHandler(backupManager);
        easterEgg = com.deathregression.easteregg.ChatDeathEasterEgg.create();
        saveTriggerHandler.setBackupManager(backupManager);
        saveTriggerHandler.setDayCounter(dayCounter);
        deathHandler.setBackupManager(backupManager);
        RegressionAPI.setBackupManager(backupManager);
        MinecraftForge.EVENT_BUS.register((Object)dayCounter);
        MinecraftForge.EVENT_BUS.register((Object)saveTriggerHandler);
        MinecraftForge.EVENT_BUS.register((Object)deathHandler);
        MinecraftForge.EVENT_BUS.register((Object)firstJoinHandler);
        MinecraftForge.EVENT_BUS.register((Object)easterEgg);
        MinecraftForge.EVENT_BUS.register((Object)playerDataKeeper);
        MinecraftForge.EVENT_BUS.register((Object)this);
        LOGGER.info("============================================");
        LOGGER.info("  DeathRegression \u2014 \u6b7b\u4ea1\u56de\u5f52\u6a21\u7ec4\u5df2\u521d\u59cb\u5316\uff08\u914d\u7f6e\u6309\u4e16\u754c\u5206\u5f00\uff09");
        LOGGER.info("============================================");
    }

    /** 配置是「服务器型」，只能在服务器启动后读，所以打印挪到这里。 */
    private void logConfig() {
        try {
            LOGGER.info("============================================");
            LOGGER.info("  \u672c\u4e16\u754c\u7684\u914d\u7f6e\uff08saves/<\u4e16\u754c>/serverconfig/deathregression-server.toml\uff09");
            LOGGER.info("  \u6b7b\u4ea1\u56de\u5f52: {}", (Object)(ModConfig.isDeathRegressionEnabled() ? "\u5f00\u542f" : "\u5df2\u5173\u95ed\uff08\u6b7b\u4ea1\u4e0d\u4f1a\u56de\u6eaf\uff09"));
            LOGGER.info("  \u4fdd\u7559\u4eba\u7269\u6570\u636e: {}", (Object)(ModConfig.isKeepPlayerDataEnabled() ? "\u5f00\u542f" : "\u5173\u95ed"));
            LOGGER.info("  \u5b58\u6863\u6a21\u5f0f: {}", ModConfig.SAVE_MODE.get());
            LOGGER.info("  \u5929\u6570\u95f4\u9694: {} \u6e38\u620f\u65e5", ModConfig.DAYS_INTERVAL.get());
            LOGGER.info("  \u5929\u6570\u6982\u7387: {}%", (Object)((int)((Double)ModConfig.DAYS_PROBABILITY.get() * 100.0)));
            LOGGER.info("  \u5b58\u6863\u51b7\u5374: {} \u5206\u949f", ModConfig.SAVE_COOLDOWN_MINUTES.get());
            LOGGER.info("  \u6700\u5927\u5907\u4efd: {} \u4e2a", ModConfig.MAX_BACKUPS.get());
            LOGGER.info("============================================");
        } catch (Exception e) {
            LOGGER.warn("\u914d\u7f6e\u5c1a\u672a\u5c31\u7eea\uff0c\u8df3\u8fc7\u6253\u5370\uff1a{}", e.getMessage());
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("DeathRegression \u901a\u7528\u8bbe\u7f6e\u5b8c\u6210");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        logConfig();
        backupManager.initialize(event.getServer());
        // 上次替换到一半崩了的话，先把现场收拾干净
        backupManager.recoverInterruptedReplacement();
        PlayerDataTracker.get(event.getServer());
        // 上次没走完流程留下的临时人物数据，清掉
        com.deathregression.regression.PlayerDataKeeper.cleanupStale(backupManager.getWorldDir());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        if (pendingBackup == null) {
            return;
        }
        Path backup = pendingBackup;
        String worldName = pendingWorldFolderName;
        pendingBackup = null;
        pendingWorldFolderName = null;
        LOGGER.info("========== \u6267\u884c\u56de\u6eaf\u6587\u4ef6\u66ff\u6362 ==========");
        try {
            Thread.sleep(500L);
        } catch (InterruptedException interruptedException) {
            // empty catch block
        }
        boolean ok = backupManager.replaceWorldWithBackup(backup);
        if (ok) {
            LOGGER.info("\u56de\u6eaf\u5b8c\u6210\uff0c\u4e16\u754c\u5df2\u6062\u590d\u5230\u5b58\u6863\u70b9");
            // 世界已经换回来了，这时候合并人物数据才不会被覆盖
            if (pendingKeepUuid != null) {
                com.deathregression.regression.PlayerDataKeeper.apply(
                        backupManager.getWorldDir(), pendingKeepDir, pendingKeepUuid);
            }
            pendingKeepDir = null;
            pendingKeepUuid = null;
            if (worldName != null) {
                AutoRejoinHandler.scheduleAutoRejoin(worldName);
            }
        } else {
            LOGGER.error("\u56de\u6eaf\u5931\u8d25\uff0c\u4e16\u754c\u672a\u53d8\u52a8");
            com.deathregression.regression.PlayerDataKeeper.deleteQuietly(pendingKeepDir);
            pendingKeepDir = null;
            pendingKeepUuid = null;
        }
    }

    /** 死亡瞬间记下的人物数据（临时目录 + 玩家）。 */
    public static void setPendingKeep(Path keepDir, java.util.UUID uuid) {
        pendingKeepDir = keepDir;
        pendingKeepUuid = uuid;
    }

    public static void markForRegression(Path backupDir, PlayerSnapshot snapshot, String worldFolderName) {
        pendingBackup = backupDir;
        pendingSnapshot = snapshot;
        pendingWorldFolderName = worldFolderName;
    }

    public static PlayerSnapshot getPendingSnapshot() {
        return pendingSnapshot;
    }

    public static BackupManager getBackupManager() {
        return backupManager;
    }

    public static DayCounter getDayCounter() {
        return dayCounter;
    }

    public static SaveTriggerHandler getSaveTriggerHandler() {
        return saveTriggerHandler;
    }

    public static DeathHandler getDeathHandler() {
        return deathHandler;
    }

    static {
        pendingBackup = null;
        pendingSnapshot = null;
        pendingWorldFolderName = null;
    }
}

