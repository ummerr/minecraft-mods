package com.labscraft.entity.client;

import com.labscraft.entity.JoshEmote;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Josh's model: a standard biped (player-format 64x64 texture) whose arms are
 * posed for the arm emotes (shrug / point / facepalm / clap). Head emotes
 * (nod / shake_head) arrive through the entity's synced pitch/head-yaw, which
 * the base {@link BipedEntityModel#setAngles} already applies.
 */
public class JoshWoodwardModel extends BipedEntityModel<JoshWoodwardRenderState> {

    public JoshWoodwardModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setAngles(JoshWoodwardRenderState state) {
        super.setAngles(state);
        JoshEmote emote = JoshEmote.fromTrackedValue(state.emote);
        if (emote == null) {
            return;
        }
        float t = state.emoteTime;
        switch (emote) {
            case SHRUG -> {
                // Both forearms out and up, palms-to-the-sky energy.
                rightArm.pitch = -0.7f;
                leftArm.pitch = -0.7f;
                rightArm.roll = 0.45f + 0.05f * MathHelper.sin(t * 0.3f);
                leftArm.roll = -0.45f - 0.05f * MathHelper.sin(t * 0.3f);
                head.roll = 0.08f;
            }
            case POINT -> {
                // Right arm straight out at eye height, tracking the head.
                rightArm.pitch = -1.57f + head.pitch * 0.5f;
                rightArm.yaw = head.yaw;
                rightArm.roll = 0.0f;
            }
            case FACEPALM -> {
                // Right hand to face; the server pitches the head down to meet it.
                rightArm.pitch = -2.6f;
                rightArm.yaw = -0.35f;
                rightArm.roll = 0.0f;
            }
            case CLAP -> {
                float wave = 0.25f * MathHelper.sin(t * 1.1f);
                rightArm.pitch = -1.57f;
                leftArm.pitch = -1.57f;
                rightArm.yaw = -0.4f + wave;
                leftArm.yaw = 0.4f - wave;
            }
            default -> {
                // NOD / SHAKE_HEAD: handled by synced head rotation.
            }
        }
    }
}
