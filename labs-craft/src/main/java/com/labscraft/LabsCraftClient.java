package com.labscraft;

import com.labscraft.block.ModBlocks;
import com.labscraft.entity.ModEntities;
import com.labscraft.entity.client.JoshWoodwardRenderer;
import com.labscraft.item.ModItems;
import com.labscraft.screen.ConsoleScreen;
import com.labscraft.screen.FlowCraftingTableScreen;
import com.labscraft.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client entrypoint for LabsCraft. Later phases register renderers and client
 * networking handlers from {@link #onInitializeClient()}.
 */
public class LabsCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(ModScreenHandlers.CONSOLE, ConsoleScreen::new);
		HandledScreens.register(ModScreenHandlers.FLOW_CRAFTING_TABLE, FlowCraftingTableScreen::new);

		EntityRendererRegistry.register(ModEntities.JOSH_WOODWARD, JoshWoodwardRenderer::new);

		registerTooltips();

		LabsCraft.LOGGER.info("LabsCraft client initialized (G1 content + G3 Josh)");
	}

	private void registerTooltips() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			String key = null;
			Formatting color = Formatting.GRAY;
			if (stack.isOf(ModItems.TPU)) {
				key = "tooltip.labscraft.tpu";
			} else if (stack.isOf(ModItems.FLOW_SKETCH)) {
				key = "tooltip.labscraft.flow_sketch";
				color = Formatting.AQUA;
			} else if (stack.isOf(ModItems.GENERATED_IMAGE)) {
				key = "tooltip.labscraft.generated_image";
				color = Formatting.YELLOW;
			} else if (stack.isOf(ModItems.GENERATED_VIDEO)) {
				key = "tooltip.labscraft.generated_video";
				color = Formatting.LIGHT_PURPLE;
			} else if (stack.isOf(ModBlocks.FLOW_CONSOLE.asItem())) {
				key = "tooltip.labscraft.flow_console";
				color = Formatting.BLUE;
			} else if (stack.isOf(ModBlocks.NANO_BANANA_CONSOLE.asItem())) {
				key = "tooltip.labscraft.nano_banana_console";
				color = Formatting.YELLOW;
			} else if (stack.isOf(ModBlocks.VEO_CONSOLE.asItem())) {
				key = "tooltip.labscraft.veo_console";
				color = Formatting.LIGHT_PURPLE;
			} else if (stack.isOf(ModBlocks.FLOW_CRAFTING_TABLE.asItem())) {
				key = "tooltip.labscraft.flow_crafting_table";
			} else if (stack.isOf(ModBlocks.TPU_ORE.asItem()) || stack.isOf(ModBlocks.DEEPSLATE_TPU_ORE.asItem())) {
				key = "tooltip.labscraft.tpu_ore";
			}
			if (key != null) {
				lines.add(Text.translatable(key).formatted(color, Formatting.ITALIC));
			}
		});
	}
}
