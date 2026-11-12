
package com.random.events;

import com.random.events.network.NetworkHandler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(RandomEvents.MOD_ID)
public class RandomEvents {

    public static final String MOD_ID = "randomevents";

    private static final String TIMER_KEY =
            "RandomEventsRemainingTicks";

    /*
     * 1200 тиков = 60 секунд.
     */
    public static final int EVENT_INTERVAL = 1200;

    public RandomEvents() {

        NetworkHandler.register();

        System.out.println(
                "[Random Events] Version 1.0.0 initialized!"
        );
    }

    @Mod.EventBusSubscriber(
            modid = MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ServerEvents {

        /**
         * Инициализация индивидуального таймера игрока.
         */
        @SubscribeEvent
        public static void onPlayerLogin(
                PlayerEvent.PlayerLoggedInEvent event
        ) {

            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            var data = player.getPersistentData();

            if (!data.contains(TIMER_KEY)) {
                data.putInt(TIMER_KEY, EVENT_INTERVAL);
            }

            syncTimer(player);

            player.sendSystemMessage(
                    Component.literal(
                            "§6[Random Events] §fМод активирован! "
                                    + "Следующее событие скоро начнётся."
                    )
            );
        }

        /**
         * Главный индивидуальный таймер событий.
         */
        @SubscribeEvent
        public static void onPlayerTick(
                TickEvent.PlayerTickEvent.Post event
        ) {

            if (!(event.player instanceof ServerPlayer player)) {
                return;
            }

            var data = player.getPersistentData();

            if (!data.contains(TIMER_KEY)) {
                data.putInt(TIMER_KEY, EVENT_INTERVAL);
            }

            int remaining = data.getInt(TIMER_KEY);

            if (remaining > 0) {
                remaining--;
            }

            /*
             * Когда таймер достигает нуля,
             * запускаем случайное событие.
             */
            if (remaining <= 0) {

                ActiveEventManager.startRandomEvents(player);

                remaining = EVENT_INTERVAL;
            }

            data.putInt(TIMER_KEY, remaining);

            /*
             * Синхронизация таймера с клиентом
             * один раз в секунду.
             */
            if (player.tickCount % 20 == 0) {
                syncTimer(player);
            }
        }

        /**
         * Сохранение таймера после смерти.
         */
        @SubscribeEvent
        public static void onPlayerClone(
                PlayerEvent.Clone event
        ) {

            var oldData =
                    event.getOriginal().getPersistentData();

            var newData =
                    event.getEntity().getPersistentData();

            if (oldData.contains(TIMER_KEY)) {
                newData.putInt(
                        TIMER_KEY,
                        oldData.getInt(TIMER_KEY)
                );
            }
        }

        /**
         * Восстановление HUD после возрождения.
         */
        @SubscribeEvent
        public static void onPlayerRespawn(
                PlayerEvent.PlayerRespawnEvent event
        ) {

            if (event.getEntity() instanceof ServerPlayer player) {
                syncTimer(player);
            }
        }

        /**
         * Синхронизация после смены измерения.
         */
        @SubscribeEvent
        public static void onDimensionChange(
                PlayerEvent.PlayerChangedDimensionEvent event
        ) {

            if (event.getEntity() instanceof ServerPlayer player) {
                syncTimer(player);
            }
        }

        /**
         * Отправка оставшегося времени клиенту.
         */
        private static void syncTimer(ServerPlayer player) {

            int remaining = player.getPersistentData()
                    .getInt(TIMER_KEY);

            NetworkHandler.sendTimer(
                    player,
                    Math.max(0, remaining)
            );
        }
    }
}
