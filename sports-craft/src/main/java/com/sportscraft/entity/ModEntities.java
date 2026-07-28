package com.sportscraft.entity;

import com.sportscraft.SportsCraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/** Entity registration, using the RegistryKey + build(KEY) idiom from labs-craft. */
public final class ModEntities {

    private ModEntities() {
    }

    public static final RegistryKey<EntityType<?>> SPORTS_BALL_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(SportsCraft.MOD_ID, "sports_ball"));

    /**
     * A small, fast-moving entity that must look smooth on the client without
     * any client-side prediction — hence a tracking interval of every tick.
     */
    public static final EntityType<SportsBallEntity> SPORTS_BALL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(SportsCraft.MOD_ID, "sports_ball"),
            EntityType.Builder.<SportsBallEntity>create(SportsBallEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25f, 0.25f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(1)
                    .build(SPORTS_BALL_KEY));

    public static void registerEntities() {
        SportsCraft.LOGGER.info("Registering entities for {}", SportsCraft.MOD_ID);
    }
}
