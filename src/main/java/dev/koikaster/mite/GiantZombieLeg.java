package dev.koikaster.mite;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

final class GiantZombieLeg extends PartEntity<SourceGiantZombieBoss> {
    static final int TORSO = -1;
    static final int RIGHT_ARM = -2;
    static final int LEFT_ARM = -3;
    private static final EntityDimensions SIZE = EntityDimensions.fixed(1.26f, 4.6f);
    private static final EntityDimensions UPPER = EntityDimensions.fixed(3.1f, 7.3f);
    private static final EntityDimensions ARM = EntityDimensions.fixed(1.5f, 4.5f);
    private static final AABB LEG_LOCAL = new AABB(-.75, 0, -.75, .75, 4.5, .75);
    private final int side;
    private LiftedLeg lifted;

    GiantZombieLeg(SourceGiantZombieBoss giant, int side) {
        super(giant);
        this.side = side;
        refreshDimensions();
    }

    void align(Vec3 feet) {
        lifted = null;
        xo = xOld = getX();
        yo = yOld = getY();
        zo = zOld = getZ();
        setPos(feet.x, feet.y, feet.z);
    }

    void alignLifted(Vec3 hip, Vec3 forward, float pitch, double groundY) {
        alignBounds(GiantZombieCombat.kneelingBounds(hip, forward, pitch, groundY));
        double sine = Math.sin(pitch);
        double cosine = Math.cos(pitch);
        Vec3 across = new Vec3(-forward.z, 0, forward.x);
        Vec3 down = forward.scale(-sine).add(0, -cosine, 0);
        Vec3 front = forward.scale(cosine).add(0, -sine, 0);
        lifted = new LiftedLeg(hip, across, down, front);
    }

    boolean hasLiftedHitbox() {
        return lifted != null;
    }

    Optional<Vec3> clipLifted(Vec3 origin, Vec3 end, double border) {
        Vec3 localOrigin = lifted.local(origin);
        AABB target = LEG_LOCAL.inflate(border);
        if (target.contains(localOrigin)) return Optional.of(origin);
        return target.clip(localOrigin, lifted.local(end)).map(lifted::world);
    }

    private record LiftedLeg(Vec3 hip, Vec3 across, Vec3 down, Vec3 front) {
        Vec3 local(Vec3 world) {
            Vec3 offset = world.subtract(hip);
            return new Vec3(offset.dot(across), offset.dot(down), offset.dot(front));
        }

        Vec3 world(Vec3 local) {
            return hip.add(across.scale(local.x)).add(down.scale(local.y)).add(front.scale(local.z));
        }
    }

    void alignKneeling(Vec3 hip, Vec3 forward, float pitch, double groundY) {
        alignBounds(GiantZombieCombat.kneelingBounds(hip, forward, pitch, groundY));
    }

    void alignBounds(AABB bounds) {
        Vec3 middle = bounds.getCenter();
        align(new Vec3(middle.x, bounds.minY, middle.z));
        setBoundingBox(bounds);
    }

    private boolean arm() { return side == RIGHT_ARM || side == LEFT_ARM; }

    @Override public EntityDimensions getDimensions(Pose pose) { return arm() ? ARM : side == TORSO ? UPPER : SIZE; }
    @Override public boolean isPickable() { return getParent().canHitPart(arm()); }
    @Override public boolean canBeHitByProjectile() { return isPickable(); }
    @Override public boolean is(Entity other) { return other == this || other == getParent(); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean hurt(DamageSource damage, float amount) {
        if (!isPickable()) return false;
        var giant = getParent();
        return side < 0 || giant.action() == SourceGiantZombieBoss.FALLEN
            ? giant.hurt(damage, amount) : giant.hurtLeg(side, damage, amount);
    }
}
