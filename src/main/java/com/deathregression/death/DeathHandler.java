/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.eventbus.api.Event
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 */
package com.deathregression.death;

import com.deathregression.DeathRegression;
import com.deathregression.backup.BackupManager;
import com.deathregression.config.ModConfig;
import com.deathregression.event.RegressionEvents;
import com.deathregression.save.PlayerDataTracker;
import com.deathregression.save.PlayerSnapshot;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DeathHandler {
    private BackupManager backupManager;

    public void setBackupManager(BackupManager mgr) {
        this.backupManager = mgr;
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer)livingEntity;
        MinecraftServer server = player.getServer();
        if (server == null || !server.isRunning()) {
            DeathRegression.LOGGER.warn("\u670d\u52a1\u5668\u672a\u8fd0\u884c\u6216\u6b63\u5728\u505c\u6b62\uff0c\u8df3\u8fc7\u6b7b\u4ea1\u5904\u7406");
            return;
        }
        if (this.backupManager == null) {
            return;
        }
        // 总开关关了就什么都不做：死亡就是普通死亡
        if (!ModConfig.isDeathRegressionEnabled()) {
            return;
        }
        String dimId = player.level().dimension().location().toString();
        if (((List)ModConfig.BLACKLISTED_DIMENSIONS.get()).contains(dimId)) {
            return;
        }
        if (!this.backupManager.hasBackup()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c\u6ca1\u6709\u53ef\u7528\u7684\u5b58\u6863\u70b9\uff01"));
            return;
        }
        RegressionEvents.BeforeRegressionEvent beforeEv = new RegressionEvents.BeforeRegressionEvent(player, (ResourceKey<Level>)player.level().dimension());
        if (MinecraftForge.EVENT_BUS.post((Event)beforeEv)) {
            return;
        }
        event.setCanceled(true);
        PlayerDataTracker.get(server).recordDeath(player.getUUID().toString());
        DeathRegression.LOGGER.info("===== \u73a9\u5bb6 {} \u89e6\u53d1\u6b7b\u4ea1\u56de\u5f52 =====", (Object)player.getName().getString());
        Path latestBackup = this.backupManager.findLatestBackup();
        PlayerSnapshot snapshot = this.backupManager.loadSnapshot(latestBackup);
        String worldFolderName = this.backupManager.getWorldDir() != null ? this.backupManager.getWorldDir().getFileName().toString() : null;

        // 死亡瞬间把人物数据从内存记下来（磁盘那份可能是旧的）。
        // 关掉 keepPlayerData 的话这里返回 null，回溯后背包也回档。
        CompoundTag liveData = new CompoundTag();
        try {
            player.saveWithoutId(liveData);
        } catch (Exception e) {
            DeathRegression.LOGGER.warn("\u4eba\u7269\u6570\u636e\uff1a\u5bfc\u51fa\u5931\u8d25\uff0c\u672c\u6b21\u80cc\u5305\u4e5f\u4f1a\u56de\u6863", e);
            liveData = null;
        }
        Path keepDir = com.deathregression.regression.PlayerDataKeeper.stash(
                this.backupManager.getWorldDir(), player.getUUID(), liveData);

        DeathRegression.markForRegression(latestBackup, snapshot, worldFolderName);
        DeathRegression.setPendingKeep(keepDir, player.getUUID());
        DeathRegression.LOGGER.info("\u6b63\u5728\u65ad\u5f00\u73a9\u5bb6\u8fde\u63a5\u4ee5\u89e6\u53d1\u670d\u52a1\u5668\u505c\u6b62...");
        player.connection.disconnect((Component)Component.literal((String)"\u00a7c\u00a7l\u4e16\u754c\u56de\u6eaf\u4e2d...\n\u00a77\u8bf7\u91cd\u65b0\u8fdb\u5165\u4e16\u754c"));
    }
}

