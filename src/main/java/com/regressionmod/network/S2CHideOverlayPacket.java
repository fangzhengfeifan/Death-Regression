/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.regressionmod.network;

import com.regressionmod.gui.RegressionScreen;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class S2CHideOverlayPacket {
    public static void encode(S2CHideOverlayPacket packet, FriendlyByteBuf buf) {
    }

    public static S2CHideOverlayPacket decode(FriendlyByteBuf buf) {
        return new S2CHideOverlayPacket();
    }

    public static void handle(S2CHideOverlayPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof RegressionScreen) {
                minecraft.setScreen(null);
            }
        });
        context.setPacketHandled(true);
    }
}

