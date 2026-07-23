package com.labscraft.world;

import com.labscraft.LabsCraft;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * TPU ore generation: Y -64..32 (trapezoid), vein size ~6, 8 veins per chunk.
 * The configured/placed features live in
 * {@code data/labscraft/worldgen/(configured|placed)_feature/tpu_ore.json}.
 */
public final class ModWorldGeneration {
    private ModWorldGeneration() {
    }

    public static final RegistryKey<PlacedFeature> TPU_ORE_PLACED_KEY = RegistryKey.of(
        RegistryKeys.PLACED_FEATURE,
        Identifier.of(LabsCraft.MOD_ID, "tpu_ore"));

    public static void registerWorldGeneration() {
        LabsCraft.LOGGER.info("Registering world generation for {}", LabsCraft.MOD_ID);

        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            TPU_ORE_PLACED_KEY);
    }
}
