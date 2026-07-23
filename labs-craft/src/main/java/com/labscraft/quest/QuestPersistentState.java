package com.labscraft.quest;

import com.labscraft.LabsCraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-attached storage of every player's {@link PlayerQuestState}, keyed by
 * UUID (never by name — PROTOCOL-V2 rule 4). Attached to the overworld so it
 * follows the player across dimensions.
 *
 * <p>Defect #9 fix: {@code markDirty()} is called only on actual mutation
 * (state creation, and by {@link QuestManager} after a change) — never on
 * plain reads.
 */
public class QuestPersistentState extends PersistentState {

    private static final String STORAGE_KEY = LabsCraft.MOD_ID + "_quests";
    private static final String NBT_PLAYERS = "players";
    private static final String NBT_STAGE = "stage";
    private static final String NBT_STAGE_SECONDS = "stage_seconds";
    private static final String NBT_OBJECTIVES = "objectives";

    private static final PersistentState.Type<QuestPersistentState> TYPE = new PersistentState.Type<>(
            QuestPersistentState::new,
            QuestPersistentState::fromNbt,
            null);

    private final Map<UUID, PlayerQuestState> states = new HashMap<>();

    public static QuestPersistentState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    /**
     * Returns the player's quest state, creating a fresh one on first access.
     * Reading an existing state does NOT mark dirty; creating one does.
     */
    public PlayerQuestState getOrCreate(UUID playerUuid) {
        PlayerQuestState existing = states.get(playerUuid);
        if (existing != null) {
            return existing;
        }
        PlayerQuestState fresh = new PlayerQuestState(System::currentTimeMillis);
        states.put(playerUuid, fresh);
        markDirty();
        return fresh;
    }

    private static QuestPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        QuestPersistentState state = new QuestPersistentState();
        NbtCompound players = nbt.getCompound(NBT_PLAYERS);
        for (String key : players.getKeys()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                LabsCraft.LOGGER.warn("Skipping malformed quest-state key '{}'", key);
                continue;
            }
            NbtCompound playerNbt = players.getCompound(key);
            Map<String, Integer> progress = new LinkedHashMap<>();
            NbtCompound objectives = playerNbt.getCompound(NBT_OBJECTIVES);
            for (String objectiveId : objectives.getKeys()) {
                progress.put(objectiveId, objectives.getInt(objectiveId));
            }
            QuestSaveData data = new QuestSaveData(
                    playerNbt.getString(NBT_STAGE),
                    progress,
                    playerNbt.getLong(NBT_STAGE_SECONDS));
            state.states.put(uuid, PlayerQuestState.load(data, System::currentTimeMillis));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound players = new NbtCompound();
        for (Map.Entry<UUID, PlayerQuestState> entry : states.entrySet()) {
            QuestSaveData data = entry.getValue().save();
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putString(NBT_STAGE, data.stageName());
            playerNbt.putLong(NBT_STAGE_SECONDS, data.secondsInStage());
            NbtCompound objectives = new NbtCompound();
            for (Map.Entry<String, Integer> objective : data.objectiveProgress().entrySet()) {
                objectives.putInt(objective.getKey(), objective.getValue());
            }
            playerNbt.put(NBT_OBJECTIVES, objectives);
            players.put(entry.getKey().toString(), playerNbt);
        }
        nbt.put(NBT_PLAYERS, players);
        return nbt;
    }
}
