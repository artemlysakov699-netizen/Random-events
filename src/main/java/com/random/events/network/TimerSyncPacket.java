package com.random.events.network;

import com.random.events.client.ClientTimer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TimerSyncPacket {

    private final int remainingTicks;

    public TimerSyncPacket(int remainingTicks) {
        this.remainingTicks = Math.max(0, remainingTicks);
    }

    public static void encode(
            TimerSyncPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeVarInt(packet.remainingTicks);
    }

    public static TimerSyncPacket decode(FriendlyByteBuf buffer) {
        return new TimerSyncPacket(buffer.readVarInt());
    }

    public static void handle(
            TimerSyncPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> ClientTimer.setRemainingTicks(
                                packet.remainingTicks
                        )
                )
        );

        context.setPacketHandled(true);
    }
}
