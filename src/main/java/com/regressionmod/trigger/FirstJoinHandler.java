/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 */
package com.regressionmod.trigger;

import com.regressionmod.RegressionMod;
import com.regressionmod.backup.BackupManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FirstJoinHandler {
    private final BackupManager backupManager;
    /** 已经为哪个世界建过初始存档点了（按世界名记，换世界会重新建）。 */
    private String backupDoneForWorld = null;

    public FirstJoinHandler(BackupManager backupManager) {
        this.backupManager = backupManager;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        if (player2.getServer() == null || !player2.getServer().isRunning()) {
            return;
        }
        String world = this.backupManager.getWorldDir() == null
                ? null : this.backupManager.getWorldDir().getFileName().toString();
        if (world == null) {
            return;
        }
        // 这个世界已经处理过就跳过（注意是按世界名判断，换世界会重新建）
        if (world.equals(this.backupDoneForWorld)) {
            return;
        }
        if (this.backupManager.hasBackup()) {
            this.backupDoneForWorld = world;
            return;
        }
        boolean ok = this.backupManager.createBackup(player2.getServer(), player2, "INITIAL");
        if (ok) {
            this.backupDoneForWorld = world;
            RegressionMod.LOGGER.info("\u5df2\u4e3a\u65b0\u751f\u4e16\u754c\u521b\u5efa\u521d\u59cb\u5b58\u6863\u70b9\uff08{}\uff09", world);
        }
    }
}

