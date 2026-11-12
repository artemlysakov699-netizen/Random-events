package com.random.events.network;

import net.minecraft.network.FriendlyByteBuf;

public class DenseFogSyncPacket {

    private final boolean active;

    public DenseFogSyncPacket(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public static void encode(
            DenseFogSyncPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(packet.active);
    }

    public static DenseFogSyncPacket decode(
            FriendlyByteBuf buffer
    ) {
        return new DenseFogSyncPacket(buffer.readBoolean());
    }
}
