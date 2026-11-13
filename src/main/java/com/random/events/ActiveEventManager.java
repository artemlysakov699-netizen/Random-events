
package com.random.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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

public final class ActiveEventManager {

    private static final Map<UUID, List<ActiveEvent>> ACTIVE_EVENTS =
            new HashMap<>();

    private static final Map<UUID, String> LAST_EVENT =
            new HashMap<>();

    private static final Random RANDOM = new Random();

    private static final double COMBINATION_CHANCE = 0.20;

    private ActiveEventManager() {
    }

    // =====================================================
    // СЛУЧАЙНЫЙ ВЫБОР СОБЫТИЙ
    // =====================================================

    public static void startRandomEvents(ServerPlayer player) {
        List<EventDefinition> allEvents = EventRegistry.getAll();

        if (allEvents.isEmpty()) {
            return;
        }

        UUID playerId = player.getUUID();
        String lastEvent = LAST_EVENT.get(playerId);

        EventDefinition first = selectWeighted(
                allEvents,
                lastEvent
        );

        if (first == null) {
            return;
        }

        boolean firstStarted = startEvent(player, first);

        if (!firstStarted) {
            return;
        }

        LAST_EVENT.put(playerId, first.id());

        // Второе событие возможно только после успешного
        // запуска первого события.
        if (first.canCombine()
                && RANDOM.nextDouble() < COMBINATION_CHANCE) {

            List<EventDefinition> compatible = new ArrayList<>();

            for (EventDefinition definition : allEvents) {
                if (!definition.id().equals(first.id())
                        && definition.canCombine()) {
                    compatible.add(definition);
                }
            }

            EventDefinition second = selectWeighted(
                    compatible,
                    first.id()
            );

            if (second != null && startEvent(player, second)) {
                LAST_EVENT.put(playerId, second.id());
            }
        }
    }

    // =====================================================
    // ЗАПУСК СОБЫТИЯ ПО ID
    // =====================================================

    public static boolean startEventById(
            ServerPlayer player,
            String eventId
    ) {
        EventDefinition definition = EventRegistry
                .getById(eventId)
                .orElse(null);

        if (definition == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "§c[Random Events] Такое событие не найдено."
                    )
            );

            return false;
        }

        if (isEventActive(player, eventId)) {
            player.sendSystemMessage(
                    Component.literal(
                            "§e[Random Events] Это событие уже активно."
                    )
            );

            return false;
        }

        boolean started = startEvent(player, definition);

        if (started) {
            LAST_EVENT.put(player.getUUID(), definition.id());
        }

        return started;
    }

    // =====================================================
    // ПРОВЕРКА АКТИВНОСТИ СОБЫТИЯ
    // =====================================================

    private static boolean isEventActive(
            ServerPlayer player,
            String eventId
    ) {
        List<ActiveEvent> events = ACTIVE_EVENTS.get(
                player.getUUID()
        );

        if (events == null) {
            return false;
        }

        for (ActiveEvent active : events) {
            if (active.definition.id().equals(eventId)) {
                return true;
            }
        }

        return false;
    }

    // =====================================================
    // ЗАПУСК СОБЫТИЯ
    // =====================================================

    private static boolean startEvent(
            ServerPlayer player,
            EventDefinition definition
    ) {
        UUID playerId = player.getUUID();

        List<ActiveEvent> events = ACTIVE_EVENTS.computeIfAbsent(
                playerId,
                ignored -> new ArrayList<>()
        );

        if (isEventActive(player, definition.id())) {
            return false;
        }

        try {
            if (isNewEvent(definition)) {
                NewEventHandler.start(player, definition);
            } else {
                EventEffectHandler.start(player, definition);
            }

            events.add(new ActiveEvent(
                    definition,
                    definition.durationTicks()
            ));

            sendStartMessage(player, definition);

            return true;

        } catch (RuntimeException exception) {

            try {
                if (isNewEvent(definition)) {
                    NewEventHandler.end(player, definition);
                } else {
                    EventEffectHandler.end(player, definition);
                }

            } catch (RuntimeException cleanupException) {
                cleanupException.printStackTrace();
            }

            System.err.println(
                    "[Random Events] Не удалось запустить событие: "
                            + definition.id()
            );

            exception.printStackTrace();

            player.sendSystemMessage(
                    Component.literal(
                            "§c[Random Events] Не удалось запустить событие."
                    )
            );

            if (events.isEmpty()) {
                ACTIVE_EVENTS.remove(playerId);
            }

            return false;
        }
    }

    // =====================================================
    // ОБНОВЛЕНИЕ СОБЫТИЙ ИГРОКА
    // =====================================================

    private static void tickPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();

        List<ActiveEvent> events = ACTIVE_EVENTS.get(playerId);

        if (events == null || events.isEmpty()) {
            return;
        }

        Iterator<ActiveEvent> iterator = events.iterator();

        while (iterator.hasNext()) {

            ActiveEvent active = iterator.next();
            EventDefinition definition = active.definition;

            try {

                // Сначала обновляем механику события.
                if (isNewEvent(definition)) {
                    NewEventHandler.tick(player, definition);
                } else {
                    EventEffectHandler.tick(player, definition);
                }

                boolean finished = false;

                // Отсчёт времени.
                if (definition.hasTimeLimit()) {
                    active.remainingTicks--;

                    finished = active.remainingTicks <= 0;
                }

                // Проверка событий, которые завершаются
                // после смерти существ или выполнения условия.
                if (!finished && isNewEvent(definition)) {
                    finished = NewEventHandler.isFinished(
                            player,
                            definition
                    );
                }

                if (finished) {

                    endEvent(player, definition);

                    sendEndMessage(player, definition);

                    iterator.remove();
                }

            } catch (RuntimeException exception) {

                System.err.println(
                        "[Random Events] Ошибка во время события: "
                                + definition.id()
                );

                exception.printStackTrace();

                try {
                    endEvent(player, definition);

                } catch (RuntimeException cleanupException) {
                    cleanupException.printStackTrace();
                }

                iterator.remove();

                player.sendSystemMessage(
                        Component.literal(
                                "§c[Random Events] Событие остановлено из-за ошибки."
                        )
                );
            }
        }

        if (events.isEmpty()) {
            ACTIVE_EVENTS.remove(playerId);
        }
    }

    // =====================================================
    // ЗАВЕРШЕНИЕ ОДНОГО СОБЫТИЯ
    // =====================================================

    private static void endEvent(
            ServerPlayer player,
            EventDefinition definition
    ) {
        if (isNewEvent(definition)) {
            NewEventHandler.end(player, definition);
        } else {
            EventEffectHandler.end(player, definition);
        }
    }

    // =====================================================
    // ОПРЕДЕЛЕНИЕ НОВЫХ СОБЫТИЙ
    // =====================================================

    private static boolean isNewEvent(
            EventDefinition definition
    ) {
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

    // =====================================================
    // ПРИНУДИТЕЛЬНОЕ ЗАВЕРШЕНИЕ ВСЕХ СОБЫТИЙ
    // =====================================================

    public static void endAllEvents(ServerPlayer player) {
        UUID playerId = player.getUUID();

        List<ActiveEvent> events = ACTIVE_EVENTS.remove(playerId);

        if (events == null || events.isEmpty()) {
            return;
        }

        for (ActiveEvent active : events) {

            try {
                endEvent(player, active.definition);

                sendEndMessage(
                        player,
                        active.definition
                );

            } catch (RuntimeException exception) {

                System.err.println(
                        "[Random Events] Ошибка завершения события: "
                                + active.definition.id()
                );

                exception.printStackTrace();
            }
        }
    }

    // =====================================================
    // ОЧИСТКА ПРИ ВЫХОДЕ ИГРОКА
    // =====================================================

    private static void handlePlayerLogout(ServerPlayer player) {
        UUID playerId = player.getUUID();

        List<ActiveEvent> events = ACTIVE_EVENTS.remove(playerId);

        if (events != null) {

            for (ActiveEvent active : events) {

                EventDefinition definition = active.definition;

                /*
                 * Старые события корректно завершаются.
                 */
                if (!isNewEvent(definition)) {
                    try {
                        EventEffectHandler.end(
                                player,
                                definition
                        );

                    } catch (RuntimeException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }

        /*
         * Новые события не завершаем принудительно.
         *
         * Это позволяет оставить их существ в мире.
         * Специальные временные объекты, например проклятый
         * сундук, удаляются в cleanupPlayer().
         */
        EventEffectHandler.cleanupPlayer(player);
        NewEventHandler.cleanupPlayer(player);

        LAST_EVENT.remove(playerId);
    }

    // =====================================================
    // ПОЛУЧЕНИЕ АКТИВНЫХ СОБЫТИЙ
    // =====================================================

    public static List<EventDefinition> getActiveEvents(
            ServerPlayer player
    ) {
        List<ActiveEvent> events = ACTIVE_EVENTS.get(
                player.getUUID()
        );

        if (events == null) {
            return List.of();
        }

        List<EventDefinition> result = new ArrayList<>();

        for (ActiveEvent active : events) {
            result.add(active.definition);
        }

        return result;
    }

    // =====================================================
    // ОСТАВШЕЕСЯ ВРЕМЯ
    // =====================================================

    public static int getRemainingTicks(
            ServerPlayer player,
            String eventId
    ) {
        List<ActiveEvent> events = ACTIVE_EVENTS.get(
                player.getUUID()
        );

        if (events == null) {
            return 0;
        }

        for (ActiveEvent active : events) {
            if (active.definition.id().equals(eventId)) {
                return active.remainingTicks;
            }
        }

        return 0;
    }

    // =====================================================
    // ВЗВЕШЕННЫЙ ВЫБОР СОБЫТИЯ
    // =====================================================

    private static EventDefinition selectWeighted(
            List<EventDefinition> events,
            String excludedId
    ) {
        long totalWeight = 0;

        for (EventDefinition definition : events) {

            if (definition.id().equals(excludedId)) {
                continue;
            }

            totalWeight += (long) definition.weight()
                    * definition.rarity().getWeight();
        }

        if (totalWeight <= 0) {
            return null;
        }

        long randomValue = RANDOM.nextLong(totalWeight);

        for (EventDefinition definition : events) {

            if (definition.id().equals(excludedId)) {
                continue;
            }

            long weight = (long) definition.weight()
                    * definition.rarity().getWeight();

            randomValue -= weight;

            if (randomValue < 0) {
                return definition;
            }
        }

        return null;
    }

    // =====================================================
    // СООБЩЕНИЕ О НАЧАЛЕ
    // =====================================================

    private static void sendStartMessage(
            ServerPlayer player,
            EventDefinition definition
    ) {
        Component prefix = Component.literal("[Random Events] ")
                .withStyle(ChatFormatting.GOLD);

        Component eventName = Component.literal(definition.name())
                .withStyle(
                        Style.EMPTY.withColor(
                                definition.rarity().getColor()
                        )
                );

        Component rarity = Component.literal(
                " (" + definition.rarity().getDisplayName() + ")"
        ).withStyle(ChatFormatting.GRAY);

        player.sendSystemMessage(
                prefix.copy()
                        .append(eventName)
                        .append(rarity)
        );
    }

    // =====================================================
    // СООБЩЕНИЕ О ЗАВЕРШЕНИИ
    // =====================================================

    private static void sendEndMessage(
            ServerPlayer player,
            EventDefinition definition
    ) {
        Component prefix = Component.literal(
                "[Random Events] Событие завершено: "
        ).withStyle(ChatFormatting.GOLD);

        Component eventName = Component.literal(definition.name())
                .withStyle(
                        Style.EMPTY.withColor(
                                definition.rarity().getColor()
                        )
                );

        player.sendSystemMessage(
                prefix.copy().append(eventName)
        );
    }

    // =====================================================
    // ДАННЫЕ АКТИВНОГО СОБЫТИЯ
    // =====================================================

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

    // =====================================================
    // СОБЫТИЯ FORGE
    // =====================================================

    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ServerEvents {

        @SubscribeEvent
        public static void onPlayerTick(
                TickEvent.PlayerTickEvent.Post event
        ) {
            if (!(event.player instanceof ServerPlayer player)) {
                return;
            }

            tickPlayer(player);
        }

        @SubscribeEvent
        public static void onPlayerLogout(
                PlayerEvent.PlayerLoggedOutEvent event
        ) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            handlePlayerLogout(player);
        }
    }
}
