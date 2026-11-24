package dev.koikaster.mite;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

final class CrabulonBrood extends PathfinderMob {
    private SourceCrabulon owner;

    CrabulonBrood(EntityType<? extends CrabulonBrood> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setNoAi(true);
    }

    static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 6);
    }

    void init(SourceCrabulon owner, Vec3 point) {
        this.owner = owner;
        setPos(point);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive()) return;
        if (owner == null || !owner.isAlive() || tickCount > 72) {
            discard();
            return;
        }
        move(MoverType.SELF, new Vec3(0, -.13, 0));
        if (tickCount % 3 == 0) level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
            getX(), getY() + .23, getZ(), 5, .23, .18, .23, .04);
        if (onGround()) {
            owner.broodLanded(level, position());
            discard();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.MYCELIUM, getX(), getY() + .2, getZ(), 16, .32, .28, .32, .09);
            discard();
        }
    }
}
