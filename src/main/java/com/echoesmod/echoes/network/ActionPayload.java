package com.echoesmod.echoes.network;

import com.echoesmod.echoes.EchoesMod;
import com.echoesmod.echoes.logic.EchoesLogic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server. action: 0 summon/dismiss, 1 switch ACT, 2..7 abilities 1..6, 8 cycle catastrophe word.
 */
public record ActionPayload(int action) implements CustomPacketPayload {
    public static final Type<ActionPayload> TYPE = new Type<>(EchoesMod.id("action"));
    public static final StreamCodec<ByteBuf, ActionPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(ActionPayload::new, ActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) EchoesLogic.handle(sp, payload.action());
        });
    }
}
