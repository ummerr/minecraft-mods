package com.labscraft.command;

import com.labscraft.quest.AdvanceResult;
import com.labscraft.quest.Objective;
import com.labscraft.quest.PlayerQuestState;
import com.labscraft.quest.QuestManager;
import com.labscraft.quest.QuestStage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * {@code /labscraft quest} — show your quest status.
 * Op-only (level 2) testing subcommands:
 * {@code set <stage>} (force-jump), {@code advance} (goes through the
 * validating authority and reports refusals), {@code reset}.
 */
public final class QuestCommands {

    private QuestCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("labscraft")
                        .then(CommandManager.literal("quest")
                                .executes(QuestCommands::showStatus)
                                .then(CommandManager.literal("set")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("stage", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (QuestStage stage : QuestStage.values()) {
                                                        builder.suggest(stage.name());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(QuestCommands::setStage)))
                                .then(CommandManager.literal("advance")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(QuestCommands::advance))
                                .then(CommandManager.literal("reset")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(QuestCommands::reset)))));
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PlayerQuestState state = QuestManager.getState(player);

        context.getSource().sendFeedback(() -> Text.literal("Stage: ").formatted(Formatting.GOLD)
                .append(Text.literal(state.stage().name()).formatted(Formatting.WHITE))
                .append(Text.literal("  (" + state.stage().displayName() + ", "
                        + state.secondsInStage() + "s)").formatted(Formatting.GRAY)), false);

        if (state.objectives().isEmpty()) {
            context.getSource().sendFeedback(() ->
                    Text.literal("No open objectives.").formatted(Formatting.GRAY), false);
        }
        for (Objective objective : state.objectives()) {
            Formatting color = objective.isDone() ? Formatting.GREEN : Formatting.YELLOW;
            String box = objective.isDone() ? "[x] " : "[ ] ";
            context.getSource().sendFeedback(() -> Text.literal(box + objective.description()
                    + " (" + objective.progress() + "/" + objective.goal() + ")").formatted(color), false);
        }
        return 1;
    }

    private static int setStage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String rawStage = StringArgumentType.getString(context, "stage");
        QuestStage stage = QuestStage.fromName(rawStage);
        if (stage == null) {
            context.getSource().sendError(Text.literal("Unknown stage '" + rawStage + "'. Valid: "
                    + java.util.Arrays.toString(QuestStage.values())));
            return 0;
        }
        QuestManager.forceSetStage(player, stage);
        context.getSource().sendFeedback(() ->
                Text.literal("Quest stage forced to " + stage.name()).formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int advance(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        AdvanceResult result = QuestManager.requestAdvance(player);
        if (result.allowed()) {
            context.getSource().sendFeedback(() -> Text.literal("Advanced: " + result.previousStage().name()
                    + " -> " + result.stage().name()).formatted(Formatting.GREEN), true);
            return 1;
        }
        context.getSource().sendError(Text.literal("Refused: " + result.reason()));
        return 0;
    }

    private static int reset(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        QuestManager.reset(player);
        context.getSource().sendFeedback(() ->
                Text.literal("Quest progress reset to NOT_STARTED.").formatted(Formatting.GOLD), true);
        return 1;
    }
}
