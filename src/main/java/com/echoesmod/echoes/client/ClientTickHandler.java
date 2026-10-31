package com.echoesmod.echoes.client;

import com.echoesmod.echoes.EchoesMod;
import com.echoesmod.echoes.network.ActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = EchoesMod.MODID, value = Dist.CLIENT)
public final class ClientTickHandler {
    private ClientTickHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        while (ClientModEvents.SUMMON.consumeClick()) send(0);
        while (ClientModEvents.SWITCH.consumeClick()) send(1);
        while (ClientModEvents.WORD.consumeClick()) send(8);
        for (int i = 0; i < ClientModEvents.ABILITY.length; i++) {
            while (ClientModEvents.ABILITY[i].consumeClick()) send(2 + i);
        }
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new ActionPayload(action));
    }
}
