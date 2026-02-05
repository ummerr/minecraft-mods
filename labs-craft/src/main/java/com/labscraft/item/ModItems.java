package com.labscraft.item;

import com.labscraft.LabsCraft;
import com.labscraft.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    // TPU - used to craft consoles
    public static final Item TPU = registerItem(
        "tpu",
        new Item(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LabsCraft.MOD_ID, "tpu"))))
    );

    // Spawn egg with Google Blue (#4285F4) and White (#FFFFFF) colors defined in spawn_egg_colors.json
    public static final Item JOSH_WOODWARD_SPAWN_EGG = registerItem(
        "josh_woodward_spawn_egg",
        new SpawnEggItem(ModEntities.JOSH_WOODWARD,
            new Item.Settings()
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LabsCraft.MOD_ID, "josh_woodward_spawn_egg"))))
    );

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(LabsCraft.MOD_ID, name), item);
    }

    public static void registerItems() {
        LabsCraft.LOGGER.info("Registering items for " + LabsCraft.MOD_ID);

        // Add TPU to ingredients tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
            content.add(TPU);
        });

        // Add spawn egg to the Spawn Eggs creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(content -> {
            content.add(JOSH_WOODWARD_SPAWN_EGG);
        });
    }
}
