/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkRegistry$ChannelBuilder
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.network.simple.SimpleChannel
 */
package com.deathregression.network;

import com.deathregression.DeathRegression;
import com.deathregression.network.S2CHideOverlayPacket;
import com.deathregression.network.S2CShowOverlayPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1.0.0";
    private static SimpleChannel INSTANCE;
    private static int packetId;

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder.named((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"deathregression", (String)"main")).networkProtocolVersion(() -> PROTOCOL_VERSION).clientAcceptedVersions(PROTOCOL_VERSION::equals).serverAcceptedVersions(PROTOCOL_VERSION::equals).simpleChannel();
        INSTANCE.registerMessage(packetId++, S2CShowOverlayPacket.class, S2CShowOverlayPacket::encode, S2CShowOverlayPacket::decode, S2CShowOverlayPacket::handle);
        INSTANCE.registerMessage(packetId++, S2CHideOverlayPacket.class, S2CHideOverlayPacket::encode, S2CHideOverlayPacket::decode, S2CHideOverlayPacket::handle);
        DeathRegression.LOGGER.info("\u7f51\u7edc\u901a\u4fe1\u5df2\u6ce8\u518c\uff0c\u5171 {} \u4e2a\u6570\u636e\u5305", (Object)packetId);
    }

    public static SimpleChannel getInstance() {
        return INSTANCE;
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        if (INSTANCE != null) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void sendToAll(Object packet) {
        if (INSTANCE != null) {
            INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
        }
    }

    public static void sendToServer(Object packet) {
        if (INSTANCE != null) {
            INSTANCE.sendToServer(packet);
        }
    }

    static {
        packetId = 0;
    }
}

