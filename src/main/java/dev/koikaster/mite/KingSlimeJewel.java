package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

final class KingSlimeJewel extends Mob implements ItemSupplier {
    private static final int WARN = 10;
    private SourceKingSlime owner;
    private static final float SHOT_SPEED = .89f;
    private Vec3 lockedTarget = Vec3.ZERO;
    private int warning;
    private int cooldown = 20;

    KingSlimeJewel(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 12).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    static KingSlimeJewel attach(ServerLevel level, SourceKingSlime owner) {
        KingSlimeJewel jewel = KingSlimeContent.JEWEL.get().create(level);
        if (jewel == null) return null;
        jewel.owner = owner;
        Vec3 position = owner.position().add(0, owner.getBbHeight() + .60, 0);
        jewel.setPos(position);
        return level.noCollision(jewel) && level.addFreshEntity(jewel) ? jewel : null;
    }

    @Override
    public ItemStack getItem() {
        return Items.AMETHYST_SHARD.getDefaultInstance();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (owner == null || !owner.isAlive() || owner.level() != level())) discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (owner != null) owner.breakJewel(this);
    }

    void followAndFire(ServerLevel level, SourceKingSlime owner, ServerPlayer target) {
        Vec3 shift = BossAttackGeometry.flatDirection(tickCount * .12).scale(owner.getBbWidth() * .55);
        Vec3 position = owner.position().add(shift).add(0, owner.getBbHeight() + .52, 0);
        if (level.hasChunkAt(BlockPos.containing(position)) && level.noCollision(this, getBoundingBox().move(position.subtract(position())))) {
            setPos(position);
        }
        setDeltaMovement(Vec3.ZERO);
        if (cooldown > 0) --cooldown;
        if (!owner.jewelAttackWindow()) {
            warning = 0;
            return;
        }
        if (warning > 0) {
            if (--warning == 0) fire(level, owner);
            return;
        }
        if (cooldown > 0 || distanceToSqr(owner) > 8 * 8 || distanceToSqr(target) > 18 * 18
                || !BossAttackGeometry.visible(level, this, position(), target.getEyePosition())) return;
        lockedTarget = target.getBoundingBox().getCenter();
        warning = WARN;
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.25f, 1.25f);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 7, .20, .20, .20, .045);
    }

    private void fire(ServerLevel level, SourceKingSlime owner) {
        cooldown = 45;
        Vec3 muzzle = position();
        if (!BossAttackGeometry.visible(level, this, muzzle, lockedTarget)) return;
        Vec3 delta = lockedTarget.subtract(muzzle);
        double flightTicks = Math.min(20, delta.length() / SHOT_SPEED);
        Vec3 aim = delta.add(0, .5 * KingSlimeJewelBolt.GRAVITY * flightTicks * flightTicks, 0).normalize();
        for (int i = -1; i <= 1; ++i) {
            var bolt = KingSlimeContent.JEWEL_BOLT.get().create(level);
            if (bolt == null) continue;
            Vec3 ray = aim.yRot(.18f * i);
            bolt.setOwner(owner);
            bolt.setPos(muzzle.add(ray.scale(.35)));
            bolt.shoot(ray.x, ray.y, ray.z, SHOT_SPEED, 0);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.1f, 1.1f);
    }
}
