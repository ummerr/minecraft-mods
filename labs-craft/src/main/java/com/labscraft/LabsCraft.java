package com.labscraft;

import com.labscraft.block.ModBlocks;
import com.labscraft.block.entity.ModBlockEntities;
import com.labscraft.command.ModCommands;
import com.labscraft.entity.ModEntities;
import com.labscraft.item.ModItems;
import com.labscraft.network.FlowConsolePackets;
import com.labscraft.network.FlowCraftingTablePackets;
import com.labscraft.network.NanoBananaConsolePackets;
import com.labscraft.network.VeoConsolePackets;
import com.labscraft.screen.ModScreenHandlers;
import com.labscraft.world.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabsCraft implements ModInitializer {
    public static final String MOD_ID = "labscraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.registerBlocks();
        ModBlockEntities.registerBlockEntities();
        ModEntities.registerEntities();
        ModItems.registerItems();
        ModScreenHandlers.registerScreenHandlers();
        FlowConsolePackets.registerServer();
        NanoBananaConsolePackets.registerServer();
        VeoConsolePackets.registerServer();
        FlowCraftingTablePackets.registerServer();
        ModWorldGeneration.registerWorldGeneration();
        ModCommands.registerCommands();
        LOGGER.info("LabsCraft initialized");
    }
}
