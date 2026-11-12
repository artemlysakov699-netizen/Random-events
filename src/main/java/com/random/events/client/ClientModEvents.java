package com.random.events.client;

import com.random.events.RandomEvents;
import com.random.events.menu.ModMenus;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = RandomEvents.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(
            RegisterMenuScreensEvent event
    ) {
        event.register(
                ModMenus.EVENT_SELECTOR.get(),
                EventSelectorScreen::new
        );
    }
}
