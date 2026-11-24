package dev.koikaster.mite;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
/** of.d permits effects on biologically living dragons; skip modern boss rejection. */
public interface SourceDragonEffectParent {boolean mite$dragonMobEffect(MobEffectInstance effect,Entity source);}
