package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class SourceRedstoneGolem extends IronGolem {
    private static final double RADIUS = 32;
    private static final double HEIGHT = 14;
    private static final byte CLAMP_POSE = 76;
    private static final byte PRESS_POSE = 77;
    private static final byte CLEAR_POSE = 78;
    static final byte CURRENT_POSE = 79;
    static final byte LIFT_POSE = 80;
    static final byte NODES_POSE = 81;
    static final byte ADVANCE_POSE = 82;
    static final byte LOB_POSE = 83;
    static final byte RETURN_POSE = 84;
    static final byte CROSSBARS_POSE = 85;
    private static final EntityDataAccessor<Long> ATTACK_POSE =
        SynchedEntityData.defineId(SourceRedstoneGolem.class, EntityDataSerializers.LONG);
    private static final double MAX_HEALTH = 300;
    private static final int COUNTER_WINDOW = 6;
    private static final int MELEE_MEMORY = 32;
    private static final double MELEE_DISTANCE = 4.25;
    private static final int LOCK_LEAD = 5;
    private static final int CONCEALED_PATH_INTERVAL = 8;
    private static final int RETREAT_GRACE = 160;
    private static final BossEncounterPresence.RetreatKeys ABSENCE = new BossEncounterPresence.RetreatKeys(
        "MiteRedstoneAbsentTicks", "MiteRedstoneAbsentSince");
    private enum Move {
        CANNON(18, 12, 20), SWEEP(10, 12, 15), PULSE(12, 12, 18), CLAMP(12, 5, 8), PRESS(16, 4, 8),
        CURRENT(13, 23, 9), LIFT(13, 12, 13), NODES(15, 15, 10), ADVANCE(11, 7, 10), LOB(13, 32, 12),
        RETURN(11, 24, 9), CROSSBARS(12, 13, 11);

        final int warning;
        final int active;
        final int recovery;

        Move(int warning, int active, int recovery) {
            this.warning = warning;
            this.active = active;
            this.recovery = recovery;
        }
    }

    private final BossAttackOrder<Move> order = new BossAttackOrder<>(Move.class);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.mite_reforged.redstone_iron_golem"),
        BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.REDSTONE_GOLEM);
    private final Set<UUID> contributors = new HashSet<>();
    private final Set<UUID> hit = new HashSet<>();
    private final Set<UUID> introViewers = new HashSet<>();
    private final BossEncounterPresence presence = new BossEncounterPresence(2);
    private long introStarted = -1;
    private Move move;
    private Move previousMove;
    private Vec3 aim;
    private Vec3 axis;
    private Vec3 recoveryStep;
    private UUID recoveryFocus;
    private RedstoneGolemPatterns.GroundCurrent current;
    private RedstoneGolemCharge charge;
    private RedstoneGolemReturn returnCharge;
    private RedstoneGolemCrossbars crossbars;
    private List<Vec3> nodes = List.of();
    private UUID target;
    private UUID meleeAttacker;
    private long meleeHitAt;
    private int age;
    private int concealedPathCooldown;
    private boolean paid;

    SourceRedstoneGolem(EntityType<? extends IronGolem> type,Level level) { super(type,level); }

    static AttributeSupplier.Builder attributes() {
        return IronGolem.createAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.ARMOR,8)
            .add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.FOLLOW_RANGE,RADIUS);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK_POSE, (long)CLEAR_POSE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive() || isNoAi()) return;
        if (concealedPathCooldown > 0) concealedPathCooldown--;
        BossCombatTuning.applyHealth(this, MAX_HEALTH, false);
        long now = level.getGameTime();
        var retreat = presence.retreat();
        if (retreat.expired(level, RETREAT_GRACE)) {
            discard();
            return;
        }
        presence.hold(level, this);
        if (introStarted < 0) introStarted = now;
        var fighters=level.getPlayers(this::isFighter);
        retreat.observe(!fighters.isEmpty());
        if (fighters.isEmpty()) {
            getNavigation().stop();
            release(level);
            resetAttack(level);
            return;
        }
        if (tickCount % 10 == 0) updateAudience(level,fighters);
        if (now - introStarted < 28) {
            fighters.stream().min(Comparator.comparingDouble(this::distanceToSqr))
                .ifPresent(player -> face(player.getBoundingBox().getCenter()));
            return;
        }
        bar.setProgress(getHealth()/getMaxHealth());
        if (move==null) {
            choose(level,fighters);
            return;
        }
        if (++age >= move.warning + move.active + move.recovery) {
            resetAttack(level);
            choose(level, fighters);
            return;
        }
        var victim = fighters.stream().filter(player -> player.getUUID().equals(target)).findFirst().orElse(null);
        if (victim == null) {
            resetAttack(level);
            return;
        }
        int recoveryAge = age - move.warning - move.active;
        if (recoveryAge >= 0) {
            brake();
            if (recoveryAge == 0) BossArenaPortals.release(level, getUUID());
            if (recoveryAge >= COUNTER_WINDOW) recover(level, victim, recoveryAge - COUNTER_WINDOW);
            return;
        }
        brake();
        BossArenaPortals.holdFollowing(level, this, RADIUS, HEIGHT);
        if (axis == null && age >= move.warning - LOCK_LEAD) {
            if (move == Move.LIFT && !RedstoneGolemPatterns.canLiftBeam(level, this, victim)) {
                resetAttack(level);
                return;
            }
            aim = victim.getBoundingBox().getCenter();
            axis = aim.subtract(mouth()).normalize();
        }
        if (age >= move.warning) {
            strike(level, fighters, age - move.warning);
            return;
        }
        faceAim(victim);
        if (age % 3 == 0) chargeParticles(level);
    }

    private void recover(ServerLevel level, ServerPlayer victim, int tick) {
        if (tick >= BossRecoveryStep.DURATION) return;
        if (recoveryStep == null) {
            ServerPlayer pressure = recentMeleeTarget(level);
            ServerPlayer focus = pressure == null ? victim : pressure;
            recoveryFocus = focus.getUUID();
            int side = (move.ordinal() & 1) == 0 ? 1 : -1;
            double bias = pressure != null && distanceToSqr(pressure) < 3.2 * 3.2 ? -.15 : .26;
            recoveryStep = BossRecoveryStep.plan(level, this, focus, side, pressure == null ? .20 : .26, bias, 7);
        }
        if (recoveryStep.lengthSqr() == 0) return;
        if (!(level.getPlayerByUUID(recoveryFocus) instanceof ServerPlayer focus) || !isFighter(focus)
                || !hasLineOfSight(focus) || !BossRecoveryStep.move(level, this, recoveryStep)) {
            recoveryStep = Vec3.ZERO;
            return;
        }
        entityData.set(ATTACK_POSE, (long)CLEAR_POSE);
        Vec3 look = focus.getBoundingBox().getCenter().subtract(getEyePosition());
        float yaw = (float)Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float)-Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
        float facing = Mth.approachDegrees(getYRot(), yaw, 22);
        setYRot(facing);
        setYHeadRot(facing);
        yBodyRot = facing;
        setXRot(Mth.approachDegrees(getXRot(), pitch, 18));
    }

    private void updateAudience(ServerLevel level,List<ServerPlayer> fighters) {
        music.sync(level,getUUID(),fighters);
        for(var player:List.copyOf(bar.getPlayers())) if(!fighters.contains(player))bar.removePlayer(player);
        for(var player:fighters) {
            bar.addPlayer(player);
            if (level.getGameTime() - introStarted < 45 && introViewers.add(player.getUUID())) {
                GroundSeaBossIntroPacket.send(player, getUUID(), introStarted, GroundSeaBossIntroPacket.Kind.REDSTONE_GOLEM);
            }
        }
    }

    private void choose(ServerLevel level,List<ServerPlayer> fighters) {
        var eye=getEyePosition();
        ServerPlayer pressure = recentMeleeTarget(level);
        var candidate = pressure != null ? pressure : fighters.stream()
            .filter(player -> BossAttackGeometry.visible(level, this, eye, player.getBoundingBox().getCenter()))
            .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if(candidate==null) {
            var concealed = fighters.stream().sorted(Comparator.comparingDouble(this::distanceToSqr)).toList();
            Vec3 origin = mouth();
            for (var player : concealed.subList(0, Math.min(3, concealed.size()))) {
                charge = RedstoneGolemCharge.target(level, this, player, origin);
                if (charge == null) continue;
                axis = charge.aim(origin);
                begin(level, player, Move.LOB);
                return;
            }
            pursueConcealed(concealed);
            return;
        }
        double distance=distanceTo(candidate);
        if(distance>18) {
            getNavigation().moveTo(candidate,.95);
            return;
        }
        double height = Math.abs(candidate.getY() - getY());
        List<Move> options = order.candidates(getRandom());
        if (pressure != null) {
            options = Stream.concat(options.stream().filter(this::preferMelee),
                options.stream().filter(option -> !preferMelee(option))).toList();
        }
        for (var option : options) {
            if (option == Move.PULSE && (distance > 7 || height > 1.6)) continue;
            if (option == Move.LOB || (option == Move.CANNON && distance < 3.5)) continue;
            if (option == Move.CLAMP) {
                axis = clampAxis(level, candidate);
                if (axis == null) continue;
            } else if (option == Move.PRESS) {
                aim = pressTarget(level, candidate);
                if (aim == null) continue;
                axis = aim.add(0, .5, 0).subtract(mouth()).normalize();
            } else if (option == Move.ADVANCE) {
                axis = RedstoneGolemAdvance.direction(level, this, candidate);
                if (axis == null) continue;
            } else if (option == Move.CURRENT) {
                current = RedstoneGolemPatterns.groundCurrent(level, this, candidate);
                if (current == null) continue;
                axis = current.axis;
            } else if (option == Move.LIFT) {
                if (!RedstoneGolemPatterns.canLiftBeam(level, this, candidate)) continue;
            } else if (option == Move.NODES) {
                nodes = RedstoneGolemPatterns.dischargeNodes(level, this, candidate, mouth());
                if (nodes.isEmpty()) continue;
                axis = nodes.get(0).add(0, .6, 0).subtract(mouth()).normalize();
            } else if (option == Move.RETURN) {
                returnCharge = RedstoneGolemReturn.plan(level, this, candidate, mouth());
                if (returnCharge == null) continue;
                axis = returnCharge.direction().normalize();
            } else if (option == Move.CROSSBARS) {
                crossbars = RedstoneGolemCrossbars.plan(level, this, candidate, fighters, mouth());
                if (crossbars == null) continue;
                axis = crossbars.lookAt(false).subtract(mouth()).normalize();
            }
            begin(level, candidate, option);
            if (pressure != null) meleeAttacker = null;
            return;
        }
    }

    private void pursueConcealed(List<ServerPlayer> fighters) {
        var navigation = getNavigation();
        if (fighters.size() == 1) {
            navigation.moveTo(fighters.get(0), .95);
            return;
        }
        if (concealedPathCooldown > 0) return;
        concealedPathCooldown = CONCEALED_PATH_INTERVAL;

        var previous = navigation.getPath();
        // The native search can reuse its current path when its old target is still in the new set.
        // Discard an incomplete path so that it cannot hide a reachable teammate.
        if (previous != null && !previous.canReach()) navigation.stop();
        var path = navigation.createPath(fighters.stream().map(ServerPlayer::blockPosition), 1);
        if (path == null) navigation.stop();
        else if (path != navigation.getPath()) navigation.moveTo(path, .95);
    }

    private boolean preferMelee(Move action) {
        return action != previousMove && switch (action) {
            case CLAMP, PRESS, PULSE, ADVANCE -> true;
            default -> false;
        };
    }

    private ServerPlayer recentMeleeTarget(ServerLevel level) {
        if (meleeAttacker == null || level.getGameTime() - meleeHitAt > MELEE_MEMORY) return null;
        if (level.getPlayerByUUID(meleeAttacker) instanceof ServerPlayer player && isFighter(player)
                && inMeleeRange(player)
                && BossAttackGeometry.visible(level, this, getEyePosition(), player.getBoundingBox().getCenter())) return player;
        return null;
    }

    private boolean inMeleeRange(ServerPlayer player) {
        Vec3 delta = player.position().subtract(position());
        return delta.horizontalDistanceSqr() <= MELEE_DISTANCE * MELEE_DISTANCE && Math.abs(delta.y) <= 1.2;
    }

    private void begin(ServerLevel level, ServerPlayer candidate, Move action) {
        move = action;
        previousMove = action;
        order.used(action);
        target = candidate.getUUID();
        age = 0;
        hit.clear();
        getNavigation().stop();
        brake();
        faceAim(candidate);
        BossArenaPortals.holdFollowing(level, this, RADIUS, HEIGHT);
        byte pose = customPose(move);
        if (pose != CLEAR_POSE) {
            // One value carries both fields so late tracking receives an atomic snapshot.
            entityData.set(ATTACK_POSE, level.getGameTime() << Byte.SIZE | Byte.toUnsignedLong(pose));
            chargeParticles(level);
        }
        level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 1.4f, .72f);
    }

    private void faceAim(ServerPlayer victim) {
        if (axis == null) face(victim.getBoundingBox().getCenter());
        else faceDirection(move == Move.SWEEP ? sweepDirection(0) : axis);
    }

    private void resetAttack(ServerLevel level) {
        if (move != null && customPose(move) != CLEAR_POSE) entityData.set(ATTACK_POSE, (long)CLEAR_POSE);
        move = null;
        aim = null;
        axis = null;
        recoveryStep = null;
        recoveryFocus = null;
        current = null;
        charge = null;
        returnCharge = null;
        crossbars = null;
        nodes = List.of();
        target = null;
        hit.clear();
        BossArenaPortals.release(level, getUUID());
    }

    private static byte customPose(Move action) {
        return switch (action) {
            case CLAMP -> CLAMP_POSE;
            case PRESS -> PRESS_POSE;
            case CURRENT -> CURRENT_POSE;
            case LIFT -> LIFT_POSE;
            case NODES -> NODES_POSE;
            case ADVANCE -> ADVANCE_POSE;
            case LOB -> LOB_POSE;
            case RETURN -> RETURN_POSE;
            case CROSSBARS -> CROSSBARS_POSE;
            default -> CLEAR_POSE;
        };
    }

    private void brake() {
        var movement = getDeltaMovement();
        if (movement.horizontalDistanceSqr() > .0001) setDeltaMovement(0, movement.y, 0);
    }

    private Vec3 mouth() {
        return getEyePosition().add(0, -.35, 0);
    }

    private Vec3 sweepDirection(int tick) {
        return BossAttackGeometry.spread(axis, (tick - 5) * .075);
    }

    private void face(Vec3 at) {
        faceDirection(at.subtract(getEyePosition()));
    }

    private void faceDirection(Vec3 toward) {
        if(toward.horizontalDistanceSqr()>=.001) {
            float yaw=(float)Math.toDegrees(Math.atan2(-toward.x,toward.z));
            setYRot(yaw);
            setYHeadRot(yaw);
            yBodyRot=yaw;
        }
        setXRot((float)-Math.toDegrees(Math.atan2(toward.y,toward.horizontalDistance())));
    }

    private void chargeParticles(ServerLevel level) {
        if (axis != null && move == Move.CLAMP) {
            Vec3 side = new Vec3(-axis.z, 0, axis.x).scale(.87);
            Vec3 hands = position().add(0, 1.1, 0).add(axis.scale(.72));
            BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, hands.add(side));
            BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, hands.subtract(side));
        } else if (axis != null && move == Move.PRESS) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, aim.x, aim.y + .12, aim.z, 8, .37, .08, .37, .025);
            BossAttackGeometry.point(level, ParticleTypes.END_ROD, aim.add(0, .12, 0));
        } else if (move == Move.LOB) {
            BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, mouth().add(axis.scale(.4)));
        } else if (move == Move.ADVANCE) {
            Vec3 shoulder = position().add(axis.scale(.5)).add(0, 1.05, 0);
            BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, shoulder);
        } else if (move == Move.CURRENT) {
            Vec3 side = new Vec3(-axis.z, 0, axis.x).scale(.85);
            Vec3 elbows = position().add(0, .92, 0);
            BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, elbows.add(side));
            BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, elbows.subtract(side));
        } else if (move == Move.NODES) {
            for (Vec3 node : nodes) BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, node.add(0, .12, 0));
        } else if (move == Move.RETURN || move == Move.CROSSBARS) {
            Vec3 hand = position().add(axis.scale(.67)).add(0, 1.05, 0);
            BossAttackGeometry.point(level, ParticleTypes.ELECTRIC_SPARK, hand);
        } else {
            var at = mouth();
            if (move == Move.LIFT) at = at.add(0, -.65, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 9, .32, .22, .32, .11);
        }
    }

    private void strike(ServerLevel level, List<ServerPlayer> fighters, int tick) {
        Vec3 mouth = mouth();
        if (tick == 0 && customPose(move) == CLEAR_POSE) level.broadcastEntityEvent(this, (byte)4);
        switch (move) {
            case CANNON -> {
                if (tick == 0) {
                    faceDirection(aim.subtract(mouth));
                    level.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 2, .7f);
                    beam(level, fighters, mouth, aim, 1.05, 16);
                }
            }
            case SWEEP -> {
                Vec3 direction = sweepDirection(tick & ~1);
                faceDirection(direction);
                if (tick % 2 == 0) beam(level, fighters, mouth, mouth.add(direction.scale(18)), .52, 9);
            }
            case PULSE -> {
                if (tick % 2 == 0) pulse(level, fighters, .9 + tick * .58);
            }
            case CLAMP -> {
                if (tick == 0 || tick == 2 || tick == 4) clamp(level, fighters, 1.85 - tick * .17);
            }
            case PRESS -> {
                if (tick == 0) press(level, fighters);
            }
            case LOB -> {
                if (tick == 0) {
                    Vec3 launch = charge.release(level, this, mouth);
                    if (launch == null) {
                        resetAttack(level);
                        return;
                    }
                    faceDirection(launch);
                    level.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1, .84f);
                }
                if (!charge.advance(level, this, fighters)) age = move.warning + move.active - 1;
            }
            case ADVANCE -> {
                if (!RedstoneGolemAdvance.move(level, this, fighters, axis)) age = move.warning + move.active - 1;
                else BossArenaPortals.holdFollowing(level, this, RADIUS, HEIGHT);
            }
            case CURRENT -> {
                if (!current.advance(level, this, fighters)) age = move.warning + move.active - 1;
            }
            case LIFT -> {
                Vec3 direction = RedstoneGolemPatterns.risingBeam(axis, (tick & ~1) / 10.0);
                faceDirection(direction);
                if (tick % 2 == 0) beam(level, fighters, mouth, mouth.add(direction.scale(17)), .48, 9);
            }
            case NODES -> {
                if (tick % 5 == 0) {
                    Vec3 node = nodes.get(tick / 5);
                    face(node.add(0, .4, 0));
                    RedstoneGolemPatterns.discharge(level, this, fighters, mouth, node);
                }
            }
            case RETURN -> {
                if (!returnCharge.tick(level, this, fighters)) age = move.warning + move.active - 1;
            }
            case CROSSBARS -> {
                int upper = RedstoneGolemCrossbars.UPPER_DELAY;
                if (tick >= 3 && tick <= upper) {
                    Vec3 low = crossbars.lookAt(false).subtract(mouth);
                    Vec3 high = crossbars.lookAt(true).subtract(mouth);
                    faceDirection(low.lerp(high, (tick - 3.0) / (upper - 3)));
                }
                if (tick == 0 || tick == upper) crossbars.strike(level, this, fighters, mouth, tick == upper);
            }
        }
    }

    private Vec3 clampAxis(ServerLevel level, ServerPlayer victim) {
        Vec3 delta = victim.position().subtract(position());
        double reach = delta.horizontalDistance();
        if (reach < 2.1 || reach > 3.8 || Math.abs(delta.y) > 1.1) return null;
        Vec3 toward = BossAttackGeometry.flatDirection(delta);
        double turn = Math.asin(1.3 / reach);
        for (double offset : new double[]{turn, -turn}) {
            Vec3 forward = BossAttackGeometry.spread(toward, offset);
            Vec3 center = position().add(forward.scale(reach * Math.cos(offset)));
            Vec3 towardGap = BossAttackGeometry.flatDirection(center.subtract(victim.position()));
            if (DragonCombat.escape(level, victim, towardGap, 1.3, 1.35)) return forward;
        }
        return null;
    }

    private void clamp(ServerLevel level, List<ServerPlayer> fighters, double offset) {
        Vec3 chest = position().add(0, 1, 0);
        Vec3 side = new Vec3(-axis.z, 0, axis.x).scale(offset);
        for (Vec3 arm : List.of(side, side.reverse())) {
            Vec3 from = chest.add(arm).add(axis.scale(.8));
            if (!BossAttackGeometry.visible(level, this, chest, from)) continue;
            Vec3 end = BossAttackGeometry.shorten(level, this, from, from.add(axis.scale(2.8)));
            BossAttackGeometry.line(level, ParticleTypes.ELECTRIC_SPARK, from, end, .19);
            BossAttackGeometry.line(level, ParticleTypes.CRIT, from, end, .34);
            for (var player : fighters) {
                var box = player.getBoundingBox();
                if (BossAttackGeometry.hits(box, from, end, .45)
                        && BossAttackGeometry.visible(level, this, chest, box.getCenter())) hurtPlayer(level, player, 11);
            }
        }
    }

    private Vec3 pressTarget(ServerLevel level, ServerPlayer victim) {
        if (distanceToSqr(victim) > 4.25 * 4.25 || Math.abs(victim.getY() - getY()) > 1.2) return null;
        Vec3 ground = BossAttackGeometry.ground(level, this, victim.position().add(0, .3, 0), .8);
        if (ground == null || !BossAttackGeometry.visible(level, this, mouth(), ground.add(0, .15, 0))) return null;
        Vec3 forward = BossAttackGeometry.flatDirection(ground.subtract(position()));
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        if (side.lengthSqr() < .001) return null;
        return DragonCombat.escape(level, victim, side, 1.45, 1.5)
            || DragonCombat.escape(level, victim, side.reverse(), 1.45, 1.5) ? ground : null;
    }

    private void press(ServerLevel level, List<ServerPlayer> fighters) {
        Vec3 ground = BossAttackGeometry.ground(level, this, aim.add(0, .15, 0), .3);
        if (ground == null || ground.distanceToSqr(aim) > .005) return;
        Vec3 base = ground.add(0, .08, 0);
        if (!BossAttackGeometry.visible(level, this, mouth(), base)) return;
        Vec3 tip = BossAttackGeometry.shorten(level, this, base, base.add(0, 1.35, 0));
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.25f, .8f);
        BossAttackGeometry.line(level, ParticleTypes.END_ROD, base, tip, .2);
        BossAttackGeometry.ring(level, ParticleTypes.ELECTRIC_SPARK, base, .62, 16);
        for (var player : fighters) {
            var box = player.getBoundingBox();
            if (box.maxY < base.y - .12 || box.minY > tip.y) continue;
            double x = Math.max(box.minX, Math.min(base.x, box.maxX));
            double z = Math.max(box.minZ, Math.min(base.z, box.maxZ));
            if (base.distanceToSqr(x, base.y, z) <= .62 * .62
                    && BossAttackGeometry.visible(level, this, mouth(), box.getCenter())) hurtPlayer(level, player, 11);
        }
    }

    boolean isClamping() {
        return attackPose() == CLAMP_POSE;
    }

    float attackPoseAge(float partialTick) {
        long snapshot = entityData.get(ATTACK_POSE);
        int length = switch ((byte)snapshot) {
            case CLAMP_POSE -> 25;
            case PRESS_POSE -> 28;
            case CURRENT_POSE -> 45;
            case LIFT_POSE -> 38;
            case NODES_POSE -> 40;
            case ADVANCE_POSE -> 28;
            case LOB_POSE -> 57;
            case RETURN_POSE -> 44;
            case CROSSBARS_POSE -> 36;
            default -> 0;
        };
        if (length == 0) return -1;
        long startedAt = snapshot >>> Byte.SIZE;
        double elapsed = Math.max(0, level().getGameTime() - startedAt + (double)partialTick);
        return elapsed < length ? (float)elapsed : -1;
    }

    byte attackPose() {
        return entityData.get(ATTACK_POSE).byteValue();
    }

    private void beam(ServerLevel level,List<ServerPlayer> players,Vec3 from,Vec3 aim,double width,float damage) {
        Vec3 to=BossAttackGeometry.shorten(level,this,from,aim.add(aim.subtract(from).normalize().scale(1.2)));
        BossAttackGeometry.line(level,ParticleTypes.ELECTRIC_SPARK,from,to,.36);
        BossAttackGeometry.line(level,ParticleTypes.END_ROD,from,to,1.2);
        for(var player:players) if(BossAttackGeometry.hits(player.getBoundingBox(),from,to,width)
                && BossAttackGeometry.visible(level,this,from,player.getBoundingBox().getCenter())) hurtPlayer(level,player,damage);
    }

    private void pulse(ServerLevel level,List<ServerPlayer> players,double radius) {
        var at=position().add(0,.18,0);
        BossAttackGeometry.ring(level,ParticleTypes.ELECTRIC_SPARK,at,radius,30);
        for(var player:players) {
            Vec3 offset=player.position().subtract(at);
            if(Math.abs(offset.y)>.9 || Math.abs(offset.horizontalDistance()-radius)>.9
                    || !BossAttackGeometry.visible(level,this,at,player.position().add(0,.2,0))) continue;
            hurtPlayer(level,player,11);
        }
    }

    void hurtPlayer(ServerLevel level,ServerPlayer player,float damage) {
        if(!hit.contains(player.getUUID()) && player.hurt(level.damageSources().mobAttack(this),damage)) hit.add(player.getUUID());
    }

    private boolean isFighter(ServerPlayer player) {
        return player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&!Incapacitation2123.incapacitated(player)
            && BossArenaPortals.withinCylinder(position(),player.position(),RADIUS*RADIUS,HEIGHT);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source,float amount) {
        boolean damaged=super.hurt(source,amount);
        if (damaged && source.getEntity() instanceof ServerPlayer player && isFighter(player)) {
            contributors.add(player.getUUID());
            if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player && inMeleeRange(player)) {
                meleeAttacker = player.getUUID();
                meleeHitAt = level().getGameTime();
            }
        }
        return damaged;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent) {
        if(paid || !(level() instanceof ServerLevel level))return;
        paid=true;
        if(source.getEntity() instanceof ServerPlayer player && isFighter(player))contributors.add(player.getUUID());
        BossRewardDelivery.settle(level, getUUID(), contributors, this::isFighter, (owner, offline) -> victoryRewards());
    }

    boolean canRecordRewardDeparture(ServerPlayer player) {
        return !paid && contributors.contains(player.getUUID()) && isFighter(player);
    }

    private List<ItemStack> victoryRewards() {
        var rewards = new ArrayList<ItemStack>(5);
        var random = getRandom();
        rewards.add(new ItemStack(RedstoneGolemContent.STAR.get()));
        rewards.add(new ItemStack(Items.REDSTONE, 8 + random.nextInt(9)));
        rewards.add(new ItemStack(random.nextBoolean() ? Items.REPEATER : Items.COMPARATOR, 1 + random.nextInt(2)));
        if (random.nextBoolean()) rewards.add(new ItemStack(Items.REDSTONE_BLOCK));
        if (random.nextInt(4) == 0) rewards.add(new ItemStack(random.nextBoolean() ? Items.PISTON : Items.DISPENSER));
        return rewards;
    }

    private void release(ServerLevel level) {
        BossArenaPortals.release(level,getUUID());
        music.stop(getUUID());
        bar.removeAllPlayers();
    }

    @Override public void die(DamageSource source) {
        super.die(source);
        if (!isAlive() && level() instanceof ServerLevel level) {
            release(level);
            presence.release(level, getUUID());
        }
    }

    @Override public void remove(RemovalReason reason) {
        if(level() instanceof ServerLevel level) {
            release(level);
            presence.release(level, getUUID());
            if (reason.shouldDestroy()) BossRewardDelivery.close(level, getUUID());
        }
        super.remove(reason);
    }

    @Override public boolean canChangeDimensions() { return false; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        presence.retreat().save(tag, ABSENCE);
        tag.putLong("MiteRedstoneIntro",introStarted);
        tag.putBoolean("MiteRedstonePaid",paid);
        var list=new net.minecraft.nbt.ListTag();
        contributors.stream().sorted().forEach(id->{
            var row=new CompoundTag();
            row.putUUID("Player",id);
            list.add(row);
        });
        tag.put("MiteRedstoneContributors",list);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        presence.retreat().load(tag, ABSENCE, RETREAT_GRACE);
        introStarted=tag.contains("MiteRedstoneIntro")?tag.getLong("MiteRedstoneIntro"):-1;
        paid=tag.getBoolean("MiteRedstonePaid");
        contributors.clear();
        var list=tag.getList("MiteRedstoneContributors",net.minecraft.nbt.Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)if(list.getCompound(i).hasUUID("Player"))contributors.add(list.getCompound(i).getUUID("Player"));
    }
}
