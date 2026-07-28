package com.sportscraft.item;

import com.sportscraft.SportsCraft;
import com.sportscraft.core.swing.Clubs;
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

    /** Long and low — the tee shot. */
    public static final Item DRIVER = register("driver",
            new ClubItem(settings("driver").maxCount(1), Clubs.DRIVER));

    /** High and short — approach shots and escaping the rough. */
    public static final Item IRON = register("iron",
            new ClubItem(settings("iron").maxCount(1), Clubs.IRON));

    /** Flat and gentle — rolls, never flies. */
    public static final Item PUTTER = register("putter",
            new ClubItem(settings("putter").maxCount(1), Clubs.PUTTER));

    private static Item.Settings settings(String name) {
        return new Item.Settings()
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(SportsCraft.MOD_ID, name)));
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(SportsCraft.MOD_ID, name), item);
    }

    public static void registerItems() {
        SportsCraft.LOGGER.info("Registering items for {}", SportsCraft.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(DRIVER);
            content.add(IRON);
            content.add(PUTTER);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(content -> content.add(GOLF_BALL));
    }
}
