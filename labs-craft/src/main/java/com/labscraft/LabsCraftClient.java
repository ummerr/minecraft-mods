package com.labscraft;

import com.labscraft.block.ModBlocks;
import com.labscraft.entity.ModEntities;
import com.labscraft.entity.client.JoshWoodwardRenderer;
import com.labscraft.item.ModItems;
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
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

        registerTooltips();

        LabsCraft.LOGGER.info("LabsCraft client initialized");
    }

    private void registerTooltips() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.isOf(ModItems.TPU)) {
                lines.add(Text.translatable("tooltip.labscraft.tpu").formatted(Formatting.GRAY, Formatting.ITALIC));
            } else if (stack.isOf(ModBlocks.FLOW_CONSOLE.asItem())) {
                lines.add(Text.translatable("tooltip.labscraft.flow_console").formatted(Formatting.BLUE, Formatting.ITALIC));
            } else if (stack.isOf(ModBlocks.NANO_BANANA_CONSOLE.asItem())) {
                lines.add(Text.translatable("tooltip.labscraft.nano_banana_console").formatted(Formatting.YELLOW, Formatting.ITALIC));
            } else if (stack.isOf(ModBlocks.VEO_CONSOLE.asItem())) {
                lines.add(Text.translatable("tooltip.labscraft.veo_console").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
            } else if (stack.isOf(ModBlocks.FLOW_CRAFTING_TABLE.asItem())) {
                lines.add(Text.translatable("tooltip.labscraft.flow_crafting_table").formatted(Formatting.GRAY, Formatting.ITALIC));
            } else if (stack.isOf(ModBlocks.TPU_ORE.asItem())) {
                lines.add(Text.translatable("tooltip.labscraft.tpu_ore").formatted(Formatting.GRAY, Formatting.ITALIC));
            } else if (stack.isOf(ModBlocks.DEEPSLATE_TPU_ORE.asItem())) {
                lines.add(Text.translatable("tooltip.labscraft.tpu_ore").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        });
    }
}
