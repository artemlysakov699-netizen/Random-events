package com.random.events.network;

import com.random.events.RandomEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(
                            RandomEvents.MOD_ID,
                            "main"
                    ),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static boolean registered = false;

    private NetworkHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        CHANNEL.registerMessage(
                0,
                TimerSyncPacket.class,
                TimerSyncPacket::encode,
                TimerSyncPacket::decode,
                TimerSyncPacket::handle
        );

        registered = true;
    }

    public static void sendTimer(ServerPlayer player, int remainingTicks) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(player),
                new TimerSyncPacket(remainingTicks)
        );
    }
}
