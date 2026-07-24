package com.labscraft.quest;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Player-facing feedback for quest progress: chat messages plus a sound hook.
 * Kept separate from {@link QuestManager} so the feedback policy can evolve
 * without touching authority logic.
 */
final class QuestNotifier {

    private QuestNotifier() {
    }

    static void notifyUpdate(ServerPlayerEntity player, QuestUpdate update) {
        // Progress ticks (not yet complete) go to the action bar, quietly.
        for (Objective objective : update.progressed()) {
            if (!objective.isDone()) {
                player.sendMessage(Text.literal(objective.description() + "  (" + objective.progress()
                        + "/" + objective.goal() + ")").formatted(Formatting.YELLOW), true);
            }
        }

        for (Objective objective : update.completed()) {
            player.sendMessage(Text.literal("[Quest] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Objective complete: ").formatted(Formatting.GREEN))
                    .append(Text.literal(objective.description()).formatted(Formatting.WHITE)), false);
            playObjectiveSound(player);
        }

        if (update.stageAdvanced()) {
            notifyStageChange(player, update.stage());
        }
    }

    static void notifyStageChange(ServerPlayerEntity player, QuestStage newStage) {
        if (newStage.isTerminal()) {
            player.sendMessage(Text.literal("[Quest] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Internship complete: " + newStage.displayName()
                            + ". Josh says the paperwork is a Q3 problem.").formatted(Formatting.AQUA)), false);
        } else {
            player.sendMessage(Text.literal("[Quest] ").formatted(Formatting.GOLD)
                    .append(Text.literal("New assignment: ").formatted(Formatting.AQUA))
                    .append(Text.literal(newStage.displayName()).formatted(Formatting.BOLD)), false);
            StageDefinition definition = QuestLine.definitionFor(newStage);
            for (ObjectiveDefinition objective : definition.objectives()) {
                player.sendMessage(Text.literal("  - " + objective.description()).formatted(Formatting.GRAY), false);
            }
        }
        playStageSound(player);
    }

    private static void playObjectiveSound(ServerPlayerEntity player) {
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }

    private static void playStageSound(ServerPlayerEntity player) {
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
