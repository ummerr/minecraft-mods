package com.sportscraft.entity.client;

import com.sportscraft.entity.SportsBallEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

/**
 * Renders the ball as its item, scaled down — a golf ball should read as a
 * small white dot, not a full-size item drop. Vanilla's flying-item renderer
 * does everything else, which keeps this mod free of custom models.
 */
public class SportsBallRenderer extends FlyingItemEntityRenderer<SportsBallEntity> {

    private static final float BALL_SCALE = 0.5f;

    public SportsBallRenderer(EntityRendererFactory.Context context) {
        super(context, BALL_SCALE, true);
    }
}
