package com.sportscraft;

import com.sportscraft.entity.ModEntities;
import com.sportscraft.entity.client.SportsBallRenderer;
import com.sportscraft.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client entrypoint: entity renderers and tooltips — SportsCraft has no screens. */
public class SportsCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.SPORTS_BALL, SportsBallRenderer::new);

		registerTooltips();

		SportsCraft.LOGGER.info("SportsCraft client initialized");
	}

	/** Clubs need to explain the charge-and-release swing somewhere. */
	private void registerTooltips() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			String key = null;
			if (stack.isOf(ModItems.DRIVER)) {
				key = "tooltip.sportscraft.driver";
			} else if (stack.isOf(ModItems.IRON)) {
				key = "tooltip.sportscraft.iron";
			} else if (stack.isOf(ModItems.PUTTER)) {
				key = "tooltip.sportscraft.putter";
			}
			if (key != null) {
				lines.add(Text.translatable(key).formatted(Formatting.GRAY, Formatting.ITALIC));
			}
		});
	}
}
