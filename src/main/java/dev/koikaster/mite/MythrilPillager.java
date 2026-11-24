package dev.koikaster.mite;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.level.Level;

final class MythrilPillager extends Pillager {
    final BossEncounterPresence encounterPresence = new BossEncounterPresence(2);
    final MythrilKnightCombat combat = new MythrilKnightCombat();

    MythrilPillager(EntityType<? extends Pillager> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        // The encounter owns the crossbow's timing and projectile.
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public IllagerArmPose getArmPose() {
        return isAggressive() && !isChargingCrossbow() ? IllagerArmPose.ATTACKING : super.getArmPose();
    }

    @Override
    public boolean canChangeDimensions() { return false; }
}
