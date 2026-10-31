package com.echoesmod.echoes.registry;

import com.echoesmod.echoes.EchoesMod;
import com.echoesmod.echoes.entity.EchoesStandEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EchoesMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EchoesStandEntity>> ECHOES =
            ENTITIES.register("echoes", () -> EntityType.Builder
                    .<EchoesStandEntity>of(EchoesStandEntity::new, MobCategory.MISC)
                    .sized(0.8f, 1.8f)
                    .noSave()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("echoes"));

    private ModEntities() {}
}
