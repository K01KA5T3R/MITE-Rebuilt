package dev.koikaster.mite;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
/** of's actual damaged-result coupling, shared by source living entities rather than a generic attack event. */
public final class OriginalCreatureVitals {
 public static void damaged(boolean affected,Object actor,Object rawSource){if(!affected)return;var entity=(LivingEntity)actor;if(entity.level().isClientSide||!ClassicDefense.scoped(entity))return;var source=(DamageSource)rawSource;if(source.getEntity() instanceof Player)OriginalDespawn.refresh(entity,-9600);if(source.is(DamageTypes.CACTUS))entity.getPersistentData().putLong("MiteLastCactusHarm",entity.level().getGameTime());}
 private OriginalCreatureVitals(){}
}
