package dev.koikaster.mite;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;

final class MythrilRavager extends Ravager {
    private static final EntityDataAccessor<Integer> ATTACK_POSE = SynchedEntityData.defineId(
        MythrilRavager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> POSE_START = SynchedEntityData.defineId(
        MythrilRavager.class, EntityDataSerializers.LONG);
    private static final MythrilKnightCombat.Move[] MOVES = MythrilKnightCombat.Move.values();

    final BossEncounterPresence encounterPresence = new BossEncounterPresence(2);
    final MythrilKnightCombat combat = new MythrilKnightCombat();

    MythrilRavager(EntityType<? extends Ravager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK_POSE, 0);
        entityData.define(POSE_START, 0L);
    }

    @Override
    protected void updateControlFlags() {
        if (!(getFirstPassenger() instanceof MythrilPillager)) {
            super.updateControlFlags();
            return;
        }
        goalSelector.setControlFlag(Goal.Flag.MOVE, true);
        goalSelector.setControlFlag(Goal.Flag.JUMP, !(getVehicle() instanceof Boat));
        goalSelector.setControlFlag(Goal.Flag.LOOK, true);
    }

    void showAttack(MythrilKnightCombat.Move move, int side, long now) {
        entityData.set(POSE_START, now);
        entityData.set(ATTACK_POSE, (move.ordinal() + 1) * side);
    }

    void clearAttackPose() {
        entityData.set(ATTACK_POSE, 0);
    }

    MythrilKnightCombat.Move attackPose() {
        int index = Math.abs(entityData.get(ATTACK_POSE)) - 1;
        return index >= 0 && index < MOVES.length ? MOVES[index] : null;
    }

    int attackSide() {
        return Integer.signum(entityData.get(ATTACK_POSE));
    }

    float attackPoseAge(float partialTick) {
        return level().getGameTime() - entityData.get(POSE_START) + partialTick;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() { return false; }
}
