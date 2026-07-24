package com.labscraft.command;

import com.labscraft.entity.JoshEmote;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.entity.ModEntities;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.List;

/**
 * {@code /labscraft josh} — G3 testing/utility commands, kept separate from
 * G2's {@link QuestCommands} (Brigadier merges the shared {@code /labscraft}
 * root). All op-only (level 2):
 * <ul>
 *   <li>{@code spawn} — spawn Josh at the caller's position</li>
 *   <li>{@code emote <emote>} — nearest Josh plays an emote</li>
 *   <li>{@code say <text>} — nearest Josh broadcasts a line (exercises the
 *       same {@code say()} path the agent bridge uses)</li>
 * </ul>
 */
public final class JoshCommands {

    private static final double SEARCH_RANGE = 32.0;

    private JoshCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("labscraft")
                        .then(CommandManager.literal("josh")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("spawn")
                                        .executes(JoshCommands::spawn))
                                .then(CommandManager.literal("emote")
                                        .then(CommandManager.argument("emote", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (JoshEmote emote : JoshEmote.values()) {
                                                        builder.suggest(emote.id());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(JoshCommands::emote)))
                                .then(CommandManager.literal("say")
                                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                                .executes(JoshCommands::say))))));
    }

    private static int spawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        JoshWoodwardEntity josh = ModEntities.JOSH_WOODWARD.create(player.getServerWorld(), SpawnReason.COMMAND);
        if (josh == null) {
            context.getSource().sendError(Text.literal("Failed to create Josh Woodward."));
            return 0;
        }
        josh.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0f);
        player.getServerWorld().spawnEntity(josh);
        context.getSource().sendFeedback(() ->
                Text.literal("Josh Woodward has joined the meeting.").formatted(Formatting.GOLD), true);
        return 1;
    }

    private static int emote(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String emoteId = StringArgumentType.getString(context, "emote");
        if (JoshEmote.fromId(emoteId) == null) {
            context.getSource().sendError(Text.literal("Unknown emote '" + emoteId + "'. Valid: "
                    + String.join(", ", java.util.Arrays.stream(JoshEmote.values()).map(JoshEmote::id).toList())));
            return 0;
        }
        JoshWoodwardEntity josh = findNearestJosh(player);
        if (josh == null) {
            context.getSource().sendError(Text.literal("No Josh Woodward within " + (int) SEARCH_RANGE + " blocks."));
            return 0;
        }
        josh.playEmote(emoteId);
        return 1;
    }

    private static int say(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        JoshWoodwardEntity josh = findNearestJosh(player);
        if (josh == null) {
            context.getSource().sendError(Text.literal("No Josh Woodward within " + (int) SEARCH_RANGE + " blocks."));
            return 0;
        }
        josh.say(StringArgumentType.getString(context, "text"));
        return 1;
    }

    private static JoshWoodwardEntity findNearestJosh(ServerPlayerEntity player) {
        List<JoshWoodwardEntity> nearby = player.getServerWorld().getEntitiesByClass(
                JoshWoodwardEntity.class,
                player.getBoundingBox().expand(SEARCH_RANGE),
                entity -> true);
        return nearby.stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo))
                .orElse(null);
    }
}
