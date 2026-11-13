
package com.random.events;

import java.util.Objects;

public record EventDefinition(
        String id,
        String name,
        EventRarity rarity,
        int weight,
        int durationTicks,
        boolean canCombine,
        EventType type
) {

    public EventDefinition {
        Objects.requireNonNull(id, "Event id cannot be null");
        Objects.requireNonNull(name, "Event name cannot be null");
        Objects.requireNonNull(rarity, "Event rarity cannot be null");
        Objects.requireNonNull(type, "Event type cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Event id cannot be blank");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Event name cannot be blank");
        }

        if (weight <= 0) {
            throw new IllegalArgumentException("Event weight must be positive");
        }

        if (durationTicks < 0) {
            throw new IllegalArgumentException(
                    "Event duration cannot be negative"
            );
        }
    }

    public int getDurationSeconds() {
        return durationTicks / 20;
    }

    public boolean hasTimeLimit() {
        return durationTicks > 0;
    }

    public String getDescription() {
        return switch (type) {
            case SUPER_SPEED ->
                    "Даёт игроку повышенную скорость передвижения на 60 секунд.";

            case JUMP_BOOST ->
                    "Увеличивает высоту прыжка игрока на 60 секунд.";

            case REGENERATION ->
                    "Восстанавливает здоровье игрока в течение 60 секунд.";

            case WEAKNESS ->
                    "Ослабляет атаки игрока на 45 секунд.";

            case SLOWNESS ->
                    "Сильно замедляет передвижение игрока на 45 секунд.";

            case HUNGER ->
                    "Вызывает сильный голод на 45 секунд.";

            case CHICKEN_RAIN ->
                    "Пять куриц появляются высоко над игроком и падают вниз.";

            case ZOMBIE_HORDE ->
                    "Рядом с игроком появляется орда из 10 зомби.";

            case SOLAR_BURST ->
                    "Под открытым дневным небом игрок начинает гореть.";

            case SPIDER_HUNT ->
                    "От 5 до 7 пауков появляются рядом и начинают охоту.";

            case WRATH_OF_SKY ->
                    "Молнии регулярно бьют рядом с игроком.";

            case CREEPER_AMBUSH ->
                    "Рядом с игроком появляются три крипера.";

            case DENSE_FOG ->
                    "Окружение игрока окутывает густой туман на 60 секунд.";

            case GIFT_OF_FATE ->
                    "Неподалёку появляется сундук с полезными случайными предметами.";

            case BLESSING ->
                    "Даёт регенерацию, сопротивление урону и скорость на 60 секунд.";

            case EXPERIENCE_FEVER ->
                    "Игрок получает в два раза больше опыта за убийство существ.";

            case WATERDROP ->
                    "Подбрасывает игрока высоко в воздух и выдаёт ведро воды для спасения.";

            case WARDEN_HUNT ->
                    "Призывает Стража неподалёку от игрока.";

            case ZOMBIE_BOSS ->
                    "Появляется босс-зомби со 100 единицами здоровья и алмазной бронёй.";

            case METEOR_SHOWER ->
                    "В течение события рядом с игроком падают метеориты.";

            case HELPERS ->
                    "Призывает пять приручённых волков, которые помогают игроку.";

            case TRAPPED_CHEST ->
                    "Создаёт сундук-ловушку. При открытии происходит мощный взрыв.";

            case PILLAGER_AMBUSH ->
                    "От 5 до 7 разбойников появляются рядом и атакуют игрока.";

            case RAVAGER_WRATH ->
                    "Призывает разорителя, который представляет серьёзную угрозу.";

            case SLIME_EPIDEMIC ->
                    "Рядом с игроком появляется группа крупных слизней.";

            case CURSED_CHEST ->
                    "Проклятый сундук: 50% шанс получить ценную награду и 50% шанс попасть в смертельную ловушку.";

            case IRON_DEFENDER ->
                    "Призывает железного защитника, который помогает игроку в бою.";

            case MYSTERIOUS_MERCHANT ->
                    "Появляется торговец с редкими товарами и высокими ценами.";

            case EXPERIENCE_RAIN ->
                    "В течение короткого времени с неба падают сферы опыта.";

            case GOLDEN_BLESSING ->
                    "Даёт мощное золотое благословение на 60 секунд.";
        };
    }

    public Impact getImpact() {
        return switch (type) {
            case SUPER_SPEED,
                 JUMP_BOOST,
                 REGENERATION,
                 GIFT_OF_FATE,
                 BLESSING,
                 EXPERIENCE_FEVER,
                 HELPERS,
                 IRON_DEFENDER,
                 EXPERIENCE_RAIN,
                 GOLDEN_BLESSING ->
                    Impact.HELPFUL;

            case WEAKNESS,
                 SLOWNESS,
                 HUNGER,
                 ZOMBIE_HORDE,
                 SOLAR_BURST,
                 SPIDER_HUNT,
                 WRATH_OF_SKY,
                 CREEPER_AMBUSH,
                 DENSE_FOG,
                 WARDEN_HUNT,
                 ZOMBIE_BOSS,
                 METEOR_SHOWER,
                 TRAPPED_CHEST,
                 PILLAGER_AMBUSH,
                 RAVAGER_WRATH,
                 SLIME_EPIDEMIC ->
                    Impact.HARMFUL;

            case CHICKEN_RAIN,
                 WATERDROP,
                 CURSED_CHEST,
                 MYSTERIOUS_MERCHANT ->
                    Impact.NEUTRAL;
        };
    }

    public enum Impact {
        HELPFUL("Помощь", 0x36C65B),
        HARMFUL("Вред", 0xE53935),
        NEUTRAL("Нейтральное", 0xAAAAAA);

        private final String displayName;
        private final int color;

        Impact(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    public enum EventType {

        SUPER_SPEED,
        JUMP_BOOST,
        REGENERATION,
        WEAKNESS,
        SLOWNESS,
        HUNGER,
        CHICKEN_RAIN,

        ZOMBIE_HORDE,
        SOLAR_BURST,
        SPIDER_HUNT,
        WRATH_OF_SKY,
        CREEPER_AMBUSH,
        DENSE_FOG,
        GIFT_OF_FATE,
        BLESSING,
        EXPERIENCE_FEVER,
        WATERDROP,
        WARDEN_HUNT,
        ZOMBIE_BOSS,
        METEOR_SHOWER,
        HELPERS,
        TRAPPED_CHEST,

        PILLAGER_AMBUSH,
        RAVAGER_WRATH,
        SLIME_EPIDEMIC,
        CURSED_CHEST,
        IRON_DEFENDER,
        MYSTERIOUS_MERCHANT,
        EXPERIENCE_RAIN,
        GOLDEN_BLESSING
    }
}
