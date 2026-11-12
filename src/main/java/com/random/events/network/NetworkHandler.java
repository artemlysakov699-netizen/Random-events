package com.random.events.network;

import com.random.events.RandomEvents;
import com.random.events.client.ClientTimer;
import com.random.events.client.DenseFogClientHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class NetworkHandler {

    public static final SimpleChannel CHANNEL =
            ChannelBuilder.named(
                    ResourceLocation.fromNamespaceAndPath(
                            RandomEvents.MOD_ID,
                            "main"
                    )
            )
            .networkProtocolVersion(1)
            .simpleChannel();

    private static boolean registered = false;

    private NetworkHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        CHANNEL.messageBuilder(TimerSyncPacket.class, 0)
                .encoder(TimerSyncPacket::encode)
                .decoder(TimerSyncPacket::decode)
                .consumerMainThread((packet, context) ->
                        DistExecutor.unsafeRunWhenOn(
                                Dist.CLIENT,
                                () -> () -> ClientTimer.setRemainingTicks(
                                        packet.getRemainingTicks()
                                )
                        )
                )
                .add();

        CHANNEL.messageBuilder(DenseFogSyncPacket.class, 1)
                .encoder(DenseFogSyncPacket::encode)
                .decoder(DenseFogSyncPacket::decode)
                .consumerMainThread((packet, context) ->
                        DistExecutor.unsafeRunWhenOn(
                                Dist.CLIENT,
                                () -> () -> DenseFogClientHandler.setActive(
                                        packet.isActive()
                                )
                        )
                )
                .add();

        registered = true;
    }

    public static void sendTimer(
            ServerPlayer player,
            int remainingTicks
    ) {
        CHANNEL.send(
                new TimerSyncPacket(remainingTicks),
                PacketDistributor.PLAYER.with(player)
        );
    }

    public static void sendDenseFog(
            ServerPlayer player,
            boolean active
    ) {
        CHANNEL.send(
                new DenseFogSyncPacket(active),
                PacketDistributor.PLAYER.with(player)
        );
    }
}
