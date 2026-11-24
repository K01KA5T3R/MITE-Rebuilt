package dev.koikaster.mite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class KingSlimeCombat {
    static final float MELEE_DAMAGE = 1.40f;
    private static final double MELEE_FREQUENCY = 1.30;

    enum Posture { INTRO, REST, WINDUP, AIRBORNE, WAVE, RECOVERY, RELOCATING, SLIDING, BURST }

    private enum Move {
        APPROACH(.57, .51, 7, 5),
        LOW(.62, .48, 8, 11),
        HIGH(.83, .98, 17, 8),
        SLIDE(.82, 0, 10, 8),
        SPRAY(0, 0, 12, 8),
        REBOUND(.68, .45, 10, 7),
        SHOVE(0, 0, 9, 10),
        REAR(0, 0, 12, 9),
        SWEEP(0, 0, 13, 8),
        CROSS(0, 0, 15, 10),
        MORTAR(0, 0, 17, 9),
        VAULT(.87, .77, 14, 10),
        PINCER(0, 0, 13, 10);

        final double speed;
        final double lift;
        final int warning;
        final int recovery;

        boolean melee() {
            return switch (this) {
                case LOW, HIGH, SLIDE, REBOUND, SHOVE, REAR, SWEEP, VAULT -> true;
                default -> false;
            };
        }

        Move(double speed, double lift, int warning, int recovery) {
            this.speed = speed;
            this.lift = lift;
            this.warning = warning;
            this.recovery = recovery;
        }
    }

    private final Set<UUID> hit = new HashSet<>();
    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private Posture posture = Posture.REST;
    private Move move;
    private int age;
    private int duration;
    private int failedApproaches;
    private long attackStartedAt;
    private int originalWarning;
    private int warningTicksSaved;
    private boolean secondHop;
    private Vec3 attackAxis = Vec3.ZERO;
    private Vec3 committedLanding = Vec3.ZERO;
    private Vec3 facing = Vec3.ZERO;
    private Vec3 launchSite = Vec3.ZERO;
    private Vec3 locked = Vec3.ZERO;
    private Vec3 landing = Vec3.ZERO;

    boolean active() {
        return (posture == Posture.WINDUP || posture == Posture.AIRBORNE) && move != Move.APPROACH
            || posture == Posture.WAVE || posture == Posture.RELOCATING
            || posture == Posture.SLIDING || posture == Posture.BURST;
    }

    boolean canChooseTarget() {
        return posture == Posture.REST;
    }

    boolean targetLostDuringWindup() {
        return posture == Posture.WINDUP;
    }

    boolean hasAttackWindup() {
        return posture == Posture.WINDUP && move != Move.APPROACH;
    }

    boolean permitsJewelShots() {
        return active() && posture != Posture.RELOCATING;
    }

    boolean missedSlamRecovery() {
        return posture == Posture.RECOVERY && move == Move.HIGH && hit.isEmpty();
    }

    void suspend(ServerLevel level, SourceKingSlime boss) {
        if (posture != Posture.REST) enter(boss, Posture.REST, 0);
        failedApproaches = 0;
        BossArenaPortals.release(level, boss.getUUID());
    }

    void tick(ServerLevel level, SourceKingSlime boss, ServerPlayer target, List<ServerPlayer> players) {
        ++age;
        switch (posture) {
            case REST -> select(level, boss, target);
            case WINDUP -> {
                if (move == Move.REAR) face(boss, attackAxis.scale(-1));
                if (age >= duration) launch(level, boss, target);
            }
            case AIRBORNE -> fly(level, boss, players);
            case WAVE -> wave(level, boss, players);
            case SLIDING -> slide(level, boss, players);
            case BURST -> burst(level, boss, players);
            case RECOVERY -> {
                if (age >= duration) enter(boss, Posture.REST, 0);
            }
            case RELOCATING -> {
                if (age >= duration) relocate(level, boss, players);
            }
            case INTRO -> throw new IllegalStateException("Intro is driven by the encounter");
        }
    }

    private void select(ServerLevel level, SourceKingSlime boss, ServerPlayer target) {
        if (!boss.onGround()) return;
        if (failedApproaches >= 4 && relocationSite(level, boss, target)) {
            enter(boss, Posture.RELOCATING, 20);
            level.sendParticles(ParticleTypes.PORTAL, locked.x, locked.y + .25, locked.z, 28, .8, .5, .8, .13);
            return;
        }
        Vec3 difference = target.position().subtract(boss.position());
        double distance = difference.horizontalDistance();
        boolean clear = BossAttackGeometry.visible(level, boss, boss.getEyePosition(), target.getEyePosition());
        move = Move.APPROACH;
        float hp = boss.healthRatio();
        attackAxis = BossAttackGeometry.flatDirection(difference);
        // Native slime movement can turn the body before this combat tick.
        // Backlash uses the last direction visibly committed by the encounter.
        Vec3 forward = facing.equals(Vec3.ZERO) ? Vec3.directionFromRotation(0, boss.getYRot()) : facing;
        double facingDot = forward.dot(attackAxis);
        Vec3 body = target.getBoundingBox().getCenter();
        if (Math.abs(difference.y) > 2.6 && difference.lengthSqr() <= 13 * 13 && sprayExit(level, boss, body) != null) {
            move = Move.SPRAY;
            order.used(move);
        } else if (clear && Math.abs(difference.y) <= 2.6) {
            for (var candidate : order.candidates(boss.getRandom())) {
                if (!eligible(candidate, distance, hp, facingDot)) continue;
                move = candidate;
                order.used(candidate);
                break;
            }
        }
        secondHop = false;
        committedLanding = target.position();
        int shortened = hp < .34f ? 3 : 0;
        boss.windupSquish(switch (move) {
            case HIGH, VAULT -> -.55f;
            case SLIDE, SHOVE -> -.62f;
            case SPRAY, MORTAR -> .17f;
            case REAR, SWEEP, CROSS, PINCER -> -.38f;
            default -> -.26f;
        });
        attackStartedAt = level.getGameTime();
        originalWarning = move.warning - shortened;
        int warning = move.melee() ? fasterMeleeTicks(originalWarning) : originalWarning;
        warningTicksSaved = originalWarning - warning;
        enter(boss, Posture.WINDUP, warning);
        boss.setDeltaMovement(boss.getDeltaMovement().multiply(.1, 1, .1));
        if (move == Move.REAR) {
            attackAxis = forward.scale(-1);
            face(boss, forward);
        } else {
            turn(boss, difference);
        }
        warnGroundMove(level, boss);
    }

    private static boolean eligible(Move candidate, double distance, float health, double facingDot) {
        return switch (candidate) {
            case APPROACH -> false;
            case LOW -> distance <= 5.4;
            case HIGH -> distance >= 4.3 && distance <= 10.5;
            case SLIDE -> distance >= 3.3 && distance <= 8.2;
            case SPRAY -> health <= .67f && distance >= 4.6 && distance <= 11.5;
            case REBOUND -> health <= .67f && distance >= 2.8 && distance <= 7.3;
            case SHOVE -> health <= .34f && distance <= 3.4;
            case REAR -> health <= .67f && distance <= 4.6 && facingDot < -.45;
            case SWEEP -> distance >= 2.0 && distance <= 5.6;
            case CROSS -> health <= .34f && distance >= 3.6 && distance <= 10.4;
            case MORTAR -> health <= .34f && distance >= 4.5 && distance <= 10.8;
            case VAULT -> health <= .34f && distance >= 4.0 && distance <= 8.7;
            case PINCER -> health <= .67f && distance >= 3.3 && distance <= 8.6;
        };
    }

    private void warnGroundMove(ServerLevel level, SourceKingSlime boss) {
        if (move == Move.REAR || move == Move.SWEEP || move == Move.CROSS || move == Move.MORTAR || move == Move.PINCER) {
            Vec3 at = boss.position().add(attackAxis.scale(move == Move.REAR ? 1.2 : .4))
                .add(0, move == Move.MORTAR ? boss.getBbHeight() + .2 : .2, 0);
            level.sendParticles(move == Move.MORTAR ? ParticleTypes.END_ROD : ParticleTypes.ITEM_SLIME,
                at.x, at.y, at.z, 13, .32, .17, .32, .09);
            level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE,
                1.4f, move == Move.MORTAR ? 1.44f : .66f);
        } else if (move == Move.SLIDE) {
            level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5f, .63f);
        } else if (move == Move.SPRAY || move == Move.SHOVE) {
            Vec3 at = boss.position().add(0, boss.getBbHeight() * (move == Move.SPRAY ? .77 : .15), 0);
            level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y, at.z, 14, .43, .21, .43, .08);
            level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.4f,
                move == Move.SPRAY ? 1.35f : .72f);
        }
    }

    private void launch(ServerLevel level, SourceKingSlime boss, ServerPlayer target) {
        launchSite = boss.position();
        Vec3 destination = switch (move) {
            case HIGH, SLIDE, VAULT -> committedLanding;
            default -> target.position();
        };
        Vec3 delta = destination.subtract(launchSite);
        double travel = move == Move.VAULT ? Math.min(delta.horizontalDistance() + .85, 10)
            : Math.min(delta.horizontalDistance(), move == Move.HIGH ? 9.2 : 5.0);
        if (move != Move.REAR) {
            turn(boss, delta);
            attackAxis = facing;
        }
        locked = launchSite.add(attackAxis.scale(travel));
        hit.clear();
        if (move == Move.SLIDE) {
            Vec3 step = attackAxis.scale(move.speed);
            boss.setDeltaMovement(step.x, Math.min(-.08, boss.getDeltaMovement().y), step.z);
            boss.hasImpulse = true;
            enter(boss, Posture.SLIDING, 12);
            return;
        }
        if (move.lift == 0) {
            int active = switch (move) {
                case SPRAY -> 17;
                case MORTAR -> 26;
                case CROSS, PINCER -> 13;
                default -> 9;
            };
            enter(boss, Posture.BURST, active);
            if (move == Move.SPRAY) spray(level, boss, target);
            if (move == Move.MORTAR) mortar(level, boss, target);
            return;
        }
        Vec3 impulse = BossAttackGeometry.flatDirection(delta).scale(move.speed * .62);
        boss.setDeltaMovement(impulse.x, move.lift, impulse.z);
        boss.hasImpulse = true;
        level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.4f,
            move == Move.HIGH ? .62f : .90f);
        enter(boss, Posture.AIRBORNE, 0);
    }

    private void fly(ServerLevel level, SourceKingSlime boss, List<ServerPlayer> players) {
        boolean landed = boss.onGround() && age > 2;
        if (landed && move != Move.VAULT) {
            land(level, boss);
            return;
        }
        Vec3 motion = boss.getDeltaMovement();
        boolean vaultStrike = move == Move.VAULT && (landed || motion.y < -.20);
        Vec3 destination = move == Move.VAULT && !vaultStrike ? locked.subtract(attackAxis.scale(3.8)) : locked;
        Vec3 offset = destination.subtract(boss.position());
        double speed = Math.min(move.speed * (1.12 - .12 * boss.healthRatio()), offset.horizontalDistance() * .45);
        Vec3 steer = BossAttackGeometry.flatDirection(offset).scale(speed);
        boss.setDeltaMovement(steer.x, motion.y, steer.z);
        if (move == Move.LOW || move == Move.REBOUND || vaultStrike) {
            var body = boss.getBoundingBox().inflate(.18, .16, .18);
            for (var player : players) {
                if (body.intersects(player.getBoundingBox())) {
                    strike(level, boss, player, boss.getEyePosition(), (vaultStrike ? 8.0f : 5.5f) * MELEE_DAMAGE, vaultStrike ? .57 : .40);
                }
            }
        }
        if (landed) land(level, boss);
    }

    private void land(ServerLevel level, SourceKingSlime boss) {
        Vec3 at = boss.position();
        boss.setDeltaMovement(Vec3.ZERO);
        if (move == Move.APPROACH) {
            if (locked.subtract(launchSite).horizontalDistance() - locked.subtract(at).horizontalDistance() < 1.15) ++failedApproaches;
            else failedApproaches = 0;
        } else {
            failedApproaches = 0;
        }
        if (move == Move.REBOUND && !secondHop) {
            secondHop = true;
            int warning = fasterMeleeTicks(6);
            warningTicksSaved += 6 - warning;
            enter(boss, Posture.WINDUP, warning);
            level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.15f, 1.33f);
            return;
        }
        if (move != Move.HIGH) {
            recover(boss, move == Move.VAULT && hit.isEmpty() ? 22 : move.recovery);
            return;
        }
        landing = at;
        var floor = level.getBlockState(boss.blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
            at.x, at.y + .18, at.z, 34, 1.18, .12, 1.18, .17);
        level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + .3, at.z, 26, 1.4, .2, 1.4, .19);
        level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 2, .65f);
        enter(boss, Posture.WAVE, 8);
    }

    private void slide(ServerLevel level, SourceKingSlime boss, List<ServerPlayer> players) {
        Vec3 step = attackAxis.scale(move.speed);
        Vec3 next = boss.position().add(step);
        Vec3 support = BossAttackGeometry.ground(level, boss, next.add(0, .35, 0), .80);
        if (age >= duration || boss.horizontalCollision || support == null) {
            boss.setDeltaMovement(0, Math.min(0, boss.getDeltaMovement().y), 0);
            recover(boss, move.recovery);
            return;
        }
        boss.setDeltaMovement(step.x, Math.min(-.08, boss.getDeltaMovement().y), step.z);
        var box = boss.getBoundingBox();
        var slideBox = new AABB(box.minX, box.minY, box.minZ, box.maxX, box.minY + .74, box.maxZ)
            .expandTowards(step.x * .7, 0, step.z * .7);
        for (var player : players) {
            if (slideBox.intersects(player.getBoundingBox())) {
                strike(level, boss, player, boss.position().add(0, .4, 0), 6.5f * MELEE_DAMAGE, .57);
            }
        }
        if (age % 2 == 0) {
            level.sendParticles(ParticleTypes.ITEM_SLIME, boss.getX(), boss.getY() + .18, boss.getZ(), 7, .43, .08, .43, .09);
        }
    }

    private void spray(ServerLevel level, SourceKingSlime boss, ServerPlayer target) {
        Vec3 body = target.getBoundingBox().getCenter();
        Vec3 exit = sprayExit(level, boss, body);
        if (exit == null) return;
        if (body.subtract(boss.position()).horizontalDistanceSqr() > .001) turn(boss, body.subtract(boss.position()));
        double flight = exit.distanceTo(body) / .90;
        Vec3 aim = body.subtract(exit).add(0, .5 * KingSlimeJewelBolt.GRAVITY * flight * flight, 0).normalize();
        for (int i = -2; i <= 2; ++i) {
            var glob = KingSlimeContent.JEWEL_BOLT.get().create(level);
            if (glob == null) continue;
            Vec3 ray = BossAttackGeometry.spread(aim, .12 * i);
            glob.setItem(new ItemStack(Items.SLIME_BALL));
            glob.setOwner(boss);
            glob.setPos(exit);
            glob.shoot(ray.x, ray.y, ray.z, .90f, 0);
            level.addFreshEntity(glob);
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5f, 1.2f);
    }

    private static Vec3 sprayExit(ServerLevel level, SourceKingSlime boss, Vec3 body) {
        Vec3 from = boss.position().add(0, boss.getBbHeight() * .68, 0);
        Vec3 direction = body.subtract(from);
        if (direction.lengthSqr() < .25) return null;
        Vec3 exit = from.add(direction.normalize().scale(Math.min(boss.getBbWidth() * .55, direction.length() * .40)));
        return BossAttackGeometry.visible(level, boss, from, exit)
            && BossAttackGeometry.visible(level, boss, exit, body) ? exit : null;
    }

    private void mortar(ServerLevel level, SourceKingSlime boss, ServerPlayer target) {
        Vec3 from = boss.position().add(0, boss.getBbHeight() + .15, 0);
        Vec3 ahead = BossAttackGeometry.flatDirection(target.position().subtract(from));
        Vec3 lateral = ahead.yRot((float)Math.PI / 2);
        for (int i = -1; i <= 1; ++i) {
            Vec3 proposed = target.position().add(lateral.scale(i * 2.6)).add(ahead.scale(Math.abs(i) * 1.3));
            Vec3 floor = BossAttackGeometry.ground(level, boss, proposed.add(0, 1.7, 0), 4.6);
            if (floor == null) continue;
            var glob = KingSlimeContent.JEWEL_BOLT.get().create(level);
            if (glob == null) continue;
            double ticks = 22;
            Vec3 velocity = floor.add(0, .18, 0).subtract(from).scale(1 / ticks)
                .add(0, KingSlimeJewelBolt.LOB_GRAVITY * ticks / 2, 0);
            glob.setItem(new ItemStack(Items.SLIME_BALL));
            glob.markLobbed();
            glob.setOwner(boss);
            glob.setPos(from);
            glob.shoot(velocity.x, velocity.y, velocity.z, (float)velocity.length(), 0);
            level.addFreshEntity(glob);
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.6f, .68f);
    }

    private void burst(ServerLevel level, SourceKingSlime boss, List<ServerPlayer> players) {
        if (move == Move.REAR && age <= 3) {
            Vec3 origin = boss.position().add(0, .42, 0);
            for (var player : players) {
                Vec3 offset = player.position().subtract(boss.position());
                if (Math.abs(offset.y) > 1.0 || offset.horizontalDistanceSqr() > 4.6 * 4.6
                        || BossAttackGeometry.flatDirection(offset).dot(attackAxis) < .63) continue;
                strike(level, boss, player, origin, 6.3f * MELEE_DAMAGE, .62);
            }
            if (age == 1) {
                Vec3 cue = origin.add(attackAxis.scale(2.4));
                level.sendParticles(ParticleTypes.ITEM_SLIME, cue.x, cue.y, cue.z, 32, 1.02, .26, 1.02, .10);
            }
        }
        if (move == Move.SWEEP && age <= 8) {
            double turn = Mth.lerp((age - 1) / 7d, -.97, .97);
            Vec3 sweep = attackAxis.yRot((float)turn);
            waveLine(level, boss, players, boss.position(), sweep, .7, 5.3, 1.0, 7.0f * MELEE_DAMAGE);
        }
        if (move == Move.CROSS || move == Move.PINCER) {
            double front = .9 + age * (move == Move.CROSS ? .66 : .78);
            if (move == Move.CROSS) {
                for (int i = 0; i < 4; ++i) {
                    waveLine(level, boss, players, boss.position(), attackAxis.yRot((float)(Math.PI / 2 * i)),
                        front - 1.18, front, .74, 7.5f);
                }
            } else {
                Vec3 side = attackAxis.yRot((float)Math.PI / 2).scale(2.35);
                for (int sign : new int[]{-1, 1}) {
                    waveLine(level, boss, players, boss.position().add(side.scale(sign)), attackAxis,
                        front - 1.22, front, .78, 6.8f);
                }
            }
        }
        if (move == Move.SHOVE && age == 1) {
            Vec3 center = boss.position();
            for (var player : players) {
                Vec3 offset = player.position().subtract(center);
                if (Math.abs(offset.y) <= .85 && offset.horizontalDistanceSqr() <= 3.6 * 3.6) {
                    strike(level, boss, player, center.add(0, .35, 0), 5.5f * MELEE_DAMAGE, .80);
                }
            }
            level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y + .24, center.z, 38, 1.25, .22, 1.25, .16);
            level.playSound(null, boss.blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 1.65f, .83f);
        }
        if (age >= duration) recover(boss, move.recovery);
    }

    private void waveLine(ServerLevel level, SourceKingSlime boss, List<ServerPlayer> players,
                          Vec3 groundOrigin, Vec3 axis, double previous, double front, double width, float damage) {
        Vec3 start = groundOrigin.add(0, .43, 0);
        Vec3 planned = start.add(axis.scale(front));
        Vec3 end = BossAttackGeometry.shorten(level, boss, start, planned);
        if (end.subtract(start).dot(axis) <= previous) return;
        Vec3 floor = BossAttackGeometry.ground(level, boss, end.add(0, .22, 0), 1.4);
        if (floor == null) return;
        Vec3 near = start.add(axis.scale(previous));
        for (var player : players) {
            if (Math.abs(player.getY() - floor.y) > .83) continue;
            if (BossAttackGeometry.hits(player.getBoundingBox(), near, end, width)) {
                strike(level, boss, player, start, damage, .60);
            }
        }
        if (age % 2 == 0) {
            level.sendParticles(ParticleTypes.ITEM_SLIME, end.x, end.y, end.z, 7, width * .45, .10, width * .45, .10);
        }
    }

    private void wave(ServerLevel level, SourceKingSlime boss, List<ServerPlayer> players) {
        double outer = 2.3 + age * .53;
        double inner = age == 1 ? 0 : outer - .93;
        for (var player : players) {
            Vec3 offset = player.position().subtract(landing);
            double distance = offset.horizontalDistance();
            if (offset.y < -.65 || offset.y > (age == 1 ? 1.55 : .77) || distance < inner || distance > outer + .25) continue;
            strike(level, boss, player, landing.add(0, .45, 0), age == 1 ? 9.0f * MELEE_DAMAGE : 7.0f, .68);
        }
        if (age % 2 == 1) {
            Vec3 center = landing.add(0, .18, 0);
            for (int i = 0; i < 16; ++i) {
                Vec3 mark = center.add(BossAttackGeometry.flatDirection(i * Math.PI / 8).scale(outer));
                if (BossAttackGeometry.visible(level, boss, center, mark)) {
                    level.sendParticles(ParticleTypes.ITEM_SLIME, mark.x, mark.y, mark.z, 2, .08, .05, .08, .07);
                }
            }
        }
        if (age >= duration) recover(boss, hit.isEmpty() ? 29 : move.recovery);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void earlyGearDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getSource().getEntity() instanceof SourceKingSlime) {
            // Bronze survival feedback showed the old quarter-damage rule was too forgiving.
            event.setAmount(event.getAmount() * .40f);
        }
    }

    private void strike(ServerLevel level, SourceKingSlime boss, ServerPlayer player, Vec3 origin, float damage, double force) {
        if (hit.contains(player.getUUID())
                || !BossAttackGeometry.visible(level, boss, origin, player.getBoundingBox().getCenter())
                || !player.hurt(level.damageSources().mobAttack(boss), damage)) return;
        hit.add(player.getUUID());
        Vec3 away = BossAttackGeometry.flatDirection(origin.subtract(player.position()));
        player.knockback(force, away.x, away.z);
    }

    private boolean relocationSite(ServerLevel level, SourceKingSlime boss, ServerPlayer target) {
        for (int i = 0; i < 8; ++i) {
            Vec3 candidate = target.position().add(BossAttackGeometry.flatDirection((i + boss.getId() % 8) * Math.PI / 4).scale(7));
            Vec3 floor = BossAttackGeometry.ground(level, boss, candidate.add(0, 1.4, 0), 3.5);
            if (floor == null || floor.distanceToSqr(boss.position()) < 16) continue;
            var bounds = boss.getBoundingBox().move(floor.subtract(boss.position()));
            if (level.getWorldBorder().isWithinBounds(BlockPos.containing(floor))
                    && level.noCollision(boss, bounds) && !level.containsAnyLiquid(bounds)) {
                locked = floor;
                return true;
            }
        }
        return false;
    }

    private void relocate(ServerLevel level, SourceKingSlime boss, List<ServerPlayer> players) {
        var bounds = boss.getBoundingBox().move(locked.subtract(boss.position()));
        if (level.hasChunkAt(BlockPos.containing(locked)) && level.noCollision(boss, bounds)
                && !level.containsAnyLiquid(bounds) && players.stream().noneMatch(player -> player.position().distanceToSqr(locked) < 16)) {
            boss.teleportTo(locked.x, locked.y, locked.z);
            boss.setDeltaMovement(Vec3.ZERO);
            level.sendParticles(ParticleTypes.ITEM_SLIME, locked.x, locked.y + .4, locked.z, 32, .75, .4, .75, .1);
            failedApproaches = 0;
        }
        enter(boss, Posture.RECOVERY, 16);
    }

    private static int fasterMeleeTicks(int ticks) {
        return Math.max(2, (int)Math.round(ticks / MELEE_FREQUENCY));
    }

    private void recover(SourceKingSlime boss, int originalRecovery) {
        if (!move.melee()) {
            enter(boss, Posture.RECOVERY, originalRecovery);
            return;
        }
        int elapsed = (int)(boss.level().getGameTime() - attackStartedAt);
        // Natural movement keeps its original time. Recover the remaining part of the faster cadence.
        int originalCycle = Math.max(originalWarning, elapsed + warningTicksSaved) + originalRecovery + 1;
        int shortened = fasterMeleeTicks(originalCycle) - elapsed - 1;
        enter(boss, Posture.RECOVERY, Mth.clamp(shortened, 2, originalRecovery));
    }
    private void enter(SourceKingSlime boss, Posture next, int ticks) {
        posture = next;
        boss.posture(next);
        age = 0;
        duration = ticks;
    }

    private void turn(SourceKingSlime boss, Vec3 direction) {
        facing = BossAttackGeometry.flatDirection(direction);
        face(boss, direction);
    }

    private static void face(SourceKingSlime boss, Vec3 delta) {
        if (delta.horizontalDistanceSqr() < 1.0e-6) return;
        float yaw = (float)(Mth.atan2(-delta.x, delta.z) * 180 / Math.PI);
        boss.setYRot(yaw);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;
    }
}
