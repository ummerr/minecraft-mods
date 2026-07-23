package com.labscraft.world.structure.plan;

/**
 * A single doorway cell cut into a wall, connecting two rooms (or a room and
 * {@link RoomType#OUTSIDE} for the entrance). Wide openings are represented as
 * several adjacent Door cells.
 */
public record Door(int x, int z, RoomType from, RoomType to) {
}
