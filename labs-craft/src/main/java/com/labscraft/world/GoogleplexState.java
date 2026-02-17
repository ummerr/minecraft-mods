package com.labscraft.world;

import com.labscraft.LabsCraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

public class GoogleplexState extends PersistentState {
    private static final String DATA_NAME = LabsCraft.MOD_ID + "_googleplex";

    private boolean generated = false;

    public GoogleplexState() {
    }

    private static final PersistentState.Type<GoogleplexState> TYPE = new PersistentState.Type<>(
        GoogleplexState::new,
        GoogleplexState::createFromNbt,
        null
    );

    public static GoogleplexState get(ServerWorld world) {
        GoogleplexState state = world.getPersistentStateManager().getOrCreate(TYPE, DATA_NAME);
        return state;
    }

    public static GoogleplexState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        GoogleplexState state = new GoogleplexState();
        state.generated = nbt.getBoolean("generated");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
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
