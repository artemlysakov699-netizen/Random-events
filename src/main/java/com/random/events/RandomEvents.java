
package com.random.events;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderGuiEvent;

import java.util.Random;

@Mod("randomevents")
public class RandomEvents {

    public static final String MOD_ID = "randomevents";
    private static final Random RANDOM = new Random();
    private static final String NEXT_EVENT = "RandomEventsNextEvent";

    public RandomEvents() {
        System.out.println("[Random Events] Version 0.2.0 loaded!");
    }

    @Mod.EventBusSubscriber(
        modid = MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class Events {

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                long time = player.serverLevel().getGameTime();
                player.getPersistentData().putLong(NEXT_EVENT, time + 1200);
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
            if (!(event.player instanceof ServerPlayer player)) {
                return;
            }

            long time = player.serverLevel().getGameTime();
            var data = player.getPersistentData();

            if (!data.contains(NEXT_EVENT)) {
                data.putLong(NEXT_EVENT, time + 1200);
                return;
            }

            if (time >= data.getLong(NEXT_EVENT)) {
                triggerRandomEvent(player);
                data.putLong(NEXT_EVENT, time + 1200);
            }
        }

        private static void triggerRandomEvent(ServerPlayer player) {
            switch (RANDOM.nextInt(7)) {

                case 0 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 300, 1));
                    player.sendSystemMessage(Component.literal(
                        "§a[Random Events] Сверхскорость!"));
                }

                case 1 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.JUMP, 300, 1));
                    player.sendSystemMessage(Component.literal(
                        "§a[Random Events] Прыгучесть!"));
                }

                case 2 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, 200, 1));
                    player.sendSystemMessage(Component.literal(
                        "§a[Random Events] Восстановление!"));
                }

                case 3 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 300, 0));
                    player.sendSystemMessage(Component.literal(
                        "§c[Random Events] Слабость!"));
                }

                case 4 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 300, 1));
                    player.sendSystemMessage(Component.literal(
                        "§c[Random Events] Замедление!"));
                }

                case 5 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.HUNGER, 300, 1));
                    player.sendSystemMessage(Component.literal(
                        "§c[Random Events] Внезапный голод!"));
                }

                case 6 -> {
                    for (int i = 0; i < 5; i++) {
                        Chicken chicken = EntityType.CHICKEN.create(
                            player.serverLevel()
                        );

                        if (chicken != null) {
                            chicken.moveTo(
                                player.getX() + RANDOM.nextInt(7) - 3,
                                player.getY() + 10,
                                player.getZ() + RANDOM.nextInt(7) - 3,
                                0,
                                0
                            );

                            player.serverLevel().addFreshEntity(chicken);
                        }
                    }

                    player.sendSystemMessage(Component.literal(
                        "§e[Random Events] КУРИНЫЙ ДЕСАНТ!"));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(
        modid = MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ClientEvents {

        private static int ticksLeft = 1200;
        private static Object lastPlayer = null;

        @SubscribeEvent
        public static void onClientTick(TickEvent.PlayerTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();

            if (event.player != mc.player) {
                return;
            }

            if (lastPlayer != event.player) {
                lastPlayer = event.player;
                ticksLeft = 1200;
            }

            if (ticksLeft > 0) {
                ticksLeft--;
            } else {
                ticksLeft = 1200;
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player == null || mc.level == null) {
                return;
            }

            int seconds = ticksLeft / 20;
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;

            String timer = String.format(
                "Следующее событие: %02d:%02d",
                minutes,
                remainingSeconds
            );

            int x = mc.getWindow().getGuiScaledWidth()
                - mc.font.width(timer) - 8;

            event.getGuiGraphics().drawString(
                mc.font,
                timer,
                x,
                8,
                0xFFFFFF,
                true
            );
        }
    }
}
