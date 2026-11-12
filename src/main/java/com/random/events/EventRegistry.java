package com.random.events;

import com.random.events.EventDefinition.EventType;

import java.util.List;
import java.util.Optional;

public final class EventRegistry {

    private static final List<EventDefinition> EVENTS = List.of(

            // =========================
            // ОБЫЧНЫЕ СОБЫТИЯ
            // =========================

            new EventDefinition(
                    "super_speed",
                    "Сверхскорость",
                    EventRarity.COMMON,
                    10,
                    60 * 20,
                    true,
                    EventType.SUPER_SPEED
            ),

            new EventDefinition(
                    "jump_boost",
                    "Прыгучесть",
                    EventRarity.COMMON,
                    10,
                    60 * 20,
                    true,
                    EventType.JUMP_BOOST
            ),

            // =========================
            // РЕДКИЕ СОБЫТИЯ
            // =========================

            new EventDefinition(
                    "regeneration",
                    "Восстановление",
                    EventRarity.RARE,
                    10,
                    60 * 20,
                    true,
                    EventType.REGENERATION
            ),

            new EventDefinition(
                    "zombie_horde",
                    "Орда зомби",
                    EventRarity.RARE,
                    8,
                    60 * 20,
                    false,
                    EventType.ZOMBIE_HORDE
            ),

            new EventDefinition(
                    "spider_hunt",
                    "Паучья охота",
                    EventRarity.RARE,
                    8,
                    60 * 20,
                    false,
                    EventType.SPIDER_HUNT
            ),

            new EventDefinition(
                    "wrath_of_sky",
                    "Гнев небес",
                    EventRarity.RARE,
                    7,
                    45 * 20,
                    false,
                    EventType.WRATH_OF_SKY
            ),

            new EventDefinition(
                    "dense_fog",
                    "Густой туман",
                    EventRarity.RARE,
                    8,
                    60 * 20,
                    true,
                    EventType.DENSE_FOG
            ),

            new EventDefinition(
                    "gift_of_fate",
                    "Подарок судьбы",
                    EventRarity.RARE,
                    7,
                    20 * 20,
                    false,
                    EventType.GIFT_OF_FATE
            ),

            new EventDefinition(
                    "experience_fever",
                    "Опытная лихорадка",
                    EventRarity.RARE,
                    7,
                    90 * 20,
                    true,
                    EventType.EXPERIENCE_FEVER
            ),

            new EventDefinition(
                    "experience_rain",
                    "Опытный дождь",
                    EventRarity.RARE,
                    7,
                    15 * 20,
                    false,
                    EventType.EXPERIENCE_RAIN
            ),

            new EventDefinition(
                    "pillager_ambush",
                    "Засада разбойников",
                    EventRarity.RARE,
                    6,
                    120 * 20,
                    false,
                    EventType.PILLAGER_AMBUSH
            ),

            // =========================
            // СВЕРХРЕДКИЕ СОБЫТИЯ
            // =========================

            new EventDefinition(
                    "weakness",
                    "Слабость",
                    EventRarity.SUPER_RARE,
                    8,
                    45 * 20,
                    true,
                    EventType.WEAKNESS
            ),

            new EventDefinition(
                    "slowness",
                    "Замедление",
                    EventRarity.SUPER_RARE,
                    8,
                    45 * 20,
                    true,
                    EventType.SLOWNESS
            ),

            new EventDefinition(
                    "hunger",
                    "Внезапный голод",
                    EventRarity.SUPER_RARE,
                    8,
                    45 * 20,
                    true,
                    EventType.HUNGER
            ),

            new EventDefinition(
                    "solar_burst",
                    "Солнечный всплеск",
                    EventRarity.SUPER_RARE,
                    7,
                    60 * 20,
                    false,
                    EventType.SOLAR_BURST
            ),

            new EventDefinition(
                    "creeper_ambush",
                    "Криперская засада",
                    EventRarity.SUPER_RARE,
                    6,
                    60 * 20,
                    false,
                    EventType.CREEPER_AMBUSH
            ),

            new EventDefinition(
                    "helpers",
                    "Помощники",
                    EventRarity.SUPER_RARE,
                    6,
                    30 * 20,
                    false,
                    EventType.HELPERS
            ),

            new EventDefinition(
                    "trapped_chest",
                    "Сундук-обманка",
                    EventRarity.SUPER_RARE,
                    6,
                    60 * 20,
                    false,
                    EventType.TRAPPED_CHEST
            ),

            // =========================
            // ЭПИЧЕСКИЕ СОБЫТИЯ
            // =========================

            new EventDefinition(
                    "chicken_rain",
                    "Куриный десант",
                    EventRarity.EPIC,
                    8,
                    30 * 20,
                    false,
                    EventType.CHICKEN_RAIN
            ),

            new EventDefinition(
                    "blessing",
                    "Благословение",
                    EventRarity.EPIC,
                    8,
                    60 * 20,
                    true,
                    EventType.BLESSING
            ),

            new EventDefinition(
                    "waterdrop",
                    "Ватердроп",
                    EventRarity.EPIC,
                    5,
                    30 * 20,
                    false,
                    EventType.WATERDROP
            ),

            new EventDefinition(
                    "mysterious_merchant",
                    "Загадочный торговец",
                    EventRarity.EPIC,
                    5,
                    5 * 60 * 20,
                    false,
                    EventType.MYSTERIOUS_MERCHANT
            ),

            new EventDefinition(
                    "slime_epidemic",
                    "Слизневая эпидемия",
                    EventRarity.EPIC,
                    5,
                    0,
                    false,
                    EventType.SLIME_EPIDEMIC
            ),

            // =========================
            // МИФИЧЕСКИЕ СОБЫТИЯ
            // =========================

            new EventDefinition(
                    "warden_hunt",
                    "Тише, он всё слышит",
                    EventRarity.MYTHIC,
                    6,
                    60 * 20,
                    false,
                    EventType.WARDEN_HUNT
            ),

            new EventDefinition(
                    "zombie_boss",
                    "Зомби-босс",
                    EventRarity.MYTHIC,
                    5,
                    90 * 20,
                    false,
                    EventType.ZOMBIE_BOSS
            ),

            new EventDefinition(
                    "ravager_wrath",
                    "Ярость разрушителя",
                    EventRarity.MYTHIC,
                    4,
                    0,
                    false,
                    EventType.RAVAGER_WRATH
            ),

            new EventDefinition(
                    "cursed_chest",
                    "Проклятый сундук",
                    EventRarity.MYTHIC,
                    4,
                    0,
                    false,
                    EventType.CURSED_CHEST
            ),

            // =========================
            // ЛЕГЕНДАРНЫЕ СОБЫТИЯ
            // =========================

            new EventDefinition(
                    "meteor_shower",
                    "Метеоритный дождь",
                    EventRarity.LEGENDARY,
                    3,
                    120 * 20,
                    false,
                    EventType.METEOR_SHOWER
            ),

            new EventDefinition(
                    "iron_defender",
                    "Железный защитник",
                    EventRarity.LEGENDARY,
                    3,
                    60 * 20,
                    false,
                    EventType.IRON_DEFENDER
            ),

            new EventDefinition(
                    "golden_blessing",
                    "Золотое благословение",
                    EventRarity.LEGENDARY,
                    3,
                    60 * 20,
                    false,
                    EventType.GOLDEN_BLESSING
            )
    );

    private EventRegistry() {
    }

    public static List<EventDefinition> getAll() {
        return EVENTS;
    }

    public static List<EventDefinition> getByRarity(
            EventRarity rarity
    ) {
        return EVENTS.stream()
                .filter(event -> event.rarity() == rarity)
                .toList();
    }

    public static Optional<EventDefinition> getById(String id) {
        return EVENTS.stream()
                .filter(event -> event.id().equals(id))
                .findFirst();
    }

    public static int getEventCount() {
        return EVENTS.size();
    }
        }
