package com.labscraft.entity;

import com.labscraft.quest.Objective;
import com.labscraft.quest.QuestStage;

import java.util.List;
import java.util.Map;

/**
 * Josh Woodward's static dialogue: the no-agent-server fallback path. Given
 * the player's current stage and live objectives, returns a stage- and
 * objective-appropriate line in Josh's deadpan Labs-PM voice.
 *
 * <p>Pure Java — no Minecraft imports — so the whole table is unit-testable.
 * Lines respect the character contract from the spec: ≤3 sentences, ≤200
 * chars, never breaks character.</p>
 *
 * <p>Selection is deterministic: the first unmet objective (in definition
 * order) picks the line pool; {@code variant} rotates through the pool so
 * repeated clicks don't repeat the same line. Counter placeholders are
 * positional: {@code %1$d} = progress, {@code %2$d} = goal.</p>
 */
public final class JoshDialogue {

    /** Character-contract cap from the spec; enforced by tests. */
    public static final int MAX_LINE_LENGTH = 200;

    private JoshDialogue() {
    }

    // ------------------------------------------------------------------
    // Line pools, keyed by objective id (the first unmet one wins)
    // ------------------------------------------------------------------

    private static final Map<String, String[]> OBJECTIVE_LINES = Map.ofEntries(
            Map.entry("meet_josh", new String[] {
                    "You must be the new APM. Welcome to Labs. Onboarding is now, because my afternoon is gone and I have a hard stop in 30.",
                    "New intern? Good. HR said you'd arrive Tuesday. It is not Tuesday. We'll log it as an early win.",
            }),
            Map.entry("mine_tpu_ore", new String[] {
                    "Compute is the bottleneck. Compute is always the bottleneck. Go mine TPU ore — you're at %1$d of %2$d.",
                    "The roadmap says %2$d TPU ore. You have %1$d. The roadmap is not a suggestion.",
                    "Procurement quoted six weeks for TPUs. You have a pickaxe. Do the math. %1$d of %2$d.",
                    "TPU ore spawns deep, below y=32. I'd come with you, but I'm double-booked. %1$d of %2$d.",
            }),
            Map.entry("craft_tpu", new String[] {
                    "Raw ore doesn't demo well. Craft %2$d TPUs — gold nuggets, redstone, an iron ingot. It's on the wiki nobody reads. %1$d done.",
                    "Assemble the TPUs yourself. Headcount for this was not approved. %1$d of %2$d.",
                    "Two TPUs, hand-built. If anyone asks, this is 'vertical integration', not 'we lost the vendor'. %1$d of %2$d.",
            }),
            Map.entry("craft_flow_table", new String[] {
                    "Next: craft a Flow Crafting Table. Yes, we built our own table. It took a whole offsite. Don't ask which one.",
                    "You'll need the Flow Crafting Table. Regular crafting tables are legacy infra. We deprecated them internally last quarter.",
                    "One Flow Crafting Table, please. The recipe is on the wiki. The wiki is down. The recipe is: planks and TPUs.",
            }),
            Map.entry("craft_nano_banana_console", new String[] {
                    "Feed the table 5 TPUs and assemble a Nano Banana Console. Don't ask about the name. Marketing won that meeting.",
                    "The Nano Banana Console ships this sprint. Which means you build it this sprint. 5 TPUs at the Flow table.",
                    "Nano Banana is our image product. The console needs 5 TPUs. The name tested well with exactly one focus group.",
            }),
            Map.entry("generate_image", new String[] {
                    "Put a TPU in the Nano Banana Console and generate an image. This is the demo. The demo is the product.",
                    "One TPU in, one image out. If it's beautiful, that was intentional. If not, it's a known issue.",
                    "Generate an image. Leadership wants to see pixels by EOD, and 'EOD' in this org means twenty minutes ago.",
            }),
            Map.entry("demo_to_josh", new String[] {
                    "You generated something? Show me. I have a review at the top of the hour and I need a slide.",
                    "Bring the image over. I want to see it before leadership does. That's not a control thing, it's a survival thing.",
            }),
            Map.entry("mine_tpu_for_veo", new String[] {
                    "Leadership saw the image. Now they want video. Video is just images at scale. Mine 5 more TPU ore — %1$d of %2$d.",
                    "Veo needs compute. Everything needs compute. Back to the mine. %1$d of %2$d.",
                    "The video org's TPU budget was reallocated to the image org, which is you, which mines its own. %1$d of %2$d.",
            }),
            Map.entry("craft_veo_console", new String[] {
                    "Assemble the Veo Console — 10 TPUs at the Flow table. The burn rate is fine. Do not look at the burn rate.",
                    "One Veo Console, 10 TPUs. That's double the Nano Banana cost, which the deck calls 'ambitious scaling'.",
            }),
            Map.entry("generate_video", new String[] {
                    "Three TPUs in the Veo Console, one video out. Ship it. We'll fix the artifacts in post. There is no post.",
                    "Generate a video. If it renders, we launch. If it doesn't, it's a research preview.",
            }));

    private static final String[] COMPLETED_LINES = {
            "The internship's done. Your return offer is in legal review, which makes it a Q3 problem. Strong work. Don't tell the others I said that.",
            "Nothing is on fire, the metrics are up, and I have nothing for you. Savor this. This is the reward.",
            "You shipped Flow end to end. I'll take it from here — mostly to the leads meeting, where I'll present it as 'we'.",
            "Working as intended. All of it, apparently. I keep checking. Take the rest of the day — you've earned a whole afternoon.",
    };

    private static final String[] FALLBACK_LINES = {
            "Status update received. Working as intended. Circle back later — I'm double-booked until further notice.",
            "I have nothing new for you and four meetings about it. Check /labscraft quest if you've lost the thread.",
            "That's not in my OKRs. Your objectives are, though. Go look at them.",
    };

    // ------------------------------------------------------------------
    // API
    // ------------------------------------------------------------------

    /**
     * The line Josh says when a player talks to him.
     *
     * @param stage      the player's current quest stage
     * @param objectives the live objectives for that stage (may be empty)
     * @param variant    any non-negative counter; rotates through the pool
     */
    public static String lineFor(QuestStage stage, List<Objective> objectives, int variant) {
        if (stage == QuestStage.COMPLETED) {
            return pick(COMPLETED_LINES, variant, 0, 0);
        }
        if (objectives != null) {
            for (Objective objective : objectives) {
                if (objective.isDone()) {
                    continue;
                }
                String[] pool = OBJECTIVE_LINES.get(objective.id());
                if (pool != null) {
                    return pick(pool, variant, objective.progress(), objective.goal());
                }
            }
        }
        return pick(FALLBACK_LINES, variant, 0, 0);
    }

    private static String pick(String[] pool, int variant, int progress, int goal) {
        String line = pool[Math.floorMod(variant, pool.length)];
        return String.format(line, progress, goal);
    }
}
