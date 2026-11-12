
package com.random.events;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public final class GoldenBlessingHandler {

    private static final String END_TIME_KEY =
            "RandomEventsGoldenBlessingEndTime";

    private static final String ACTIVE_KEY =
            "RandomEventsGoldenBlessingActive";

    private static final String EVENT_ID =
            "golden_blessing";

    private static final int DURATION_SECONDS = 60;
    private static final int AMPLIFIER = 255;

    private static final List<Holder<MobEffect>> EFFECTS = List.of(
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DIG_SPEED,
            MobEffects.DAMAGE_BOOST,
            MobEffects.JUMP,
            MobEffects.REGENERATION,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.WATER_BREATHING,
            MobEffects.INVISIBILITY,
            MobEffects.NIGHT_VISION,
            MobEffects.HEALTH_BOOST,
            MobEffects.ABSORPTION,
            MobEffects.SATURATION,
            MobEffects.LUCK,
            MobEffects.SLOW_FALLING,
            MobEffects.CONDUIT_POWER,
            MobEffects.DOLPHINS_GRACE,
            MobEffects.HERO_OF_THE_VILLAGE
    );

    private GoldenBlessingHandler() {
    }

    // =====================================================
    // ЗАПУСК ЗОЛОТОГО БЛАГОСЛОВЕНИЯ
    // =====================================================

    public static void start(ServerPlayer player) {
        long now = getGameTime(player);

        player.getPersistentData().putLong(
                END_TIME_KEY,
                now + DURATION_SECONDS * 20L
        );

        player.getPersistentData().putBoolean(
                ACTIVE_KEY,
                true
        );

        applyEffects(player);

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Random Events] §eЗолотое благословение!"
                )
        );
    }

    // =====================================================
    // ОБНОВЛЕНИЕ СОБЫТИЯ
    // =====================================================

    public static void tick(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }

        long remaining = getRemainingTicks(player);

        if (remaining <= 0) {
            finish(player);
            return;
        }

        if (player.tickCount % 20 == 0) {
            applyEffects(player);
        }
    }

    // =====================================================
    // ВОССТАНОВЛЕНИЕ ПОСЛЕ ВХОДА И ВОЗРОЖДЕНИЯ
    // =====================================================

    public static void restore(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }

        if (getRemainingTicks(player) <= 0) {
            finish(player);
            return;
        }

        applyEffects(player);
    }

    // =====================================================
    // ПЕРЕНОС ДАННЫХ ПРИ СМЕРТИ
    // =====================================================

    public static void copyPlayerData(
            net.minecraft.world.entity.player.Player original,
            net.minecraft.world.entity.player.Player replacement
    ) {
        var oldData = original.getPersistentData();
        var newData = replacement.getPersistentData();

        if (oldData.contains(ACTIVE_KEY)) {
            newData.putBoolean(
                    ACTIVE_KEY,
                    oldData.getBoolean(ACTIVE_KEY)
            );
        }

        if (oldData.contains(END_TIME_KEY)) {
            newData.putLong(
                    END_TIME_KEY,
                    oldData.getLong(END_TIME_KEY)
            );
        }
    }

    // =====================================================
    // ПРОВЕРКА АКТИВНОСТИ
    // =====================================================

    public static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ACTIVE_KEY);
    }

    // =====================================================
    // ОСТАВШЕЕСЯ ВРЕМЯ
    // =====================================================

    public static long getRemainingTicks(ServerPlayer player) {
        if (!isActive(player)) {
            return 0L;
        }

        long endTime = player.getPersistentData().getLong(
                END_TIME_KEY
        );

        return Math.max(
                0L,
                endTime - getGameTime(player)
        );
    }

    // =====================================================
    // ЗАВЕРШЕНИЕ СОБЫТИЯ
    // =====================================================

    public static void finish(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }

        /*
         * Если событие ещё отслеживается ActiveEventManager,
         * его собственное сообщение об окончании будет отправлено
         * менеджером. Если игрок вышел с сервера, менеджер уже
         * не отслеживает событие, поэтому сообщение отправляем сами.
         */
        boolean managedByEventManager =
                ActiveEventManager.getRemainingTicks(
                        player,
                        EVENT_ID
                ) > 0;

        player.getPersistentData().putBoolean(
                ACTIVE_KEY,
                false
        );

        player.getPersistentData().putLong(
                END_TIME_KEY,
                0L
        );

        removeOwnEffects(player);

        if (!managedByEventManager) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§6[Random Events] §eЗолотое благословение закончилось."
                    )
            );
        }
    }

    // =====================================================
    // ВЫДАЧА ЭФФЕКТОВ
    // =====================================================

    private static void applyEffects(ServerPlayer player) {
        long remaining = getRemainingTicks(player);

        if (remaining <= 0) {
            return;
        }

        int duration = (int) Math.min(
                remaining,
                Integer.MAX_VALUE
        );

        for (Holder<MobEffect> effect : EFFECTS) {
            player.addEffect(
                    new MobEffectInstance(
                            effect,
                            duration,
                            AMPLIFIER,
                            false,
                            true,
                            true
                    )
            );
        }
    }

    // =====================================================
    // УДАЛЕНИЕ ЭФФЕКТОВ
    // =====================================================

    private static void removeOwnEffects(ServerPlayer player) {
        for (Holder<MobEffect> effect : EFFECTS) {
            MobEffectInstance instance = player.getEffect(effect);

            if (instance == null) {
                continue;
            }

            if (instance.getAmplifier() == AMPLIFIER) {
                player.removeEffect(effect);
            }
        }
    }

    // =====================================================
    // ПОЛУЧЕНИЕ ИГРОВОГО ВРЕМЕНИ
    // =====================================================

    private static long getGameTime(ServerPlayer player) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return 0L;
        }

        return server.overworld().getGameTime();
    }

    // =====================================================
    // НЕЗАВИСИМОЕ ОБНОВЛЕНИЕ
    // =====================================================

    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class PlayerEvents {

        @SubscribeEvent
        public static void onPlayerTick(
                TickEvent.PlayerTickEvent.Post event
        ) {
            if (!(event.player instanceof ServerPlayer player)) {
                return;
            }

            tick(player);
        }
    }
}
