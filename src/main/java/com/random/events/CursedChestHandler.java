
package com.random.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class CursedChestHandler {

    private static final Random RANDOM = new Random();

    private static final Map<UUID, ChestData> CHESTS =
            new HashMap<>();

    private CursedChestHandler() {
    }

    /**
     * Создаёт проклятый сундук рядом с инициатором события.
     */
    public static boolean start(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Не допускаем два проклятых сундука от одного события.
        remove(player);

        BlockPos pos = findSafePosition(player, level);

        if (pos == null) {
            player.sendSystemMessage(Component.literal(
                    "§c[Random Events] Не удалось найти место для проклятого сундука."
            ));
            return false;
        }

        if (!level.setBlock(
                pos,
                Blocks.CHEST.defaultBlockState(),
                3
        )) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            level.removeBlock(pos, false);
            return false;
        }

        chest.clearContent();
        chest.setChanged();

        CHESTS.put(
                player.getUUID(),
                new ChestData(level, pos.immutable())
        );

        level.sendParticles(
                ParticleTypes.SOUL,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                12,
                0.35,
                0.25,
                0.35,
                0.02
        );

        player.sendSystemMessage(Component.literal(
                "§5[Random Events] §dПроклятый сундук появился неподалёку..."
        ));

        return true;
    }

    /**
     * Проверяет, существует ли ещё не открытый сундук события.
     */
    public static boolean isActive(ServerPlayer player) {
        ChestData data = CHESTS.get(player.getUUID());

        if (data == null) {
            return false;
        }

        if (!data.level.hasChunkAt(data.pos)) {
            CHESTS.remove(player.getUUID());
            return false;
        }

        return data.level.getBlockState(data.pos).is(Blocks.CHEST);
    }

    /**
     * Удаляет сундук при завершении события,
     * смерти или выходе инициатора.
     */
    public static void remove(ServerPlayer player) {
        ChestData data = CHESTS.remove(player.getUUID());

        if (data == null) {
            return;
        }

        if (data.level.hasChunkAt(data.pos)
                && data.level.getBlockState(data.pos).is(Blocks.CHEST)) {
            data.level.removeBlock(data.pos, false);
        }
    }

    /**
     * Обрабатывает открытие проклятого сундука.
     * Запись удаляется до выдачи награды или взрыва,
     * поэтому повторно открыть его и получить награду нельзя.
     */
    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ChestListener {

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

            ChestData matched = null;
            UUID ownerId = null;

            for (Map.Entry<UUID, ChestData> entry : CHESTS.entrySet()) {
                ChestData data = entry.getValue();

                if (data.level.dimension().equals(
                        player.serverLevel().dimension()
                ) && data.pos.equals(event.getPos())) {
                    matched = data;
                    ownerId = entry.getKey();
                    break;
                }
            }

            if (matched == null || ownerId == null) {
                return;
            }

            // Не даём обычному GUI сундука открыться.
            event.setCanceled(true);

            // Удаляем запись до любого действия:
            // повторное нажатие уже не сможет повторить результат.
            CHESTS.remove(ownerId);

            ServerLevel level = matched.level;
            BlockPos pos = matched.pos;

            if (level.hasChunkAt(pos)
                    && level.getBlockState(pos).is(Blocks.CHEST)) {
                level.removeBlock(pos, false);
            }

            if (RANDOM.nextBoolean()) {
                triggerTrap(player, level, pos);
            } else {
                giveReward(player, level, pos);
            }
        }
    }

    /**
     * Ловушка: взрыв и принудительная смерть открывшего игрока.
     * Прямой вызов die используется для обхода обычной
     * проверки тотема при получении смертельного урона.
     */
    private static void triggerTrap(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos
    ) {
        player.sendSystemMessage(Component.literal(
                "§4§l[Random Events] ПРОКЛЯТИЕ! СУНДУК БЫЛ ЛОВУШКОЙ!"
        ));

        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                35,
                0.8,
                0.7,
                0.8,
                0.08
        );

        level.explode(
                player,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                5.0F,
                Level.ExplosionInteraction.TNT
        );

        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(level.damageSources().genericKill());
        }
    }

    /**
     * Награда: от 1 до 8 случайных ценных предметов.
     */
    private static void giveReward(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos
    ) {
        List<ItemStack> loot = createLoot();

        int amount = 1 + RANDOM.nextInt(8);

        player.sendSystemMessage(Component.literal(
                "§6[Random Events] §eСундук оказался сокровищем! Ты получил награду."
        ));

        for (int i = 0; i < amount; i++) {
            ItemStack stack = loot.get(
                    RANDOM.nextInt(loot.size())
            ).copy();

            if (!player.getInventory().add(stack)) {
                ItemEntity itemEntity = new ItemEntity(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() + 0.7,
                        pos.getZ() + 0.5,
                        stack
                );

                level.addFreshEntity(itemEntity);
            }
        }

        level.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                30,
                0.5,
                0.5,
                0.5,
                0.1
        );
    }

    private static List<ItemStack> createLoot() {
        List<ItemStack> loot = new ArrayList<>();

        loot.add(new ItemStack(Items.DIAMOND, 3 + RANDOM.nextInt(6)));
        loot.add(new ItemStack(Items.NETHERITE_INGOT, 1));
        loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
        loot.add(new ItemStack(Items.TOTEM_OF_UNDYING, 1));
        loot.add(new ItemStack(Items.ELYTRA, 1));
        loot.add(new ItemStack(Items.NETHERITE_SWORD, 1));
        loot.add(new ItemStack(Items.NETHERITE_CHESTPLATE, 1));
        loot.add(new ItemStack(Items.DIAMOND_BLOCK, 1 + RANDOM.nextInt(3)));
        loot.add(new ItemStack(Items.GOLDEN_APPLE, 2 + RANDOM.nextInt(5)));
        loot.add(new ItemStack(Items.EMERALD_BLOCK, 2 + RANDOM.nextInt(5)));
        loot.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 16 + RANDOM.nextInt(17)));

        return loot;
    }

    /**
     * Ищет свободное место на поверхности.
     * Не заменяет существующие блоки и контейнеры.
     */
    private static BlockPos findSafePosition(
            ServerPlayer player,
            ServerLevel level
    ) {
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = player.blockPosition().getX()
                    + RANDOM.nextInt(17) - 8;

            int z = player.blockPosition().getZ()
                    + RANDOM.nextInt(17) - 8;

            BlockPos check = new BlockPos(
                    x,
                    player.blockPosition().getY(),
                    z
            );

            if (!level.hasChunkAt(check)) {
                continue;
            }

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            if (y <= level.getMinBuildHeight()
                    || y >= level.getMaxBuildHeight() - 2) {
                continue;
            }

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();

            if (!level.getBlockState(pos).isAir()
                    || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }

            if (!level.getFluidState(pos).isEmpty()
                    || !level.getFluidState(pos.above()).isEmpty()) {
                continue;
            }

            if (!level.getBlockState(below).isFaceSturdy(
                    level,
                    below,
                    Direction.UP
            )) {
                continue;
            }

            if (level.getBlockEntity(pos) != null) {
                continue;
            }

            return pos;
        }

        return null;
    }

    private static final class ChestData {

        private final ServerLevel level;
        private final BlockPos pos;

        private ChestData(ServerLevel level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }
    }
}
