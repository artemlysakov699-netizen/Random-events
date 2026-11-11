package com.random.events.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class ClientTimer {

    private static int remainingTicks = -1;
    private static long lastUpdateGameTime = -1;
    private static ClientLevel syncedLevel = null;

    private ClientTimer() {
    }

    public static void setRemainingTicks(int ticks) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            remainingTicks = Math.max(0, ticks);
            lastUpdateGameTime = -1;
            syncedLevel = null;
            return;
        }

        remainingTicks = Math.max(0, ticks);
        lastUpdateGameTime = minecraft.level.getGameTime();
        syncedLevel = minecraft.level;
    }

    public static int getRemainingTicks() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return -1;
        }

        if (syncedLevel != minecraft.level || lastUpdateGameTime < 0) {
            return -1;
        }

        long elapsed = minecraft.level.getGameTime() - lastUpdateGameTime;

        if (elapsed < 0) {
            return -1;
        }

        long result = remainingTicks - elapsed;

        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, result));
    }

    public static void reset() {
        remainingTicks = -1;
        lastUpdateGameTime = -1;
        syncedLevel = null;
    }
}
