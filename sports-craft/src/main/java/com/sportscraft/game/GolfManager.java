package com.sportscraft.game;

import com.sportscraft.core.golf.GolfHoleDef;
import net.minecraft.server.MinecraftServer;

/**
 * Facade over golf state, per {@code QuestManager}: the rest of the mod talks to
 * this rather than reaching into {@link SportsPersistentState} directly.
 *
 * <p>M3 scope is hole registration. The round state machine and the
 * {@code SportsHooks} subscriptions that drive it arrive in M4.</p>
 */
public final class GolfManager {

    private GolfManager() {
    }

    public static void registerHole(MinecraftServer server, GolfHoleDef hole) {
        SportsPersistentState.get(server).registerHole(hole);
    }

    public static int holeCount(MinecraftServer server) {
        return SportsPersistentState.get(server).holes().size();
    }

    public static HoleRegistry holes(MinecraftServer server) {
        return SportsPersistentState.get(server).holes();
    }
}
