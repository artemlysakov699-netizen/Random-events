
package com.random.events;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod("randomevents")
public class RandomEvents {

    public static final String MOD_ID = "randomevents";

    private static final Random RANDOM = new Random();
    private static final String NEXT_EVENT = "RandomEventsNextEvent";

    public RandomEvents() {
        System.out.println("[Random Events] Version 0.2.0 loaded!");
    }

    // SERVER EVENTS
    @Mod.EventBusSubscriber(
        modid = MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class Events {

        @SubscribeEvent
        public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
        ) {
            if (event.getEntity() instanceof ServerPlayer player) {
                long time = player.serverLevel().getGameTime();

                player.getPersistentData().putLong(
                    NEXT_EVENT,
                    time + 1200
                );
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(
            TickEvent.PlayerTickEvent.Post event
        ) {
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

        private static void triggerRandomEvent(
            ServerPlayer player
        ) {
            switch (RANDOM.nextInt(7)) {

                case 0 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        300,
                        1
                    ));

                    player.sendSystemMessage(Component.literal(
                        "§a[Random Events] Сверхскорость!"
                    ));
                }

                case 1 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.JUMP,
                        300,
                        1
                    ));

                    player.sendSystemMessage(Component.literal(
                        "§a[Random Events] Прыгучесть!"
                    ));
                }

                case 2 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION,
                        200,
                        1
                    ));

                    player.sendSystemMessage(Component.literal(
                        "§a[Random Events] Восстановление!"
                    ));
                }

                case 3 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS,
                        300,
                        0
                    ));

                    player.sendSystemMessage(Component.literal(
                        "§c[Random Events] Слабость!"
                    ));
                }

                case 4 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        300,
                        1
                    ));

                    player.sendSystemMessage(Component.literal(
                        "§c[Random Events] Замедление!"
                    ));
                }

                case 5 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.HUNGER,
                        300,
                        1
                    ));

                    player.sendSystemMessage(Component.literal(
                        "§c[Random Events] Внезапный голод!"
                    ));
                }

                case 6 -> {
                    // CHICKEN AIRDROP
                    for (int i = 0; i < 5; i++) {

                        Chicken chicken = EntityType.CHICKEN.create(
                            player.serverLevel()
                        );

                        if (chicken != null) {

                            double offsetX =
                                RANDOM.nextInt(7) - 3;

                            double offsetZ =
                                RANDOM.nextInt(7) - 3;

                            chicken.moveTo(
                                player.getX() + offsetX,
                                player.getY() + 10,
                                player.getZ() + offsetZ,
                                RANDOM.nextFloat() * 360,
                                0
                            );

                            player.serverLevel().addFreshEntity(
                                chicken
                            );
                        }
                    }

                    player.sendSystemMessage(Component.literal(
                        "§e[Random Events] КУРИНЫЙ ДЕСАНТ!"
                    ));
                }
            }
        }
    }

    // CLIENT HUD
    @Mod.EventBusSubscriber(
        modid = MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
    )
    public static class ClientEvents {

        private static Object lastLevel = null;
        private static long nextEventTime = -1;

        @SubscribeEvent
        public static void addHudLayer(
            AddGuiOverlayLayersEvent event
        ) {

            event.getLayeredDraw().add(
                ResourceLocation.fromNamespaceAndPath(
                    MOD_ID,
                    "event_timer"
                ),

                (guiGraphics, deltaTracker) -> {

                    Minecraft mc = Minecraft.getInstance();

                    if (mc.player == null || mc.level == null) {
                        return;
                    }

                    long gameTime = mc.level.getGameTime();

                    // Reset timer when entering another world
                    if (lastLevel != mc.level) {
                        lastLevel = mc.level;
                        nextEventTime = gameTime + 1200;
                    }

                    long remaining = nextEventTime - gameTime;

                    if (remaining <= 0) {
                        nextEventTime = gameTime + 1200;
                        remaining = 1200;
                    }

                    int seconds = (int) Math.ceil(
                        remaining / 20.0
                    );

                    int minutes = seconds / 60;
                    int rest = seconds % 60;

                    String timer = String.format(
                        "Следующее событие: %02d:%02d",
                        minutes,
                        rest
                    );

                    int x =
                        mc.getWindow().getGuiScaledWidth()
                        - mc.font.width(timer) - 8;

                    guiGraphics.drawString(
                        mc.font,
                        timer,
                        x,
                        8,
                        0xFFFFFF,
                        true
                    );
                }
            );
        }
    }
}
