package com.labscraft;

import com.labscraft.entity.ModEntities;
import com.labscraft.entity.client.JoshWoodwardRenderer;
import com.labscraft.network.FlowConsolePackets;
import com.labscraft.network.FlowCraftingTablePackets;
import com.labscraft.network.NanoBananaConsolePackets;
import com.labscraft.network.VeoConsolePackets;
import com.labscraft.screen.FlowConsoleScreen;
import com.labscraft.screen.FlowCraftingTableScreen;
import com.labscraft.screen.NanoBananaConsoleScreen;
import com.labscraft.screen.VeoConsoleScreen;
import com.labscraft.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class LabsCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.JOSH_WOODWARD, JoshWoodwardRenderer::new);
        HandledScreens.register(ModScreenHandlers.FLOW_CONSOLE_SCREEN_HANDLER, FlowConsoleScreen::new);
        HandledScreens.register(ModScreenHandlers.NANO_BANANA_CONSOLE_SCREEN_HANDLER, NanoBananaConsoleScreen::new);
        HandledScreens.register(ModScreenHandlers.VEO_CONSOLE_SCREEN_HANDLER, VeoConsoleScreen::new);
        HandledScreens.register(ModScreenHandlers.FLOW_CRAFTING_TABLE_SCREEN_HANDLER, FlowCraftingTableScreen::new);
        FlowConsolePackets.registerClient();
        NanoBananaConsolePackets.registerClient();
        VeoConsolePackets.registerClient();
        FlowCraftingTablePackets.registerClient();
        LabsCraft.LOGGER.info("LabsCraft client initialized");
    }
}
