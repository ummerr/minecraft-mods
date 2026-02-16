package com.labscraft.quest;

import com.labscraft.LabsCraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestManager extends PersistentState {
    private static final String DATA_NAME = LabsCraft.MOD_ID + "_quests";

    private final Map<UUID, QuestStage> playerStages = new HashMap<>();

    public QuestManager() {
    }

    private static final PersistentState.Type<QuestManager> TYPE = new PersistentState.Type<>(
        QuestManager::new,
        QuestManager::createFromNbt,
        null
    );

    public static QuestManager get(ServerWorld world) {
        QuestManager state = world.getPersistentStateManager().getOrCreate(TYPE, DATA_NAME);
        state.markDirty();
        return state;
    }

    public static QuestManager get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not available");
        }
        return get(overworld);
    }

    public static QuestManager createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        QuestManager manager = new QuestManager();
        NbtCompound stagesNbt = nbt.getCompound("playerStages");

        for (String key : stagesNbt.getKeys()) {
            try {
                UUID playerId = UUID.fromString(key);
                String stageName = stagesNbt.getString(key);
                QuestStage stage = QuestStage.valueOf(stageName);
                manager.playerStages.put(playerId, stage);
            } catch (IllegalArgumentException e) {
                LabsCraft.LOGGER.warn("Failed to load quest stage for player {}: {}", key, e.getMessage());
            }
        }

        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound stagesNbt = new NbtCompound();

        for (Map.Entry<UUID, QuestStage> entry : playerStages.entrySet()) {
            stagesNbt.putString(entry.getKey().toString(), entry.getValue().name());
        }

        nbt.put("playerStages", stagesNbt);
        return nbt;
    }

    public QuestStage getStage(ServerPlayerEntity player) {
        return playerStages.getOrDefault(player.getUuid(), QuestStage.NOT_STARTED);
    }

    public void setStage(ServerPlayerEntity player, QuestStage stage) {
        QuestStage currentStage = getStage(player);
        if (stage != currentStage) {
            playerStages.put(player.getUuid(), stage);
            markDirty();

            // Notify player of quest update with sound
            player.sendMessage(
                Text.literal("[Quest Updated] ")
                    .formatted(Formatting.GOLD)
                    .append(Text.literal(stage.getDisplayName()).formatted(Formatting.YELLOW)),
                false
            );
            player.getServerWorld().playSound(null, player.getBlockPos(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.7f, 1.0f);

            LabsCraft.LOGGER.info("Player {} quest stage updated to {}", player.getName().getString(), stage.name());
        }
    }

    public void advanceStage(ServerPlayerEntity player) {
        QuestStage current = getStage(player);
        QuestStage next = current.next();
        if (next != current) {
            setStage(player, next);
        }
    }

    public boolean isAtStage(ServerPlayerEntity player, QuestStage stage) {
        return getStage(player) == stage;
    }

    public boolean hasReachedStage(ServerPlayerEntity player, QuestStage stage) {
        return getStage(player).isAtLeast(stage);
    }
}
