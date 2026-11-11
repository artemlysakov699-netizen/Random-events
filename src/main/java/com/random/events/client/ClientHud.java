package com.random.events.client;

import com.random.events.RandomEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = RandomEvents.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class ClientHud {

    private ClientHud() {
    }

    @SubscribeEvent
    public static void addHudLayer(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                ResourceLocation.fromNamespaceAndPath(
                        RandomEvents.MOD_ID,
                        "event_timer"
                ),
                (guiGraphics, deltaTracker) -> {
                    Minecraft minecraft = Minecraft.getInstance();

                    if (minecraft.player == null || minecraft.level == null) {
                        return;
                    }

                    int ticks = ClientTimer.getRemainingTicks();

                    if (ticks < 0) {
                        return;
                    }

                    int seconds = (int) Math.ceil(ticks / 20.0);
                    int minutes = seconds / 60;
                    int remainingSeconds = seconds % 60;

                    String timer = String.format(
                            "Следующее событие: %02d:%02d",
                            minutes,
                            remainingSeconds
                    );

                    guiGraphics.drawString(
                            minecraft.font,
                            Component.literal(timer),
                            8,
                            8,
                            0xFFFFFF,
                            true
                    );
                }
        );
    }
}
