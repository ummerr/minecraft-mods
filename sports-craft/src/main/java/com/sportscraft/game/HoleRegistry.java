package com.sportscraft.game;

import com.sportscraft.core.golf.GolfHoleDef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.math.Vec3d;

/**
 * Every hole that has been built with {@code /sports build golf}, keyed by cup
 * position so rebuilding at the same spot replaces rather than duplicates.
 *
 * <p>Held here rather than inside {@code GolfManager} so that persistence has a
 * single, obvious thing to save and restore.</p>
 */
public final class HoleRegistry {

    private final Map<String, GolfHoleDef> holes = new LinkedHashMap<>();

    public void register(GolfHoleDef hole) {
        holes.put(hole.id(), hole);
    }

    public Collection<GolfHoleDef> all() {
        return holes.values();
    }

    public int size() {
        return holes.size();
    }

    public void clear() {
        holes.clear();
    }

    public void addAll(Collection<GolfHoleDef> incoming) {
        for (GolfHoleDef hole : incoming) {
            register(hole);
        }
    }

    /** The hole whose tee is nearest to a position, within {@code maxDistance}. */
    public GolfHoleDef nearestByTee(Vec3d pos, double maxDistance) {
        GolfHoleDef best = null;
        double bestDistance = maxDistance;
        for (GolfHoleDef hole : holes.values()) {
            double dx = pos.x - hole.teeX();
            double dz = pos.z - hole.teeZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = hole;
            }
        }
        return best;
    }

    public Collection<GolfHoleDef> snapshot() {
        return new ArrayList<>(holes.values());
    }
}
