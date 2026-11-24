package dev.koikaster.mite;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 player death orbs belong to the player who lost them. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class ClassicDeathExperience {
    private static final String OWNER = "MiteClassicDeathOrbOwner";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || WorldContentMode.rebuilt(player.level())) return;
        var level = player.serverLevel();
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        int amount = PlayerState.data(player).getInt("Xp") / 3;
        while (amount > 0) {
            int value = ExperienceOrb.getExperienceValue(amount);
            var orb = new ExperienceOrb(level, player.getX(), player.getY() + .5, player.getZ(), value);
            orb.getPersistentData().putUUID(OWNER, player.getUUID());
            if (orb instanceof WorldContentMode.OrbAgeAccess age) age.mite$extendClassicDeathAge();
            level.addFreshEntity(orb);
            amount -= value;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pickup(PlayerXpEvent.PickupXp event) {
        var data = event.getOrb().getPersistentData();
        if (data.hasUUID(OWNER) && !event.getEntity().getUUID().equals(data.getUUID(OWNER))) event.setCanceled(true);
    }

    public static boolean ordinaryOrb(ExperienceOrb orb) {
        return !orb.getPersistentData().hasUUID(OWNER);
    }

    private ClassicDeathExperience() {}
}
