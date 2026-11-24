package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

/** A small, ordinary slime fighter tied to the current encounter. */
final class KingSlimeMinion extends Slime {
    static final double OWNER_RANGE = 42;

    private static final int SHOT_COOLDOWN = 48;
    private static final int SHOT_WARNING = 5;
    private static final float SHOT_SPEED = .82f;
    private SourceKingSlime owner;
    private int shotCooldown;
    private int shotWarning;
    private Vec3 shotTarget;

    KingSlimeMinion(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.ATTACK_DAMAGE, 2).add(Attributes.FOLLOW_RANGE, 20);
    }

    static KingSlimeMinion spawn(ServerLevel level, SourceKingSlime owner, Vec3 position) {
        return spawn(level, owner, position, null);
    }

    static KingSlimeMinion restore(ServerLevel level, SourceKingSlime owner, Saved saved, Vec3 position) {
        var block = BlockPos.containing(position);
        if (!level.isInWorldBounds(block) || !level.isPositionEntityTicking(block)) return null;
        return spawn(level, owner, position, saved);
    }

    private static KingSlimeMinion spawn(ServerLevel level, SourceKingSlime owner, Vec3 position, Saved saved) {
        KingSlimeMinion minion = KingSlimeContent.MINION.get().create(level);
        if (minion == null) return null;
        minion.owner = owner;
        minion.shotCooldown = 7 + minion.getRandom().nextInt(9);
        minion.setSize(2, true);
        if (saved != null) {
            minion.setUUID(saved.uuid());
            minion.setHealth(saved.health());
        }
        minion.xpReward = 0;
        minion.setPos(position);
        if (!level.getWorldBorder().isWithinBounds(minion.blockPosition())
                || !level.noCollision(minion) || level.containsAnyLiquid(minion.getBoundingBox())) return null;
        return level.addFreshEntity(minion) ? minion : null;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return owner != null && target instanceof ServerPlayer player && owner.canTarget(player) && super.canAttack(target);
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && (owner == null || !owner.isAlive() || owner.level() != level() || distanceToSqr(owner) > OWNER_RANGE * OWNER_RANGE)) {
            discard();
            return;
        }
        super.tick();
        if (level() instanceof ServerLevel server && isAlive() && !isNoAi()) tickRangedAttack(server);
    }

    private void tickRangedAttack(ServerLevel level) {
        if (!permitsSlimeBall()) {
            shotWarning = 0;
            shotTarget = null;
            return;
        }
        if (shotWarning > 0) {
            if (--shotWarning == 0) {
                fireSlimeBall(level);
                shotTarget = null;
                shotCooldown = SHOT_COOLDOWN;
            }
            return;
        }
        if (shotCooldown > 0) {
            --shotCooldown;
            return;
        }
        if (tickCount % 4 != 0) return;
        var target = getTarget();
        if (!(target instanceof ServerPlayer player) || !owner.canTarget(player)
                || distanceToSqr(player) < 9 || distanceToSqr(player) > 196
                || !BossAttackGeometry.visible(level, this, getEyePosition(), player.getBoundingBox().getCenter())) return;
        shotTarget = player.getBoundingBox().getCenter();
        shotWarning = SHOT_WARNING;
        level.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, .75f, 1.35f);
        level.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 6, .16, .13, .16, .04);
    }

    private void fireSlimeBall(ServerLevel level) {
        Vec3 muzzle = getEyePosition();
        if (!BossAttackGeometry.visible(level, this, muzzle, shotTarget)) return;
        Vec3 delta = shotTarget.subtract(muzzle);
        double flightTicks = Math.min(20, delta.length() / SHOT_SPEED);
        Vec3 aim = delta.add(0, .5 * KingSlimeJewelBolt.GRAVITY * flightTicks * flightTicks, 0).normalize();
        var ball = KingSlimeContent.JEWEL_BOLT.get().create(level);
        if (ball == null) return;
        ball.setItem(new ItemStack(Items.SLIME_BALL));
        ball.setOwner(this);
        ball.setPos(muzzle);
        ball.shoot(aim.x, aim.y, aim.z, SHOT_SPEED, 0);
        if (level.addFreshEntity(ball)) {
            level.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, .85f, 1.27f);
        }
    }

    boolean permitsSlimeBall() {
        return isAlive() && !isNoAi() && owner != null && owner.level() == level() && owner.permitsJewelFlight()
            && distanceToSqr(owner) <= OWNER_RANGE * OWNER_RANGE;
    }

    boolean mayHitWithSlimeBall(ServerPlayer player) {
        return permitsSlimeBall() && owner.canTarget(player);
    }

    boolean belongsTo(SourceKingSlime king) {
        return owner == king;
    }

    Saved saveFollower() {
        return new Saved(getUUID(), position(), getHealth());
    }

    record Saved(UUID uuid, Vec3 position, float health) {
        CompoundTag write() {
            var tag = new CompoundTag();
            tag.putUUID("UUID", uuid);
            tag.putDouble("X", position.x);
            tag.putDouble("Y", position.y);
            tag.putDouble("Z", position.z);
            tag.putFloat("Health", health);
            return tag;
        }

        static Saved read(CompoundTag tag) {
            if (!tag.hasUUID("UUID") || !tag.contains("X", Tag.TAG_DOUBLE) || !tag.contains("Y", Tag.TAG_DOUBLE)
                    || !tag.contains("Z", Tag.TAG_DOUBLE) || !tag.contains("Health", Tag.TAG_FLOAT)) return null;
            var at = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            float health = tag.getFloat("Health");
            if (!Double.isFinite(at.x) || !Double.isFinite(at.y) || !Double.isFinite(at.z)
                    || !Float.isFinite(health) || health <= 0) return null;
            return new Saved(tag.getUUID("UUID"), at, health);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        ((SourceMagmaNativeParent)(Object)this).mite$magmaParentRemove(reason);
    }
}
