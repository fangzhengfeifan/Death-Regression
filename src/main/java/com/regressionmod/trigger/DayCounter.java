/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$ServerTickEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 */
package com.regressionmod.trigger;

import com.regressionmod.RegressionMod;
import com.regressionmod.config.ModConfig;
import com.regressionmod.trigger.SaveTriggerHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DayCounter {
    private int daysElapsed = 0;
    private long lastDayTime = -1L;
    private SaveTriggerHandler saveTriggerHandler;

    public void setSaveTriggerHandler(SaveTriggerHandler handler) {
        this.saveTriggerHandler = handler;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        long currentDayTime = overworld.getDayTime();
        if (this.lastDayTime < 0L) {
            this.lastDayTime = currentDayTime;
            return;
        }
        long currentDay = currentDayTime / 24000L;
        long lastDay = this.lastDayTime / 24000L;
        if (currentDay > lastDay) {
            ++this.daysElapsed;
            int daysInterval = (Integer)ModConfig.DAYS_INTERVAL.get();
            RegressionMod.LOGGER.debug("\u65b0\u7684\u4e00\u5929\uff01\u5df2\u8fc7 {} \u5929\uff0c\u89e6\u53d1\u95f4\u9694 {} \u5929", (Object)this.daysElapsed, (Object)daysInterval);
            if (this.daysElapsed >= daysInterval) {
                if (this.saveTriggerHandler != null) {
                    this.saveTriggerHandler.onDayTrigger(server);
                }
                this.daysElapsed = 0;
            }
        }
        this.lastDayTime = currentDayTime;
    }

    public int getDaysElapsed() {
        return this.daysElapsed;
    }

    public void reset() {
        this.daysElapsed = 0;
        this.lastDayTime = -1L;
    }
}

