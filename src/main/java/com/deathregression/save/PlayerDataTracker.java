/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.storage.DimensionDataStorage
 */
package com.deathregression.save;

import com.deathregression.DeathRegression;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class PlayerDataTracker
extends SavedData {
    private static final String DATA_NAME = "deathregression_player_data";
    private final Map<String, Integer> deathCounts = new HashMap<String, Integer>();
    private final Map<String, Integer> regressionCounts = new HashMap<String, Integer>();
    private long lastSaveTime = 0L;

    public static PlayerDataTracker load(CompoundTag tag) {
        PlayerDataTracker tracker = new PlayerDataTracker();
        CompoundTag deathTag = tag.getCompound("DeathCounts");
        for (String key : deathTag.getAllKeys()) {
            tracker.deathCounts.put(key, deathTag.getInt(key));
        }
        CompoundTag regTag = tag.getCompound("RegressionCounts");
        for (String key : regTag.getAllKeys()) {
            tracker.regressionCounts.put(key, regTag.getInt(key));
        }
        tracker.lastSaveTime = tag.getLong("LastSaveTime");
        DeathRegression.LOGGER.debug("PlayerDataTracker \u5df2\u52a0\u8f7d\uff1a\u8bb0\u5f55 {} \u4e2a\u73a9\u5bb6\u7684\u6570\u636e", (Object)tracker.deathCounts.size());
        return tracker;
    }

    public CompoundTag save(CompoundTag tag) {
        CompoundTag deathTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : this.deathCounts.entrySet()) {
            deathTag.putInt(entry.getKey(), entry.getValue().intValue());
        }
        tag.put("DeathCounts", (Tag)deathTag);
        CompoundTag regTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : this.regressionCounts.entrySet()) {
            regTag.putInt(entry.getKey(), entry.getValue().intValue());
        }
        tag.put("RegressionCounts", (Tag)regTag);
        tag.putLong("LastSaveTime", this.lastSaveTime);
        return tag;
    }

    public void recordDeath(String playerUUID) {
        int count = this.deathCounts.getOrDefault(playerUUID, 0) + 1;
        this.deathCounts.put(playerUUID, count);
        this.setDirty();
    }

    public void recordRegression(String playerUUID) {
        int count = this.regressionCounts.getOrDefault(playerUUID, 0) + 1;
        this.regressionCounts.put(playerUUID, count);
        this.setDirty();
    }

    public int getDeathCount(String playerUUID) {
        return this.deathCounts.getOrDefault(playerUUID, 0);
    }

    public int getRegressionCount(String playerUUID) {
        return this.regressionCounts.getOrDefault(playerUUID, 0);
    }

    public void recordSaveTime() {
        this.lastSaveTime = System.currentTimeMillis();
        this.setDirty();
    }

    public long getLastSaveTime() {
        return this.lastSaveTime;
    }

    public boolean isCooldownPassed(int cooldownMinutes) {
        long cooldownMillis;
        if (this.lastSaveTime <= 0L) {
            return true;
        }
        long elapsedMillis = System.currentTimeMillis() - this.lastSaveTime;
        return elapsedMillis >= (cooldownMillis = (long)cooldownMinutes * 60L * 1000L);
    }

    public long getCooldownRemainingMillis(int cooldownMinutes) {
        if (this.lastSaveTime <= 0L) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - this.lastSaveTime;
        long cooldown = (long)cooldownMinutes * 60L * 1000L;
        return Math.max(0L, cooldown - elapsed);
    }

    public static PlayerDataTracker get(ServerLevel serverLevel) {
        DimensionDataStorage storage = serverLevel.getDataStorage();
        return (PlayerDataTracker)storage.computeIfAbsent(PlayerDataTracker::load, PlayerDataTracker::new, DATA_NAME);
    }

    public static PlayerDataTracker get(MinecraftServer server) {
        return PlayerDataTracker.get(server.overworld());
    }

    public String getCooldownRemainingFormatted(int cooldownMinutes) {
        long remainingMs = this.getCooldownRemainingMillis(cooldownMinutes);
        if (remainingMs <= 0L) {
            return "0 \u79d2";
        }
        long totalSeconds = remainingMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0L) {
            return minutes + " \u5206\u949f " + seconds + " \u79d2";
        }
        return seconds + " \u79d2";
    }
}

