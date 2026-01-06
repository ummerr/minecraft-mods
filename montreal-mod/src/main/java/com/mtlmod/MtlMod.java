package com.mtlmod;

import com.mtlmod.item.ModItems;
import com.mtlmod.quest.QuestManager;
import com.mtlmod.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * MTL MEMORIES - A love letter to Montreal 2004-2008
 * 
 * This mod recreates the McGill experience through quests, items, and atmosphere.
 * Built with love by two friends who survived McLennan all-nighters together.
 */
@Mod(MtlMod.MOD_ID)
public class MtlMod {
    
    // This is your mod's unique identifier - used everywhere
    public static final String MOD_ID = "mtlmod";
    
    // Logger for debugging - prints messages to the console
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // The quest manager handles all mission logic
    public static QuestManager questManager;

    public MtlMod() {
        // Get the event bus - this is how Forge mods hook into the game
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register our custom items (poutine, bagels, etc.)
        ModItems.register(modEventBus);
        
        // These methods run at specific points during game startup
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // Register ourselves for game events (player actions, world events, etc.)
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("MTL Memories loaded - Bienvenue à Montréal!");
    }

    /**
     * Common setup - runs on both client and server
     * Good place for registering game mechanics
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize the networking system (for multiplayer quest sync)
            ModNetwork.register();
            
            // Initialize the quest manager
            questManager = new QuestManager();
            questManager.loadQuests();
            
            LOGGER.info("Quest system initialized - {} quests loaded", questManager.getQuestCount());
        });
    }

    /**
     * Client setup - runs only on the player's game (not servers)
     * Good place for rendering, keybinds, GUI stuff
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup complete - ready to explore Montreal!");
    }
}
