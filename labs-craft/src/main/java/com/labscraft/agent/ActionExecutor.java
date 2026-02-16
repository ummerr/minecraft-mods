package com.labscraft.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.labscraft.LabsCraft;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.item.ModItems;
import com.labscraft.quest.QuestManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ActionExecutor {

    private record ScheduledAction(JsonObject action, int executeTick) {}

    private final List<ScheduledAction> pendingActions = new ArrayList<>();
    private int currentTick = 0;

    public void scheduleActions(JsonArray actions, int baseTick) {
        synchronized (pendingActions) {
            for (JsonElement element : actions) {
                JsonObject action = element.getAsJsonObject();
                int delay = action.has("delay_ticks") ? action.get("delay_ticks").getAsInt() : 0;
                pendingActions.add(new ScheduledAction(action, baseTick + delay));
            }
        }
    }

    public void tick(JoshWoodwardEntity josh, ServerPlayerEntity player, ServerWorld world) {
        currentTick++;

        List<ScheduledAction> toExecute = new ArrayList<>();
        synchronized (pendingActions) {
            var it = pendingActions.iterator();
            while (it.hasNext()) {
                ScheduledAction sa = it.next();
                if (currentTick >= sa.executeTick()) {
                    toExecute.add(sa);
                    it.remove();
                }
            }
        }

        for (ScheduledAction sa : toExecute) {
            executeAction(sa.action(), josh, player, world);
        }
    }

    public boolean hasPendingActions() {
        synchronized (pendingActions) {
            return !pendingActions.isEmpty();
        }
    }

    private void executeAction(JsonObject action, JoshWoodwardEntity josh,
                               ServerPlayerEntity player, ServerWorld world) {
        String type = action.get("type").getAsString();

        switch (type) {
            case "SAY" -> executeSay(action, player);
            case "WALK_TO" -> executeWalkTo(action, josh);
            case "EMOTE" -> executeEmote(action, josh, player);
            case "GIVE_ITEM" -> executeGiveItem(action, player);
            case "ADVANCE_QUEST" -> executeAdvanceQuest(player, world);
            case "WAIT" -> {} // No-op
            default -> LabsCraft.LOGGER.warn("[ActionExecutor] Unknown action type: {}", type);
        }
    }

    private void executeSay(JsonObject action, ServerPlayerEntity player) {
        String text = action.get("text").getAsString();
        player.sendMessage(Text.literal("<Josh Woodward> " + text), false);
    }

    private void executeWalkTo(JsonObject action, JoshWoodwardEntity josh) {
        JsonObject pos = action.getAsJsonObject("position");
        double x = pos.get("x").getAsDouble();
        double y = pos.get("y").getAsDouble();
        double z = pos.get("z").getAsDouble();

        josh.getNavigation().startMovingTo(x, y, z, 0.6);
    }

    private void executeEmote(JsonObject action, JoshWoodwardEntity josh, ServerPlayerEntity player) {
        String emote = action.get("emote").getAsString();

        // Emotes are visual feedback — for now, send as text action description
        String emoteText = switch (emote) {
            case "nod" -> "* Josh nods *";
            case "shake_head" -> "* Josh shakes his head *";
            case "shrug" -> "* Josh shrugs *";
            case "point" -> "* Josh points *";
            default -> "* Josh " + emote + " *";
        };
        player.sendMessage(Text.literal(emoteText), false);
    }

    private void executeGiveItem(JsonObject action, ServerPlayerEntity player) {
        String itemId = action.get("item").getAsString();
        int quantity = action.has("quantity") ? action.get("quantity").getAsInt() : 1;

        // Support labscraft items by name
        ItemStack stack;
        if (itemId.equals("labscraft:tpu")) {
            stack = new ItemStack(ModItems.TPU, quantity);
        } else {
            // Try registry lookup for other items
            var item = Registries.ITEM.get(Identifier.of(itemId));
            stack = new ItemStack(item, quantity);
        }

        if (!player.getInventory().insertStack(stack)) {
            ItemEntity itemEntity = new ItemEntity(
                    player.getWorld(),
                    player.getX(), player.getY(), player.getZ(),
                    stack
            );
            player.getWorld().spawnEntity(itemEntity);
        }
    }

    private void executeAdvanceQuest(ServerPlayerEntity player, ServerWorld world) {
        QuestManager questManager = QuestManager.get(world);
        questManager.advanceStage(player);
    }
}
