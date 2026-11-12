
package com.random.events;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public final class GoldenBlessingHandler {

    private static final String END_TIME_KEY =
            "RandomEventsGoldenBlessingEndTime";

    private static final String ACTIVE_KEY =
            "RandomEventsGoldenBlessingActive";

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

    /**
     * Запускает благословение на 60 секунд.
     */
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

    /**
     * Обновляет эффекты, пока благословение активно.
     */
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

    /**
     * Восстанавливает эффекты после входа или возрождения.
     */
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

    /**
     * Переносит данные благословения при смерти игрока.
     */
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

    /**
     * Проверяет, активно ли благословение.
     */
    public static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ACTIVE_KEY);
    }

    /**
     * Возвращает оставшееся время в тиках.
     */
    public static long getRemainingTicks(ServerPlayer player) {
        if (!isActive(player)) {
            return 0;
        }

        long endTime = player.getPersistentData().getLong(
                END_TIME_KEY
        );

        return Math.max(0, endTime - getGameTime(player));
    }

    /**
     * Принудительно завершает благословение.
     */
    public static void finish(ServerPlayer player) {
        player.getPersistentData().putBoolean(
                ACTIVE_KEY,
                false
        );

        player.getPersistentData().putLong(
                END_TIME_KEY,
                0L
        );

        for (Holder<MobEffect> effect : EFFECTS) {
            player.removeEffect(effect);
        }

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Random Events] §eЗолотое благословение закончилось."
                )
        );
    }

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

    private static long getGameTime(ServerPlayer player) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return 0L;
        }

        return server.overworld().getGameTime();
    }
}
