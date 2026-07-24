package com.labscraft.logic;

/**
 * Pure-Java pool of plausible generation "prompts" used to flavor the items the
 * consoles produce, so each generated artifact feels distinct. No Minecraft
 * imports so it can be unit tested directly.
 */
public final class PromptPool {
    private final String[] prompts;

    public PromptPool(String[] prompts) {
        if (prompts == null || prompts.length == 0) {
            throw new IllegalArgumentException("prompt pool must not be empty");
        }
        this.prompts = prompts.clone();
    }

    public int size() {
        return prompts.length;
    }

    /** Deterministic pick; any long (including negatives) maps into the pool. */
    public String pick(long seed) {
        return prompts[(int) Math.floorMod(seed, prompts.length)];
    }

    /** Formats a seed as the 8-hex-digit id shown in artifact lore. */
    public static String formatSeed(long seed) {
        return String.format("%08x", (int) seed);
    }

    /** Prompts for the Flow Console (rough concept sketches). */
    public static final PromptPool FLOW_SKETCH = new PromptPool(new String[] {
        "low-poly capybara barista",
        "isometric ramen shop at dusk",
        "blueprint of a foldable canoe",
        "wireframe llama in a hammock",
        "storyboard: intern's first standup",
        "concept art, moss-covered server rack",
        "one-line drawing of a satisfied PM",
        "napkin sketch of the Q3 roadmap",
        "doodle: robot watering a bonsai",
        "rough pass, lighthouse made of books",
        "gesture study: cat refusing feedback",
        "thumbnail sketch of a cloud castle",
    });

    /** Prompts for the Nano Banana Console (polished images). */
    public static final PromptPool GENERATED_IMAGE = new PromptPool(new String[] {
        "photoreal corgi in a tiny hard hat",
        "watercolor of a neon koi pond",
        "studio portrait of a stoic axolotl",
        "golden-hour skyline made of circuit boards",
        "oil painting, banana wearing a lab coat",
        "macro shot of dew on a keyboard",
        "cozy cabin interior, aurora through window",
        "retro travel poster for the Deepslate layer",
        "flat-lay of a perfectly organized desk",
        "surreal staircase looping into a teacup",
        "origami crane made of graph paper",
        "still life: redstone dust and gold nuggets",
    });

    /** Prompts for the Veo Console (short videos). */
    public static final PromptPool GENERATED_VIDEO = new PromptPool(new String[] {
        "drone flyover of a waffle canyon, 8s",
        "timelapse: city built from dominoes",
        "slow-mo otter high-five, cinematic",
        "POV: riding a paper airplane to work",
        "hummingbird landing on a chess piece",
        "loop of rain on a diner window, lofi",
        "penguin waddling through an office, 12s",
        "orbit shot of a floating island bakery",
        "match cut: sunrise to a fried egg",
        "tracking shot, marble run through a library",
        "stop-motion sandwich assembling itself",
        "crane shot over a tulip field maze",
    });
}
