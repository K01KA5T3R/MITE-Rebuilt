package dev.koikaster.mite;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

final class QueenBeeShot extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> HONEY = SynchedEntityData.defineId(QueenBeeShot.class, EntityDataSerializers.BOOLEAN);
    static final float HONEY_GRAVITY = .064f;
    private boolean poison;
    private float damage;

    QueenBeeShot(EntityType<? extends QueenBeeShot> type, Level level) { super(type, level); }

    void init(SourceQueenBee owner, Vec3 start, Vec3 speed, boolean honey, boolean poison, float damage) {
        setOwner(owner);
        setPos(start);
        entityData.set(HONEY, honey);
        if (honey) setItem(new ItemStack(Items.HONEYCOMB));
        this.poison = poison;
        this.damage = damage;
        shoot(speed.x, speed.y, speed.z, (float)speed.length(), 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HONEY, false);
    }

    boolean honey() { return entityData.get(HONEY); }

    @Override
    protected Item getDefaultItem() { return Items.SPECTRAL_ARROW; }

    @Override
    protected float getGravity() { return honey() ? HONEY_GRAVITY : .003f; }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void tick() {
        if (!level().isClientSide && (tickCount >= 38 || !(getOwner() instanceof SourceQueenBee queen) || !queen.isAlive())) {
            discard();
            return;
        }
        super.tick();
        if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(honey() ? ParticleTypes.FALLING_HONEY : ParticleTypes.CRIT, getX(), getY(), getZ(), 0, .025, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity candidate) {
        return !honey() && candidate instanceof ServerPlayer player && getOwner() instanceof SourceQueenBee queen
            && queen.canTarget(player) && super.canHitEntity(candidate);
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof SourceQueenBee queen)) return;
        Vec3 at = hit.getLocation();
        if (honey()) {
            queen.stickyImpact(level, at);
        } else {
            if (hit instanceof EntityHitResult direct && direct.getEntity() instanceof ServerPlayer player
                    && player.hurt(level.damageSources().thrown(this, queen), damage) && poison) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, BossCombatTuning.queenPoisonDuration(42)), queen);
            }
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, .19, .19, .19, .05);
        }
        discard();
    }
}
