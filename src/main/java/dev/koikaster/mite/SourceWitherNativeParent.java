package dev.koikaster.mite;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.damagesource.DamageSource;
/** Bridges live on the direct Monster parent boundary of WitherBoss. */
public interface SourceWitherNativeParent {
 void mite$witherMonsterAi();
 boolean mite$witherMonsterHurt(DamageSource source,float amount);
 ServerBossEvent mite$witherBossEvent();
}
