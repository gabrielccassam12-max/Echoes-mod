package com.echoesmod.echoes.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** "Heavy" (can't jump, very slow) and "Frozen" (completely immobilised) states. */
public final class Status {
    private static final Map<UUID, long[]> MAP = new HashMap<>(); // [0]=heavy until, [1]=frozen until

    private Status() {}

    public static void clear() { MAP.clear(); }

    public static void heavy(LivingEntity e, int ticks, int slowAmp) {
        long now = e.level().getGameTime();
        long[] s = MAP.computeIfAbsent(e.getUUID(), k -> new long[2]);
        s[0] = Math.max(s[0], now + ticks);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, slowAmp, false, true));
    }

    public static void freeze(LivingEntity e, int ticks) {
        long now = e.level().getGameTime();
        long[] s = MAP.computeIfAbsent(e.getUUID(), k -> new long[2]);
        s[1] = Math.max(s[1], now + ticks);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
    }

    public static boolean isHeavy(LivingEntity e) {
        long[] s = MAP.get(e.getUUID());
        return s != null && s[0] > e.level().getGameTime();
    }

    public static void onTick(EntityTickEvent.Pre ev) {
        if (MAP.isEmpty()) return;
        if (!(ev.getEntity() instanceof LivingEntity le) || le.level().isClientSide) return;
        long[] s = MAP.get(le.getUUID());
        if (s == null) return;
        long now = le.level().getGameTime();
        if (s[0] <= now && s[1] <= now) { MAP.remove(le.getUUID()); return; }
        Vec3 dm = le.getDeltaMovement();
        if (s[1] > now) {
            le.setDeltaMovement(0, Math.min(dm.y, 0), 0);
            le.stopUsingItem();
            le.setTicksFrozen(le.getTicksRequiredToFreeze());
            if (le instanceof Mob m) m.getNavigation().stop();
            le.hurtMarked = true;
        } else if (dm.y > 0) {
            le.setDeltaMovement(dm.x, 0, dm.z);
            le.hurtMarked = true;
        }
    }
}
