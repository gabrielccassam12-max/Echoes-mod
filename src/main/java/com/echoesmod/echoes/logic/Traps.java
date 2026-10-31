package com.echoesmod.echoes.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Placed words: ACT 1 SOUND TRAP and ACT 2 FREEZE!. */
public final class Traps {
    public enum Type { SOUND, FREEZE }

    private static final class Trap {
        Type type; ServerLevel level; Vec3 pos; long expire; UUID owner;
    }

    private static final List<Trap> TRAPS = new ArrayList<>();

    private Traps() {}

    public static void clear() { TRAPS.clear(); }

    public static void add(Type type, ServerLevel level, Vec3 pos, int ticks, UUID owner) {
        Trap t = new Trap();
        t.type = type; t.level = level; t.pos = pos; t.owner = owner;
        t.expire = level.getGameTime() + ticks;
        TRAPS.add(t);
    }

    public static void tick() {
        if (TRAPS.isEmpty()) return;
        Iterator<Trap> it = TRAPS.iterator();
        while (it.hasNext()) {
            Trap t = it.next();
            long now = t.level.getGameTime();
            if (now >= t.expire) { it.remove(); continue; }
            if (now % 10 == 0) {
                EchoesLogic.fx(t.level, t.type == Type.SOUND ? ParticleTypes.NOTE : ParticleTypes.SNOWFLAKE,
                        t.pos, 3, 0.3, 0.02);
            }
            if (now % 4 != 0) continue;

            final double r = t.type == Type.SOUND ? 4.0 : 2.0;
            final Trap trap = t;
            List<LivingEntity> near = t.level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(t.pos, t.pos).inflate(r),
                    e -> e.isAlive() && !e.getUUID().equals(trap.owner) && e.distanceToSqr(trap.pos) <= r * r);
            if (near.isEmpty()) continue;

            ServerPlayer owner = t.level.getServer().getPlayerList().getPlayer(t.owner);
            if (t.type == Type.SOUND) {
                DamageSource src = owner != null ? owner.damageSources().playerAttack(owner)
                        : t.level.damageSources().magic();
                EchoesLogic.fx(t.level, ParticleTypes.SONIC_BOOM, t.pos, 1, 0, 0);
                EchoesLogic.fx(t.level, ParticleTypes.NOTE, t.pos, 30, 1.5, 0.5);
                EchoesLogic.snd(t.level, t.pos, "entity.warden.sonic_boom", 1.2f, 1.2f);
                for (LivingEntity e : near) {
                    e.invulnerableTime = 0;
                    e.hurt(src, 6f);
                    e.removeEffect(MobEffects.INVISIBILITY);
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                }
            } else {
                int n = 0;
                for (LivingEntity e : near) {
                    if (n++ >= 3) break;
                    Status.freeze(e, 60);
                    EchoesLogic.fx(t.level, ParticleTypes.SNOWFLAKE, e.position().add(0, 1, 0), 25, 0.4, 0.05);
                }
                EchoesLogic.snd(t.level, t.pos, "block.glass.break", 1f, 1.5f);
            }
            it.remove();
        }
    }
}
