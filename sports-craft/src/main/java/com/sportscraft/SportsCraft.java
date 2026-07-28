package com.sportscraft;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for SportsCraft. Registration order mirrors LabsCraft:
 * items, entities, then the game-logic layers that subscribe to
 * {@link SportsHooks}.
 */
public class SportsCraft implements ModInitializer {
	public static final String MOD_ID = "sportscraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("SportsCraft initialized");
	}
}
