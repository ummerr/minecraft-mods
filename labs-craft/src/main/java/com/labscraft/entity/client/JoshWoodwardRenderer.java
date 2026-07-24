package com.labscraft.entity.client;

import com.labscraft.entity.JoshWoodwardEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

/**
 * Renderer for Josh Woodward: a plain biped built from the zombie model layer
 * (identical part layout to a player, no player-specific render state) wearing
 * the default wide-arm player texture. Deliberately simple and working over
 * fancy, per the v1 postmortem.
 */
public class JoshWoodwardRenderer
        extends BipedEntityRenderer<JoshWoodwardEntity, JoshWoodwardRenderState, JoshWoodwardModel> {

    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/player/wide/steve.png");

    public JoshWoodwardRenderer(EntityRendererFactory.Context context) {
        super(context, new JoshWoodwardModel(context.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public Identifier getTexture(JoshWoodwardRenderState state) {
        return TEXTURE;
    }

    @Override
    public JoshWoodwardRenderState createRenderState() {
        return new JoshWoodwardRenderState();
    }

    @Override
    public void updateRenderState(JoshWoodwardEntity entity, JoshWoodwardRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.emote = entity.getEmoteTrackedValue();
        state.emoteTime = entity.age + tickDelta;
    }
}
