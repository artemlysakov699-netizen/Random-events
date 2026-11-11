
package com.random.events.network;

import net.minecraft.network.FriendlyByteBuf;

public class TimerSyncPacket {

    private final int remainingTicks;

    public TimerSyncPacket(int remainingTicks) {
        this.remainingTicks = Math.max(0, remainingTicks);
    }

    public int getRemainingTicks() {
        return remainingTicks;
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
}
