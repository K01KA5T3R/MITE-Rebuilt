package dev.koikaster.mite;
/** og.a -> tw.a without tn's final clear/return-input leaf. The already sealed
 * parent RNG/order is retained; the ordinary owner keeps the new group. */
public final class SourceZombieSpawn {
 public record Group(boolean baby,boolean villager) implements SourcePigmanParentSpawn.ZombieGroup {}
 public static Object initialize(SourcePigmanParentSpawn.Host h,Object input){
  return initialize(h,input,1f);
 }
 static Object initialize(SourcePigmanParentSpawn.Host h,Object input,float localFactor){
  h.followBonus(h.nextGaussian()*.05);h.lootPickup(true);Object group=input;
  if(group==null)group=new Group(h.worldNextFloat()<.05f,h.worldNextFloat()<.05f);
  if(group instanceof SourcePigmanParentSpawn.ZombieGroup flags){if(flags.villager())h.villager(true);if(flags.baby())h.baby(true);}
  h.equipment();
  if(h.headEmpty()&&h.halloween()&&h.nextFloat()<.25f){h.pumpkin(h.nextFloat()<.1f);h.headDropChance(0);}
  h.knockbackBonus(h.nextDouble()*(double).05f);
  if(h.nextFloat()<localFactor*.05f){h.reinforcementBonus(h.nextDouble()*.25+.5);h.maximumHealthBonus(h.nextDouble()*3+1);}
  return group;
 }
 private SourceZombieSpawn(){}
}
