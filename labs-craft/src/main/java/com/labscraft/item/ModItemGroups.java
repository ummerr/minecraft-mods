package com.labscraft.item;

import com.labscraft.LabsCraft;
import com.labscraft.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** A dedicated "LabsCraft" creative tab holding every mod item and block. */
public final class ModItemGroups {
    private ModItemGroups() {
    }

    public static final ItemGroup LABSCRAFT = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of(LabsCraft.MOD_ID, "labscraft"),
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.TPU))
            .displayName(Text.translatable("itemgroup.labscraft"))
            .entries((displayContext, entries) -> {
                entries.add(ModItems.TPU);
                entries.add(ModItems.FLOW_SKETCH);
                entries.add(ModItems.GENERATED_IMAGE);
                entries.add(ModItems.GENERATED_VIDEO);
                entries.add(ModBlocks.TPU_ORE);
                entries.add(ModBlocks.DEEPSLATE_TPU_ORE);
                entries.add(ModBlocks.FLOW_CRAFTING_TABLE);
                entries.add(ModBlocks.FLOW_CONSOLE);
                entries.add(ModBlocks.NANO_BANANA_CONSOLE);
                entries.add(ModBlocks.VEO_CONSOLE);
            })
            .build());

    public static void registerItemGroups() {
        LabsCraft.LOGGER.info("Registering item groups for {}", LabsCraft.MOD_ID);
    }
}
