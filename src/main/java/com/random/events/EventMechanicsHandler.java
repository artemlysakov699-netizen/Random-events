package com.random.events;

import com.random.events.EventDefinition.EventType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class EventMechanicsHandler {

    private static final Random RANDOM = new Random();

    private static final Map<UUID, WaterdropData> WATERDROPS =
            new HashMap<>();

    private static final Map<UUID, TrapData> TRAPS =
            new HashMap<>();

    private static final Map<UUID, Integer> METEOR_COUNTS =
            new HashMap<>();

    private EventMechanicsHandler() {}

    public static void start(
            ServerPlayer player,
            EventDefinition definition
    ) {
        switch (definition.type()) {
            case GIFT_OF_FATE ->
                    createGiftChest(player);

            case WATERDROP ->
                    startWaterdrop(player);

            case TRAPPED_CHEST ->
                    createTrap(player);

            case METEOR_SHOWER ->
                    METEOR_COUNTS.put(player.getUUID(), 0);

            case EXPERIENCE_FEVER, DENSE_FOG -> {
                // Эти события обрабатываются во время действия.
            }

            default -> {
            }
        }
    }

    public static void tick(
            ServerPlayer player,
            EventDefinition definition
    ) {
        switch (definition.type()) {
            case METEOR_SHOWER ->
                    tickMeteorShower(player);

            case WATERDROP -> {
                // Временный предмет восстанавливается при завершении.
            }

            default -> {
            }
        }
    }

    public static void end(
            ServerPlayer player,
            EventDefinition definition
    ) {
        switch (definition.type()) {
            case WATERDROP ->
                    endWaterdrop(player);

            case TRAPPED_CHEST ->
                    removeTrap(player);

            case METEOR_SHOWER ->
                    METEOR_COUNTS.remove(player.getUUID());

            default -> {
            }
        }
    }

    public static void cleanupPlayer(ServerPlayer player) {
        endWaterdrop(player);
        removeTrap(player);
        METEOR_COUNTS.remove(player.getUUID());
    }

    private static void createGiftChest(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        BlockPos pos = findSafePosition(player, level);

        if (pos == null) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§c[Random Events] Не удалось найти место для сундука."
                    )
            );
            return;
        }

        level.setBlock(
                pos,
                Blocks.CHEST.defaultBlockState(),
                3
        );

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            level.removeBlock(pos, false);
            return;
        }

        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND, 1 + RANDOM.nextInt(3)),
                new ItemStack(Items.GOLD_INGOT, 2 + RANDOM.nextInt(5)),
                new ItemStack(Items.IRON_INGOT, 4 + RANDOM.nextInt(9)),
                new ItemStack(Items.EMERALD, 1 + RANDOM.nextInt(3)),
                new ItemStack(Items.GOLDEN_APPLE, 1),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 3 + RANDOM.nextInt(5)),
                new ItemStack(Items.COOKED_BEEF, 4 + RANDOM.nextInt(8)),
                new ItemStack(Items.ARROW, 8 + RANDOM.nextInt(17)),
                new ItemStack(Items.ENDER_PEARL, 1 + RANDOM.nextInt(2))
        };

        int itemCount = 3 + RANDOM.nextInt(4);

        for (int i = 0; i < itemCount; i++) {
            ItemStack stack = loot[RANDOM.nextInt(loot.length)].copy();

            int slot = RANDOM.nextInt(chest.getContainerSize());

            if (chest.getItem(slot).isEmpty()) {
                chest.setItem(slot, stack);
            }
        }

        chest.setChanged();

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Random Events] §eПодарок судьбы появился неподалёку!"
                )
        );
    }

    private static void startWaterdrop(ServerPlayer player) {
        UUID id = player.getUUID();

        if (WATERDROPS.containsKey(id)) {
            return;
        }

        ItemStack originalOffhand =
                player.getOffhandItem().copy();

        WaterdropData data = new WaterdropData(
                player.getX(),
                player.getY(),
                player.getZ(),
                originalOffhand
        );

        WATERDROPS.put(id, data);

        player.setItemSlot(
                net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                new ItemStack(Items.WATER_BUCKET)
        );

        double targetY = Math.min(
                player.getY() + 200.0,
                player.serverLevel().getMaxBuildHeight() - 10.0
        );

        player.teleportTo(
                player.getX(),
                targetY,
                player.getZ()
        );

        player.fallDistance = 0.0F;

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§b[Random Events] Ватердроп! Используй ведро воды, чтобы спастись!"
                )
        );
    }

    private static void endWaterdrop(ServerPlayer player) {
        WaterdropData data = WATERDROPS.remove(player.getUUID());

        if (data == null) {
            return;
        }

        player.setItemSlot(
                net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                data.originalOffhand.copy()
        );

        player.fallDistance = 0.0F;

        if (player.isAlive()) {
            player.teleportTo(
                    data.x,
                    data.y,
                    data.z
            );
        }
    }

    private static void createTrap(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        BlockPos chestPos = findSafePosition(player, level);

        if (chestPos == null) {
            return;
        }

        BlockPos tntPos = chestPos.below();

        if (!level.getBlockState(tntPos).isAir()) {
            return;
        }

        level.setBlock(
                tntPos,
                Blocks.TNT.defaultBlockState(),
                3
        );

        level.setBlock(
                chestPos,
                Blocks.CHEST.defaultBlockState(),
                3
        );

        BlockEntity blockEntity = level.getBlockEntity(chestPos);

        if (blockEntity instanceof ChestBlockEntity chest) {
            // Сундук намеренно пустой.
            chest.clearContent();
            chest.setChanged();
        }

        TRAPS.put(
                player.getUUID(),
                new TrapData(level, chestPos, tntPos)
        );

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§6[Random Events] §eГде-то рядом появился подозрительный сундук..."
                )
        );
    }

    private static void removeTrap(ServerPlayer player) {
        TrapData data = TRAPS.remove(player.getUUID());

        if (data == null) {
            return;
        }

        if (data.level.getBlockState(data.chestPos)
                .is(Blocks.CHEST)) {
            data.level.removeBlock(data.chestPos, false);
        }

        if (data.level.getBlockState(data.tntPos)
                .is(Blocks.TNT)) {
            data.level.removeBlock(data.tntPos, false);
        }
    }

    private static void tickMeteorShower(ServerPlayer player) {
        if (!player.isAlive() || player.tickCount % 40 != 0) {
            return;
        }

        UUID id = player.getUUID();

        int count = METEOR_COUNTS.getOrDefault(id, 0);

        if (count >= 24) {
            return;
        }

        ServerLevel level = player.serverLevel();

        int offsetX = RANDOM.nextInt(17) - 8;
        int offsetZ = RANDOM.nextInt(17) - 8;

        int x = player.blockPosition().getX() + offsetX;
        int z = player.blockPosition().getZ() + offsetZ;

        int y = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                x,
                z
        );

        if (y <= level.getMinBuildHeight()
                || y >= level.getMaxBuildHeight() - 2) {
            return;
        }

        // Ограниченная мощность взрыва для уменьшения нагрузки.
        level.explode(
                null,
                x + 0.5,
                y + 0.5,
                z + 0.5,
                2.5F,
                Level.ExplosionInteraction.TNT
        );

        METEOR_COUNTS.put(id, count + 1);
    }

    private static BlockPos findSafePosition(
            ServerPlayer player,
            ServerLevel level
    ) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = player.blockPosition().getX()
                    + RANDOM.nextInt(15) - 7;

            int z = player.blockPosition().getZ()
                    + RANDOM.nextInt(15) - 7;

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos pos = new BlockPos(x, y, z);

            if (y <= level.getMinBuildHeight()
                    || y >= level.getMaxBuildHeight() - 2) {
                continue;
            }

            if (!level.getBlockState(pos).isAir()
                    || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }

            if (!level.getFluidState(pos).isEmpty()
                    || !level.getFluidState(pos.above()).isEmpty()) {
                continue;
            }

            if (!level.getBlockState(pos.below()).isFaceSturdy(
                    level,
                    pos.below(),
                    net.minecraft.core.Direction.UP
            )) {
                continue;
            }

            return pos;
        }

        return null;
    }

    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class EventListeners {

        @SubscribeEvent
        public static void onExperienceDrop(
                LivingExperienceDropEvent event
        ) {
            Player attacker = event.getAttackingPlayer();

            if (!(attacker instanceof ServerPlayer player)) {
                return;
            }

            boolean active = ActiveEventManager
                    .getActiveEvents(player)
                    .stream()
                    .anyMatch(definition ->
                            definition.type() == EventType.EXPERIENCE_FEVER
                    );

            if (!active) {
                return;
            }

            event.setDroppedExperience(
                    event.getDroppedExperience() * 2
            );
        }

        @SubscribeEvent
        public static void onChestOpen(
                PlayerInteractEvent.RightClickBlock event
        ) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            if (event.getLevel().isClientSide()) {
                return;
            }

            TrapData data = TRAPS.get(player.getUUID());

            if (data == null) {
                return;
            }

            if (!data.level.dimension().equals(
                    player.serverLevel().dimension())) {
                return;
            }

            if (!event.getPos().equals(data.chestPos)) {
                return;
            }

            TRAPS.remove(player.getUUID());

            data.level.removeBlock(data.chestPos, false);
            data.level.removeBlock(data.tntPos, false);

            data.level.explode(
                    player,
                    data.tntPos.getX() + 0.5,
                    data.tntPos.getY() + 0.5,
                    data.tntPos.getZ() + 0.5,
                    4.0F,
                    Level.ExplosionInteraction.TNT
            );
        }
    }

    private static final class WaterdropData {
        private final double x;
        private final double y;
        private final double z;
        private final ItemStack originalOffhand;

        private WaterdropData(
                double x,
                double y,
                double z,
                ItemStack originalOffhand
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.originalOffhand = originalOffhand;
        }
    }

    private static final class TrapData {
        private final ServerLevel level;
        private final BlockPos chestPos;
        private final BlockPos tntPos;

        private TrapData(
                ServerLevel level,
                BlockPos chestPos,
                BlockPos tntPos
        ) {
            this.level = level;
            this.chestPos = chestPos;
            this.tntPos = tntPos;
        }
    }
        }
