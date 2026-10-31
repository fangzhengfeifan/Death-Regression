/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.regressionmod.network;

import com.regressionmod.gui.RegressionScreen;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class S2CShowOverlayPacket {
    public static void encode(S2CShowOverlayPacket packet, FriendlyByteBuf buf) {
    }

    public static S2CShowOverlayPacket decode(FriendlyByteBuf buf) {
        return new S2CShowOverlayPacket();
    }

    public static void handle(S2CShowOverlayPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen((Screen)new RegressionScreen());
        });
        context.setPacketHandled(true);
    }
}

