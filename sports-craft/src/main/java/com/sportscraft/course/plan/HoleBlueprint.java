package com.sportscraft.course.plan;

import java.util.List;

/**
 * A planned golf hole as a flat grid of {@link SurfaceCell}s, produced by
 * {@link HolePlanner} and checked by {@link HoleValidator} before anything is
 * placed in the world.
 *
 * <p>Coordinates are blueprint-local: {@code x} runs across the hole, {@code z}
 * runs from the tee (z=0) towards the green. {@code CoursePlacer} maps them 1:1
 * onto world axes, so a hole placed at an origin plays northwards.</p>
 *
 * <p>Grids are arrays, so this is a class rather than a record — array identity
 * would make record equality useless. {@link #signature()} exists for the
 * determinism tests.</p>
 */
public final class HoleBlueprint {

    private final SurfaceCell[][] grid;
    private final int teeX;
    private final int teeZ;
    private final int cupX;
    private final int cupZ;
    private final int par;
    private final long seed;
    private final List<Hazard> hazards;

    /** A named hazard, kept for validation and for the {@code /sports info} readout. */
    public record Hazard(SurfaceCell type, int centerX, int centerZ, int radius) {
    }

    HoleBlueprint(SurfaceCell[][] grid, int teeX, int teeZ, int cupX, int cupZ,
                  int par, long seed, List<Hazard> hazards) {
        this.grid = grid;
        this.teeX = teeX;
        this.teeZ = teeZ;
        this.cupX = cupX;
        this.cupZ = cupZ;
        this.par = par;
        this.seed = seed;
        this.hazards = List.copyOf(hazards);
    }

    public int width() {
        return grid.length;
    }

    public int length() {
        return grid[0].length;
    }

    /** Returns the cell, or null when the coordinates fall outside the hole. */
    public SurfaceCell get(int x, int z) {
        if (!inBounds(x, z)) {
            return null;
        }
        return grid[x][z];
    }

    public boolean inBounds(int x, int z) {
        return x >= 0 && x < width() && z >= 0 && z < length();
    }

    /** True when a ball can rest here — the BFS's notion of a passable cell. */
    public boolean isPlayable(int x, int z) {
        SurfaceCell cell = get(x, z);
        return cell != null && cell.playable();
    }

    public int teeX() {
        return teeX;
    }

    public int teeZ() {
        return teeZ;
    }

    public int cupX() {
        return cupX;
    }

    public int cupZ() {
        return cupZ;
    }

    public int par() {
        return par;
    }

    public long seed() {
        return seed;
    }

    public List<Hazard> hazards() {
        return hazards;
    }

    /** Straight-line tee-to-cup distance, which is what par is derived from. */
    public int holeLength() {
        int dx = cupX - teeX;
        int dz = cupZ - teeZ;
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    public int countCells(SurfaceCell type) {
        int count = 0;
        for (SurfaceCell[] column : grid) {
            for (SurfaceCell cell : column) {
                if (cell == type) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * A stable fingerprint of the whole grid. Two blueprints planned from the
     * same seed must produce the same signature — that is the determinism
     * contract the placer relies on to rebuild an identical hole.
     */
    public String signature() {
        StringBuilder sb = new StringBuilder(width() * length() + 32);
        for (int z = 0; z < length(); z++) {
            for (int x = 0; x < width(); x++) {
                sb.append((char) ('a' + grid[x][z].ordinal()));
            }
        }
        sb.append('|').append(teeX).append(',').append(teeZ)
                .append('|').append(cupX).append(',').append(cupZ)
                .append('|').append(par);
        return sb.toString();
    }
}
