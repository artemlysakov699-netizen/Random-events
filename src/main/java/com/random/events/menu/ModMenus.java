package com.random.events.menu;

import com.random.events.RandomEvents;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    RandomEvents.MOD_ID
            );

    public static final RegistryObject<MenuType<EventSelectorMenu>>
            EVENT_SELECTOR = MENUS.register(
                    "event_selector",
                    () -> new MenuType<>(
                            EventSelectorMenu::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
