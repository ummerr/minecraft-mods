package com.labscraft.entity;

import com.labscraft.LabsCraft;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<JoshWoodwardEntity> JOSH_WOODWARD = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(LabsCraft.MOD_ID, "josh_woodward"),
        EntityType.Builder.create(JoshWoodwardEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.6F, 1.8F) // Player-sized
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(LabsCraft.MOD_ID, "josh_woodward")))
    );

    public static void registerEntities() {
        LabsCraft.LOGGER.info("Registering entities for " + LabsCraft.MOD_ID);

        // Register default attributes for Josh
        FabricDefaultAttributeRegistry.register(JOSH_WOODWARD, MobEntity.createMobAttributes());
    }
}
