package com.echoesmod.echoes.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** The Stand itself: floats near its owner, has 3 forms (ACT 1/2/3). */
public class EchoesStandEntity extends Entity {
    private static final EntityDataAccessor<Integer> ACT =
            SynchedEntityData.defineId(EchoesStandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK =
            SynchedEntityData.defineId(EchoesStandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(EchoesStandEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public EchoesStandEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ACT, 1);
        builder.define(ATTACK, 0);
        builder.define(OWNER, Optional.empty());
    }

    public int getAct() { return this.entityData.get(ACT); }
    public void setAct(int act) { this.entityData.set(ACT, Math.max(1, Math.min(3, act))); }
    public int getAttackTicks() { return this.entityData.get(ATTACK); }
    public void setAttackTicks(int t) { this.entityData.set(ATTACK, t); }
    public UUID getOwnerId() { return this.entityData.get(OWNER).orElse(null); }
    public void setOwnerId(UUID id) { this.entityData.set(OWNER, Optional.ofNullable(id)); }

    public Player getOwner() {
        UUID id = getOwnerId();
        return id == null ? null : this.level().getPlayerByUUID(id);
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = getOwner();
        if (owner == null || (!owner.isAlive() && !this.level().isClientSide)) {
            if (!this.level().isClientSide) this.discard();
            return;
        }
        int atk = getAttackTicks();
        if (!this.level().isClientSide && atk > 0) setAttackTicks(atk - 1);

        float yaw = owner.getYRot();
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad), fz = Math.cos(rad);   // forward
        double rx = -Math.cos(rad), rz = -Math.sin(rad);  // right

        int act = getAct();
        double up = act == 1 ? 1.0 : (act == 2 ? 0.2 : 0.0);
        up += Math.sin(this.tickCount * 0.12) * (act == 3 ? 0.02 : 0.08);

        double x, z;
        if (atk > 0 && act != 1) {              // attacking: stand in front of the owner
            x = owner.getX() + fx * 1.6 + rx * 0.3;
            z = owner.getZ() + fz * 1.6 + rz * 0.3;
        } else {                                // idle: behind-right of the owner
            x = owner.getX() - fx * 0.9 + rx * 0.8;
            z = owner.getZ() - fz * 0.9 + rz * 0.8;
        }
        this.setPos(x, owner.getY() + up, z);
        this.setYRot(yaw);
        this.setXRot(0);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) setOwnerId(tag.getUUID("Owner"));
        setAct(Math.max(1, tag.getInt("Act")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID id = getOwnerId();
        if (id != null) tag.putUUID("Owner", id);
        tag.putInt("Act", getAct());
    }
}
