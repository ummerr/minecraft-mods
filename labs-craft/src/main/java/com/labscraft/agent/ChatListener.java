package com.labscraft.agent;

import com.labscraft.LabsCraft;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.entity.ModEntities;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

public class ChatListener {

    private static final double CHAT_LISTEN_RANGE = 16.0;

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.getContent().getString();
            ServerWorld world = (ServerWorld) sender.getWorld();

            // Find all Josh entities within range
            Box searchBox = sender.getBoundingBox().expand(CHAT_LISTEN_RANGE);
            List<JoshWoodwardEntity> nearbyJoshes = world.getEntitiesByType(
                    ModEntities.JOSH_WOODWARD,
                    searchBox,
                    josh -> true
            );

            for (JoshWoodwardEntity josh : nearbyJoshes) {
                RecentEventsTracker tracker = josh.getEventsTracker(sender);
                tracker.addChatMessage(sender.getName().getString(), text);

                LabsCraft.LOGGER.debug("[ChatListener] Captured chat for Josh: {} said \"{}\"",
                        sender.getName().getString(), text);
            }
        });

        LabsCraft.LOGGER.info("[ChatListener] Registered server chat listener");
    }
}
