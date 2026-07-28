package com.sportscraft;

import com.sportscraft.entity.ModEntities;
import com.sportscraft.entity.client.SportsBallRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client entrypoint: entity renderers only — SportsCraft has no screens. */
public class SportsCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.SPORTS_BALL, SportsBallRenderer::new);

		SportsCraft.LOGGER.info("SportsCraft client initialized");
	}
}
