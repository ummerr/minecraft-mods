package com.labscraft.entity.client;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;

/**
 * Render state for Josh Woodward: the biped state plus the currently playing
 * emote (0 = none, otherwise {@link com.labscraft.entity.JoshEmote#trackedValue()})
 * and a smooth time value the model uses to animate arm emotes.
 */
public class JoshWoodwardRenderState extends BipedEntityRenderState {
    /** Tracked emote value copied from the entity; 0 when idle. */
    public int emote;

    /** Entity age + tick delta, for smooth periodic arm motion. */
    public float emoteTime;
}
