
package com.random.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = RandomEvents.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ActiveEventManager {

    private static final Random RANDOM = new Random();

    private static final Map<UUID, List<ActiveEvent>> ACTIVE_EVENTS =
            new HashMap<>();

    private static final Map<UUID, String> LAST_EVENT =
            new HashMap<>();

    private static final double COMBINATION_CHANCE = 0.20;

    private ActiveEventManager() {
    }

    /**
     * Запускает случайное событие для игрока.
     */
    public static void startRandomEvents(ServerPlayer player) {

        UUID playerId = player.getUUID();

        List<EventDefinition> availableEvents =
                EventRegistry.getAll();

        if (availableEvents.isEmpty()) {
            return;
        }

        String lastEventId = LAST_EVENT.get(playerId);

        EventDefinition firstEvent = selectWeighted(
                availableEvents,
                lastEventId
        );

        if (firstEvent == null) {
            return;
        }

        startEvent(player, firstEvent);

        LAST_EVENT.put(playerId, firstEvent.id());

        /*
         * Иногда запускается второе совместимое событие.
         */
        if (firstEvent.canCombine()
                && RANDOM.nextDouble() < COMBINATION_CHANCE) {

            List<EventDefinition> compatibleEvents =
                    availableEvents.stream()
                            .filter(EventDefinition::canCombine)
                            .filter(event ->
                                    !event.id().equals(firstEvent.id())
                            )
                            .toList();

            EventDefinition secondEvent = selectWeighted(
                    compatibleEvents,
                    LAST_EVENT.get(playerId)
            );

            if (secondEvent != null) {
                startEvent(player, secondEvent);
                LAST_EVENT.put(playerId, secondEvent.id());
            }
        }
    }

    /**
     * Запускает конкретное событие.
     */
    public static void startEvent(
            ServerPlayer player,
            EventDefinition definition
    ) {

        UUID playerId = player.getUUID();

        List<ActiveEvent> playerEvents =
                ACTIVE_EVENTS.computeIfAbsent(
                        playerId,
                        id -> new ArrayList<>()
                );

        boolean alreadyActive = playerEvents.stream()
                .anyMatch(active ->
                        active.definition.id().equals(definition.id())
                );

        if (alreadyActive) {
            return;
        }

        ActiveEvent activeEvent = new ActiveEvent(
                definition,
                definition.durationTicks()
        );

        playerEvents.add(activeEvent);

        sendStartMessage(player, definition);

        System.out.println(
                "[Random Events] Started: "
                        + definition.name()
                        + " for "
                        + player.getGameProfile().getName()
        );
    }

    /**
     * Обрабатывает активные события игрока.
     */
    public static void tickPlayer(ServerPlayer player) {

        UUID playerId = player.getUUID();

        List<ActiveEvent> playerEvents =
                ACTIVE_EVENTS.get(playerId);

        if (playerEvents == null || playerEvents.isEmpty()) {
            return;
        }

        Iterator<ActiveEvent> iterator =
                playerEvents.iterator();

        while (iterator.hasNext()) {

            ActiveEvent activeEvent = iterator.next();

            activeEvent.remainingTicks--;

            if (activeEvent.remainingTicks <= 0) {

                sendEndMessage(
                        player,
                        activeEvent.definition
                );

                System.out.println(
                        "[Random Events] Finished: "
                                + activeEvent.definition.name()
                                + " for "
                                + player.getGameProfile().getName()
                );

                iterator.remove();
            }
        }

        if (playerEvents.isEmpty()) {
            ACTIVE_EVENTS.remove(playerId);
        }
    }

    /**
     * Завершает все активные события игрока.
     */
    public static void endAllEvents(ServerPlayer player) {

        UUID playerId = player.getUUID();

        List<ActiveEvent> playerEvents =
                ACTIVE_EVENTS.remove(playerId);

        if (playerEvents == null) {
            return;
        }

        for (ActiveEvent activeEvent : playerEvents) {

            sendEndMessage(
                    player,
                    activeEvent.definition
            );
        }
    }

    /**
     * Возвращает список активных событий игрока.
     */
    public static List<EventDefinition> getActiveEvents(
            UUID playerId
    ) {

        List<ActiveEvent> playerEvents =
                ACTIVE_EVENTS.get(playerId);

        if (playerEvents == null) {
            return List.of();
        }

        return playerEvents.stream()
                .map(active -> active.definition)
                .toList();
    }

    /**
     * Возвращает оставшееся время события.
     */
    public static int getRemainingTicks(
            UUID playerId,
            String eventId
    ) {

        List<ActiveEvent> playerEvents =
                ACTIVE_EVENTS.get(playerId);

        if (playerEvents == null) {
            return 0;
        }

        for (ActiveEvent activeEvent : playerEvents) {

            if (activeEvent.definition.id().equals(eventId)) {
                return Math.max(0, activeEvent.remainingTicks);
            }
        }

        return 0;
    }

    /**
     * Выбирает событие с учётом веса редкости и самого события.
     */
    private static EventDefinition selectWeighted(
            List<EventDefinition> events,
            String excludedId
    ) {

        List<EventDefinition> candidates = events.stream()
                .filter(event ->
                        excludedId == null
                                || !event.id().equals(excludedId)
                )
                .toList();

        if (candidates.isEmpty()) {
            candidates = events;
        }

        if (candidates.isEmpty()) {
            return null;
        }

        long totalWeight = 0;

        for (EventDefinition event : candidates) {

            totalWeight +=
                    (long) event.weight()
                            * event.rarity().getWeight();
        }

        if (totalWeight <= 0) {
            return candidates.get(0);
        }

        long randomValue = RANDOM.nextLong(totalWeight);

        for (EventDefinition event : candidates) {

            long currentWeight =
                    (long) event.weight()
                            * event.rarity().getWeight();

            randomValue -= currentWeight;

            if (randomValue < 0) {
                return event;
            }
        }

        return candidates.get(0);
    }

    /**
     * Уведомление о запуске события.
     */
    private static void sendStartMessage(
            ServerPlayer player,
            EventDefinition definition
    ) {

        Component prefix = Component.literal("[Random Events] ")
                .withStyle(ChatFormatting.GOLD);

        Component eventName = Component.literal(definition.name())
                .withStyle(
                        Style.EMPTY.withColor(
                                TextColor.fromRgb(
                                        definition.rarity().getColor()
                                )
                        )
                );

        Component rarity = Component.literal(
                        " (" + definition.rarity().getDisplayName() + ")"
                )
                .withStyle(ChatFormatting.GRAY);

        player.sendSystemMessage(
                prefix.copy()
                        .append(eventName)
                        .append(rarity)
        );
    }

    /**
     * Уведомление о завершении события.
     */
    private static void sendEndMessage(
            ServerPlayer player,
            EventDefinition definition
    ) {

        Component message = Component.literal(
                        "[Random Events] Событие завершено: "
                )
                .withStyle(ChatFormatting.GRAY)
                .append(
                        Component.literal(definition.name())
                                .withStyle(
                                        Style.EMPTY.withColor(
                                                TextColor.fromRgb(
                                                        definition.rarity().getColor()
                                                )
                                        )
                                )
                );

        player.sendSystemMessage(message);
    }

    /**
     * Обработка активных событий.
     */
    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent.Post event
    ) {

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        tickPlayer(player);
    }

    /**
     * Очистка данных при выходе игрока.
     */
    @SubscribeEvent
    public static void onPlayerLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        endAllEvents(player);
        LAST_EVENT.remove(player.getUUID());
    }

    /**
     * Внутреннее состояние одного активного события.
     */
    private static final class ActiveEvent {

        private final EventDefinition definition;
        private int remainingTicks;

        private ActiveEvent(
                EventDefinition definition,
                int remainingTicks
        ) {

            this.definition = definition;
            this.remainingTicks = remainingTicks;
        }
    }
}
