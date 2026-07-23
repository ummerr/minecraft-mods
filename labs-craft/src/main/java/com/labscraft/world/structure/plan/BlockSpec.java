package com.labscraft.world.structure.plan;

/**
 * Abstract building material used by the pure-Java blueprint layer. Each value
 * is later translated to a real {@code BlockState} by the Minecraft-facing
 * placer; this enum itself has no Minecraft imports so the planner and its
 * tests can run on the plain JVM test classpath.
 *
 * <p>{@link #passable()} is the walkability contract used by the reachability
 * validator: a cell is walkable if the specs at body height (y=1 and y=2) are
 * both passable. Carpets are passable (players stand on them); everything
 * solid, including furniture, is not.</p>
 */
public enum BlockSpec {
    // Empty space
    AIR(true),
    /** An intentional opening cut into a wall (doorway). */
    DOOR_AIR(true),

    // Structure
    FLOOR(false),
    FLOOR_LAB(false),
    WALL(false),
    WALL_ACCENT_BLUE(false),
    WALL_ACCENT_RED(false),
    WALL_ACCENT_YELLOW(false),
    WALL_ACCENT_GREEN(false),
    GLASS(false),
    CEILING(false),
    LIGHT(false),

    // Google-colored carpet accents (walkable)
    CARPET_BLUE(true),
    CARPET_RED(true),
    CARPET_YELLOW(true),
    CARPET_GREEN(true),

    // Furniture
    DESK(false),
    DESK_LAMP(false),
    TABLE(false),
    CHAIR_NORTH(false),
    CHAIR_SOUTH(false),
    CHAIR_EAST(false),
    CHAIR_WEST(false),
    RECEPTION(false),
    COUNTER(false),
    BARREL(false),
    CAKE(false),
    FRIDGE(false),
    PLANT_FERN(false),
    PLANT_BAMBOO(false),
    SCREEN(false),

    // Lab equipment
    SERVER_RACK(false),
    SERVER_LIGHT(false),
    CONSOLE_FLOW(false),
    CONSOLE_NANO_BANANA(false),
    CONSOLE_VEO(false),
    FLOW_CRAFTING_TABLE(false);

    private final boolean passable;

    BlockSpec(boolean passable) {
        this.passable = passable;
    }

    /** Whether a player can occupy / walk through a cell holding this spec. */
    public boolean passable() {
        return passable;
    }
}
