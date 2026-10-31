package com.echoesmod.echoes.registry;

import com.echoesmod.echoes.EchoesMod;
import com.echoesmod.echoes.item.StandArrowItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EchoesMod.MODID);

    public static final DeferredItem<Item> STAND_ARROW =
            ITEMS.register("stand_arrow", () -> new StandArrowItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
