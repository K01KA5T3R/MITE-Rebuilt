package dev.koikaster.mite;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class ShockStarItem extends Item {
    private static final double RADIUS = 5;
    private static final int COOLDOWN = 15 * 20;

    ShockStarItem() { super(new Properties().stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level instanceof ServerLevel server) {
            var center = player.getBoundingBox().getCenter();
            for (var target : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
                    actor -> canPush(server, player, actor, center))) {
                Vec3 inward = center.subtract(target.getBoundingBox().getCenter());
                if (inward.horizontalDistanceSqr() < .001) {
                    double yaw = Math.toRadians(player.getYRot());
                    inward = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
                }
                Vec3 before = target.getDeltaMovement();
                target.knockback(1.22, inward.x, inward.z);
                // The target client owns player movement. Broadcast an actual impulse to that player too.
                if (target instanceof ServerPlayer && !before.equals(target.getDeltaMovement())) {
                    target.hurtMarked = true;
                }
            }
            player.getCooldowns().addCooldown(this, COOLDOWN);
            server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.25f, .66f);
            BossAttackGeometry.ring(server, ParticleTypes.ELECTRIC_SPARK, player.position().add(0, .2, 0), RADIUS, 32);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static boolean canPush(ServerLevel level, Player player, LivingEntity target, Vec3 center) {
        if (target == player || !target.isAlive() || target.isSpectator() || BossCombatTuning.knockbackImmune(target)
                || target.getBoundingBox().getCenter().distanceToSqr(center) > RADIUS * RADIUS) return false;
        if (target instanceof Player opponent && (!level.getServer().isPvpAllowed() || !player.canHarmPlayer(opponent))) return false;
        return BossAttackGeometry.visible(level, player, center, target.getBoundingBox().getCenter());
    }
}
