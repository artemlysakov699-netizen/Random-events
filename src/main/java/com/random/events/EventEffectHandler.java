
package com.random.events;

import com.random.events.EventDefinition.EventType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class EventEffectHandler {

    private static final Random RANDOM = new Random();

    private static final String EVENT_CREEPER_TAG =
            "RandomEventsProtectedCreeper";

    private static final Map<UUID, Map<String, List<Entity>>> TRACKED_ENTITIES =
            new HashMap<>();

    private EventEffectHandler() {}

    public static void start(ServerPlayer player, EventDefinition definition) {
        ServerLevel level = player.serverLevel();

        switch (definition.type()) {
            case SUPER_SPEED ->
                    player.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED,
                            definition.durationTicks(), 1));

            case JUMP_BOOST ->
                    player.addEffect(new MobEffectInstance(
                            MobEffects.JUMP,
                            definition.durationTicks(), 1));

            case REGENERATION ->
                    player.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION,
                            definition.durationTicks(), 1));

            case WEAKNESS ->
                    player.addEffect(new MobEffectInstance(
                            MobEffects.WEAKNESS,
                            definition.durationTicks(), 0));

            case SLOWNESS ->
                    player.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN,
                            definition.durationTicks(), 1));

            case HUNGER ->
                    player.addEffect(new MobEffectInstance(
                            MobEffects.HUNGER,
                            definition.durationTicks(), 1));

            case BLESSING -> {
                player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION,
                        definition.durationTicks(), 1));
                player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        definition.durationTicks(), 0));
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        definition.durationTicks(), 0));
            }

            case ZOMBIE_HORDE ->
                    spawnZombieHorde(player, level, definition);

            case SPIDER_HUNT ->
                    spawnSpiderHunt(player, level, definition);

            case WRATH_OF_SKY ->
                    summonLightning(player, level, definition);

            case CREEPER_AMBUSH ->
                    spawnCreepers(player, level, definition);

            case CHICKEN_RAIN ->
                    spawnChickenRain(player, level, definition);

            case WARDEN_HUNT ->
                    spawnWarden(player, level, definition);

            case ZOMBIE_BOSS ->
                    spawnZombieBoss(player, level, definition);

            case HELPERS ->
                    spawnHelpers(player, level);

            case SOLAR_BURST, DENSE_FOG, GIFT_OF_FATE,
                 EXPERIENCE_FEVER, WATERDROP, METEOR_SHOWER,
                 TRAPPED_CHEST -> {
                // Эти механики запускаются отдельным обработчиком.
            }
        }

        EventMechanicsHandler.start(player, definition);
    }

    public static void tick(ServerPlayer player, EventDefinition definition) {
        if (definition.type() == EventType.SOLAR_BURST) {
            ServerLevel level = player.serverLevel();

            boolean daytime = level.isDay();
            boolean canSeeSky = level.canSeeSky(player.blockPosition());
            boolean raining = level.isRaining();

            if (daytime && canSeeSky && !raining
                    && player.isAlive()
                    && !player.isInWaterRainOrBubble()
                    && player.tickCount % 20 == 0) {
                player.setRemainingFireTicks(40);
            }
        }

        if (definition.type() == EventType.WRATH_OF_SKY
                && player.tickCount % 40 == 0) {
            summonLightning(player, player.serverLevel(), definition);
        }

        EventMechanicsHandler.tick(player, definition);
    }

    public static void end(ServerPlayer player, EventDefinition definition) {
        EventMechanicsHandler.end(player, definition);

        if (definition.type() == EventType.HELPERS) {
            return;
        }

        UUID playerId = player.getUUID();
        Map<String, List<Entity>> playerEntities =
                TRACKED_ENTITIES.get(playerId);

        if (playerEntities == null) {
            return;
        }

        List<Entity> entities = playerEntities.remove(definition.id());

        if (entities != null) {
            for (Entity entity : entities) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }
        }

        if (playerEntities.isEmpty()) {
            TRACKED_ENTITIES.remove(playerId);
        }
    }

    public static void cleanupPlayer(ServerPlayer player) {
        EventMechanicsHandler.cleanupPlayer(player);

        UUID playerId = player.getUUID();

        Map<String, List<Entity>> playerEntities =
                TRACKED_ENTITIES.remove(playerId);

        if (playerEntities == null) {
            return;
        }

        for (List<Entity> entities : playerEntities.values()) {
            for (Entity entity : entities) {
                if (entity.isAlive()) {
                    entity.discard();
                }
            }
        }
    }

    private static void spawnZombieHorde(
            ServerPlayer player,
            ServerLevel level,
            EventDefinition definition
    ) {
        for (int i = 0; i < 10; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null
                    && spawnNearPlayer(player, zombie, level)) {
                trackEntity(player, definition, zombie);
            }
        }
    }

    private static void spawnSpiderHunt(
            ServerPlayer player,
            ServerLevel level,
            EventDefinition definition
    ) {
        int count = 5 + RANDOM.nextInt(3);

        for (int i = 0; i < count; i++) {
            Spider spider = EntityType.SPIDER.create(level);
            if (spider != null
                    && spawnNearPlayer(player, spider, level)) {
                trackEntity(player, definition, spider);
            }
        }
    }

    private static void spawnCreepers(
            ServerPlayer player,
            ServerLevel level,
            EventDefinition definition
    ) {
        for (int i = 0; i < 3; i++) {
            Creeper creeper = EntityType.CREEPER.create(level);

            if (creeper == null) {
                continue;
            }

            creeper.getPersistentData().putBoolean(
                    EVENT_CREEPER_TAG, true);

            if (spawnNearPlayer(player, creeper, level)) {
                trackEntity(player, definition, creeper);
            }
        }
    }

    private static void spawnChickenRain(
            ServerPlayer player,
            ServerLevel level,
            EventDefinition definition
    ) {
        for (int i = 0; i < 5; i++) {
            Chicken chicken = EntityType.CHICKEN.create(level);
            if (chicken == null) {
                continue;
            }

            double x = player.getX() + RANDOM.nextInt(7) - 3;
            double z = player.getZ() + RANDOM.nextInt(7) - 3;
            double y = Math.min(
                    player.getY() + 20,
                    level.getMaxBuildHeight() - 2
            );

            chicken.moveTo(
                    x, y, z,
                    RANDOM.nextFloat() * 360.0F, 0.0F
            );

            if (level.addFreshEntity(chicken)) {
                trackEntity(player, definition, chicken);
            }
        }
    }

    private static void summonLightning(
            ServerPlayer player,
            ServerLevel level,
            EventDefinition definition
    ) {
        double x = player.getX() + RANDOM.nextInt(13) - 6;
        double z = player.getZ() + RANDOM.nextInt(13) - 6;

        int y = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                (int) x, (int) z
        );

        LightningBolt lightning =
                EntityType.LIGHTNING_BOLT.create(level);

        if (lightning == null) {
            return;
        }

        lightning.moveTo(x, y, z);

        if (level.addFreshEntity(lightning)) {
            trackEntity(player, definition, lightning);
        }
    }

    private static void spawnWarden(
            ServerPlayer player,
            ServerLevel level,
            EventDefinition definition
    ) {
        Warden warden = EntityType.WARDEN.create(level);

        if (warden != null
                && spawnNearPlayer(player, warden, level)) {
            trackEntity(player, definition, warden);
        }
    }

    private static void spawnZombieBoss(
            ServerPlayer player,
            ServerLevel level,
            EventDefinition definition
    ) {
        Zombie zombie = EntityType.ZOMBIE.create(level);

        if (zombie == null) {
            return;
        }

        zombie.setCustomName(Component.literal("Зомби-босс"));
        zombie.setCustomNameVisible(true);

        if (zombie.getAttribute(Attributes.MAX_HEALTH) != null) {
            zombie.getAttribute(Attributes.MAX_HEALTH)
                    .setBaseValue(100.0);
        }

        zombie.setHealth(100.0F);

        zombie.setItemSlot(
                EquipmentSlot.HEAD,
                new ItemStack(Items.DIAMOND_HELMET));
        zombie.setItemSlot(
                EquipmentSlot.CHEST,
                new ItemStack(Items.DIAMOND_CHESTPLATE));
        zombie.setItemSlot(
                EquipmentSlot.LEGS,
                new ItemStack(Items.DIAMOND_LEGGINGS));
        zombie.setItemSlot(
                EquipmentSlot.FEET,
                new ItemStack(Items.DIAMOND_BOOTS));

        zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        zombie.setDropChance(EquipmentSlot.CHEST, 0.0F);
        zombie.setDropChance(EquipmentSlot.LEGS, 0.0F);
        zombie.setDropChance(EquipmentSlot.FEET, 0.0F);

        if (spawnNearPlayer(player, zombie, level)) {
            trackEntity(player, definition, zombie);
        }
    }

    private static void spawnHelpers(
            ServerPlayer player,
            ServerLevel level
    ) {
        for (int i = 0; i < 5; i++) {
            Wolf wolf = EntityType.WOLF.create(level);

            if (wolf == null
                    || !spawnNearPlayer(player, wolf, level)) {
                continue;
            }

            wolf.tame(player);
            wolf.setOrderedToSit(false);
            wolf.setCustomName(Component.literal("Помощник"));
            wolf.setCustomNameVisible(true);
        }
    }

    /**
     * Ищет безопасную точку на поверхности.
     * Максимум 12 попыток.
     */
    private static boolean spawnNearPlayer(
            ServerPlayer player,
            Entity entity,
            ServerLevel level
    ) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int offsetX = RANDOM.nextInt(11) - 5;
            int offsetZ = RANDOM.nextInt(11) - 5;

            int blockX = player.blockPosition().getX() + offsetX;
            int blockZ = player.blockPosition().getZ() + offsetZ;

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    blockX, blockZ
            );

            BlockPos feetPos = new BlockPos(blockX, y, blockZ);
            BlockPos belowPos = feetPos.below();
            BlockPos headPos = feetPos.above();

            if (y <= level.getMinBuildHeight()
                    || y >= level.getMaxBuildHeight() - 2) {
                continue;
            }

            BlockState floor = level.getBlockState(belowPos);
            BlockState feet = level.getBlockState(feetPos);
            BlockState head = level.getBlockState(headPos);

            if (!floor.isFaceSturdy(
                    level, belowPos, net.minecraft.core.Direction.UP)) {
                continue;
            }

            if (!feet.isAir() || !head.isAir()) {
                continue;
            }

            if (!level.getFluidState(feetPos).isEmpty()
                    || !level.getFluidState(headPos).isEmpty()) {
                continue;
            }

            double x = blockX + 0.5;
            double z = blockZ + 0.5;

            entity.moveTo(
                    x, y, z,
                    RANDOM.nextFloat() * 360.0F,
                    0.0F
            );

            if (!level.noCollision(entity)) {
                continue;
            }

            return level.addFreshEntity(entity);
        }

        return false;
    }

    private static void trackEntity(
            ServerPlayer player,
            EventDefinition definition,
            Entity entity
    ) {
        TRACKED_ENTITIES
                .computeIfAbsent(
                        player.getUUID(),
                        id -> new HashMap<>())
                .computeIfAbsent(
                        definition.id(),
                        id -> new ArrayList<>())
                .add(entity);
    }

    /**
     * Защита построек только от криперов события.
     */
    @Mod.EventBusSubscriber(
            modid = RandomEvents.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ExplosionProtection {

        @SubscribeEvent
        public static void onExplosion(ExplosionEvent.Detonate event) {
            Entity source = event.getExplosion()
                    .getDirectSourceEntity();

            if (!(source instanceof Creeper creeper)) {
                return;
            }

            if (creeper.getPersistentData().getBoolean(
                    EVENT_CREEPER_TAG)) {
                event.getAffectedBlocks().clear();
            }
        }
    }
}
