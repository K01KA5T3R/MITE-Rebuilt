package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/** Preserve the exact owned actor while vanilla releases a stored shoulder pet. */
final class LegacyShoulderParrot {
    static boolean keeps(EntityJoinLevelEvent event, ServerLevel level, WorldExtraSettings settings) {
        if (event.loadedFromDisk() || !settings.classicVisuals()
                || event.getEntity().getType() != EntityType.PARROT
                || !(event.getEntity() instanceof Parrot bird)) return false;
        UUID ownerId = bird.getOwnerUUID();
        if (ownerId == null || !bird.isTame()) return false;
        var owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != level) return false;
        return matches(owner.getShoulderEntityLeft(), bird.getUUID())
            || matches(owner.getShoulderEntityRight(), bird.getUUID());
    }

    private static boolean matches(CompoundTag shoulder, UUID actorId) {
        return "minecraft:parrot".equals(shoulder.getString("id"))
            && shoulder.hasUUID("UUID") && actorId.equals(shoulder.getUUID("UUID"));
    }

    private LegacyShoulderParrot() {}
}
