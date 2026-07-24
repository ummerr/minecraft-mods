package com.labscraft.world;

import com.labscraft.LabsCraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * One-bit world-attached flag: has the Googleplex been auto-generated in this
 * world yet? Read by {@link GoogleplexAutoGenerator} on every server start so
 * the office is only ever built once per world, no matter how often the save
 * is reloaded.
 */
public class GoogleplexState extends PersistentState {

    private static final String STORAGE_KEY = LabsCraft.MOD_ID + "_googleplex";

    private static final PersistentState.Type<GoogleplexState> TYPE = new PersistentState.Type<>(
            GoogleplexState::new,
            GoogleplexState::fromNbt,
            null);

    private boolean generated = false;

    public static GoogleplexState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    private static GoogleplexState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        GoogleplexState state = new GoogleplexState();
        state.generated = nbt.getBoolean("generated");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putBoolean("generated", generated);
        return nbt;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
        markDirty();
    }
}
