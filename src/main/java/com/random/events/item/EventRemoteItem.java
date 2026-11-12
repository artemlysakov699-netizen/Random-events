package com.random.events.item;

import com.random.events.menu.EventSelectorMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EventRemoteItem extends Item {

    public EventRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, menuPlayer) ->
                                    new EventSelectorMenu(
                                            containerId,
                                            inventory
                                    ),
                            Component.literal("Выбор события")
                    )
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide()
        );
    }
}
