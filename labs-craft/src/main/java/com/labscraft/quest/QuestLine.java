package com.labscraft.quest;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The LabsCraft internship quest line: every stage's objectives, defined
 * against real game events. Content ids are referenced as namespaced strings
 * so this layer stays free of Minecraft classes. Pure Java.
 */
public final class QuestLine {

    // Content ids owned by the content layer (G1). String-referenced on purpose.
    public static final String TPU = "labscraft:tpu";
    public static final String TPU_ORE = "labscraft:tpu_ore";
    public static final String DEEPSLATE_TPU_ORE = "labscraft:deepslate_tpu_ore";
    public static final String FLOW_CRAFTING_TABLE = "labscraft:flow_crafting_table";
    public static final String NANO_BANANA_CONSOLE = "labscraft:nano_banana_console";
    public static final String VEO_CONSOLE = "labscraft:veo_console";
    public static final String GENERATED_IMAGE = "labscraft:generated_image";
    public static final String GENERATED_VIDEO = "labscraft:generated_video";

    private static final Set<String> ANY = Set.of();
    private static final Set<String> NO_PREREQS = Set.of();
    private static final Set<String> TPU_ORES = Set.of(TPU_ORE, DEEPSLATE_TPU_ORE);

    private static final Map<QuestStage, StageDefinition> STAGES = buildStages();

    private QuestLine() {
    }

    private static Map<QuestStage, StageDefinition> buildStages() {
        Map<QuestStage, StageDefinition> stages = new EnumMap<>(QuestStage.class);

        stages.put(QuestStage.NOT_STARTED, new StageDefinition(QuestStage.NOT_STARTED, List.of(
                new ObjectiveDefinition("meet_josh",
                        "Report to Josh Woodward for onboarding",
                        1, QuestEventType.TALKED_TO_JOSH, ANY, NO_PREREQS, true))));

        stages.put(QuestStage.FLOW_INTRO, new StageDefinition(QuestStage.FLOW_INTRO, List.of(
                new ObjectiveDefinition("mine_tpu_ore",
                        "Mine 3 TPU Ore",
                        3, QuestEventType.BLOCK_MINED, TPU_ORES, NO_PREREQS, false),
                new ObjectiveDefinition("craft_tpu",
                        "Craft 2 TPU",
                        2, QuestEventType.ITEM_CRAFTED, Set.of(TPU), NO_PREREQS, false))));

        stages.put(QuestStage.LEARNING_PIPELINE, new StageDefinition(QuestStage.LEARNING_PIPELINE, List.of(
                new ObjectiveDefinition("craft_flow_table",
                        "Craft a Flow Crafting Table",
                        1, QuestEventType.ITEM_CRAFTED, Set.of(FLOW_CRAFTING_TABLE), NO_PREREQS, false),
                new ObjectiveDefinition("craft_nano_banana_console",
                        "Assemble a Nano Banana Console",
                        1, QuestEventType.ITEM_CRAFTED, Set.of(NANO_BANANA_CONSOLE), NO_PREREQS, false))));

        stages.put(QuestStage.FIRST_GENERATION, new StageDefinition(QuestStage.FIRST_GENERATION, List.of(
                new ObjectiveDefinition("generate_image",
                        "Generate an image with the Nano Banana Console",
                        1, QuestEventType.GENERATION_COMPLETED, Set.of(GENERATED_IMAGE), NO_PREREQS, false),
                new ObjectiveDefinition("demo_to_josh",
                        "Show your generated image to Josh",
                        1, QuestEventType.TALKED_TO_JOSH, ANY, Set.of("generate_image"), true))));

        stages.put(QuestStage.VIDEO_LAUNCH, new StageDefinition(QuestStage.VIDEO_LAUNCH, List.of(
                new ObjectiveDefinition("mine_tpu_for_veo",
                        "Mine 5 more TPU Ore",
                        5, QuestEventType.BLOCK_MINED, TPU_ORES, NO_PREREQS, false),
                new ObjectiveDefinition("craft_veo_console",
                        "Assemble a Veo Console",
                        1, QuestEventType.ITEM_CRAFTED, Set.of(VEO_CONSOLE), NO_PREREQS, false),
                new ObjectiveDefinition("generate_video",
                        "Generate a video with the Veo Console",
                        1, QuestEventType.GENERATION_COMPLETED, Set.of(GENERATED_VIDEO), NO_PREREQS, false))));

        stages.put(QuestStage.COMPLETED, new StageDefinition(QuestStage.COMPLETED, List.of()));

        return stages;
    }

    public static StageDefinition definitionFor(QuestStage stage) {
        return STAGES.get(stage);
    }
}
