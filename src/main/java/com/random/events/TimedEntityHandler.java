
package com.random.events;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.IronGolem;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class TimedEntityHandler {

    private static final String END_TIME_KEY =
            "RandomEventsIronDefenderEndTime";

    private static final long DEFENDER_LIFETIME_TICKS =
            60L * 20L;

    private TimedEntityHandler() {
    }

    /**
     * Устанавливает время окончания жизни железного защитника.
     *
     * Используется игровое время измерения, поэтому если чанк
     * временно выгрузится, оставшееся время не начнётся заново.
     */
    public static void setDefenderLifetime(IronGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }

        long endTime = level.getGameTime()
                + DEFENDER_LIFETIME_TICKS;

        golem.getPersistentData().putLong(
                END_TIME_KEY,
                endTime
        );
    }

    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class EntityEvents {

        @SubscribeEvent
        public static void onLivingTick(
                LivingEvent.LivingTickEvent event
        ) {
            if (!(event.getEntity() instanceof IronGolem golem)) {
                return;
            }

            if (golem.level().isClientSide()) {
                return;
            }

            if (!(golem.level() instanceof ServerLevel level)) {
                return;
            }

            var data = golem.getPersistentData();

            if (!data.contains(END_TIME_KEY)) {
                return;
            }

            long endTime = data.getLong(END_TIME_KEY);
            long currentTime = level.getGameTime();

            if (currentTime >= endTime) {
                removeDefender(golem, level);
            }
        }

        private static void removeDefender(
                IronGolem golem,
                ServerLevel level
        ) {
            if (golem.isRemoved()) {
                return;
            }

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    golem.getX(),
                    golem.getY() + 1.0,
                    golem.getZ(),
                    35,
                    0.8,
                    1.0,
                    0.8,
                    0.12
            );

            level.sendParticles(
                    ParticleTypes.CRIT,
                    golem.getX(),
                    golem.getY() + 1.0,
                    golem.getZ(),
                    20,
                    0.6,
                    0.8,
                    0.6,
                    0.1
            );

            golem.discard();
        }
    }
}
