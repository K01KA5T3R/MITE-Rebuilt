package dev.koikaster.mite;
/** source Shadow daylight destruction, dark healing and concrete nearby-light disable owner. */
public final class OriginalShadowLife {
 private final Mobs.Undead mob;
 public OriginalShadowLife(Mobs.Undead mob){this.mob=mob;}
 public void beforeAi(){if(!mob.level().isClientSide){if(SourceSpecialSpawns.sourceDay(mob.level())&&mob.level().canSeeSky(mob.blockPosition())&&!mob.level().isRaining())mob.hurt(OriginalElementalDamage.sunlight(mob),1000);else if(mob.tickCount%40==0){int heal=(int)((.4f-OriginalWeather.brightness(mob))*10);if(heal>0)mob.heal(heal);}}OriginalTorchInteraction.disable(mob);}
}
