package com.echoesmod.echoes.events;

import com.echoesmod.echoes.EchoesMod;
import com.echoesmod.echoes.logic.EchoesLogic;
import com.echoesmod.echoes.logic.Scheduler;
import com.echoesmod.echoes.logic.StandData;
import com.echoesmod.echoes.logic.Status;
import com.echoesmod.echoes.logic.Traps;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = EchoesMod.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        Scheduler.tick();
        Traps.tick();
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Pre event) {
        Status.onTick(event);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        Scheduler.clear();
        Traps.clear();
        Status.clear();
        EchoesLogic.clear();
    }

    /** Keep the Stand data after dying. */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains("echoes", 10)) {
            event.getEntity().getPersistentData().put("echoes", old.getCompound("echoes").copy());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echoes")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("awaken").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    StandData.awaken(p);
                    p.sendSystemMessage(Component.literal("§aEchoes despertou. Aperte R para invocar."));
                    return 1;
                }))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("act", IntegerArgumentType.integer(1, 3)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int act = IntegerArgumentType.getInteger(ctx, "act");
                            StandData.awaken(p);
                            StandData.setUnlocked(p, act);
                            p.sendSystemMessage(Component.literal("§eAtos liberados até o ACT " + act + ". Aperte X para trocar."));
                            return 1;
                        }))));
    }
}
