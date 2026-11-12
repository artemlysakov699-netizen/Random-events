
package com.random.events.menu;

import com.random.events.ActiveEventManager;
import com.random.events.EventDefinition;
import com.random.events.EventRegistry;
import com.random.events.EventDefinition.EventType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;

import java.util.List;

public class EventSelectorMenu extends AbstractContainerMenu {

    private static final int SLOT_COUNT = 54;

    private final SimpleContainer menuContainer;

    public EventSelectorMenu(
            int containerId,
            Inventory playerInventory
    ) {
        super(ModMenus.EVENT_SELECTOR.get(), containerId);

        this.menuContainer = new SimpleContainer(SLOT_COUNT);

        List<EventDefinition> events = EventRegistry.getAll();

        for (int i = 0; i < events.size() && i < SLOT_COUNT; i++) {
            EventDefinition definition = events.get(i);

            ItemStack icon = new ItemStack(
                    getIcon(definition.type())
            );

            Component displayName = Component.literal(definition.name())
                    .withStyle(style -> style.withColor(
                            definition.rarity().getColor()
                    ))
                    .append(
                            Component.literal(
                                    " [" + definition.rarity()
                                            .getDisplayName() + "]"
                            ).withStyle(ChatFormatting.GRAY)
                    );

            icon.set(DataComponents.CUSTOM_NAME, displayName);

            menuContainer.setItem(i, icon);
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            final int slotIndex = i;

            addSlot(
                    new Slot(
                            menuContainer,
                            slotIndex,
                            8 + (slotIndex % 9) * 18,
                            18 + (slotIndex / 9) * 18
                    ) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    }
            );
        }
    }

    private static Item getIcon(EventType type) {
        return switch (type) {

            case SUPER_SPEED -> Items.SUGAR;
            case JUMP_BOOST -> Items.RABBIT_FOOT;
            case REGENERATION -> Items.GOLDEN_APPLE;

            case ZOMBIE_HORDE -> Items.ZOMBIE_SPAWN_EGG;
            case SPIDER_HUNT -> Items.SPIDER_EYE;
            case WRATH_OF_SKY -> Items.LIGHTNING_ROD;
            case DENSE_FOG -> Items.GRAY_DYE;
            case GIFT_OF_FATE -> Items.CHEST;
            case EXPERIENCE_FEVER -> Items.EXPERIENCE_BOTTLE;

            case WEAKNESS -> Items.FERMENTED_SPIDER_EYE;
            case SLOWNESS -> Items.COBWEB;
            case HUNGER -> Items.ROTTEN_FLESH;
            case SOLAR_BURST -> Items.BLAZE_POWDER;
            case CREEPER_AMBUSH -> Items.CREEPER_SPAWN_EGG;
            case HELPERS -> Items.BONE;
            case TRAPPED_CHEST -> Items.TNT;

            case CHICKEN_RAIN -> Items.CHICKEN_SPAWN_EGG;
            case BLESSING -> Items.ENCHANTED_GOLDEN_APPLE;
            case WATERDROP -> Items.WATER_BUCKET;

            case WARDEN_HUNT -> Items.SCULK_SHRIEKER;
            case ZOMBIE_BOSS -> Items.ZOMBIE_HEAD;

            case METEOR_SHOWER -> Items.FIRE_CHARGE;
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

        if (clickType != ClickType.PICKUP) {
            return;
        }

        List<EventDefinition> events = EventRegistry.getAll();

        if (slotId >= events.size()) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
                && !player.level().isClientSide()) {

            EventDefinition selected = events.get(slotId);

            ActiveEventManager.startEventById(
                    serverPlayer,
                    selected.id()
            );

            player.closeContainer();
        }
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
