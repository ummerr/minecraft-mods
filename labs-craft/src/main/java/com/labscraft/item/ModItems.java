package com.labscraft.item;

import com.labscraft.LabsCraft;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    private ModItems() {
    }

    /** TPU — mined from TPU ore, spent at consoles and the Flow Crafting Table. */
    public static final Item TPU = register("tpu", new Item(settings("tpu")));

    /** Produced by the Flow Console (1 TPU). */
    public static final Item FLOW_SKETCH = register("flow_sketch",
        new Item(settings("flow_sketch").maxCount(16)));

    /** Produced by the Nano Banana Console (1 TPU). */
    public static final Item GENERATED_IMAGE = register("generated_image",
        new Item(settings("generated_image").maxCount(16).rarity(Rarity.UNCOMMON)));

    /** Produced by the Veo Console (3 TPU). */
    public static final Item GENERATED_VIDEO = register("generated_video",
        new Item(settings("generated_video").maxCount(16).rarity(Rarity.RARE)));

    private static Item.Settings settings(String name) {
        return new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LabsCraft.MOD_ID, name)));
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(LabsCraft.MOD_ID, name), item);
    }

    public static void registerItems() {
        LabsCraft.LOGGER.info("Registering items for {}", LabsCraft.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> {
            content.add(TPU);
            content.add(FLOW_SKETCH);
            content.add(GENERATED_IMAGE);
            content.add(GENERATED_VIDEO);
        });
    }
}
