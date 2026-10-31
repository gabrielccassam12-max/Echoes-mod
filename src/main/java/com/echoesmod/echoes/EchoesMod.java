package com.echoesmod.echoes;

import com.echoesmod.echoes.registry.ModEntities;
import com.echoesmod.echoes.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(EchoesMod.MODID)
public class EchoesMod {
    public static final String MODID = "echoes";

    public EchoesMod(IEventBus modBus) {
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
