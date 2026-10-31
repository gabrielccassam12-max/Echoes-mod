package com.echoesmod.echoes.client;

import com.echoesmod.echoes.entity.EchoesStandEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * One model class, three shapes (ACT 1 / 2 / 3), built from cubes in code.
 * Textures are 64x128 and split in 5 horizontal colour bands (see BAND_*): every cube samples
 * a solid band, so each part's colour is chosen by the band, not by UV painting.
 */
public class EchoesModel extends EntityModel<EchoesStandEntity> {
    // band start rows in the texture (25px each)
    public static final int G = 0, D = 25, Y = 50, K = 75, W = 100;

    private final ModelPart root;
    private final ModelPart body, armL, armR;
    private final List<ModelPart> tail = new ArrayList<>();
    private final int act;

    public EchoesModel(ModelPart root, int act) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.act = act;
        this.body = root.getChild("body");
        this.armL = body.getChild("armL");
        this.armR = body.getChild("armR");
        ModelPart p = body;
        for (int i = 0; i < 8; i++) {
            if (!p.hasChild("tail" + i)) break;
            p = p.getChild("tail" + i);
            tail.add(p);
        }
    }

    // -------------------------------------------------------------------
    private static PartDefinition box(PartDefinition parent, String name, int band,
                                      float x, float y, float z, int w, int h, int d,
                                      float px, float py, float pz) {
        return parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, band).addBox(x, y, z, w, h, d),
                PartPose.offset(px, py, pz));
    }

    private static PartDefinition tailSeg(PartDefinition parent, int i, int band, int size, int len,
                                          float px, float py, float pz, float xRot) {
        return parent.addOrReplaceChild("tail" + i,
                CubeListBuilder.create().texOffs(0, band).addBox(-size / 2f, -size / 2f, 0, size, size, len),
                PartPose.offsetAndRotation(px, py, pz, xRot, 0, 0));
    }

    // -------------------------------------------------------------------
    /** ACT 1: green tadpole shell, gold horn + ribs, long spotted tail, gold scorpion stinger. */
    public static LayerDefinition act1() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 14, 0));

        box(body, "shell", G, -5, -6, -5, 10, 9, 10, 0, 0, 0);
        box(body, "shellTop", D, -3, -8, -3, 6, 2, 6, 0, 0, 0);
        box(body, "horn", Y, -1, -11, -5, 2, 5, 2, 0, 0, 0);
        for (int i = 0; i < 3; i++) box(body, "rib" + i, Y, -4, 3 + 2 * i, -4, 8, 1, 8, 0, 0, 0);
        box(body, "face", W, -2.5f, 2, -6, 5, 3, 2, 0, 0, 0);
        box(body, "armL", G, -1, 0, -1, 2, 7, 2, -6, 1, -3);
        box(body, "armR", G, -1, 0, -1, 2, 7, 2, 6, 1, -3);

        int[] size = {5, 5, 4, 4, 3, 3};
        PartDefinition parent = body;
        for (int i = 0; i < 6; i++) {
            parent = tailSeg(parent, i, i % 2 == 0 ? G : D, size[i], 6,
                    0, i == 0 ? 1 : 0, i == 0 ? 5 : 6, i == 0 ? 0.55f : 0.3f);
        }
        PartDefinition stinger = box(parent, "stinger", Y, -1.5f, -1.5f, 0, 3, 3, 5, 0, 0, 6);
        box(stinger, "hook", Y, -0.5f, -4, 3, 1, 4, 1, 0, 0, 2);
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** ACT 2: winged "manta" head with gold gem, humanoid green torso, long black tail with arrow tip. */
    public static LayerDefinition act2() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 12, 0));

        box(body, "torso", G, -3, -8, -2, 6, 8, 4, 0, 0, 0);
        box(body, "pelvis", D, -3, 0, -2, 6, 2, 4, 0, 0, 0);
        PartDefinition head = box(body, "head", G, -3, -6, -3, 6, 6, 6, 0, -8, 0);
        box(head, "crest", G, -8, -8, -4, 16, 2, 8, 0, 0, 0);
        box(head, "crestMid", D, -3, -10, -3, 6, 2, 6, 0, 0, 0);
        box(head, "gem", Y, -1, -12, -1, 2, 2, 2, 0, 0, 0);
        box(head, "eyeL", Y, -2.5f, -3.5f, -3.5f, 2, 1, 1, 0, 0, 0);
        box(head, "eyeR", Y, 0.5f, -3.5f, -3.5f, 2, 1, 1, 0, 0, 0);

        PartDefinition armL = box(body, "armL", G, -2, 0, -1.5f, 2, 8, 3, -4, -7, 0);
        box(armL, "handL", W, -2.5f, 8, -2, 3, 3, 4, 0, 0, 0);
        PartDefinition armR = box(body, "armR", G, 0, 0, -1.5f, 2, 8, 3, 4, -7, 0);
        box(armR, "handR", W, -0.5f, 8, -2, 3, 3, 4, 0, 0, 0);

        int[] size = {5, 4, 4, 3, 3, 2};
        float[] rot = {-1.0f, -0.3f, 0.1f, 0.5f, 0.5f, 0.4f};
        PartDefinition parent = body;
        for (int i = 0; i < 6; i++) {
            parent = tailSeg(parent, i, K, size[i], 5, 0, i == 0 ? 2 : 0, i == 0 ? 1.5f : 5, rot[i]);
        }
        PartDefinition arrow = box(parent, "arrow", W, -0.5f, -0.5f, 0, 1, 1, 3, 0, 0, 5);
        box(arrow, "arrowHead", W, -2, -0.5f, 0, 4, 1, 3, 0, 0, 3);
        return LayerDefinition.create(mesh, 64, 128);
    }

    /** ACT 3: grey humanoid, green orbs, yellow eyes, green "3" on the belly. */
    public static LayerDefinition act3() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0, 12, 0));

        box(body, "torso", G, -4, -12, -2, 8, 12, 4, 0, 0, 0);
        // the "3" on the belly (front is -Z)
        box(body, "three1", D, -1.5f, -6, -2.6f, 3, 1, 1, 0, 0, 0);
        box(body, "three2", D, -1.5f, -4, -2.6f, 3, 1, 1, 0, 0, 0);
        box(body, "three3", D, -1.5f, -2, -2.6f, 3, 1, 1, 0, 0, 0);
        box(body, "three4", D, 1.5f, -6, -2.6f, 1, 5, 1, 0, 0, 0);

        PartDefinition head = box(body, "head", G, -3.5f, -8, -3.5f, 7, 8, 7, 0, -12, 0);
        box(head, "orb1", D, -1, -8, -4, 2, 2, 1, 0, 0, 0);
        box(head, "orb2", D, -1, -2, -4, 2, 2, 1, 0, 0, 0);
        box(head, "eyeL", Y, -3, -5, -4, 2, 1, 1, 0, 0, 0);
        box(head, "eyeR", Y, 1, -5, -4, 2, 1, 1, 0, 0, 0);

        PartDefinition armL = box(body, "armL", G, -2, 0, -2, 4, 12, 4, -6, -11, 0);
        box(armL, "orbL", D, -3.5f, 1, -1.5f, 2, 3, 3, 0, 0, 0);
        box(armL, "fistL", K, -2.5f, 9, -2.5f, 5, 3, 5, 0, 0, 0);
        PartDefinition armR = box(body, "armR", G, -2, 0, -2, 4, 12, 4, 6, -11, 0);
        box(armR, "orbR", D, 1.5f, 1, -1.5f, 2, 3, 3, 0, 0, 0);
        box(armR, "fistR", K, -2.5f, 9, -2.5f, 5, 3, 5, 0, 0, 0);

        PartDefinition legL = box(body, "legL", G, -2, 0, -2, 4, 12, 4, -2, 0, 0);
        box(legL, "kneeL", D, -1.5f, 4, -3, 3, 3, 1, 0, 0, 0);
        box(legL, "bootL", K, -2.5f, 8, -2.5f, 5, 4, 5, 0, 0, 0);
        PartDefinition legR = box(body, "legR", G, -2, 0, -2, 4, 12, 4, 2, 0, 0);
        box(legR, "kneeR", D, -1.5f, 4, -3, 3, 3, 1, 0, 0, 0);
        box(legR, "bootR", K, -2.5f, 8, -2.5f, 5, 4, 5, 0, 0, 0);
        return LayerDefinition.create(mesh, 64, 128);
    }

    // -------------------------------------------------------------------
    @Override
    public void setupAnim(EchoesStandEntity e, float limbSwing, float limbSwingAmount,
                          float age, float netHeadYaw, float headPitch) {
        boolean atk = e.getAttackTicks() > 0;
        for (int i = 0; i < tail.size(); i++) {
            tail.get(i).yRot = Mth.sin(age * (atk ? 0.6f : 0.12f) - i * 0.7f) * (atk ? 0.45f : 0.25f);
        }
        float swing = atk ? Mth.sin(age * 1.8f) * 0.55f : Mth.sin(age * 0.1f) * 0.05f;
        float base = atk ? (act == 1 ? -0.6f : -1.45f) : 0f;
        armR.xRot = base + swing;
        armL.xRot = base - swing;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
        root.render(ps, vc, light, overlay, color);
    }
}
