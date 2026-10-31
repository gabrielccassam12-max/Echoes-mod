package com.echoesmod.echoes.logic;

import com.echoesmod.echoes.entity.EchoesStandEntity;
import com.echoesmod.echoes.registry.ModEntities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** All server-side gameplay: summon, act switching, cooldowns and the 18 abilities. */
public final class EchoesLogic {
    private EchoesLogic() {}

    /** Set to true if BOOM! / SONIC CATASTROPHE should also break blocks. */
    private static final boolean BOOM_BREAKS_BLOCKS = false;
    private static final int MASTERY_NEEDED = 100;

    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

    // seconds, [act-1][slot]
    private static final int[][] CD = {
            {8, 15, 12, 20, 18, 90},
            {8, 14, 18, 20, 16, 100},
            {9, 18, 10, 12, 25, 120}};

    private static final String[][] NAMES = {
            {"SOUND BARRAGE", "SOUND TRAP", "ECHO SCREAM", "SONIC SCOUT", "LOUD WORD", "SONIC OVERLOAD"},
            {"WHOOSH!", "SIZZLE!", "FREEZE!", "BOOM!", "SPLAT!", "SONIC CATASTROPHE"},
            {"ACT 3 BARRAGE", "THREE FREEZE", "HEAVY BREAKER", "DORA DASH", "GRAVITY FIELD", "THREE FREEZE: MAXIMUM"}};

    private static final String[] WORDS = {"BOOM", "SIZZLE", "WHOOSH"};

    public static void clear() { COOLDOWNS.clear(); }

    // =====================================================================
    //  Entry point (called from the network packet)
    // =====================================================================
    public static void handle(ServerPlayer p, int action) {
        if (!StandData.isAwakened(p)) {
            say(p, "Seu Stand ainda não despertou. Use a Flecha de Stand (ou /echoes awaken).");
            return;
        }
        if (action == 0) toggleStand(p);
        else if (action == 1) switchAct(p);
        else if (action == 8) cycleWord(p);
        else if (action >= 2 && action <= 7) useAbility(p, action - 2);
    }

    private static void toggleStand(ServerPlayer p) {
        EchoesStandEntity stand = getStand(p);
        if (stand != null) {
            stand.discard();
            say(p, "Echoes recolhido.");
            return;
        }
        ServerLevel l = p.serverLevel();
        EchoesStandEntity s = new EchoesStandEntity(ModEntities.ECHOES.get(), l);
        s.setOwnerId(p.getUUID());
        s.setAct(StandData.getAct(p));
        s.setPos(p.getX(), p.getY(), p.getZ());
        l.addFreshEntity(s);
        snd(l, p.position(), "block.amethyst_block.chime", 1f, 1.2f);
        say(p, "ECHOES ACT " + StandData.getAct(p) + "!");
    }

    private static void switchAct(ServerPlayer p) {
        int unlocked = StandData.getUnlocked(p);
        if (unlocked <= 1) {
            say(p, "Você ainda não evoluiu o Echoes. Domine o ACT 1 (" + StandData.getMastery(p, 1) + "/" + MASTERY_NEEDED + ").");
            return;
        }
        int act = StandData.getAct(p);
        act = act >= unlocked ? 1 : act + 1;
        StandData.setAct(p, act);
        EchoesStandEntity s = getStand(p);
        if (s != null) s.setAct(act);
        snd(p.serverLevel(), p.position(), "block.beacon.activate", 0.7f, 1.6f);
        say(p, "ECHOES ACT " + act);
    }

    private static void cycleWord(ServerPlayer p) {
        int m = StandData.cycleWordMode(p);
        say(p, "Palavra da SONIC CATASTROPHE: " + WORDS[m] + "!");
    }

    private static void useAbility(ServerPlayer p, int slot) {
        EchoesStandEntity stand = getStand(p);
        if (stand == null) { say(p, "Invoque o Echoes primeiro (R)."); return; }
        int act = StandData.getAct(p);
        long now = p.serverLevel().getGameTime();
        long[] cd = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new long[18]);
        int idx = (act - 1) * 6 + slot;
        if (now < cd[idx]) {
            say(p, NAMES[act - 1][slot] + " em recarga: " + ((cd[idx] - now + 19) / 20) + "s");
            return;
        }
        boolean ok;
        if (act == 1) ok = act1(p, stand, slot);
        else if (act == 2) ok = act2(p, stand, slot);
        else ok = act3(p, stand, slot);
        if (!ok) return;

        cd[idx] = now + CD[act - 1][slot] * 20L;
        int m = StandData.addMastery(p, act, slot == 5 ? 15 : 5);
        int unlocked = StandData.getUnlocked(p);
        if (act == unlocked && act < 3 && m >= MASTERY_NEEDED) {
            StandData.setUnlocked(p, act + 1);
            p.sendSystemMessage(Component.literal("§a★ Domínio do ACT " + act + " completo! §eACT " + (act + 1)
                    + " desbloqueado §7(aperte X para trocar)."));
        } else {
            say(p, "「" + NAMES[act - 1][slot] + "」  §7domínio " + m + "/" + MASTERY_NEEDED);
        }
    }

    // =====================================================================
    //  ACT 1 — SOUND CREATION
    // =====================================================================
    private static boolean act1(ServerPlayer p, EchoesStandEntity s, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0: { // SOUND BARRAGE
                for (int i = 0; i < 15; i++) {
                    final int n = i;
                    Scheduler.later(i * 2, () -> {
                        if (!live(p)) return;
                        Vec3 from = s.position().add(0, 0.9, 0);
                        LivingEntity tg = aim(p, 24);
                        Vec3 to = tg != null ? tg.getBoundingBox().getCenter()
                                : p.getEyePosition().add(p.getLookAngle().scale(14));
                        trail(l, ParticleTypes.NOTE, from, to);
                        snd(l, from, "block.note_block.pling", 0.6f, 0.7f + n * 0.05f);
                        if (tg != null) {
                            hit(p, tg, 1.5f);
                            if (n == 14) knock(p, tg, 1.0);
                        }
                    });
                }
                return true;
            }
            case 1: { // SOUND TRAP
                HitResult hr = p.pick(20, 0f, false);
                if (hr.getType() != HitResult.Type.BLOCK) { say(p, "Mire em um bloco (parede ou chão)."); return false; }
                BlockHitResult br = (BlockHitResult) hr;
                Vec3 pos = Vec3.atCenterOf(br.getBlockPos().relative(br.getDirection()));
                Traps.add(Traps.Type.SOUND, l, pos, 60 * 20, p.getUUID());
                fx(l, ParticleTypes.NOTE, pos, 12, 0.4, 0.1);
                snd(l, pos, "block.note_block.bell", 1f, 1f);
                return true;
            }
            case 2: { // ECHO SCREAM
                Vec3 start = p.getEyePosition().add(0, -0.2, 0);
                Vec3 dir = p.getLookAngle();
                Vec3 end = start.add(dir.scale(20));
                BlockHitResult bh = l.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                double max = Math.min(20, bh.getLocation().distanceTo(start));
                Set<LivingEntity> hitSet = new HashSet<>();
                snd(l, start, "entity.warden.sonic_boom", 1f, 1.3f);
                for (int d = 1; d <= max; d++) {
                    final int dist = d;
                    Scheduler.later(d / 2, () -> {
                        Vec3 pt = start.add(dir.scale(dist));
                        fx(l, ParticleTypes.NOTE, pt, 4, 0.4, 0.2);
                        if (dist % 4 == 0) fx(l, ParticleTypes.SONIC_BOOM, pt, 1, 0, 0);
                        for (LivingEntity e : foesNear(p, pt, 1.6)) {
                            if (hitSet.add(e)) {
                                hit(p, e, 6f);
                                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                            }
                        }
                    });
                }
                return true;
            }
            case 3: { // SONIC SCOUT
                List<LivingEntity> list = foesNear(p, p.position(), 30);
                for (LivingEntity e : list) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, false));
                }
                trail(l, ParticleTypes.END_ROD, s.position().add(0, 0.9, 0), p.getEyePosition().add(p.getLookAngle().scale(30)));
                snd(l, p.position(), "entity.bat.takeoff", 1f, 1.5f);
                say(p, "SONIC SCOUT: " + list.size() + " alvo(s) detectado(s) em 30 blocos.");
                return true;
            }
            case 4: { // LOUD WORD
                Vec3 c = aimPoint(p, 20);
                fx(l, ParticleTypes.SONIC_BOOM, c.add(0, 1, 0), 1, 0, 0);
                fx(l, ParticleTypes.NOTE, c.add(0, 1, 0), 30, 2.0, 0.5);
                snd(l, c, "entity.warden.sonic_boom", 1.5f, 0.7f);
                for (LivingEntity e : foesNear(p, c, 5)) {
                    hit(p, e, 5f);
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    e.stopUsingItem(); // interrupts charged attacks (bow, crossbow...)
                }
                return true;
            }
            default: { // SONIC OVERLOAD
                final Vec3 c = p.position();
                for (int w = 0; w < 6; w++) {
                    Scheduler.later(w * 10, () -> {
                        if (!live(p)) return;
                        for (int k = 0; k < 4; k++) {
                            double a = l.random.nextDouble() * Math.PI * 2;
                            double d = 2 + l.random.nextDouble() * 8;
                            Vec3 pt = c.add(Math.cos(a) * d, 1.0, Math.sin(a) * d);
                            fx(l, ParticleTypes.EXPLOSION, pt, 1, 0, 0);
                            fx(l, ParticleTypes.NOTE, pt, 8, 0.5, 0.3);
                        }
                        snd(l, c, "block.note_block.bass", 1.5f, 0.6f);
                        for (LivingEntity e : foesNear(p, c, 10)) hit(p, e, 3f);
                    });
                }
                Scheduler.later(60, () -> {
                    if (!live(p)) return;
                    fx(l, ParticleTypes.EXPLOSION_EMITTER, c.add(0, 1, 0), 1, 0, 0);
                    snd(l, c, "entity.generic.explode", 2f, 0.8f);
                    for (LivingEntity e : foesNear(p, c, 10)) {
                        hit(p, e, 8f);
                        Vec3 d = e.position().subtract(c).normalize();
                        e.setDeltaMovement(d.x * 1.6, 0.9, d.z * 1.6);
                        e.hurtMarked = true;
                    }
                });
                return true;
            }
        }
    }

    // =====================================================================
    //  ACT 2 — SOUND EFFECTS
    // =====================================================================
    private static boolean act2(ServerPlayer p, EchoesStandEntity s, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0: { // WHOOSH!
                Vec3 start = p.getEyePosition();
                Vec3 dir = p.getLookAngle();
                AABB box = new AABB(start, start.add(dir.scale(10))).inflate(2.5);
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box, x -> isFoe(p, x))) {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(start).normalize();
                    if (to.dot(dir) < 0.6) continue;
                    e.setDeltaMovement(dir.x * 2.2, 0.35, dir.z * 2.2);
                    e.hurtMarked = true;
                }
                for (Projectile pr : l.getEntitiesOfClass(Projectile.class, box, x -> x.getOwner() != p)) {
                    if (pr instanceof Fireball) pr.discard();
                    else pr.setDeltaMovement(dir.scale(Math.max(0.8, pr.getDeltaMovement().length())));
                }
                for (int i = 1; i <= 10; i++) fx(l, ParticleTypes.CLOUD, start.add(dir.scale(i)).add(0, -0.3, 0), 6, 0.5, 0.1);
                snd(l, start, "entity.wind_charge.wind_burst", 1f, 1f);
                return true;
            }
            case 1: { // SIZZLE!
                LivingEntity t = aim(p, 20);
                if (t == null) { say(p, "Sem alvo para o SIZZLE!"); return false; }
                t.setRemainingFireTicks(100);
                hit(p, t, 2f);
                for (LivingEntity e : foesNear(p, t.position(), 3)) {
                    if (e != t) e.setRemainingFireTicks(60);
                }
                fx(l, ParticleTypes.FLAME, t.position().add(0, 1, 0), 40, 0.6, 0.05);
                snd(l, t.position(), "item.firecharge.use", 1f, 1f);
                return true;
            }
            case 2: { // FREEZE!
                Vec3 c = aimPoint(p, 16);
                Traps.add(Traps.Type.FREEZE, l, c.add(0, 0.2, 0), 15 * 20, p.getUUID());
                fx(l, ParticleTypes.SNOWFLAKE, c.add(0, 0.5, 0), 25, 0.8, 0.05);
                snd(l, c, "block.glass.break", 1f, 1.4f);
                return true;
            }
            case 3: { // BOOM!
                Vec3 c = aimPoint(p, 24);
                fx(l, ParticleTypes.SMOKE, c.add(0, 0.5, 0), 20, 0.4, 0.02);
                snd(l, c, "entity.tnt.primed", 1f, 1f);
                Scheduler.later(20, () -> { if (live(p)) boom(p, l, c, 5, 10f); });
                return true;
            }
            case 4: { // SPLAT!
                LivingEntity t = aim(p, 16);
                if (t == null) { say(p, "Sem alvo para o SPLAT!"); return false; }
                Status.heavy(t, 80, 6);
                fx(l, ParticleTypes.ITEM_SLIME, t.position().add(0, 0.5, 0), 40, 0.5, 0.1);
                snd(l, t.position(), "entity.slime.squish", 1.2f, 0.8f);
                return true;
            }
            default: { // SONIC CATASTROPHE
                final int mode = StandData.getWordMode(p);
                final Vec3 c = p.position();
                if (mode == 0) { // BOOM: chain explosions
                    for (int i = 0; i < 8; i++) {
                        Scheduler.later(i * 6, () -> {
                            if (!live(p)) return;
                            double a = l.random.nextDouble() * Math.PI * 2;
                            double d = 3 + l.random.nextDouble() * 9;
                            boom(p, l, c.add(Math.cos(a) * d, 0, Math.sin(a) * d), 4, 7f);
                        });
                    }
                } else if (mode == 1) { // SIZZLE: area fire
                    for (int i = 0; i < 5; i++) {
                        Scheduler.later(i * 20, () -> {
                            if (!live(p)) return;
                            fx(l, ParticleTypes.FLAME, c.add(0, 0.5, 0), 80, 6, 0.05);
                            snd(l, c, "item.firecharge.use", 1.5f, 0.7f);
                            for (LivingEntity e : foesNear(p, c, 12)) {
                                e.setRemainingFireTicks(100);
                                hit(p, e, 2f);
                                fx(l, ParticleTypes.FLAME, e.position().add(0, 1, 0), 10, 0.4, 0.05);
                            }
                        });
                    }
                } else { // WHOOSH: wind storm
                    for (int i = 0; i < 6; i++) {
                        Scheduler.later(i * 8, () -> {
                            if (!live(p)) return;
                            snd(l, c, "entity.wind_charge.wind_burst", 1.5f, 0.8f);
                            for (int k = 0; k < 24; k++) {
                                double a = k * Math.PI / 12;
                                fx(l, ParticleTypes.CLOUD, c.add(Math.cos(a) * 6, 1, Math.sin(a) * 6), 2, 0.3, 0.1);
                            }
                            for (LivingEntity e : foesNear(p, c, 12)) {
                                Vec3 d = e.position().subtract(c).normalize();
                                e.setDeltaMovement(d.x * 1.2, 0.7, d.z * 1.2);
                                e.hurtMarked = true;
                                hit(p, e, 2f);
                            }
                        });
                    }
                }
                say(p, "SONIC CATASTROPHE: " + WORDS[mode] + "!");
                return true;
            }
        }
    }

    // =====================================================================
    //  ACT 3 — 3 FREEZE
    // =====================================================================
    private static boolean act3(ServerPlayer p, EchoesStandEntity s, int slot) {
        ServerLevel l = p.serverLevel();
        switch (slot) {
            case 0: { // ACT 3 BARRAGE
                LivingEntity t = aim(p, 10);
                if (t == null) { say(p, "Sem alvo ao alcance (10 blocos)."); return false; }
                s.setAttackTicks(70);
                punches(p, t, 20, 3, 0, 1.5f, true);
                return true;
            }
            case 1: { // THREE FREEZE
                LivingEntity t = aim(p, 18);
                if (t == null) { say(p, "Sem alvo para o THREE FREEZE."); return false; }
                int dur = p.distanceTo(t) < 6 ? 100 : 80; // closer = stronger
                Status.heavy(t, dur, 6);
                s.setAttackTicks(15);
                fx(l, ParticleTypes.SMOKE, t.position().add(0, 1, 0), 40, 0.5, 0.02);
                snd(l, t.position(), "block.anvil.land", 1f, 0.5f);
                return true;
            }
            case 2: { // HEAVY BREAKER
                LivingEntity t = aim(p, 6);
                if (t == null) { say(p, "Sem alvo ao alcance (6 blocos)."); return false; }
                float dmg = 8f + (Status.isHeavy(t) ? 6f : 0f);
                s.setAttackTicks(12);
                t.invulnerableTime = 0;
                t.hurt(p.damageSources().indirectMagic(p, p), dmg); // magic damage ignores armour = "breaks defence"
                t.setDeltaMovement(0, -1.5, 0);
                t.hurtMarked = true;
                fx(l, ParticleTypes.EXPLOSION, t.position().add(0, 1, 0), 3, 0.3, 0);
                snd(l, t.position(), "entity.generic.explode", 1f, 1.4f);
                return true;
            }
            case 3: { // DORA DASH
                LivingEntity t = aim(p, 12);
                if (t == null) { say(p, "Sem alvo ao alcance (12 blocos)."); return false; }
                s.setAttackTicks(25);
                hit(p, t, 5f);
                knock(p, t, 1.2);
                snd(l, t.position(), "entity.player.attack.knockback", 1f, 1f);
                punches(p, t, 5, 2, 4, 1.5f, false);
                return true;
            }
            case 4: { // GRAVITY FIELD
                final Vec3 c = p.position();
                s.setAttackTicks(12);
                snd(l, c, "block.anvil.land", 1.5f, 0.5f);
                for (int t = 0; t <= 120; t += 4) {
                    final int tt = t;
                    Scheduler.later(t, () -> {
                        if (!live(p)) return;
                        for (LivingEntity e : foesNear(p, c, 8)) Status.heavy(e, 12, 3);
                        for (Projectile pr : l.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(8), x -> x.getOwner() != p)) {
                            pr.setDeltaMovement(pr.getDeltaMovement().scale(0.6));
                        }
                        if (tt % 8 == 0) {
                            for (int k = 0; k < 24; k++) {
                                double a = k * Math.PI / 12;
                                fx(l, ParticleTypes.SMOKE, c.add(Math.cos(a) * 8, 0.2, Math.sin(a) * 8), 1, 0.1, 0);
                            }
                        }
                    });
                }
                return true;
            }
            default: { // THREE FREEZE: MAXIMUM
                final LivingEntity t = aim(p, 25);
                if (t == null) { say(p, "Sem alvo para o THREE FREEZE: MAXIMUM."); return false; }
                Status.heavy(t, 100, 6);
                Status.freeze(t, 100);
                s.setAttackTicks(100);
                final BlockParticleOption crack = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
                for (int i = 0; i < 5; i++) {
                    Scheduler.later(i * 20, () -> fx(l, crack, t.position(), 40, 0.8, 0.1));
                }
                punches(p, t, 15, 3, 20, 2f, false);
                Scheduler.later(100, () -> {
                    if (!live(p)) return;
                    Vec3 c = t.position();
                    fx(l, ParticleTypes.SONIC_BOOM, c.add(0, 1, 0), 1, 0, 0);
                    fx(l, ParticleTypes.EXPLOSION_EMITTER, c.add(0, 1, 0), 1, 0, 0);
                    snd(l, c, "entity.generic.explode", 2f, 0.7f);
                    if (t.isAlive()) hit(p, t, 15f);
                    for (LivingEntity e : foesNear(p, c, 6)) {
                        if (e == t) continue;
                        Vec3 d = e.position().subtract(c).normalize();
                        e.setDeltaMovement(d.x * 1.5, 0.7, d.z * 1.5);
                        e.hurtMarked = true;
                    }
                });
                return true;
            }
        }
    }

    // =====================================================================
    //  Helpers (package-visible: used by Traps too)
    // =====================================================================
    private static void punches(ServerPlayer p, LivingEntity t, int count, int interval, int delay, float dmg, boolean finisher) {
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < count; i++) {
            final boolean last = i == count - 1;
            Scheduler.later(delay + i * interval, () -> {
                if (!live(p) || !t.isAlive()) return;
                hit(p, t, dmg);
                fx(l, ParticleTypes.CRIT, t.getBoundingBox().getCenter(), 6, 0.3, 0.2);
                snd(l, t.position(), "entity.player.attack.strong", 0.8f, 0.9f + l.random.nextFloat() * 0.3f);
                if (last && finisher) {
                    hit(p, t, 5f);
                    knock(p, t, 1.6);
                    say(p, "§a§lDORA DORA DORA!");
                    fx(l, ParticleTypes.EXPLOSION, t.getBoundingBox().getCenter(), 2, 0.2, 0);
                }
            });
        }
    }

    private static void boom(ServerPlayer p, ServerLevel l, Vec3 c, double r, float dmg) {
        fx(l, ParticleTypes.EXPLOSION_EMITTER, c, 1, 0, 0);
        snd(l, c, "entity.generic.explode", 1.5f, 1f);
        for (LivingEntity e : foesNear(p, c, r)) {
            hit(p, e, dmg);
            Vec3 d = e.position().subtract(c).normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(d.x * 0.9, 0.5, d.z * 0.9));
            e.hurtMarked = true;
        }
        if (BOOM_BREAKS_BLOCKS) l.explode(p, c.x, c.y, c.z, 2.5f, Level.ExplosionInteraction.BLOCK);
    }

    static EchoesStandEntity getStand(ServerPlayer p) {
        List<EchoesStandEntity> list = p.serverLevel().getEntitiesOfClass(EchoesStandEntity.class,
                p.getBoundingBox().inflate(64), s -> p.getUUID().equals(s.getOwnerId()));
        return list.isEmpty() ? null : list.get(0);
    }

    static boolean live(ServerPlayer p) { return p.isAlive() && !p.isRemoved(); }

    static void say(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e instanceof ArmorStand) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return false;
        return true;
    }

    static List<LivingEntity> foesNear(ServerPlayer p, Vec3 c, double r) {
        final double rr = (r + 0.7) * (r + 0.7);
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r + 1),
                e -> isFoe(p, e) && e.distanceToSqr(c) <= rr);
    }

    /** Nearest living target roughly in front of the player (auto-aim cone ~20 degrees). */
    static LivingEntity aim(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(range), x -> isFoe(p, x))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > range || d < 0.01) continue;
            if (to.scale(1 / d).dot(look) < 0.94) continue;
            if (!p.hasLineOfSight(e)) continue;
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    static Vec3 aimPoint(ServerPlayer p, double range) {
        LivingEntity t = aim(p, range);
        if (t != null) return t.position();
        return p.pick(range, 0f, false).getLocation();
    }

    /** Damage without the vanilla knockback (so rapid barrages don't push the target away). */
    static void hit(ServerPlayer p, LivingEntity t, float dmg) {
        Vec3 before = t.getDeltaMovement();
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), dmg);
        t.setDeltaMovement(before);
    }

    static void knock(ServerPlayer p, LivingEntity t, double strength) {
        t.knockback(strength, p.getX() - t.getX(), p.getZ() - t.getZ());
        t.hurtMarked = true;
    }

    public static void fx(ServerLevel l, ParticleOptions pt, Vec3 v, int n, double spread, double speed) {
        l.sendParticles(pt, v.x, v.y, v.z, n, spread, spread, spread, speed);
    }

    static void trail(ServerLevel l, ParticleOptions pt, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.1) return;
        Vec3 step = d.scale(1 / len);
        for (double t = 0; t < len; t += 0.8) fx(l, pt, a.add(step.scale(t)), 1, 0.05, 0.0);
    }

    public static void snd(ServerLevel l, Vec3 v, String path, float vol, float pitch) {
        SoundEvent se = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("minecraft:" + path));
        if (se != null) l.playSound(null, v.x, v.y, v.z, se, SoundSource.PLAYERS, vol, pitch);
    }
}
