package dev.koikaster.mite;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Active original re/aci/aps/aqs/tm call sites; no general cap on every spawn or equipment pickup. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceLifecycle {
 public static void targetStarted(Object actor){var mob=(Mob)actor;if(OriginalDespawn.handled(mob))OriginalDespawn.refresh(mob,-1200);}
 public static void spawnedFromSourceBlock(Object actor){if(actor instanceof Mob mob&&OriginalDespawn.handled(mob))OriginalDespawn.refresh(mob,-9600);}
 public static void meleeResult(boolean affected,Object actor,Object victim){if(affected&&actor instanceof Monster mob&&OriginalDespawn.handled(mob)&&!mob.level().isClientSide&&victim instanceof Player player&&player.isAlive())OriginalDespawn.refresh(mob,-9600);}
 @SubscribeEvent public static void naturalWaterSpawn(MobSpawnEvent.FinalizeSpawn event){var mob=event.getEntity();if(event.getSpawnType()==MobSpawnType.NATURAL&&mob instanceof WaterAnimal&&OriginalDespawn.handled(mob))OriginalDespawn.refresh(mob,-9600);}
 private SourceLifecycle(){}
}
