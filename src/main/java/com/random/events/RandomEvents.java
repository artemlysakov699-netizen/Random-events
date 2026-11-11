package com.random.events;

import com.random.events.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

@Mod(RandomEvents.MOD_ID)
public class RandomEvents {

    public static final String MOD_ID = "randomevents";

    private static final String TIMER_KEY = "RandomEventsRemainingTicks";
    private static final int EVENT_INTERVAL = 1200;
    private static final Random RANDOM = new Random();

    private static final List<RandomEvent> EVENTS = List.of(

            new RandomEvent(
                    "Сверхскорость",
                    Rarity.COMMON,
                    25,
                    player -> player.addEffect(
                            new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1)
                    )
            ),

            new RandomEvent(
                    "Прыгучесть",
                    Rarity.COMMON,
                    22,
                    player -> player.addEffect(
                            new MobEffectInstance(MobEffects.JUMP, 300, 1)
                    )
            ),

            new RandomEvent(
                    "Восстановление",
                    Rarity.RARE,
                    12,
                    player -> player.addEffect(
                            new MobEffectInstance(MobEffects.REGENERATION, 200, 1)
                    )
            ),

            new RandomEvent(
                    "Слабость",
                    Rarity.UNCOMMON,
                    22,
                    player -> player.addEffect(
                            new MobEffectInstance(MobEffects.WEAKNESS, 300, 0)
                    )
            ),

            new RandomEvent(
                    "Замедление",
                    Rarity.UNCOMMON,
                    22,
                    player -> player.addEffect(
                            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1)
                    )
            ),

            new RandomEvent(
                    "Внезапный голод",
                    Rarity.UNCOMMON,
                    14,
                    player -> player.addEffect(
                            new MobEffectInstance(MobEffects.HUNGER, 300, 1)
                    )
            ),

            new RandomEvent(
                    "Куриный десант",
                    Rarity.EPIC,
                    5,
                    RandomEvents::spawnChickenRain
            )
    );

    public RandomEvents() {
        NetworkHandler.register();
        System.out.println("[Random Events] Version 1.0.0 initialized!");
    }

    private enum Rarity {
        COMMON("Обычное"),
        UNCOMMON("Необычное"),
        RARE("Редкое"),
        EPIC("Эпическое"),
        LEGENDARY("Легендарное");

        private final String displayName;

        Rarity(String displayName) {
            this.displayName = displayName;
        }
    }

    private record RandomEvent(
            String name,
            Rarity rarity,
            int weight,
            Consumer<ServerPlayer> action
    ) {
    }

    @Mod.EventBusSubscriber(
            modid = MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ServerEvents {

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            var data = player.getPersistentData();

            if (!data.contains(TIMER_KEY)) {
                data.putInt(TIMER_KEY, EVENT_INTERVAL);
            }

            syncTimer(player);
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
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
                data.putInt(TIMER_KEY, remaining);
            }

            if (remaining <= 0) {
                triggerRandomEvent(player);
                data.putInt(TIMER_KEY, EVENT_INTERVAL);
            }

            if (player.tickCount % 20 == 0) {
                syncTimer(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            var oldData = event.getOriginal().getPersistentData();
            var newData = event.getEntity().getPersistentData();

            if (oldData.contains(TIMER_KEY)) {
                newData.putInt(TIMER_KEY, oldData.getInt(TIMER_KEY));
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
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
    }

    private static void syncTimer(ServerPlayer player) {
        int remaining = player.getPersistentData().getInt(TIMER_KEY);

        NetworkHandler.sendTimer(player, Math.max(0, remaining));
    }

    private static void triggerRandomEvent(ServerPlayer player) {
        RandomEvent selected = selectRandomEvent();

        selected.action().accept(player);

        String message =
                "§6[Random Events] §f"
                        + selected.name()
                        + " §7("
                        + selected.rarity().displayName
                        + ")";

        player.sendSystemMessage(Component.literal(message));

        System.out.println(
                "[Random Events] Event started for "
                        + player.getGameProfile().getName()
                        + ": "
                        + selected.name()
        );
    }

    private static RandomEvent selectRandomEvent() {
        int totalWeight = EVENTS.stream()
                .mapToInt(RandomEvent::weight)
                .sum();

        int randomValue = RANDOM.nextInt(totalWeight);

        for (RandomEvent event : EVENTS) {
            randomValue -= event.weight();

            if (randomValue < 0) {
                return event;
            }
        }

        return EVENTS.get(0);
    }

    private static void spawnChickenRain(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        double spawnY = Math.min(
                player.getY() + 20,
                level.getMaxBuildHeight() - 2
        );

        for (int i = 0; i < 5; i++) {
            Chicken chicken = EntityType.CHICKEN.create(level);

            if (chicken == null) {
                continue;
            }

            double offsetX = RANDOM.nextInt(7) - 3;
            double offsetZ = RANDOM.nextInt(7) - 3;

            chicken.moveTo(
                    player.getX() + offsetX,
                    spawnY,
                    player.getZ() + offsetZ,
                    RANDOM.nextFloat() * 360.0F,
                    0.0F
            );

            level.addFreshEntity(chicken);
        }
    }
}
