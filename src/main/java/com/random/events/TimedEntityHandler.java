
package com.random.events;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class TimedEntityHandler {

    private static final String LIFETIME_KEY =
            "RandomEventsIronDefenderLifetime";

    private static final int DEFENDER_LIFETIME_TICKS =
            60 * 20;

    private TimedEntityHandler() {
    }

    /**
     * Устанавливает железному защитнику время жизни — 60 секунд.
     * Таймер сохраняется в данных самой сущности.
     */
    public static void setDefenderLifetime(IronGolem golem) {
        golem.getPersistentData().putInt(
                LIFETIME_KEY,
                DEFENDER_LIFETIME_TICKS
        );
    }

    /**
     * Проверяет таймер каждого живого существа.
     * Обрабатываются только големы с нашим специальным тегом.
     */
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

            var data = golem.getPersistentData();

            if (!data.contains(LIFETIME_KEY)) {
                return;
            }

            if (!(golem.level() instanceof ServerLevel level)) {
                return;
            }

            int remaining = data.getInt(LIFETIME_KEY);

            if (remaining <= 0) {
                removeDefender(golem, level);
                return;
            }

            remaining--;

            data.putInt(LIFETIME_KEY, remaining);

            if (remaining <= 0) {
                removeDefender(golem, level);
            }
        }

        private static void removeDefender(
                IronGolem golem,
                ServerLevel level
        ) {
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
