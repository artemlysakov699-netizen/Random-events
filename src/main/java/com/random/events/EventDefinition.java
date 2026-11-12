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

    public enum EventType {

        // Старые события
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

        // Новые события
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
