package com.sportscraft.game;

import com.sportscraft.core.golf.GolfRound;
import com.sportscraft.core.golf.Scorecard;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * All player-facing feedback for a round, in one place so the rules layer stays
 * about rules. Actionbar for the running stroke count, chat for the things worth
 * remembering.
 */
public final class SportsNotifier {

    private SportsNotifier() {
    }

    /** Running score after a stroke settles. */
    public static void strokeUpdate(ServerPlayerEntity player, GolfRound round) {
        double toGo = round.hole().horizontalDistanceToCup(player.getX(), player.getZ());
        player.sendMessage(Text.literal("Stroke " + round.strokes()
                + "  ·  par " + round.par()
                + "  ·  " + (int) Math.round(toGo) + "m to the pin"), true);
    }

    public static void penalty(ServerPlayerEntity player, GolfRound round, String reason) {
        player.sendMessage(Text.literal(reason + " — penalty stroke. Playing " + round.strokes() + ".")
                .formatted(Formatting.RED), false);
        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.7f, 1.0f);
    }

    /** The moment the ball drops: chat line, sound, and a burst at the cup. */
    public static void holedOut(ServerPlayerEntity player, GolfRound round, ServerWorld world) {
        String name = round.scoreName();
        Formatting color = Scorecard.isBirdieOrBetter(round.strokes(), round.par())
                ? Formatting.GOLD : Formatting.WHITE;

        world.getServer().getPlayerManager().broadcast(
                Text.literal(player.getName().getString() + " — " + name + " (")
                        .append(Text.literal(round.strokes() + " on a par " + round.par()))
                        .append(Text.literal(", " + round.toParString() + ")"))
                        .formatted(color),
                false);

        if (Scorecard.isBirdieOrBetter(round.strokes(), round.par())) {
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else {
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, 1.0f, 1.2f);
        }

        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                round.hole().cupX() + 0.5, round.hole().cupY() + 1.5, round.hole().cupZ() + 0.5,
                24, 0.4, 0.6, 0.4, 0.05);
    }

    public static void roundStarted(ServerPlayerEntity player, GolfRound round) {
        player.sendMessage(Text.literal("Round started: par " + round.par()
                        + ", " + round.hole().length() + " blocks. Good luck.")
                .formatted(Formatting.GREEN), false);
    }

    public static void roundQuit(ServerPlayerEntity player, GolfRound round) {
        player.sendMessage(Text.literal("Round abandoned after " + round.strokes() + " stroke"
                + (round.strokes() == 1 ? "" : "s") + ".").formatted(Formatting.YELLOW), false);
    }
}
