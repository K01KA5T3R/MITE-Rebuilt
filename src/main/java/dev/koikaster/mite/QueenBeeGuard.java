package dev.koikaster.mite;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

final class QueenBeeGuard extends PathfinderMob {
    private SourceQueenBee owner;
    private UUID prey;

    QueenBeeGuard(EntityType<? extends QueenBeeGuard> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setNoAi(true);
    }

    static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 7).add(Attributes.KNOCKBACK_RESISTANCE, .35);
    }

    void init(SourceQueenBee owner, Vec3 position, UUID prey) {
        this.owner = owner;
        this.prey = prey;
        setPos(position);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void tick() {
        if (!level().isClientSide) setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive()) return;
        if (owner == null || !owner.isAlive() || tickCount > 140 || !owner.withinArena(position())) {
            discard();
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(prey);
        if (player == null || player.level() != level || !owner.canTarget(player)) {
            player = level.getPlayers(owner::canTarget).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (player == null) {
                discard();
                return;
            }
            prey = player.getUUID();
        }
        Vec3 target = player.position().add(0, .94, 0);
        Vec3 diff = target.subtract(position());
        Vec3 before = position();
        Vec3 step = diff.normalize().scale(tickCount <= 10 ? .18 : .29);
        move(MoverType.SELF, step);
        if (position().distanceToSqr(before) < .008) {
            move(MoverType.SELF, step.scale(.45).add(0, .29, 0));
        }
        Vec3 actual = position().subtract(before);
        if (actual.horizontalDistanceSqr() > .001) {
            float yaw = (float)Math.toDegrees(Math.atan2(-actual.x, actual.z));
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
        if (tickCount > 10 && getBoundingBox().inflate(.18).intersects(player.getBoundingBox())
                && BossAttackGeometry.visible(level, this, getEyePosition(), player.getBoundingBox().getCenter())
                && player.hurt(level.damageSources().mobAttack(this), 5.4f)) {
            level.playSound(null, blockPosition(), SoundEvents.BEE_STING, SoundSource.HOSTILE, .80f, 1.30f);
            discard();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY() + .20, getZ(), 9, .23, .18, .23, .05);
            discard();
        }
    }
}
