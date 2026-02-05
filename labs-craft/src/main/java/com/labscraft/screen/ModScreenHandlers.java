package com.labscraft.screen;

import com.labscraft.LabsCraft;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<FlowConsoleScreenHandler> FLOW_CONSOLE_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LabsCraft.MOD_ID, "flow_console"),
            new ScreenHandlerType<>(FlowConsoleScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );

    public static final ScreenHandlerType<NanoBananaConsoleScreenHandler> NANO_BANANA_CONSOLE_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LabsCraft.MOD_ID, "nano_banana_console"),
            new ScreenHandlerType<>(NanoBananaConsoleScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );

    public static final ScreenHandlerType<VeoConsoleScreenHandler> VEO_CONSOLE_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LabsCraft.MOD_ID, "veo_console"),
            new ScreenHandlerType<>(VeoConsoleScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );

    public static final ScreenHandlerType<FlowCraftingTableScreenHandler> FLOW_CRAFTING_TABLE_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(LabsCraft.MOD_ID, "flow_crafting_table"),
            new ScreenHandlerType<>(FlowCraftingTableScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );

    public static void registerScreenHandlers() {
        LabsCraft.LOGGER.info("Registering screen handlers for " + LabsCraft.MOD_ID);
    }
}
