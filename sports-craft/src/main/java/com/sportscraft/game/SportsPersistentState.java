package com.sportscraft.game;

import com.sportscraft.SportsCraft;
import com.sportscraft.core.golf.GolfHoleDef;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/**
 * World-attached storage for SportsCraft, per {@code QuestPersistentState}:
 * attached to the overworld so it follows players across dimensions, and keyed
 * by UUID rather than name.
 *
 * <p>Balls persist through their own entity NBT, so this only needs to hold the
 * holes that have been built. In-progress rounds are re-established when a
 * player's ball is found again, which keeps the save format small and means a
 * round can never reference a ball that no longer exists.</p>
 *
 * <p>{@code markDirty()} is called only on real mutation, never on plain reads.</p>
 */
public class SportsPersistentState extends PersistentState {

    private static final String STORAGE_KEY = SportsCraft.MOD_ID + "_holes";
    private static final String NBT_HOLES = "holes";

    private static final String NBT_TEE_X = "tee_x";
    private static final String NBT_TEE_Y = "tee_y";
    private static final String NBT_TEE_Z = "tee_z";
    private static final String NBT_CUP_X = "cup_x";
    private static final String NBT_CUP_Y = "cup_y";
    private static final String NBT_CUP_Z = "cup_z";
    private static final String NBT_PAR = "par";
    private static final String NBT_LENGTH = "length";
    private static final String NBT_SEED = "seed";

    private static final PersistentState.Type<SportsPersistentState> TYPE = new PersistentState.Type<>(
            SportsPersistentState::new,
            SportsPersistentState::fromNbt,
            null);

    private final HoleRegistry holes = new HoleRegistry();

    public static SportsPersistentState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    public HoleRegistry holes() {
        return holes;
    }

    public void registerHole(GolfHoleDef hole) {
        holes.register(hole);
        markDirty();
    }

    private static SportsPersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        SportsPersistentState state = new SportsPersistentState();
        NbtList list = nbt.getList(NBT_HOLES, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound holeNbt = list.getCompound(i);
            state.holes.register(new GolfHoleDef(
                    holeNbt.getInt(NBT_TEE_X), holeNbt.getInt(NBT_TEE_Y), holeNbt.getInt(NBT_TEE_Z),
                    holeNbt.getInt(NBT_CUP_X), holeNbt.getInt(NBT_CUP_Y), holeNbt.getInt(NBT_CUP_Z),
                    holeNbt.getInt(NBT_PAR), holeNbt.getInt(NBT_LENGTH), holeNbt.getLong(NBT_SEED)));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (GolfHoleDef hole : holes.all()) {
            NbtCompound holeNbt = new NbtCompound();
            holeNbt.putInt(NBT_TEE_X, hole.teeX());
            holeNbt.putInt(NBT_TEE_Y, hole.teeY());
            holeNbt.putInt(NBT_TEE_Z, hole.teeZ());
            holeNbt.putInt(NBT_CUP_X, hole.cupX());
            holeNbt.putInt(NBT_CUP_Y, hole.cupY());
            holeNbt.putInt(NBT_CUP_Z, hole.cupZ());
            holeNbt.putInt(NBT_PAR, hole.par());
            holeNbt.putInt(NBT_LENGTH, hole.length());
            holeNbt.putLong(NBT_SEED, hole.seed());
            list.add(holeNbt);
        }
        nbt.put(NBT_HOLES, list);
        return nbt;
    }
}
