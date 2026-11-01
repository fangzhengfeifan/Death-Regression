/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.eventbus.api.Event
 */
package com.deathregression.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

public class RegressionEvents {

    public static class AfterRegressionEvent
    extends Event {
        private final ServerPlayer player;
        private final ResourceKey<Level> dimension;

        public AfterRegressionEvent(ServerPlayer player, ResourceKey<Level> dimension) {
            this.player = player;
            this.dimension = dimension;
        }

        public ServerPlayer getPlayer() {
            return this.player;
        }

        public ResourceKey<Level> getDimension() {
            return this.dimension;
        }
    }

    public static class BeforeRegressionEvent
    extends Event {
        private final ServerPlayer player;
        private final ResourceKey<Level> dimension;

        public BeforeRegressionEvent(ServerPlayer player, ResourceKey<Level> dimension) {
            this.player = player;
            this.dimension = dimension;
        }

        public ServerPlayer getPlayer() {
            return this.player;
        }

        public ResourceKey<Level> getDimension() {
            return this.dimension;
        }
    }

    public static class AfterAutoSaveEvent
    extends Event {
        private final ServerPlayer player;
        private final ResourceKey<Level> dimension;
        private final String triggerType;

        public AfterAutoSaveEvent(ServerPlayer player, ResourceKey<Level> dimension, String triggerType) {
            this.player = player;
            this.dimension = dimension;
            this.triggerType = triggerType;
        }

        public ServerPlayer getPlayer() {
            return this.player;
        }

        public ResourceKey<Level> getDimension() {
            return this.dimension;
        }

        public String getTriggerType() {
            return this.triggerType;
        }
    }

    public static class BeforeAutoSaveEvent
    extends Event {
        private final ServerPlayer player;
        private final ResourceKey<Level> dimension;
        private final String triggerType;

        public BeforeAutoSaveEvent(ServerPlayer player, ResourceKey<Level> dimension, String triggerType) {
            this.player = player;
            this.dimension = dimension;
            this.triggerType = triggerType;
        }

        public ServerPlayer getPlayer() {
            return this.player;
        }

        public ResourceKey<Level> getDimension() {
            return this.dimension;
        }

        public String getTriggerType() {
            return this.triggerType;
        }
    }
}

