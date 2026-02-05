package com.labscraft.block;

import com.labscraft.LabsCraft;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block FLOW_CONSOLE = registerBlock("flow_console",
        new FlowConsoleBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LabsCraft.MOD_ID, "flow_console")))
            .strength(2.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
            .luminance(state -> 8)
        )
    );

    public static final Block TPU_ORE = registerBlock("tpu_ore",
        new Block(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LabsCraft.MOD_ID, "tpu_ore")))
            .strength(3.0f, 3.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
            .luminance(state -> 3)
        )
    );

    public static final Block DEEPSLATE_TPU_ORE = registerBlock("deepslate_tpu_ore",
        new Block(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LabsCraft.MOD_ID, "deepslate_tpu_ore")))
            .strength(4.5f, 3.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.DEEPSLATE)
            .luminance(state -> 3)
        )
    );

    public static final Block NANO_BANANA_CONSOLE = registerBlock("nano_banana_console",
        new NanoBananaConsoleBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LabsCraft.MOD_ID, "nano_banana_console")))
            .strength(2.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
            .luminance(state -> 10)
        )
    );

    public static final Block VEO_CONSOLE = registerBlock("veo_console",
        new VeoConsoleBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LabsCraft.MOD_ID, "veo_console")))
            .strength(2.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.METAL)
            .luminance(state -> 12)
        )
    );

    public static final Block FLOW_CRAFTING_TABLE = registerBlock("flow_crafting_table",
        new FlowCraftingTableBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LabsCraft.MOD_ID, "flow_crafting_table")))
            .strength(2.5f)
            .sounds(BlockSoundGroup.WOOD)
        )
    );

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(LabsCraft.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Item item = new BlockItem(block, new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LabsCraft.MOD_ID, name)))
            .useBlockPrefixedTranslationKey()
        );
        Registry.register(Registries.ITEM, Identifier.of(LabsCraft.MOD_ID, name), item);
    }

    public static void registerBlocks() {
        LabsCraft.LOGGER.info("Registering blocks for " + LabsCraft.MOD_ID);

        // Add to functional blocks tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
            content.add(FLOW_CONSOLE);
            content.add(FLOW_CRAFTING_TABLE);
            content.add(NANO_BANANA_CONSOLE);
            content.add(VEO_CONSOLE);
        });

        // Add ores to natural blocks tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(content -> {
            content.add(TPU_ORE);
            content.add(DEEPSLATE_TPU_ORE);
        });
    }
}
