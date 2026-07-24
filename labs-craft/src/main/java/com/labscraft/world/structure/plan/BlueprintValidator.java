package com.labscraft.world.structure.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Pure-Java structural checks over a {@link GoogleplexBlueprint}. Used by unit
 * tests and as a pre-placement sanity gate in the Minecraft-facing placer.
 * Returns a list of human-readable problems; an empty list means the
 * blueprint is sound.
 */
public final class BlueprintValidator {

    private BlueprintValidator() {
    }

    public static List<String> validate(GoogleplexBlueprint bp) {
        List<String> problems = new ArrayList<>();
        checkRoomsInBounds(bp, problems);
        checkRoomsDisjoint(bp, problems);
        checkFloorSolid(bp, problems);
        checkCeilingCovered(bp, problems);
        checkPerimeterSealed(bp, problems);
        checkDoors(bp, problems);
        checkReachability(bp, problems);
        return problems;
    }

    private static void checkRoomsInBounds(GoogleplexBlueprint bp, List<String> problems) {
        for (Room room : bp.rooms()) {
            if (room.minX() < 1 || room.minZ() < 1
                    || room.maxX() > GoogleplexBlueprint.SIZE_X - 2
                    || room.maxZ() > GoogleplexBlueprint.SIZE_Z - 2) {
                problems.add("Room " + room.type() + " extends outside the interior: " + room);
            }
        }
    }

    private static void checkRoomsDisjoint(GoogleplexBlueprint bp, List<String> problems) {
        List<Room> rooms = bp.rooms();
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                if (rooms.get(i).overlaps(rooms.get(j))) {
                    problems.add("Rooms overlap: " + rooms.get(i).type() + " and " + rooms.get(j).type());
                }
            }
        }
    }

    private static void checkFloorSolid(GoogleplexBlueprint bp, List<String> problems) {
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                if (bp.get(x, 0, z).passable()) {
                    problems.add("Hole in the floor at (" + x + "," + z + ")");
                }
            }
        }
    }

    private static void checkCeilingCovered(GoogleplexBlueprint bp, List<String> problems) {
        for (Room room : bp.rooms()) {
            for (int x = room.minX(); x <= room.maxX(); x++) {
                for (int z = room.minZ(); z <= room.maxZ(); z++) {
                    if (bp.get(x, 5, z) == BlockSpec.AIR) {
                        problems.add("Hole in the ceiling over " + room.type() + " at (" + x + "," + z + ")");
                    }
                }
            }
        }
    }

    /**
     * Every perimeter cell at body height must be impassable except the
     * declared entrance doorway — no accidental holes in the shell.
     */
    private static void checkPerimeterSealed(GoogleplexBlueprint bp, List<String> problems) {
        int maxX = GoogleplexBlueprint.SIZE_X - 1;
        int maxZ = GoogleplexBlueprint.SIZE_Z - 1;
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
                checkPerimeterCell(bp, x, y, 0, problems);
                checkPerimeterCell(bp, x, y, maxZ, problems);
            }
            for (int z = 1; z < maxZ; z++) {
                checkPerimeterCell(bp, 0, y, z, problems);
                checkPerimeterCell(bp, maxX, y, z, problems);
            }
        }
    }

    private static void checkPerimeterCell(GoogleplexBlueprint bp, int x, int y, int z, List<String> problems) {
        if (!bp.get(x, y, z).passable()) {
            return;
        }
        boolean isEntranceDoor = bp.doors().stream()
                .anyMatch(d -> d.from() == RoomType.OUTSIDE && d.x() == x && d.z() == z);
        if (!isEntranceDoor) {
            problems.add("Unsealed perimeter cell at (" + x + "," + y + "," + z + ")");
        }
    }

    /**
     * Every door cell must be walkable and must sit between its two declared
     * rooms (or between a room and the outside, for the entrance).
     */
    private static void checkDoors(GoogleplexBlueprint bp, List<String> problems) {
        for (Door door : bp.doors()) {
            if (!bp.isWalkable(door.x(), door.z())) {
                problems.add("Door at (" + door.x() + "," + door.z() + ") is not walkable");
            }
            if (!touchesRoom(bp, door.x(), door.z(), door.from())) {
                problems.add("Door at (" + door.x() + "," + door.z() + ") does not touch " + door.from());
            }
            if (!touchesRoom(bp, door.x(), door.z(), door.to())) {
                problems.add("Door at (" + door.x() + "," + door.z() + ") does not touch " + door.to());
            }
        }
    }

    private static boolean touchesRoom(GoogleplexBlueprint bp, int x, int z, RoomType type) {
        int[][] neighbors = {{x + 1, z}, {x - 1, z}, {x, z + 1}, {x, z - 1}};
        for (int[] n : neighbors) {
            boolean outside = n[0] < 0 || n[0] >= GoogleplexBlueprint.SIZE_X
                    || n[1] < 0 || n[1] >= GoogleplexBlueprint.SIZE_Z;
            if (type == RoomType.OUTSIDE) {
                if (outside) {
                    return true;
                }
            } else if (!outside && bp.room(type).contains(n[0], n[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * BFS over walkable cells from the entrance doorway; every room must have
     * at least one reachable cell. This is the "the office is actually
     * traversable" invariant.
     */
    private static void checkReachability(GoogleplexBlueprint bp, List<String> problems) {
        boolean[][] visited = new boolean[GoogleplexBlueprint.SIZE_X][GoogleplexBlueprint.SIZE_Z];
        Deque<int[]> queue = new ArrayDeque<>();

        int startX = GoogleplexBlueprint.ENTRANCE_X;
        int startZ = GoogleplexBlueprint.ENTRANCE_Z;
        if (!bp.isWalkable(startX, startZ)) {
            problems.add("Entrance at (" + startX + "," + startZ + ") is not walkable");
            return;
        }
        visited[startX][startZ] = true;
        queue.add(new int[]{startX, startZ});

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int[][] neighbors = {{cell[0] + 1, cell[1]}, {cell[0] - 1, cell[1]},
                    {cell[0], cell[1] + 1}, {cell[0], cell[1] - 1}};
            for (int[] n : neighbors) {
                if (n[0] < 0 || n[0] >= GoogleplexBlueprint.SIZE_X
                        || n[1] < 0 || n[1] >= GoogleplexBlueprint.SIZE_Z) {
                    continue;
                }
                if (!visited[n[0]][n[1]] && bp.isWalkable(n[0], n[1])) {
                    visited[n[0]][n[1]] = true;
                    queue.add(n);
                }
            }
        }

        for (Room room : bp.rooms()) {
            boolean reachable = false;
            for (int x = room.minX(); x <= room.maxX() && !reachable; x++) {
                for (int z = room.minZ(); z <= room.maxZ() && !reachable; z++) {
                    reachable = visited[x][z];
                }
            }
            if (!reachable) {
                problems.add("Room " + room.type() + " is not reachable from the entrance");
            }
        }
    }
}
