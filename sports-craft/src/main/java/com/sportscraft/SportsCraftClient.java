package com.sportscraft;

import net.fabricmc.api.ClientModInitializer;

/** Client entrypoint: entity renderers only — SportsCraft has no screens. */
public class SportsCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SportsCraft.LOGGER.info("SportsCraft client initialized");
	}
}
