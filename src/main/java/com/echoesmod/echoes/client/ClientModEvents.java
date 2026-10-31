package com.echoesmod.echoes.client;

import com.echoesmod.echoes.EchoesMod;
import com.echoesmod.echoes.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = EchoesMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    public static final ModelLayerLocation ACT1 = new ModelLayerLocation(EchoesMod.id("echoes_act1"), "main");
    public static final ModelLayerLocation ACT2 = new ModelLayerLocation(EchoesMod.id("echoes_act2"), "main");
    public static final ModelLayerLocation ACT3 = new ModelLayerLocation(EchoesMod.id("echoes_act3"), "main");

    private static final String CAT = "key.categories.echoes";
    public static final KeyMapping SUMMON = new KeyMapping("key.echoes.summon", GLFW.GLFW_KEY_R, CAT);
    public static final KeyMapping SWITCH = new KeyMapping("key.echoes.switch", GLFW.GLFW_KEY_X, CAT);
    public static final KeyMapping WORD = new KeyMapping("key.echoes.word", GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping[] ABILITY = {
            new KeyMapping("key.echoes.ability1", GLFW.GLFW_KEY_Z, CAT),
            new KeyMapping("key.echoes.ability2", GLFW.GLFW_KEY_C, CAT),
            new KeyMapping("key.echoes.ability3", GLFW.GLFW_KEY_V, CAT),
            new KeyMapping("key.echoes.ability4", GLFW.GLFW_KEY_B, CAT),
            new KeyMapping("key.echoes.ability5", GLFW.GLFW_KEY_N, CAT),
            new KeyMapping("key.echoes.ability6", GLFW.GLFW_KEY_M, CAT)};

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ACT1, EchoesModel::act1);
        e.registerLayerDefinition(ACT2, EchoesModel::act2);
        e.registerLayerDefinition(ACT3, EchoesModel::act3);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.ECHOES.get(), EchoesRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(SUMMON);
        e.register(SWITCH);
        e.register(WORD);
        for (KeyMapping k : ABILITY) e.register(k);
    }
}
