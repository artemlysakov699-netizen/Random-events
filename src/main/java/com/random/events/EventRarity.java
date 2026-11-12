
package com.random.events;

public enum EventRarity {

    COMMON(
            "Обычное",
            0xAAAAAA,
            40
    ),

    RARE(
            "Редкое",
            0x36C65B,
            25
    ),

    SUPER_RARE(
            "Сверх редкое",
            0x2685E8,
            17
    ),

    EPIC(
            "Эпическое",
            0xA34DE0,
            10
    ),

    MYTHIC(
            "Мифическое",
            0xE53935,
            6
    ),

    LEGENDARY(
            "Легендарное",
            0xFFD700,
            2
    );

    private final String displayName;
    private final int color;
    private final int weight;

    EventRarity(
            String displayName,
            int color,
            int weight
    ) {
        this.displayName = displayName;
        this.color = color;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public int getWeight() {
        return weight;
    }
}
