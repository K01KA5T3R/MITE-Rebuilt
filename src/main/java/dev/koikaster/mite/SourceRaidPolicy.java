package dev.koikaster.mite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Native patrols supply their leader's banner. Village raids remain disabled. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceRaidPolicy {
 public static boolean suppressNative(ServerPlayer player){player.removeEffect(MobEffects.BAD_OMEN);return true;}
 public static void stopNative(Raid raid){raid.stop();}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void omen(MobEffectEvent.Applicable e){if(e.getEntity() instanceof Player&&e.getEffectInstance().getEffect()==MobEffects.BAD_OMEN)e.setResult(Event.Result.DENY);}
 @SubscribeEvent public static void player(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.START&&!e.player.level().isClientSide)e.player.removeEffect(MobEffects.BAD_OMEN);}
 private SourceRaidPolicy(){}
}
