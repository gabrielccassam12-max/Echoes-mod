package com.echoesmod.echoes.item;

import com.echoesmod.echoes.logic.StandData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click to awaken the Stand (Echoes ACT 1). */
public class StandArrowItem extends Item {
    public StandArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (StandData.isAwakened(sp)) {
                sp.displayClientMessage(Component.literal("Você já despertou o Echoes."), true);
                return InteractionResultHolder.fail(stack);
            }
            sp.hurt(sp.damageSources().generic(), 2.0f);
            StandData.awaken(sp);
            stack.shrink(1);
            sp.sendSystemMessage(Component.literal("§a★ Seu Stand despertou: ECHOES ACT 1! §7Aperte R para invocá-lo."));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
