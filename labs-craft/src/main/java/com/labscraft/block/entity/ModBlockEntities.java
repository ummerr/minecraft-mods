package com.labscraft.block.entity;

import com.labscraft.LabsCraft;
import com.labscraft.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<FlowConsoleBlockEntity> FLOW_CONSOLE =
        register("flow_console", FlowConsoleBlockEntity::new, ModBlocks.FLOW_CONSOLE);

    public static final BlockEntityType<NanoBananaConsoleBlockEntity> NANO_BANANA_CONSOLE =
        register("nano_banana_console", NanoBananaConsoleBlockEntity::new, ModBlocks.NANO_BANANA_CONSOLE);

    public static final BlockEntityType<VeoConsoleBlockEntity> VEO_CONSOLE =
        register("veo_console", VeoConsoleBlockEntity::new, ModBlocks.VEO_CONSOLE);

    public static final BlockEntityType<FlowCraftingTableBlockEntity> FLOW_CRAFTING_TABLE =
        register("flow_crafting_table", FlowCraftingTableBlockEntity::new, ModBlocks.FLOW_CRAFTING_TABLE);

    private static <T extends BlockEntity> BlockEntityType<T> register(
        String name,
        FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
        Block... blocks
    ) {
        Identifier id = Identifier.of(LabsCraft.MOD_ID, name);
        return Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id,
            FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build()
        );
    }

    public static void registerBlockEntities() {
        LabsCraft.LOGGER.info("Registering block entities for " + LabsCraft.MOD_ID);
    }
}
