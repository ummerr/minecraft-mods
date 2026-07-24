package com.labscraft.item;

import com.labscraft.logic.PromptPool;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Builds the flavored artifact stacks the consoles produce: a custom name drawn
 * from a pool of plausible prompts plus lore (prompt, model, seed) so each
 * generated item feels distinct. These items are later used as quest rewards
 * and Josh trade-ins.
 */
public enum GeneratedArtifacts {
    FLOW_SKETCH(PromptPool.FLOW_SKETCH, "flow-1-sketch", Formatting.AQUA),
    GENERATED_IMAGE(PromptPool.GENERATED_IMAGE, "nano-banana-xl", Formatting.YELLOW),
    GENERATED_VIDEO(PromptPool.GENERATED_VIDEO, "veo-3", Formatting.LIGHT_PURPLE);

    private final PromptPool pool;
    private final String modelName;
    private final Formatting nameColor;

    GeneratedArtifacts(PromptPool pool, String modelName, Formatting nameColor) {
        this.pool = pool;
        this.modelName = modelName;
        this.nameColor = nameColor;
    }

    /** The item this artifact type is made of (resolved lazily to avoid init cycles). */
    public Item item() {
        return switch (this) {
            case FLOW_SKETCH -> ModItems.FLOW_SKETCH;
            case GENERATED_IMAGE -> ModItems.GENERATED_IMAGE;
            case GENERATED_VIDEO -> ModItems.GENERATED_VIDEO;
        };
    }

    /**
     * Creates a single flavored artifact stack. {@code seed} selects the prompt
     * and is echoed in the lore; pass a random long.
     */
    public ItemStack create(long seed) {
        String prompt = pool.pick(seed);
        ItemStack stack = new ItemStack(item());
        stack.set(DataComponentTypes.CUSTOM_NAME,
            Text.translatable(item().getTranslationKey())
                .append(Text.literal(": “" + prompt + "”"))
                .styled(style -> style.withItalic(false).withColor(nameColor)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
            Text.literal("prompt: " + prompt).styled(s -> s.withItalic(false).withColor(Formatting.GRAY)),
            Text.literal("model: " + modelName).styled(s -> s.withItalic(false).withColor(Formatting.DARK_GRAY)),
            Text.literal("seed: " + PromptPool.formatSeed(seed)).styled(s -> s.withItalic(false).withColor(Formatting.DARK_GRAY))
        )));
        return stack;
    }
}
