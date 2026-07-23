package com.labscraft.screen;

import com.labscraft.LabsCraft;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    private ModScreenHandlers() {
    }

    /** One handler type shared by all three consoles (identical layout; title differs). */
    public static final ScreenHandlerType<ConsoleScreenHandler> CONSOLE =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LabsCraft.MOD_ID, "console"),
            new ScreenHandlerType<>(ConsoleScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static final ScreenHandlerType<FlowCraftingTableScreenHandler> FLOW_CRAFTING_TABLE =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LabsCraft.MOD_ID, "flow_crafting_table"),
            new ScreenHandlerType<>(FlowCraftingTableScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static void registerScreenHandlers() {
        LabsCraft.LOGGER.info("Registering screen handlers for {}", LabsCraft.MOD_ID);
    }
}
