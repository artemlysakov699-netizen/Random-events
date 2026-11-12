package com.random.events.client;

import com.random.events.RandomEvents;

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
            if (!active) {
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
            if (!active) {
                return;
            }

            event.setRed(0.58F);
            event.setGreen(0.61F);
            event.setBlue(0.64F);
        }
    }
}
