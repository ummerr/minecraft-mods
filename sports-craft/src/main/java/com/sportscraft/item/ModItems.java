package com.sportscraft.item;

import com.sportscraft.SportsCraft;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModItems {

    private ModItems() {
    }

    /** The ball as an inventory item; also what the ball entity renders as. */
    public static final Item GOLF_BALL = register("golf_ball",
            new Item(settings("golf_ball").maxCount(16)));

    private static Item.Settings settings(String name) {
        return new Item.Settings()
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SportsCraft.MOD_ID, name)));
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(SportsCraft.MOD_ID, name), item);
    }

    public static void registerItems() {
        SportsCraft.LOGGER.info("Registering items for {}", SportsCraft.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> content.add(GOLF_BALL));
    }
}
