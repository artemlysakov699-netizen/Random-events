
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
    private static final String TIMER_KEY = "RandomEventsRemainingTicks";
    public static final int EVENT_INTERVAL = 1200;
    private static final int DIAGNOSTIC_LOG_INTERVAL = 200;

    public RandomEvents() {
        NetworkHandler.register();
        System.out.println(
                "[Random Events][DIAGNOSTIC] Mod constructor executed."
        );
    }

    @Mod.EventBusSubscriber(
            modid = MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ServerEvents {

        @SubscribeEvent
        public static void onPlayerLogin(
                PlayerEvent.PlayerLoggedInEvent event
        ) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            var data = player.getPersistentData();
            boolean initialized = !data.contains(TIMER_KEY);

            if (initialized) {
                data.putInt(TIMER_KEY, EVENT_INTERVAL);
            }

            int remaining = data.getInt(TIMER_KEY);

            System.out.println(
                    "[Random Events][DIAGNOSTIC] Player login: "
                            + player.getGameProfile().getName()
                            + ", timer=" + remaining
                            + ", initialized=" + initialized
            );

            syncTimer(player);

            player.sendSystemMessage(
                    Component.literal(
                            "§6[Random Events] §fМод активирован! "
                                    + "Следующее событие скоро начнётся."
                    )
            );
        }

        @SubscribeEvent
        public static void onPlayerTick(
                TickEvent.PlayerTickEvent.Post event
        ) {
            if (!(event.player instanceof ServerPlayer player)) {
                return;
            }

            var data = player.getPersistentData();

            if (!data.contains(TIMER_KEY)) {
                System.out.println(
                        "[Random Events][DIAGNOSTIC] Timer key missing. Initializing."
                );
                data.putInt(TIMER_KEY, EVENT_INTERVAL);
            }

            int remaining = data.getInt(TIMER_KEY);

            if (remaining > 0) {
                remaining--;
            }

            if (remaining <= 0) {
                System.out.println(
                        "[Random Events][DIAGNOSTIC] TIMER REACHED ZERO! "
                                + "Calling startRandomEvents()."
                );

                ActiveEventManager.startRandomEvents(player);

                remaining = EVENT_INTERVAL;

                System.out.println(
                        "[Random Events][DIAGNOSTIC] Event selection returned. "
                                + "Timer reset to " + EVENT_INTERVAL
                );
            }

            data.putInt(TIMER_KEY, remaining);

            if (player.tickCount % DIAGNOSTIC_LOG_INTERVAL == 0) {
                System.out.println(
                        "[Random Events][DIAGNOSTIC] Server tick working. "
                                + "tickCount=" + player.tickCount
                                + ", remainingTicks=" + remaining
                );
            }

            if (player.tickCount % 20 == 0) {
                syncTimer(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(
                PlayerEvent.Clone event
        ) {
            var oldData = event.getOriginal().getPersistentData();
            var newData = event.getEntity().getPersistentData();

            if (oldData.contains(TIMER_KEY)) {
                newData.putInt(
                        TIMER_KEY,
                        oldData.getInt(TIMER_KEY)
                );
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(
                PlayerEvent.PlayerRespawnEvent event
        ) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncTimer(player);
            }
        }

        @SubscribeEvent
        public static void onDimensionChange(
                PlayerEvent.PlayerChangedDimensionEvent event
        ) {
            if (event.getEntity() instanceof ServerPlayer player) {
                syncTimer(player);
            }
        }

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
