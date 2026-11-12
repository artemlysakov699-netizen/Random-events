
package com.random.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class NewEventHandler {

    private static final Random RANDOM = new Random();

    private static final Map<UUID, Map<String, List<Entity>>> EVENT_ENTITIES =
            new HashMap<>();

    private static final Map<UUID, Integer> EXPERIENCE_RAIN_TICKS =
            new HashMap<>();

    private static final Map<UUID, Integer> EXPERIENCE_ORBS_SPAWNED =
            new HashMap<>();

    private static final Map<UUID, Integer> EXPERIENCE_RAIN_TARGETS =
            new HashMap<>();

    private static final Map<UUID, Integer> MERCHANT_TICKS =
            new HashMap<>();

    private static final Map<UUID, Boolean> MERCHANT_WARNING_SENT =
            new HashMap<>();

    private static final int MERCHANT_DURATION = 5 * 60 * 20;
    private static final int MERCHANT_WARNING_TIME = 60 * 20;

    private static final int EXPERIENCE_RAIN_DURATION = 15 * 20;
    private static final int EXPERIENCE_RAIN_MIN_ORBS = 20;
    private static final int EXPERIENCE_RAIN_MAX_ORBS = 30;

    private NewEventHandler() {
    }

    // =====================================================
    // ЗАПУСК СОБЫТИЙ
    // =====================================================

    public static void start(
            ServerPlayer player,
            EventDefinition definition
    ) {
        switch (definition.type()) {

            case PILLAGER_AMBUSH ->
                    spawnPillagerAmbush(player, definition);

            case RAVAGER_WRATH ->
                    spawnRavager(player, definition);

            case SLIME_EPIDEMIC ->
                    spawnSlimeEpidemic(player, definition);

            case IRON_DEFENDER ->
                    spawnIronDefender(player, definition);

            case MYSTERIOUS_MERCHANT ->
                    spawnMerchant(player, definition);

            case EXPERIENCE_RAIN ->
                    startExperienceRain(player);

            case GOLDEN_BLESSING ->
                    GoldenBlessingHandler.start(player);

            case CURSED_CHEST ->
                    CursedChestHandler.start(player);

            default -> {
            }
        }
    }

    // =====================================================
    // ОБНОВЛЕНИЕ СОБЫТИЙ
    // =====================================================

    public static void tick(
            ServerPlayer player,
            EventDefinition definition
    ) {
        switch (definition.type()) {

            case IRON_DEFENDER ->
                    protectPlayer(player, definition);

            case MYSTERIOUS_MERCHANT ->
                    tickMerchant(player);

            case EXPERIENCE_RAIN ->
                    tickExperienceRain(player);

            case GOLDEN_BLESSING ->
                    GoldenBlessingHandler.tick(player);

            default -> {
            }
        }
    }

    // =====================================================
    // ПРОВЕРКА ЗАВЕРШЕНИЯ
    // =====================================================

    public static boolean isFinished(
            ServerPlayer player,
            EventDefinition definition
    ) {
        UUID playerId = player.getUUID();

        return switch (definition.type()) {

            case RAVAGER_WRATH ->
                    !player.isAlive();

            case SLIME_EPIDEMIC ->
                    !hasLivingEntities(playerId, definition.id());

            case PILLAGER_AMBUSH ->
                    !hasLivingEntities(playerId, definition.id());

            case CURSED_CHEST ->
                    !CursedChestHandler.isActive(player);

            default -> false;
        };
    }

    // =====================================================
    // ЗАВЕРШЕНИЕ СОБЫТИЙ
    // =====================================================

    public static void end(
            ServerPlayer player,
            EventDefinition definition
    ) {
        UUID playerId = player.getUUID();

        switch (definition.type()) {

            case EXPERIENCE_RAIN -> {
                EXPERIENCE_RAIN_TICKS.remove(playerId);
                EXPERIENCE_ORBS_SPAWNED.remove(playerId);
                EXPERIENCE_RAIN_TARGETS.remove(playerId);
            }

            case MYSTERIOUS_MERCHANT -> {
                MERCHANT_TICKS.remove(playerId);
                MERCHANT_WARNING_SENT.remove(playerId);

                // Торговца не удаляем:
                // его собственный таймер завершит его жизнь.
                forgetTrackedEntities(
                        playerId,
                        definition.id()
                );
            }

            case GOLDEN_BLESSING -> {
                // Таймер управляется GoldenBlessingHandler.
            }

            case CURSED_CHEST ->
                    CursedChestHandler.remove(player);

            case PILLAGER_AMBUSH,
                 IRON_DEFENDER ->
                    removeTrackedEntities(
                            playerId,
                            definition.id()
                    );

            case RAVAGER_WRATH,
                 SLIME_EPIDEMIC ->
                    forgetTrackedEntities(
                            playerId,
                            definition.id()
                    );

            default -> {
            }
        }
    }

    // =====================================================
    // ОЧИСТКА ПРИ ВЫХОДЕ ИГРОКА
    // =====================================================

    public static void cleanupPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();

        CursedChestHandler.remove(player);

        EVENT_ENTITIES.remove(playerId);

        EXPERIENCE_RAIN_TICKS.remove(playerId);
        EXPERIENCE_ORBS_SPAWNED.remove(playerId);
        EXPERIENCE_RAIN_TARGETS.remove(playerId);

        MERCHANT_TICKS.remove(playerId);
        MERCHANT_WARNING_SENT.remove(playerId);

        /*
         * Благословение хранится в PersistentData игрока.
         * Таймер железного защитника хранится у самого голема.
         * Таймер торговца хранится у самого торговца.
         */
    }

    // =====================================================
    // ЗАСАДА РАЗБОЙНИКОВ
    // =====================================================

    private static void spawnPillagerAmbush(
            ServerPlayer player,
            EventDefinition definition
    ) {
        ServerLevel level = player.serverLevel();

        int count = 5 + RANDOM.nextInt(3);
        int spawned = 0;

        player.sendSystemMessage(
                Component.literal("§c§l[Random Events] ЗАСАДА!")
        );

        for (int i = 0; i < count; i++) {

            Pillager pillager =
                    EntityType.PILLAGER.create(level);

            if (pillager == null) {
                continue;
            }

            pillager.setItemSlot(
                    EquipmentSlot.MAINHAND,
                    new ItemStack(Items.CROSSBOW)
            );

            pillager.setTarget(player);
            pillager.setPersistenceRequired();

            if (spawnBehindPlayer(
                    player,
                    pillager,
                    level,
                    12,
                    24
            )) {
                track(player, definition, pillager);
                spawned++;
            }
        }

        System.out.println(
                "[Random Events] Pillager Ambush: "
                        + spawned + "/" + count
        );
    }

    // =====================================================
    // ЯРОСТЬ РАЗРУШИТЕЛЯ
    // =====================================================

    private static void spawnRavager(
            ServerPlayer player,
            EventDefinition definition
    ) {
        ServerLevel level = player.serverLevel();

        Ravager ravager =
                EntityType.RAVAGER.create(level);

        if (ravager == null) {
            return;
        }

        ravager.setPersistenceRequired();
        ravager.setTarget(player);

        if (ravager.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            ravager.getAttribute(Attributes.ATTACK_DAMAGE)
                    .setBaseValue(50.0);
        }

        if (spawnBehindPlayer(
                player,
                ravager,
                level,
                12,
                20
        )) {
            track(player, definition, ravager);

            player.sendSystemMessage(
                    Component.literal(
                            "§4[Random Events] §cРазрушитель идёт за тобой!"
                    )
            );
        }
    }

    // =====================================================
    // СЛИЗНЕВАЯ ЭПИДЕМИЯ
    // =====================================================

    private static void spawnSlimeEpidemic(
            ServerPlayer player,
            EventDefinition definition
    ) {
        ServerLevel level = player.serverLevel();

        int count = 5 + RANDOM.nextInt(4);
        int spawned = 0;

        for (int i = 0; i < count; i++) {

            Slime slime =
                    EntityType.SLIME.create(level);

            if (slime == null) {
                continue;
            }

            slime.setSize(4, true);
            slime.setPersistenceRequired();

            if (spawnNearPlayer(
                    player,
                    slime,
                    level,
                    10,
                    20
            )) {
                slime.setTarget(player);

                track(player, definition, slime);
                spawned++;
            }
        }

        player.sendSystemMessage(
                Component.literal(
                        "§a[Random Events] §2Началась слизневая эпидемия!"
                )
        );

        System.out.println(
                "[Random Events] Slime Epidemic: "
                        + spawned + "/" + count
        );
    }

    // =====================================================
    // ЖЕЛЕЗНЫЙ ЗАЩИТНИК
    // =====================================================

    private static void spawnIronDefender(
            ServerPlayer player,
            EventDefinition definition
    ) {
        ServerLevel level = player.serverLevel();

        IronGolem golem =
                EntityType.IRON_GOLEM.create(level);

        if (golem == null) {
            return;
        }

        golem.setPersistenceRequired();
        golem.setInvulnerable(true);

        golem.setCustomName(
                Component.literal("Железный защитник")
        );

        golem.setCustomNameVisible(true);

        if (golem.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            golem.getAttribute(Attributes.ATTACK_DAMAGE)
                    .setBaseValue(100.0);
        }

        if (!spawnNearPlayer(
                player,
                golem,
                level,
                3,
                8
        )) {
            return;
        }

        TimedEntityHandler.setDefenderLifetime(golem);

        track(player, definition, golem);

        level.sendParticles(
                ParticleTypes.CLOUD,
                golem.getX(),
                golem.getY() + 1,
                golem.getZ(),
                30,
                0.8,
                1.0,
                0.8,
                0.1
        );

        level.sendParticles(
                ParticleTypes.CRIT,
                golem.getX(),
                golem.getY() + 1,
                golem.getZ(),
                25,
                0.7,
                0.8,
                0.7,
                0.1
        );

        player.sendSystemMessage(
                Component.literal(
                        "§6[Random Events] §eЖелезный защитник прибыл!"
                )
        );
    }

    private static void protectPlayer(
            ServerPlayer player,
            EventDefinition definition
    ) {
        List<Entity> entities = getTrackedEntities(
                player.getUUID(),
                definition.id()
        );

        if (entities == null || player.tickCount % 10 != 0) {
            return;
        }

        for (Entity entity : entities) {

            if (!(entity instanceof IronGolem golem)
                    || !golem.isAlive()) {
                continue;
            }

            List<Mob> nearby = golem.level().getEntitiesOfClass(
                    Mob.class,
                    golem.getBoundingBox().inflate(16),
                    mob -> mob.isAlive()
                            && mob != golem
                            && mob instanceof Enemy
                            && mob.getType() != EntityType.ENDER_DRAGON
            );

            Mob closestHostile = null;
            double closestDistance = Double.MAX_VALUE;

            for (Mob hostile : nearby) {
                double distance = golem.distanceToSqr(hostile);

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHostile = hostile;
                }
            }

            if (closestHostile != null) {
                golem.setTarget(closestHostile);
            }
        }
    }

    // =====================================================
    // ЗАГАДОЧНЫЙ ТОРГОВЕЦ
    // =====================================================

    private static void spawnMerchant(
            ServerPlayer player,
            EventDefinition definition
    ) {
        ServerLevel level = player.serverLevel();

        WanderingTrader trader =
                EntityType.WANDERING_TRADER.create(level);

        if (trader == null) {
            return;
        }

        if (!spawnNearPlayer(
                player,
                trader,
                level,
                4,
                8
        )) {
            player.sendSystemMessage(
                    Component.literal(
                            "§c[Random Events] Не удалось найти место для торговца."
                    )
            );
            return;
        }

        // Независимый таймер на 5 минут.
        TimedTraderHandler.setLifetime(trader);

        addMerchantTrade(
                trader,
                randomEmeraldPrice(),
                new ItemStack(Items.NETHERITE_INGOT)
        );

        addMerchantTrade(
                trader,
                randomEmeraldPrice(),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)
        );

        addMerchantTrade(
                trader,
                randomEmeraldPrice(),
                new ItemStack(Items.TOTEM_OF_UNDYING)
        );

        addMerchantTrade(
                trader,
                randomEmeraldPrice(),
                new ItemStack(Items.ELYTRA)
        );

        addMerchantTrade(
                trader,
                randomEmeraldPrice(),
                new ItemStack(Items.DIAMOND_CHESTPLATE)
        );

        track(player, definition, trader);

        MERCHANT_TICKS.put(
                player.getUUID(),
                MERCHANT_DURATION
        );

        MERCHANT_WARNING_SENT.put(
                player.getUUID(),
                false
        );

        player.sendSystemMessage(
                Component.literal(
                        "§6[Random Events] §eЗагадочный торговец появился рядом!"
                )
        );
    }

    private static int randomEmeraldPrice() {
        return 32 + RANDOM.nextInt(33);
    }

    private static void addMerchantTrade(
            WanderingTrader trader,
            int emeraldCost,
            ItemStack result
    ) {
        ItemStack emeralds = new ItemStack(
                Items.EMERALD,
                emeraldCost
        );

        MerchantOffer offer = new MerchantOffer(
                emeralds,
                result,
                1,
                30,
                0.0F
        );

        trader.getOffers().add(offer);
    }

    private static void tickMerchant(ServerPlayer player) {
        UUID playerId = player.getUUID();

        Integer remaining = MERCHANT_TICKS.get(playerId);

        if (remaining == null) {
            return;
        }

        remaining--;

        MERCHANT_TICKS.put(playerId, remaining);

        if (remaining <= MERCHANT_WARNING_TIME
                && !MERCHANT_WARNING_SENT.getOrDefault(
                        playerId,
                        false
                )) {

            MERCHANT_WARNING_SENT.put(playerId, true);

            player.sendSystemMessage(
                    Component.literal(
                            "§6[Random Events] §eОсталась 1 минута! "
                                    + "Успей совершить покупки!"
                    )
            );
        }
    }

    // =====================================================
    // ОПЫТНЫЙ ДОЖДЬ
    // =====================================================

    private static void startExperienceRain(ServerPlayer player) {
        UUID playerId = player.getUUID();

        EXPERIENCE_RAIN_TICKS.put(playerId, 0);
        EXPERIENCE_ORBS_SPAWNED.put(playerId, 0);

        int target = EXPERIENCE_RAIN_MIN_ORBS
                + RANDOM.nextInt(
                        EXPERIENCE_RAIN_MAX_ORBS
                                - EXPERIENCE_RAIN_MIN_ORBS + 1
                );

        EXPERIENCE_RAIN_TARGETS.put(
                playerId,
                target
        );

        player.sendSystemMessage(
                Component.literal(
                        "§a[Random Events] §eНачался опытный дождь!"
                )
        );
    }

    private static void tickExperienceRain(ServerPlayer player) {
        UUID playerId = player.getUUID();

        int elapsed = EXPERIENCE_RAIN_TICKS.getOrDefault(
                playerId,
                0
        );

        int target = EXPERIENCE_RAIN_TARGETS.getOrDefault(
                playerId,
                EXPERIENCE_RAIN_MIN_ORBS
        );

        if (elapsed >= EXPERIENCE_RAIN_DURATION) {
            return;
        }

        elapsed++;

        EXPERIENCE_RAIN_TICKS.put(
                playerId,
                elapsed
        );

        // Равномерно распределяем сферы по всем 15 секундам.
        if (elapsed % 10 != 0
                && elapsed != EXPERIENCE_RAIN_DURATION) {
            return;
        }

        int spawned = EXPERIENCE_ORBS_SPAWNED.getOrDefault(
                playerId,
                0
        );

        int desiredTotal = (int) Math.ceil(
                (double) target * elapsed / EXPERIENCE_RAIN_DURATION
        );

        int amount = Math.max(
                0,
                desiredTotal - spawned
        );

        ServerLevel level = player.serverLevel();

        for (int i = 0; i < amount; i++) {

            double x = player.getX()
                    + RANDOM.nextDouble() * 16 - 8;

            double z = player.getZ()
                    + RANDOM.nextDouble() * 16 - 8;

            double y = Math.min(
                    player.getY() + 12 + RANDOM.nextDouble() * 8,
                    level.getMaxBuildHeight() - 2
            );

            // Обычная малая сфера даёт 1 XP.
            // Здесь каждая сфера даёт 2 XP.
            ExperienceOrb orb = new ExperienceOrb(
                    level,
                    x,
                    y,
                    z,
                    2
            );

            level.addFreshEntity(orb);
        }

        EXPERIENCE_ORBS_SPAWNED.put(
                playerId,
                spawned + amount
        );
    }

    // =====================================================
    // ОТСЛЕЖИВАНИЕ СУЩЕСТВ
    // =====================================================

    private static void track(
            ServerPlayer player,
            EventDefinition definition,
            Entity entity
    ) {
        EVENT_ENTITIES
                .computeIfAbsent(
                        player.getUUID(),
                        ignored -> new HashMap<>()
                )
                .computeIfAbsent(
                        definition.id(),
                        ignored -> new ArrayList<>()
                )
                .add(entity);
    }

    private static List<Entity> getTrackedEntities(
            UUID playerId,
            String eventId
    ) {
        Map<String, List<Entity>> playerEntities =
                EVENT_ENTITIES.get(playerId);

        if (playerEntities == null) {
            return null;
        }

        return playerEntities.get(eventId);
    }

    private static boolean hasLivingEntities(
            UUID playerId,
            String eventId
    ) {
        List<Entity> entities = getTrackedEntities(
                playerId,
                eventId
        );

        if (entities == null || entities.isEmpty()) {
            return false;
        }

        for (Entity entity : entities) {
            if (entity.isAlive()) {
                return true;
            }
        }

        return false;
    }

    private static void removeTrackedEntities(
            UUID playerId,
            String eventId
    ) {
        List<Entity> entities = getTrackedEntities(
                playerId,
                eventId
        );

        if (entities != null) {
            for (Entity entity : entities) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }
        }

        forgetTrackedEntities(playerId, eventId);
    }

    private static void forgetTrackedEntities(
            UUID playerId,
            String eventId
    ) {
        Map<String, List<Entity>> playerEntities =
                EVENT_ENTITIES.get(playerId);

        if (playerEntities == null) {
            return;
        }

        playerEntities.remove(eventId);

        if (playerEntities.isEmpty()) {
            EVENT_ENTITIES.remove(playerId);
        }
    }

    // =====================================================
    // ПОИСК МЕСТА ДЛЯ ПОЯВЛЕНИЯ
    // =====================================================

    private static boolean spawnNearPlayer(
            ServerPlayer player,
            Entity entity,
            ServerLevel level,
            int minDistance,
            int maxDistance
    ) {
        for (int attempt = 0; attempt < 40; attempt++) {

            double angle = RANDOM.nextDouble() * Math.PI * 2;

            double distance = minDistance
                    + RANDOM.nextDouble()
                    * (maxDistance - minDistance);

            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;

            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);

            BlockPos checkPos = new BlockPos(
                    blockX,
                    player.blockPosition().getY(),
                    blockZ
            );

            if (!level.hasChunkAt(checkPos)) {
                continue;
            }

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    blockX,
                    blockZ
            );

            if (y <= level.getMinBuildHeight()
                    || y >= level.getMaxBuildHeight() - 3) {
                continue;
            }

            BlockPos feet = new BlockPos(blockX, y, blockZ);
            BlockPos floor = feet.below();
            BlockPos head = feet.above();

            if (!level.getBlockState(floor).isFaceSturdy(
                    level,
                    floor,
                    Direction.UP
            )) {
                continue;
            }

            if (!level.getBlockState(feet).isAir()
                    || !level.getBlockState(head).isAir()) {
                continue;
            }

            if (!level.getFluidState(feet).isEmpty()
                    || !level.getFluidState(head).isEmpty()) {
                continue;
            }

            entity.moveTo(
                    x,
                    y,
                    z,
                    RANDOM.nextFloat() * 360,
                    0
            );

            if (!level.noCollision(entity)) {
                continue;
            }

            return level.addFreshEntity(entity);
        }

        return false;
    }

    private static boolean spawnBehindPlayer(
            ServerPlayer player,
            Entity entity,
            ServerLevel level,
            int minDistance,
            int maxDistance
    ) {
        double yaw = Math.toRadians(player.getYRot());

        double behindX = Math.sin(yaw);
        double behindZ = -Math.cos(yaw);

        for (int attempt = 0; attempt < 40; attempt++) {

            double distance = minDistance
                    + RANDOM.nextDouble()
                    * (maxDistance - minDistance);

            double x = player.getX() + behindX * distance;
            double z = player.getZ() + behindZ * distance;

            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);

            BlockPos checkPos = new BlockPos(
                    blockX,
                    player.blockPosition().getY(),
                    blockZ
            );

            if (!level.hasChunkAt(checkPos)) {
                continue;
            }

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    blockX,
                    blockZ
            );

            if (y <= level.getMinBuildHeight()
                    || y >= level.getMaxBuildHeight() - 3) {
                continue;
            }

            BlockPos feet = new BlockPos(blockX, y, blockZ);
            BlockPos floor = feet.below();
            BlockPos head = feet.above();

            if (!level.getBlockState(floor).isFaceSturdy(
                    level,
                    floor,
                    Direction.UP
            )) {
                continue;
            }

            if (!level.getBlockState(feet).isAir()
                    || !level.getBlockState(head).isAir()) {
                continue;
            }

            if (!level.getFluidState(feet).isEmpty()
                    || !level.getFluidState(head).isEmpty()) {
                continue;
            }

            entity.moveTo(
                    x,
                    y,
                    z,
                    RANDOM.nextFloat() * 360,
                    0
            );

            if (!level.noCollision(entity)) {
                continue;
            }

            return level.addFreshEntity(entity);
        }

        return false;
    }
}
