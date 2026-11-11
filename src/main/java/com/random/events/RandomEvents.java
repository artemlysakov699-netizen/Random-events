
package com.random.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod("randomevents")
public class RandomEvents {

    public static final String MOD_ID = "randomevents";
    private static final Random RANDOM = new Random();
    private static final String NEXT_EVENT = "RandomEventsNextEvent";

    public RandomEvents() {
        System.out.println("[Random Events] Mod loaded!");
    }

    @Mod.EventBusSubscriber(
        modid = MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class Events {

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
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§a[Random Events] Сверхскорость!"
                        ));
                }

                case 1 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.JUMP, 300, 1));
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§a[Random Events] Прыгучесть!"
                        ));
                }

                case 2 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, 200, 1));
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§a[Random Events] Восстановление!"
                        ));
                }

                case 3 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, 300, 0));
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c[Random Events] Слабость!"
                        ));
                }

                case 4 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 300, 1));
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c[Random Events] Замедление!"
                        ));
                }

                case 5 -> {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.HUNGER, 300, 1));
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§c[Random Events] Внезапный голод!"
                        ));
                }

                case 6 -> {
                    for (int i = 0; i < 5; i++) {
                        Chicken chicken = EntityType.CHICKEN.create(
                            player.serverLevel()
                        );

                        if (chicken != null) {
                            chicken.moveTo(
                                player.getX() + RANDOM.nextInt(7) - 3,
                                player.getY(),
                                player.getZ() + RANDOM.nextInt(7) - 3,
                                0,
                                0
                            );
                            player.serverLevel().addFreshEntity(chicken);
                        }
                    }

                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§e[Random Events] Куриный десант!"
                        ));
                }
            }
        }
    }
}
