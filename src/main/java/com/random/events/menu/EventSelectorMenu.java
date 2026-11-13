
package com.random.events.menu;

import com.random.events.ActiveEventManager;
import com.random.events.EventDefinition;
import com.random.events.EventDefinition.Impact;
import com.random.events.EventDefinition.EventType;
import com.random.events.EventRarity;
import com.random.events.EventRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public class EventSelectorMenu extends AbstractContainerMenu {

    private static final int SLOT_COUNT = 54;
    private static final int EVENT_START_SLOT = 18;
    private static final int EVENT_SLOTS = SLOT_COUNT - EVENT_START_SLOT;

    private static final int DESCRIPTION_LINE_LENGTH = 42;

    private final SimpleContainer menuContainer;

    private EventRarity rarityFilter = null;
    private Impact impactFilter = null;

    public EventSelectorMenu(
            int containerId,
            Inventory playerInventory
    ) {
        super(ModMenus.EVENT_SELECTOR.get(), containerId);

        menuContainer = new SimpleContainer(SLOT_COUNT);

        for (int i = 0; i < SLOT_COUNT; i++) {
            final int slotIndex = i;

            addSlot(new Slot(
                    menuContainer,
                    slotIndex,
                    8 + (slotIndex % 9) * 18,
                    18 + (slotIndex / 9) * 18
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        refreshContents();
    }

    private void refreshContents() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            menuContainer.setItem(i, ItemStack.EMPTY);
        }

        createFilterButtons();

        List<EventDefinition> events = getFilteredEvents();

        for (int i = 0; i < events.size() && i < EVENT_SLOTS; i++) {
            EventDefinition definition = events.get(i);

            menuContainer.setItem(
                    EVENT_START_SLOT + i,
                    createEventIcon(definition)
            );
        }
    }

    private void createFilterButtons() {
        setFilterButton(
                0,
                Items.COMPASS,
                "Все редкости",
                rarityFilter == null,
                "Показывать события любой редкости."
        );

        EventRarity[] rarities = EventRarity.values();

        for (int i = 0; i < rarities.length; i++) {
            EventRarity rarity = rarities[i];

            setFilterButton(
                    i + 1,
                    getRarityIcon(rarity),
                    rarity.getDisplayName(),
                    rarityFilter == rarity,
                    "Показывать только события этой редкости."
            );
        }

        setFilterButton(
                7,
                Items.BOOK,
                "Все типы",
                impactFilter == null,
                "Показывать помощь, вред и нейтральные события."
        );

        setFilterButton(
                8,
                Items.GOLDEN_APPLE,
                "Помощь",
                impactFilter == Impact.HELPFUL,
                "Показывать события, полезные игроку."
        );

        setFilterButton(
                9,
                Items.TNT,
                "Вред",
                impactFilter == Impact.HARMFUL,
                "Показывать опасные и негативные события."
        );

        setFilterButton(
                10,
                Items.PAPER,
                "Нейтральные",
                impactFilter == Impact.NEUTRAL,
                "Показывать события с нейтральным или смешанным эффектом."
        );

        setInfoButton(
                11,
                Items.FILLED_MAP,
                "Фильтры редкости",
                "Кнопки 1–7: выбери нужную редкость."
        );

        setInfoButton(
                12,
                Items.WRITABLE_BOOK,
                "Фильтры воздействия",
                "Кнопки 8–11: помощь, вред или нейтральные события."
        );

        setInfoButton(
                13,
                Items.KNOWLEDGE_BOOK,
                "Как пользоваться",
                "Наведись на событие, чтобы прочитать его описание. "
                        + "Нажми левой кнопкой мыши, чтобы активировать его."
        );

        setInfoButton(
                14,
                Items.CLOCK,
                "Длительность",
                "В подсказке события указано время его действия "
                        + "или условие завершения."
        );

        setInfoButton(
                15,
                Items.BOOK,
                "Всего событий",
                "В Random Events зарегистрировано "
                        + EventRegistry.getAll().size() + " событий."
        );

        setInfoButton(
                16,
                Items.BARRIER,
                "Сброс фильтров",
                "Нажми кнопку «Все редкости» и «Все типы», "
                        + "чтобы снова увидеть полный список."
        );
    }

    private void setFilterButton(
            int slot,
            Item item,
            String name,
            boolean selected,
            String description
    ) {
        ItemStack stack = new ItemStack(item);

        Component displayName = Component.literal(
                (selected ? "✔ " : "") + name
        ).withStyle(
                selected ? ChatFormatting.GREEN : ChatFormatting.WHITE
        );

        stack.set(DataComponents.CUSTOM_NAME, displayName);

        List<Component> lore = new ArrayList<>();

        addWrappedLore(
                lore,
                description,
                ChatFormatting.GRAY
        );

        lore.add(Component.literal(
                selected ? "Выбран этот фильтр" : "Нажми, чтобы выбрать"
        ).withStyle(
                selected ? ChatFormatting.GREEN : ChatFormatting.YELLOW
        ));

        stack.set(
                DataComponents.LORE,
                new ItemLore(lore)
        );

        menuContainer.setItem(slot, stack);
    }

    private void setInfoButton(
            int slot,
            Item item,
            String name,
            String description
    ) {
        ItemStack stack = new ItemStack(item);

        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(name)
                        .withStyle(ChatFormatting.AQUA)
        );

        List<Component> lore = new ArrayList<>();

        addWrappedLore(
                lore,
                description,
                ChatFormatting.GRAY
        );

        stack.set(
                DataComponents.LORE,
                new ItemLore(lore)
        );

        menuContainer.setItem(slot, stack);
    }

    private ItemStack createEventIcon(EventDefinition definition) {
        ItemStack icon = new ItemStack(
                getIcon(definition.type())
        );

        Component displayName = Component.literal(
                definition.name()
        ).withStyle(style ->
                style.withColor(definition.rarity().getColor())
        );

        icon.set(
                DataComponents.CUSTOM_NAME,
                displayName
        );

        List<Component> lore = new ArrayList<>();

        lore.add(Component.literal(
                "Редкость: " + definition.rarity().getDisplayName()
        ).withStyle(style ->
                style.withColor(definition.rarity().getColor())
        ));

        lore.add(Component.literal(
                "Тип: " + definition.getImpact().getDisplayName()
        ).withStyle(style ->
                style.withColor(definition.getImpact().getColor())
        ));

        lore.add(Component.empty());

        lore.add(Component.literal("Описание события:")
                .withStyle(ChatFormatting.GOLD));

        addWrappedLore(
                lore,
                definition.getDescription(),
                ChatFormatting.GRAY
        );

        lore.add(Component.empty());

        if (definition.hasTimeLimit()) {
            lore.add(Component.literal(
                    "Длительность: "
                            + definition.getDurationSeconds()
                            + " сек."
            ).withStyle(ChatFormatting.YELLOW));
        } else {
            lore.add(Component.literal(
                    "Длительность: до выполнения условия"
            ).withStyle(ChatFormatting.YELLOW));
        }

        lore.add(Component.empty());

        lore.add(Component.literal(
                "Нажми, чтобы активировать"
        ).withStyle(ChatFormatting.GREEN));

        icon.set(
                DataComponents.LORE,
                new ItemLore(lore)
        );

        return icon;
    }

    /**
     * Разбивает длинный текст на отдельные строки подсказки.
     */
    private static void addWrappedLore(
            List<Component> lore,
            String text,
            ChatFormatting formatting
    ) {
        if (text == null || text.isBlank()) {
            return;
        }

        String[] words = text.trim().split("\\s+");

        StringBuilder line = new StringBuilder();

        for (String word : words) {

            if (line.length() == 0) {
                line.append(word);
                continue;
            }

            if (line.length() + 1 + word.length()
                    > DESCRIPTION_LINE_LENGTH) {

                lore.add(
                        Component.literal(line.toString())
                                .withStyle(formatting)
                );

                line.setLength(0);
                line.append(word);

            } else {
                line.append(' ').append(word);
            }
        }

        if (line.length() > 0) {
            lore.add(
                    Component.literal(line.toString())
                            .withStyle(formatting)
            );
        }
    }

    private List<EventDefinition> getFilteredEvents() {
        return EventRegistry.getAll().stream()
                .filter(event ->
                        rarityFilter == null
                                || event.rarity() == rarityFilter
                )
                .filter(event ->
                        impactFilter == null
                                || event.getImpact() == impactFilter
                )
                .toList();
    }

    private static Item getRarityIcon(EventRarity rarity) {
        return switch (rarity) {
            case COMMON -> Items.GRAY_DYE;
            case RARE -> Items.EMERALD;
            case SUPER_RARE -> Items.LAPIS_LAZULI;
            case EPIC -> Items.AMETHYST_SHARD;
            case MYTHIC -> Items.REDSTONE;
            case LEGENDARY -> Items.GOLD_INGOT;
        };
    }

    private static Item getIcon(EventType type) {
        return switch (type) {
            case SUPER_SPEED -> Items.SUGAR;
            case JUMP_BOOST -> Items.RABBIT_FOOT;
            case REGENERATION -> Items.GOLDEN_APPLE;
            case WEAKNESS -> Items.FERMENTED_SPIDER_EYE;
            case SLOWNESS -> Items.COBWEB;
            case HUNGER -> Items.ROTTEN_FLESH;
            case CHICKEN_RAIN -> Items.CHICKEN_SPAWN_EGG;
            case ZOMBIE_HORDE -> Items.ZOMBIE_SPAWN_EGG;
            case SOLAR_BURST -> Items.BLAZE_POWDER;
            case SPIDER_HUNT -> Items.SPIDER_EYE;
            case WRATH_OF_SKY -> Items.LIGHTNING_ROD;
            case CREEPER_AMBUSH -> Items.CREEPER_SPAWN_EGG;
            case DENSE_FOG -> Items.GRAY_DYE;
            case GIFT_OF_FATE -> Items.CHEST;
            case BLESSING -> Items.ENCHANTED_GOLDEN_APPLE;
            case EXPERIENCE_FEVER -> Items.EXPERIENCE_BOTTLE;
            case WATERDROP -> Items.WATER_BUCKET;
            case WARDEN_HUNT -> Items.SCULK_SHRIEKER;
            case ZOMBIE_BOSS -> Items.ZOMBIE_HEAD;
            case METEOR_SHOWER -> Items.FIRE_CHARGE;
            case HELPERS -> Items.BONE;
            case TRAPPED_CHEST -> Items.TNT;
            case PILLAGER_AMBUSH -> Items.CROSSBOW;
            case RAVAGER_WRATH -> Items.SADDLE;
            case SLIME_EPIDEMIC -> Items.SLIME_BALL;
            case CURSED_CHEST -> Items.CHEST;
            case IRON_DEFENDER -> Items.IRON_BLOCK;
            case MYSTERIOUS_MERCHANT -> Items.EMERALD;
            case EXPERIENCE_RAIN -> Items.EXPERIENCE_BOTTLE;
            case GOLDEN_BLESSING -> Items.GOLDEN_APPLE;
        };
    }

    @Override
    public void clicked(
            int slotId,
            int button,
            ClickType clickType,
            Player player
    ) {
        if (slotId < 0) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (slotId >= SLOT_COUNT
                || clickType != ClickType.PICKUP
                || button != 0) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)
                || player.level().isClientSide()) {
            return;
        }

        if (slotId >= 0 && slotId <= 6) {
            if (slotId == 0) {
                rarityFilter = null;
            } else {
                rarityFilter = EventRarity.values()[slotId - 1];
            }

            refreshContents();
            broadcastChanges();
            return;
        }

        if (slotId >= 7 && slotId <= 10) {
            impactFilter = switch (slotId) {
                case 7 -> null;
                case 8 -> Impact.HELPFUL;
                case 9 -> Impact.HARMFUL;
                case 10 -> Impact.NEUTRAL;
                default -> null;
            };

            refreshContents();
            broadcastChanges();
            return;
        }

        if (slotId < EVENT_START_SLOT) {
            return;
        }

        int eventIndex = slotId - EVENT_START_SLOT;

        List<EventDefinition> events = getFilteredEvents();

        if (eventIndex < 0 || eventIndex >= events.size()) {
            return;
        }

        EventDefinition selected = events.get(eventIndex);

        ActiveEventManager.startEventById(
                serverPlayer,
                selected.id()
        );

        player.closeContainer();
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
