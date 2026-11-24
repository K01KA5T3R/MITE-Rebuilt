package dev.koikaster.mite;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.Vec3;

/** A breakable charge with native projectile picking and no saved attack state. */
final class BossAttackFocus extends ShulkerBullet {
    BossAttackFocus(ServerLevel level, Vec3 center) {
        super(EntityType.SHULKER_BULLET, level);
        setPos(center.x, center.y, center.z);
        setNoGravity(true);
    }

    @Override public boolean shouldBeSaved() { return false; }
}
