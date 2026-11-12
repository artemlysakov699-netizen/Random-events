package com.random.events;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.WanderingTrader;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

public final class TimedTraderHandler {

    private static final String END_TIME_KEY =
            "RandomEventsMerchantEndTime";

    private static final String OWNER_UUID_KEY =
            "RandomEventsMerchantOwner";

    private static final String WARNING_TRIGGERED_KEY =
            "RandomEventsMerchantWarningTriggered";

    private static final String WARNING_SENT_KEY =
            "RandomEventsMerchantWarningSent";

    private static final long LIFETIME_TICKS =
            5L * 60L * 20L;

    private static final long WARNING_TIME_TICKS =
            60L * 20L;

    private TimedTraderHandler() {
    }

    /**
     * Назначает торговцу независимый таймер на пять минут
     * и сохраняет UUID игрока, вызвавшего событие.
     */
    public static void setLifetime(
            WanderingTrader trader,
            ServerPlayer owner
    ) {
        if (!(trader.level() instanceof ServerLevel level)) {
            return;
        }

        var data = trader.getPersistentData();

        long endTime = level.getGameTime() + LIFETIME_TICKS;

        data.putLong(END_TIME_KEY, endTime);
        data.putUUID(OWNER_UUID_KEY, owner.getUUID());
        data.putBoolean(WARNING_TRIGGERED_KEY, false);
        data.putBoolean(WARNING_SENT_KEY, false);

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
            long currentTime = level.getGameTime();

            // Сначала проверяем исчезновение торговца.
            if (currentTime >= endTime) {
                removeTrader(trader, level);
                return;
            }

            long remainingTicks = endTime - currentTime;

            // Когда остаётся одна минута, активируем предупреждение.
            if (remainingTicks <= WARNING_TIME_TICKS
                    && !data.getBoolean(WARNING_TRIGGERED_KEY)) {

                data.putBoolean(WARNING_TRIGGERED_KEY, true);
            }

            // Если предупреждение ещё не отправлено,
            // пытаемся найти игрока-владельца.
            // Это также позволяет отправить сообщение после его возвращения.
            if (data.getBoolean(WARNING_TRIGGERED_KEY)
                    && !data.getBoolean(WARNING_SENT_KEY)) {

                sendWarningIfPossible(trader, level);
            }
        }

        private static void sendWarningIfPossible(
                WanderingTrader trader,
                ServerLevel level
        ) {
            var data = trader.getPersistentData();

            if (!data.hasUUID(OWNER_UUID_KEY)) {
                return;
            }

            UUID ownerId = data.getUUID(OWNER_UUID_KEY);

            ServerPlayer owner = level.getServer()
                    .getPlayerList()
                    .getPlayer(ownerId);

            // Игрок пока не в сети — предупреждение остаётся ожидающим.
            if (owner == null) {
                return;
            }

            owner.sendSystemMessage(
                    Component.literal(
                            "§6[Random Events] §eДо исчезновения "
                                    + "загадочного торговца осталась 1 минута! "
                                    + "Успей совершить покупки!"
                    )
            );

            data.putBoolean(WARNING_SENT_KEY, true);
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
