package com.random.events.item;

import com.random.events.RandomEvents;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    RandomEvents.MOD_ID
            );

    public static final RegistryObject<Item> EVENT_REMOTE =
            ITEMS.register(
                    "event_remote",
                    () -> new EventRemoteItem(
                            new Item.Properties().stacksTo(1)
                    )
            );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
