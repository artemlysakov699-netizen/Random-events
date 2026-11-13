
package com.random.events.client;

import com.random.events.menu.EventSelectorMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventSelectorScreen
        extends AbstractContainerScreen<EventSelectorMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    "textures/gui/container/generic_54.png"
            );

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 126;
    private static final int EVENT_START_SLOT = 18;

    public EventSelectorScreen(
            EventSelectorMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        this.titleLabelX = 8;
        this.titleLabelY = 6;

        // Инвентарь игрока в планшете не отображается.
        this.inventoryLabelY = 999;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.blit(
                BACKGROUND,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight
        );
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawString(
                font,
                title,
                titleLabelX,
                titleLabelY,
                0x404040,
                false
        );
    }

    /**
     * Явно отображает полное описание события
     * при наведении курсора на его иконку.
     */
    @Override
    protected void renderTooltip(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        Slot hoveredSlot = getSlotUnderMouse();

        if (hoveredSlot == null
                || hoveredSlot.index < EVENT_START_SLOT) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        ItemStack stack = hoveredSlot.getItem();

        if (stack.isEmpty()) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();

        // Название события.
        tooltip.add(stack.getHoverName());

        // Полное описание, редкость, тип и длительность.
        ItemLore lore = stack.get(DataComponents.LORE);

        if (lore != null) {
            tooltip.addAll(lore.styledLines());
        }

        if (tooltip.size() <= 1) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        graphics.renderTooltip(
                font,
                tooltip,
                Optional.empty(),
                mouseX,
                mouseY
        );
    }
}
