
package com.random.events.client;

import com.random.events.RandomEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class DenseFogClientHandler {

    private static volatile boolean active = false;

    private DenseFogClientHandler() {
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }

    public static void reset() {
        active = false;
    }

    /**
     * Пользовательский туман не применяется,
     * пока открыто игровое меню.
     *
     * Это предотвращает конфликты отрисовки
     * при отображении интерфейса поверх мира.
     */
    private static boolean shouldRenderCustomFog() {
        Minecraft minecraft = Minecraft.getInstance();

        return active
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null;
    }

    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ClientEvents {

        @SubscribeEvent
        public static void onRenderFog(
                ViewportEvent.RenderFog event
        ) {
            if (!shouldRenderCustomFog()) {
                return;
            }

            if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
                return;
            }

            event.setNearPlaneDistance(0.5F);
            event.setFarPlaneDistance(14.0F);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onComputeFogColor(
                ViewportEvent.ComputeFogColor event
        ) {
            if (!shouldRenderCustomFog()) {
                return;
            }

            event.setRed(0.58F);
            event.setGreen(0.61F);
            event.setBlue(0.64F);
        }
    }
}
