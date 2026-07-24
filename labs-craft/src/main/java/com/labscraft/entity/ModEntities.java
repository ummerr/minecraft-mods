package com.labscraft.entity;

import com.labscraft.LabsCraft;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/** Entity registration for LabsCraft (G3): Josh Woodward + his spawn egg. */
public final class ModEntities {

    private ModEntities() {
    }

    public static final RegistryKey<EntityType<?>> JOSH_WOODWARD_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(LabsCraft.MOD_ID, "josh_woodward"));

    public static final EntityType<JoshWoodwardEntity> JOSH_WOODWARD = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(LabsCraft.MOD_ID, "josh_woodward"),
            EntityType.Builder.create(JoshWoodwardEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.95f)
                    .eyeHeight(1.62f)
                    .maxTrackingRange(10)
                    .build(JOSH_WOODWARD_KEY));

    public static final Item JOSH_WOODWARD_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            Identifier.of(LabsCraft.MOD_ID, "josh_woodward_spawn_egg"),
            new SpawnEggItem(JOSH_WOODWARD, new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM,
                            Identifier.of(LabsCraft.MOD_ID, "josh_woodward_spawn_egg")))));

    public static void registerEntities() {
        LabsCraft.LOGGER.info("Registering entities for {}", LabsCraft.MOD_ID);
        FabricDefaultAttributeRegistry.register(JOSH_WOODWARD, JoshWoodwardEntity.createJoshAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(content -> content.add(JOSH_WOODWARD_SPAWN_EGG));
    }
}
