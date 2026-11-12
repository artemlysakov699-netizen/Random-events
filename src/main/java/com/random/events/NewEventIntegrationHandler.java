
package com.random.events;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = RandomEvents.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class NewEventIntegrationHandler {

    private static final Map<UUID, Set<String>> STARTED_EVENTS =
            new HashMap<>();

    private NewEventIntegrationHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();

        Set<String> started = STARTED_EVENTS.computeIfAbsent(
                playerId,
                ignored -> new HashSet<>()
        );

        Set<String> currentlyActive = new HashSet<>();

        for (EventDefinition definition :
                ActiveEventManager.getActiveEvents(player)) {

            if (!isNewEvent(definition)) {
                continue;
            }

            String eventId = definition.id();
            currentlyActive.add(eventId);

            if (started.add(eventId)) {
                NewEventHandler.start(player, definition);
            }

            NewEventHandler.tick(player, definition);
        }

        Set<String> finished = new HashSet<>(started);
        finished.removeAll(currentlyActive);

        for (String eventId : finished) {
            EventRegistry.getById(eventId).ifPresent(
                    definition -> NewEventHandler.end(player, definition)
            );

            started.remove(eventId);
        }

        if (started.isEmpty()) {
            STARTED_EVENTS.remove(playerId);
        }
    }

    private static boolean isNewEvent(EventDefinition definition) {
        return switch (definition.type()) {
            case PILLAGER_AMBUSH,
                 RAVAGER_WRATH,
                 SLIME_EPIDEMIC,
                 CURSED_CHEST,
                 IRON_DEFENDER,
                 MYSTERIOUS_MERCHANT,
                 EXPERIENCE_RAIN,
                 GOLDEN_BLESSING -> true;

            default -> false;
        };
    }

    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        STARTED_EVENTS.remove(player.getUUID());
        NewEventHandler.cleanupPlayer(player);
    }
}
