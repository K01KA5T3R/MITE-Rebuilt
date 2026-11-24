package dev.koikaster.mite;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

final class CrabulonSpore extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> FAN = SynchedEntityData.defineId(CrabulonSpore.class, EntityDataSerializers.BOOLEAN);
    static final float LOB_GRAVITY = .046f;
    static final float FAN_GRAVITY = .008f;

    CrabulonSpore(EntityType<? extends CrabulonSpore> type, Level level) {
        super(type, level);
    }

    void init(SourceCrabulon owner, Vec3 start, Vec3 velocity, boolean fan) {
        entityData.set(FAN, fan);
        if (fan) setItem(new ItemStack(Items.RED_MUSHROOM));
        setOwner(owner);
        setPos(start);
        shoot(velocity.x, velocity.y, velocity.z, (float)velocity.length(), 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FAN, false);
    }

    @Override
    protected Item getDefaultItem() { return Items.BROWN_MUSHROOM; }

    @Override
    protected float getGravity() { return entityData.get(FAN) ? FAN_GRAVITY : LOB_GRAVITY; }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void tick() {
        if (!level().isClientSide && (tickCount >= 42 || !(getOwner() instanceof SourceCrabulon crab) || !crab.isAlive())) {
            discard();
            return;
        }
        super.tick();
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.MYCELIUM, getX(), getY(), getZ(), 0, .016, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity candidate) {
        return (entityData.get(FAN) || getDeltaMovement().y < 0) && candidate instanceof ServerPlayer player
            && getOwner() instanceof SourceCrabulon crab && crab.canTarget(player) && super.canHitEntity(candidate);
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof SourceCrabulon crab)) return;
        boolean fan = entityData.get(FAN);
        if (hit instanceof EntityHitResult direct && (fan || getDeltaMovement().y < 0)) {
            direct.getEntity().hurt(level.damageSources().thrown(this, crab), fan ? 8.0f : 10.6f);
        }
        Vec3 at = hit.getLocation();
        level.sendParticles(ParticleTypes.MYCELIUM, at.x, at.y, at.z, 13, .39, .24, .39, .07);
        discard();
    }
}
