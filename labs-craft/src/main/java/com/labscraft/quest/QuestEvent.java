package com.labscraft.quest;

/**
 * A single game event fed into the quest engine. {@code id} is a namespaced
 * identifier string (e.g. {@code "labscraft:tpu_ore"}); it is null for events
 * that carry no subject (TALKED_TO_JOSH). Pure Java.
 */
public record QuestEvent(QuestEventType type, String id) {

    public static QuestEvent blockMined(String blockId) {
        return new QuestEvent(QuestEventType.BLOCK_MINED, blockId);
    }

    public static QuestEvent itemCrafted(String itemId) {
        return new QuestEvent(QuestEventType.ITEM_CRAFTED, itemId);
    }

    public static QuestEvent generationCompleted(String itemId) {
        return new QuestEvent(QuestEventType.GENERATION_COMPLETED, itemId);
    }

    public static QuestEvent talkedToJosh() {
        return new QuestEvent(QuestEventType.TALKED_TO_JOSH, null);
    }
}
