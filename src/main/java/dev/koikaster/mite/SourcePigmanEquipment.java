package dev.koikaster.mite;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Applicable ye/og equipment branches plus restored 0.4.16 mob poor-quality penalty helper. */
public final class SourcePigmanEquipment {
 public interface Random {float nextFloat();}
 public interface Stack {
  boolean tool();boolean bow();boolean armor();boolean enchanted();boolean tagged();
  boolean damageable();int damage();int maximum();void damage(int value);
  float combinedToolDamage();float multipliedProtection();
 }
 public interface Drops extends Random {
  int slots();Stack item(int slot);float chance(int slot);boolean massiveFall();boolean pickedUpHeld();
  void dropAndClear(int slot);
 }
 public static void wear(Stack stack,Random random){
  if(stack.damageable()){
   float fraction=(stack.enchanted()?.5f:.7f)+.3f*random.nextFloat();
   int damage=(int)((float)stack.maximum()*fraction);
   if(damage>=stack.maximum())damage=stack.maximum()-1;
   stack.damage(damage);
  }
 }
 public static void applyRandomQuality(ItemStack stack,RandomSource random){
  ClassicQuality.applyMobQuality(stack,random);
 }
 public static boolean replace(Stack ground,Stack current,int slot){
  if(current==null)return true;
  if(slot==0){
   if(current.bow())return ground.bow()&&!current.enchanted()&&(ground.enchanted()||ground.damage()<current.damage());
   if(ground.tool()&&!current.tool())return true;
   if(ground.tool()&&current.tool()){
    if(ground.combinedToolDamage()==current.combinedToolDamage())return !current.enchanted()&&(ground.enchanted()||ground.damage()<current.damage()||ground.tagged()&&!current.tagged());
    return ground.combinedToolDamage()>current.combinedToolDamage();
   }
   return false;
  }
  if(ground.armor()&&!current.armor())return true;
  if(ground.armor()&&current.armor()){
   if(ground.multipliedProtection()==current.multipliedProtection())return ground.enchanted()&&!current.enchanted()||ground.damage()<current.damage()||ground.tagged()&&!current.tagged();
   return ground.multipliedProtection()>current.multipliedProtection();
  }
  return false;
 }
 public static void drops(Drops host,boolean recent,int looting){
  for(int slot=0;slot<host.slots();slot++){
   Stack stack=host.item(slot);boolean guaranteed=host.chance(slot)>1f;
   if(stack==null)continue;
   boolean held=slot==0;
   if(host.massiveFall()&&host.chance(slot)<1f&&host.nextFloat()<.9f&&(!held||!host.pickedUpHeld()))continue;
   if((!held||!recent&&!host.pickedUpHeld()&&!(host.nextFloat()<.05f))&&(!recent&&!guaranteed||!(host.nextFloat()-(float)looting*.01f<host.chance(slot))))continue;
   host.dropAndClear(slot);
  }
 }
 private SourcePigmanEquipment(){}
}
