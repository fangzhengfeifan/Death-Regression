/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 */
package com.deathregression.trigger;

import com.deathregression.config.ModConfig;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class SafetyChecker {
    public static boolean isSafeToSave(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (((Boolean)ModConfig.REQUIRE_FULL_HEALTH.get()).booleanValue() && !SafetyChecker.isFullHealth(player)) {
            return false;
        }
        if (!SafetyChecker.isNotInCombat(player)) {
            return false;
        }
        if (!SafetyChecker.isOnGround(player)) {
            return false;
        }
        return !SafetyChecker.isDimensionBlacklisted((ResourceKey<Level>)player.level().dimension());
    }

    public static boolean isFullHealth(ServerPlayer player) {
        return player.getHealth() >= player.getMaxHealth();
    }

    public static boolean isNotInCombat(ServerPlayer player) {
        int currentTick = player.tickCount;
        if (player.hurtTime > 0) {
            return false;
        }
        if (player.invulnerableTime > 0) {
            return false;
        }
        if (player.getLastHurtByMobTimestamp() > 0 && currentTick - player.getLastHurtByMobTimestamp() < 40) {
            return false;
        }
        LivingEntity lastHurtBy = player.getLastHurtByMob();
        return lastHurtBy == null || !lastHurtBy.isAlive() || !(player.distanceToSqr((Entity)lastHurtBy) < 256.0);
    }

    public static boolean isOnGround(ServerPlayer player) {
        if (!player.onGround()) {
            return false;
        }
        if (player.isInWater()) {
            return false;
        }
        return !player.isInLava();
    }

    public static boolean isDimensionBlacklisted(ResourceKey<Level> dimensionKey) {
        List blacklist = (List)ModConfig.BLACKLISTED_DIMENSIONS.get();
        if (blacklist.isEmpty()) {
            return false;
        }
        return blacklist.contains(dimensionKey.location().toString());
    }
}

