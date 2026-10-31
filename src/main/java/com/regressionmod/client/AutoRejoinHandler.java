/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.ReceivingLevelScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.ScreenEvent$Opening
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 */
package com.regressionmod.client;

import com.regressionmod.RegressionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="regressionmod", value={Dist.CLIENT})
public class AutoRejoinHandler {
    private static volatile String pendingWorldName;
    private static volatile boolean rejoinInProgress;

    public static void scheduleAutoRejoin(String worldName) {
        pendingWorldName = worldName;
        rejoinInProgress = false;
        RegressionMod.LOGGER.info("[AutoRejoin] \u5df2\u6392\u961f: {}", (Object)worldName);
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (pendingWorldName == null || rejoinInProgress) {
            return;
        }
        Screen newScreen = event.getScreen();
        if (newScreen == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return;
        }
        String className = newScreen.getClass().getSimpleName();
        if (className.contains("Progress") || className.contains("Level") || className.contains("Loading")) {
            return;
        }
        rejoinInProgress = true;
        String name = pendingWorldName;
        pendingWorldName = null;
        RegressionMod.LOGGER.info("[AutoRejoin] \u62e6\u622a\u753b\u9762 '{}'\uff0c\u7acb\u5373\u8fdb\u5165\u4e16\u754c: {}", (Object)className, (Object)name);
        event.setCanceled(true);
        mc.setScreen((Screen)new ReceivingLevelScreen());
        Screen current = mc.screen;
        mc.createWorldOpenFlows().loadLevel((Screen)(current != null ? current : new ReceivingLevelScreen()), name);
    }
}

