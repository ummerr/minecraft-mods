package com.labscraft;

import com.labscraft.agent.AgentBridge;
import com.labscraft.block.ModBlocks;
import com.labscraft.block.entity.ModBlockEntities;
import com.labscraft.command.GoogleplexCommands;
import com.labscraft.command.JoshCommands;
import com.labscraft.entity.ModEntities;
import com.labscraft.integration.AgentSmokeTest;
import com.labscraft.integration.QuestIntegration;
import com.labscraft.integration.RuntimeSmokeTest;
import com.labscraft.item.ModItemGroups;
import com.labscraft.item.ModItems;
import com.labscraft.quest.QuestManager;
import com.labscraft.screen.ModScreenHandlers;
import com.labscraft.world.GoogleplexAutoGenerator;
import com.labscraft.world.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for LabsCraft. Registration order: items, blocks, block
 * entities, entities, worldgen, quests, networking. Later gates (quests,
 * Josh, agent bridge) hook in via {@link LabsCraftHooks} and add their own
 * registration calls here.
 */
public class LabsCraft implements ModInitializer {
	public static final String MOD_ID = "labscraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerItems();
		ModBlocks.registerBlocks();
		ModBlockEntities.registerBlockEntities();
		ModEntities.registerEntities();
		ModItemGroups.registerItemGroups();
		ModScreenHandlers.registerScreenHandlers();
		ModWorldGeneration.registerWorldGeneration();

		QuestManager.register();
		QuestIntegration.register();
		JoshCommands.register();
		GoogleplexCommands.register();
		GoogleplexAutoGenerator.register();
		AgentBridge.register();

		// Env-gated integration harnesses; inert unless LABSCRAFT_*_SMOKETEST is set.
		RuntimeSmokeTest.register();
		AgentSmokeTest.register();

		LOGGER.info("LabsCraft initialized");
	}
}
