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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

final class KingSlimeJewelBolt extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> LOBBED = SynchedEntityData.defineId(KingSlimeJewelBolt.class, EntityDataSerializers.BOOLEAN);
    private static final int FLIGHT_TICKS = 23;
    static final float GRAVITY = .025f;
    static final float LOB_GRAVITY = .052f;

    KingSlimeJewelBolt(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LOBBED, false);
    }

    void markLobbed() {
        entityData.set(LOBBED, true);
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AMETHYST_SHARD;
    }

    @Override
    protected float getGravity() {
        return entityData.get(LOBBED) ? LOB_GRAVITY : GRAVITY;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && (tickCount >= (entityData.get(LOBBED) ? 35 : FLIGHT_TICKS)
                || !hasLiveOwner())) {
            discard();
            return;
        }
        super.tick();
    }

    private boolean hasLiveOwner() {
        return getOwner() instanceof KingSlimeMinion minion ? minion.permitsSlimeBall()
            : getOwner() instanceof SourceKingSlime boss && boss.permitsJewelFlight();
    }

    @Override
    protected boolean canHitEntity(Entity candidate) {
        if (entityData.get(LOBBED) && getDeltaMovement().y >= 0 || !(candidate instanceof ServerPlayer player)) return false;
        boolean allowed = getOwner() instanceof KingSlimeMinion minion ? minion.mayHitWithSlimeBall(player)
            : getOwner() instanceof SourceKingSlime boss && boss.canTarget(player);
        return allowed && super.canHitEntity(candidate);
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!(level() instanceof ServerLevel level)) return;
        if (entityData.get(LOBBED)) {
            if (getDeltaMovement().y < 0 && getOwner() instanceof SourceKingSlime boss) splash(level, boss, hit);
            discard();
            return;
        }
        if (hit instanceof EntityHitResult victim && getOwner() instanceof KingSlimeMinion minion) {
            victim.getEntity().hurt(damageSources().thrown(this, minion), 2.5f);
        } else if (hit instanceof EntityHitResult victim && getOwner() instanceof SourceKingSlime boss) {
            boolean slime = getItem().is(Items.SLIME_BALL);
            if (victim.getEntity().hurt(damageSources().thrown(this, boss), slime ? 4.2f : 4.5f)
                    && slime && victim.getEntity() instanceof ServerPlayer player) {
                var away = BossAttackGeometry.flatDirection(position().subtract(player.position()));
                player.knockback(.38, away.x, away.z);
            }
        }
        discard();
    }

    private void splash(ServerLevel level, SourceKingSlime boss, HitResult hit) {
        Vec3 at = hit.getLocation().add(0, .22, 0);
        if (hit instanceof BlockHitResult block) at = at.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.02));
        Vec3 origin = at;
        for (var player : level.getPlayers(boss::canTarget)) {
            Vec3 offset = player.position().subtract(at);
            if (Math.abs(offset.y) > 1.3 || offset.horizontalDistanceSqr() > 1.85 * 1.85
                    || !BossAttackGeometry.visible(level, boss, origin, player.getBoundingBox().getCenter())) continue;
            if (player.hurt(damageSources().thrown(this, boss), 5.8f)) {
                var away = BossAttackGeometry.flatDirection(at.subtract(player.position()));
                player.knockback(.51, away.x, away.z);
            }
        }
        level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y, at.z, 24, .71, .14, .71, .13);
    }
}
