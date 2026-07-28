package com.sportscraft;

import com.sportscraft.command.SportsCommands;
import com.sportscraft.entity.ModEntities;
import com.sportscraft.game.GolfManager;
import com.sportscraft.integration.SportsSmokeTest;
import com.sportscraft.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for SportsCraft. Registration order mirrors LabsCraft: items,
 * entities, then commands. Game-logic layers subscribe to {@link SportsHooks}
 * rather than being called into by the ball itself.
 */
public class SportsCraft implements ModInitializer {
	public static final String MOD_ID = "sportscraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerItems();
		ModEntities.registerEntities();
		GolfManager.register();
		SportsCommands.register();

		// Env-gated harness; inert unless SPORTSCRAFT_SMOKETEST=true.
		SportsSmokeTest.register();

		LOGGER.info("SportsCraft initialized");
	}
}
