package dev.koikaster.mite;

/** og.a -> tw.a -> tn.a. The entity supplies the region factor for the world mode.
 * The leaf deliberately returns its input, discarding tw's new group record. */
public final class SourcePigmanParentSpawn {
 public interface Host {
  double nextGaussian();float nextFloat();double nextDouble();float worldNextFloat();
  void followBonus(double amount);void lootPickup(boolean value);void equipment();boolean headEmpty();
  boolean halloween();void pumpkin(boolean lit);void headDropChance(float value);
  void knockbackBonus(double amount);void reinforcementBonus(double amount);void maximumHealthBonus(double amount);
  void villager(boolean value);void baby(boolean value);
 }
 /** Non-null source ty input is represented by these two flags. Other group
  * types pass through unchanged, just as tw's instanceof check does. */
 public interface ZombieGroup {boolean baby();boolean villager();}
 public static Object initialize(Host host,Object input){
  return initialize(host,input,1f);
 }
 static Object initialize(Host host,Object input,float localFactor){
  host.followBonus(host.nextGaussian()*.05);
  host.lootPickup(true);
  Object parentGroup=input;
  if(parentGroup==null)parentGroup=new Group(host.worldNextFloat()<.05f,host.worldNextFloat()<.05f);
  if(parentGroup instanceof ZombieGroup group){if(group.villager())host.villager(true);if(group.baby())host.baby(true);}
  host.equipment();
  if(host.headEmpty()&&host.halloween()&&host.nextFloat()<.25f){host.pumpkin(host.nextFloat()<.1f);host.headDropChance(0);}
  host.knockbackBonus(host.nextDouble()*(double).05f);
  if(host.nextFloat()<localFactor*.05f){host.reinforcementBonus(host.nextDouble()*.25+.5);host.maximumHealthBonus(host.nextDouble()*3+1);}
  host.villager(false);
  return input;
 }
 private record Group(boolean baby,boolean villager) implements ZombieGroup {}
 private SourcePigmanParentSpawn(){}
}
