package com.mtlmod.network;

import com.mtlmod.MtlMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network handler for multiplayer synchronization.
 * 
 * This handles syncing quest progress between server and client,
 * so your friend can see their own progress while playing on your server.
 * 
 * For now it's a skeleton - we'll add packets as needed.
 */
public class ModNetwork {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MtlMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    public static void register() {
        // We'll register packets here later when we need multiplayer sync
        // Example:
        // INSTANCE.registerMessage(0, QuestSyncPacket.class, 
        //         QuestSyncPacket::encode, QuestSyncPacket::decode, 
        //         QuestSyncPacket::handle);
    }
}
