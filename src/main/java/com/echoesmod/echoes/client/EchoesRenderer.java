package com.echoesmod.echoes.client;

import com.echoesmod.echoes.EchoesMod;
import com.echoesmod.echoes.entity.EchoesStandEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class EchoesRenderer extends EntityRenderer<EchoesStandEntity> {
    private static final ResourceLocation[] TEX = {
            EchoesMod.id("textures/entity/echoes_act1.png"),
            EchoesMod.id("textures/entity/echoes_act2.png"),
            EchoesMod.id("textures/entity/echoes_act3.png")};

    private final EchoesModel[] models = new EchoesModel[3];

    public EchoesRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        models[0] = new EchoesModel(ctx.bakeLayer(ClientModEvents.ACT1), 1);
        models[1] = new EchoesModel(ctx.bakeLayer(ClientModEvents.ACT2), 2);
        models[2] = new EchoesModel(ctx.bakeLayer(ClientModEvents.ACT3), 3);
    }

    @Override
    public void render(EchoesStandEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        int act = Mth.clamp(e.getAct(), 1, 3);
        EchoesModel m = models[act - 1];
        ps.pushPose();
        float bodyYaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.501f, 0f);
        m.setupAnim(e, 0f, 0f, e.tickCount + pt, 0f, 0f);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX[act - 1]));
        m.renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, 0xE6FFFFFF);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EchoesStandEntity e) {
        return TEX[Mth.clamp(e.getAct(), 1, 3) - 1];
    }
}
