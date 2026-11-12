
package com.random.events;

import com.random.events.EventDefinition.EventType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class NewEventHandler {

    private static final Random RANDOM = new Random();

    private static final Map<UUID, List<Entity>> EVENT_ENTITIES =
            new HashMap<>();

    private static final Map<UUID, Integer> EXPERIENCE_RAIN_TICKS =
            new HashMap<>();

    private static final Map<UUID, Integer> EXPERIENCE_ORBS_SPAWNED =
            new HashMap<>();

    private static final Map<UUID, Integer> MERCHANT_TICKS =
            new HashMap<>();

    private static final Map<UUID, Boolean> MERCHANT_WARNING_SENT =
            new HashMap<>();

    private static final int MERCHANT_DURATION = 5 * 60 * 20;
    private static final int MERCHANT_WARNING_TIME = 60 * 20;

    private static final int EXPERIENCE_RAIN_DURATION = 15 * 20;

    private NewEventHandler() {
    }

    public static void start(
            ServerPlayer player,
            EventDefinition definition
    ) {
        switch (definition.type()) {

            case PILLAGER_AMBUSH ->
                    spawnPillagerAmbush(player);

            case RAVAGER_WRATH ->
                    spawnRavager(player);

            case SLIME_EPIDEMIC ->
                    spawnSlimeEpidemic(player);

            case IRON_DEFENDER ->
                    spawnIronDefender(player);

            case MYSTERIOUS_MERCHANT ->
                    spawnMerchant(player);

            case EXPERIENCE_RAIN ->
                    startExperienceRain(player);

            case GOLDEN_BLESSING ->
                    startGoldenBlessing(player);

            case CURSED_CHEST ->
                    startCursedChest(player);

            default -> {
            }
        }
    }

    public static void tick(
            ServerPlayer player,
            EventDefinition definition
    ) {
        switch (definition.type()) {

            case IRON_DEFENDER ->
                    protectPlayer(player);

            case MYSTERIOUS_MERCHANT ->
                    tickMerchant(player);

            case EXPERIENCE_RAIN ->
                    tickExperienceRain(player);

            case GOLDEN_BLESSING ->
                    tickGoldenBlessing(player);

            default -> {
            }
        }
    }

    public static boolean isFinished(
            ServerPlayer player,
            EventDefinition definition
    ) {
        UUID id = player.getUUID();

        return switch (definition.type()) {

            case RAVAGER_WRATH ->
                    !player.isAlive();

            case SLIME_EPIDEMIC ->
                    !hasLivingEntities(id);

            case CURSED_CHEST ->
                    !hasLivingEntities(id);

            default -> false;
        };
    }

    public static void end(
            ServerPlayer player,
            EventDefinition definition
    ) {
        UUID id = player.getUUID();

        switch (definition.type()) {

            case EXPERIENCE_RAIN -> {
                EXPERIENCE_RAIN_TICKS.remove(id);
                EXPERIENCE_ORBS_SPAWNED.remove(id);
            }

            case MYSTERIOUS_MERCHANT -> {
                MERCHANT_TICKS.remove(id);
                MERCHANT_WARNING_SENT.remove(id);
            }

            case GOLDEN_BLESSING ->
                    endGoldenBlessing(player);

            case CURSED_CHEST ->
                    removeTrackedEntities(id);

            default -> {
            }
        }

        if (definition.type() == EventType.PILLAGER_AMBUSH
                || definition.type() == EventType.IRON_DEFENDER) {
            removeTrackedEntities(id);
        }

        /*
         * Разрушитель и слизни намеренно не удаляются
         * при обычном завершении события.
         */
    }

    public static void cleanupPlayer(ServerPlayer player) {
        UUID id = player.getUUID();

        /*
         * При выходе игрока не удаляем:
         * - Разрушителя;
         * - слизней;
         * - торговца;
         * - уже появившиеся сферы опыта;
         * - железного защитника.
         *
         * Эти существа должны оставаться в мире.
         */

        EXPERIENCE_RAIN_TICKS.remove(id);
        EXPERIENCE_ORBS_SPAWNED.remove(id);

        MERCHANT_TICKS.remove(id);
        MERCHANT_WARNING_SENT.remove(id);
    }

    private static void spawnPillagerAmbush(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        int count = 5 + RANDOM.nextInt(3);
        int spawned = 0;

        player.sendSystemMessage(
                Component.literal("§c§l[Random Events] ЗАСАДА!")
        );

        for (int i = 0; i < count; i++) {
            Pillager pillager = EntityType.PILLAGER.create(level);

            if (pillager == null) {
                continue;
            }

            pillager.setItemSlot(
                    net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new ItemStack(Items.CROSSBOW)
            );

            pillager.setTarget(player);
            pillager.setPersistenceRequired();

            if (spawnBehindPlayer(player, pillager, level, 12, 24)) {
                track(player, pillager);
                spawned++;
            }
        }

        System.out.println(
                "[Random Events] Pillager Ambush: "
                        + spawned + "/" + count
        );
    }

    private static void spawnRavager(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        Ravager ravager = EntityType.RAVAGER.create(level);

        if (ravager == null) {
            return;
        }

        ravager.setPersistenceRequired();
        ravager.setTarget(player);

        if (spawnBehindPlayer(player, ravager, level, 12, 20)) {
            track(player, ravager);

            player.sendSystemMessage(
                    Component.literal(
                            "§4[Random Events] §cРазрушитель идёт за тобой!"
                    )
            );
        }
    }

    private static void spawnSlimeEpidemic(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        int count = 5 + RANDOM.nextInt(4);
        int spawned = 0;

        for (int i = 0; i < count; i++) {
            Slime slime = EntityType.SLIME.create(level);

            if (slime == null) {
                continue;
            }

            slime.setSize(4, true);
            slime.setPersistenceRequired();

            if (spawnNearPlayer(player, slime, level, 10, 20)) {
                slime.setTarget(player);
                track(player, slime);
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

    private static void spawnIronDefender(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        IronGolem golem = EntityType.IRON_GOLEM.create(level);

        if (golem == null) {
            return;
        }

        golem.setPersistenceRequired();
        golem.setInvulnerable(true);

        golem.setCustomName(
                Component.literal("Железный защитник")
        );

        golem.setCustomNameVisible(true);

        if (spawnNearPlayer(player, golem, level, 3, 8)) {
            track(player, golem);

            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CLOUD,
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
                    net.minecraft.core.particles.ParticleTypes.CRIT,
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
    }

    private static void protectPlayer(ServerPlayer player) {
        UUID id = player.getUUID();

        List<Entity> entities = EVENT_ENTITIES.get(id);

        if (entities == null) {
            return;
        }

        for (Entity entity : entities) {
            if (!(entity instanceof IronGolem golem)
                    || !golem.isAlive()) {
                continue;
            }

            if (golem.tickCount % 10 != 0) {
                continue;
            }

            List<Mob> nearby = golem.level().getEntitiesOfClass(
                    Mob.class,
                    golem.getBoundingBox().inflate(16),
                    mob -> mob.isAlive()
                            && mob != golem
                            && mob != player
                            && mob.getTarget() != null
                            && mob.getTarget().isAlive()
            );

            for (Mob hostile : nearby) {
                if (hostile instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                    continue;
                }

                if (hostile instanceof net.minecraft.world.entity.monster.Enemy) {
                    golem.setTarget(hostile);
                    break;
                }
            }
        }
    }

    private static void spawnMerchant(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        WanderingTrader trader =
                EntityType.WANDERING_TRADER.create(level);

        if (trader == null) {
            return;
        }

        if (!spawnNearPlayer(player, trader, level, 4, 8)) {
            player.sendSystemMessage(
                    Component.literal(
                            "§c[Random Events] Не удалось найти место для торговца."
                    )
            );
            return;
        }

        trader.setPersistenceRequired();
        trader.setDespawnDelay(MERCHANT_DURATION);

        addMerchantTrade(
                trader,
                new ItemStack(Items.DIAMOND, 3),
                32 + RANDOM.nextInt(17),
                new ItemStack(Items.NETHERITE_INGOT)
        );

        addMerchantTrade(
                trader,
                new ItemStack(Items.DIAMOND, 2),
                32 + RANDOM.nextInt(17),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)
        );

        addMerchantTrade(
                trader,
                new ItemStack(Items.DIAMOND, 2),
                32 + RANDOM.nextInt(17),
                new ItemStack(Items.TOTEM_OF_UNDYING)
        );

        addMerchantTrade(
                trader,
                new ItemStack(Items.DIAMOND, 1),
                32 + RANDOM.nextInt(17),
                new ItemStack(Items.ELYTRA)
        );

        addMerchantTrade(
                trader,
                new ItemStack(Items.DIAMOND, 2),
                32 + RANDOM.nextInt(17),
                new ItemStack(Items.DIAMOND_CHESTPLATE)
        );

        track(player, trader);

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

    private static void addMerchantTrade(
            WanderingTrader trader,
            ItemStack additionalCost,
            int emeraldCost,
            ItemStack result
    ) {
        ItemStack emeralds = new ItemStack(
                Items.EMERALD,
                emeraldCost
        );

        MerchantOffer offer = new MerchantOffer(
                emeralds,
                additionalCost,
                result,
                1,
                30,
                0.0F
        );

        trader.getOffers().add(offer);
    }

    private static void tickMerchant(ServerPlayer player) {
        UUID id = player.getUUID();

        Integer remaining = MERCHANT_TICKS.get(id);

        if (remaining == null) {
            return;
        }

        remaining--;

        MERCHANT_TICKS.put(id, remaining);

        if (remaining <= MERCHANT_WARNING_TIME
                && !MERCHANT_WARNING_SENT.getOrDefault(id, false)) {

            MERCHANT_WARNING_SENT.put(id, true);

            player.sendSystemMessage(
                    Component.literal(
                            "§6[Random Events] §eОсталась 1 минута! "
                                    + "Успей совершить покупки!"
                    )
            );
        }
    }

    private static void startExperienceRain(ServerPlayer player) {
        UUID id = player.getUUID();

        EXPERIENCE_RAIN_TICKS.put(id, 0);

        EXPERIENCE_ORBS_SPAWNED.put(
                id,
                0
        );

        player.sendSystemMessage(
                Component.literal(
                        "§a[Random Events] §eНачался опытный дождь!"
                )
        );
    }

    private static void tickExperienceRain(ServerPlayer player) {
        UUID id = player.getUUID();

        int elapsed = EXPERIENCE_RAIN_TICKS.getOrDefault(id, 0);

        if (elapsed >= EXPERIENCE_RAIN_DURATION) {
            return;
        }

        elapsed++;

        EXPERIENCE_RAIN_TICKS.put(id, elapsed);

        if (elapsed % 10 != 0) {
            return;
        }

        int spawned = EXPERIENCE_ORBS_SPAWNED.getOrDefault(id, 0);

        if (spawned >= 30) {
            return;
        }

        int amount = Math.min(
                2 + RANDOM.nextInt(3),
                30 - spawned
        );

        ServerLevel level = player.serverLevel();

        for (int i = 0; i < amount; i++) {
            double x = player.getX() + RANDOM.nextDouble() * 16 - 8;
            double z = player.getZ() + RANDOM.nextDouble() * 16 - 8;

            double y = player.getY() + 12 + RANDOM.nextDouble() * 8;

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
                id,
                spawned + amount
        );
    }

    private static void startGoldenBlessing(ServerPlayer player) {
        player.sendSystemMessage(
                Component.literal(
                        "§6[Random Events] §eЗолотое благословение!"
                )
        );

        tickGoldenBlessing(player);
    }

    private static void tickGoldenBlessing(ServerPlayer player) {
        int duration = 40;

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                duration,
                4,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOW_FALLING,
                duration,
                0,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION,
                duration,
                4,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                duration,
                4,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,
                duration,
                0,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WATER_BREATHING,
                duration,
                0,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.NIGHT_VISION,
                duration,
                0,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,
                duration,
                4,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION,
                duration,
                4,
                false,
                true
        ));

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.SATURATION,
                duration,
                4,
                false,
                true
        ));
    }

    private static void endGoldenBlessing(ServerPlayer player) {
        player.removeEffect(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOW_FALLING
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.REGENERATION
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.WATER_BREATHING
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.NIGHT_VISION
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.ABSORPTION
        );

        player.removeEffect(
                net.minecraft.world.effect.MobEffects.SATURATION
        );
    }

    private static void startCursedChest(ServerPlayer player) {
        player.sendSystemMessage(
                Component.literal(
                        "§5[Random Events] Проклятый сундук ещё не подключён."
                )
        );
    }

    private static boolean hasLivingEntities(UUID playerId) {
        List<Entity> entities = EVENT_ENTITIES.get(playerId);

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

    private static void removeTrackedEntities(UUID playerId) {
        List<Entity> entities = EVENT_ENTITIES.remove(playerId);

        if (entities == null) {
            return;
        }

        for (Entity entity : entities) {
            if (entity.isAlive()) {
                entity.discard();
            }
        }
    }

    private static void track(
            ServerPlayer player,
            Entity entity
    ) {
        EVENT_ENTITIES.computeIfAbsent(
                player.getUUID(),
                id -> new ArrayList<>()
        ).add(entity);
    }

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

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    blockX,
                    blockZ
            );

            BlockPos pos = new BlockPos(
                    blockX,
                    y,
                    blockZ
            );

            if (!level.hasChunkAt(pos)
                    || y <= level.getMinBuildHeight()
                    || y >= level.getMaxBuildHeight() - 3) {
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

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    blockX,
                    blockZ
            );

            BlockPos pos = new BlockPos(blockX, y, blockZ);

            if (!level.hasChunkAt(pos)
                    || y <= level.getMinBuildHeight()
                    || y >= level.getMaxBuildHeight() - 3) {
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
