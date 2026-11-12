
package com.random.events;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class TimedTraderHandler {

    private static final String END_TIME_KEY =
            "RandomEventsMerchantEndTime";

    private static final long LIFETIME_TICKS =
            5L * 60L * 20L;

    private TimedTraderHandler() {
    }

    /**
     * Назначает торговцу независимое время жизни — 5 минут.
     */
    public static void setLifetime(WanderingTrader trader) {
        if (!(trader.level() instanceof ServerLevel level)) {
            return;
        }

        long endTime = level.getGameTime() + LIFETIME_TICKS;

        trader.getPersistentData().putLong(
                END_TIME_KEY,
                endTime
        );

        trader.setDespawnDelay((int) LIFETIME_TICKS);
        trader.setPersistenceRequired();
    }

    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class TraderEvents {

        @SubscribeEvent
        public static void onTraderTick(
                LivingEvent.LivingTickEvent event
        ) {
            if (!(event.getEntity() instanceof WanderingTrader trader)) {
                return;
            }

            if (trader.level().isClientSide()) {
                return;
            }

            if (!(trader.level() instanceof ServerLevel level)) {
                return;
            }

            var data = trader.getPersistentData();

            if (!data.contains(END_TIME_KEY)) {
                return;
            }

            long endTime = data.getLong(END_TIME_KEY);

            if (level.getGameTime() >= endTime) {
                removeTrader(trader, level);
            }
        }

        private static void removeTrader(
                WanderingTrader trader,
                ServerLevel level
        ) {
            if (trader.isRemoved()) {
                return;
            }

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    trader.getX(),
                    trader.getY() + 1.0,
                    trader.getZ(),
                    25,
                    0.6,
                    0.8,
                    0.6,
                    0.08
            );

            trader.discard();
        }
    }
}
