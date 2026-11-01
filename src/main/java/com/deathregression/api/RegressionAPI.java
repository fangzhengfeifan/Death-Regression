/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 */
package com.deathregression.api;

import com.deathregression.backup.BackupManager;
import com.deathregression.save.PlayerDataTracker;
import com.deathregression.trigger.SafetyChecker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class RegressionAPI {
    private static BackupManager backupManager;

    public static void setBackupManager(BackupManager mgr) {
        backupManager = mgr;
    }

    public static int getDeathCount(Player player) {
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            return PlayerDataTracker.get(sp.serverLevel()).getDeathCount(player.getUUID().toString());
        }
        return 0;
    }

    public static int getRegressionCount(Player player) {
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            return PlayerDataTracker.get(sp.serverLevel()).getRegressionCount(player.getUUID().toString());
        }
        return 0;
    }

    public static long getLastSaveTime(Level level) {
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            return PlayerDataTracker.get(sl).getLastSaveTime();
        }
        return 0L;
    }

    public static boolean isSafeToSave(Player player) {
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            return SafetyChecker.isSafeToSave(sp);
        }
        return false;
    }

    public static boolean hasBackup() {
        return backupManager != null && backupManager.hasBackup();
    }
}

