package com.labscraft.entity.client;

import com.labscraft.LabsCraft;
import com.labscraft.entity.JoshWoodwardEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.util.Identifier;

public class JoshWoodwardRenderer extends MobEntityRenderer<JoshWoodwardEntity, ZombieEntityRenderState, ZombieEntityModel<ZombieEntityRenderState>> {

    // Skin texture - see resources/assets/labscraft/textures/entity/skins/README.md for format details
    private static final Identifier TEXTURE = Identifier.of(LabsCraft.MOD_ID, "textures/entity/skins/josh_woodward.png");

    public JoshWoodwardRenderer(EntityRendererFactory.Context context) {
        super(context, new ZombieEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ZombieEntityRenderState createRenderState() {
        return new ZombieEntityRenderState();
    }

    @Override
    public Identifier getTexture(ZombieEntityRenderState state) {
        return TEXTURE;
    }
}
