package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

final class WitherSideHead extends PartEntity<SourceWitherBoss> {
    private static final EntityDimensions SIZE = EntityDimensions.fixed(.82f, .82f);
    private final int head;

    WitherSideHead(SourceWitherBoss owner, int head) {
        super(owner);
        this.head = head;
        refreshDimensions();
    }

    void align(Vec3 center) {
        xo = xOld = getX();
        yo = yOld = getY();
        zo = zOld = getZ();
        setPos(center.x, center.y - SIZE.height * .5, center.z);
    }

    @Override public EntityDimensions getDimensions(Pose pose) { return SIZE; }
    @Override public boolean isPickable() { return getParent().ritualHeadTargetable(head); }
    @Override public boolean canBeHitByProjectile() { return isPickable(); }
    @Override public boolean is(Entity other) { return other == this || other == getParent(); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean hurt(DamageSource damage, float amount) { return getParent().hurtSideHead(head, damage, amount); }
}
