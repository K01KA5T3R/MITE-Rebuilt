package dev.koikaster.mite;

/** tw.addRandomWeapon/og.bw. Human58D fixes the weapon pool at its initial
 * day instead of unlocking stronger weapons as days pass; tension is1. */
public final class SourceZombieEquipment {
 public enum Weapon {WOOD_SHOVEL,RUSTED_SHOVEL,WOOD_CUDGEL,WOOD_CLUB,RUSTED_SWORD,RUSTED_DAGGER,RUSTED_SHEARS,RUSTED_SCYTHE}
 public interface Host {
  boolean villager();boolean hard();float nextFloat();int nextInt(int bound);
  void weapon(Weapon item,boolean mayEnchant);boolean armorEmpty(int sourceSlot);void armor(int sourceSlot,int material,boolean mayEnchant);
 }
 public static void weapon(Host h){
  if(h.nextFloat()<(h.villager()?.2f:.05f)){
   int choice=h.nextInt(h.villager()?5:7);
   Weapon item=choice==0?Weapon.WOOD_SHOVEL:choice<3?Weapon.RUSTED_SHOVEL:h.villager()?(choice==3?Weapon.RUSTED_SHEARS:Weapon.RUSTED_SCYTHE):switch(choice){case 3->Weapon.WOOD_CUDGEL;case 4->Weapon.WOOD_CLUB;case 5->Weapon.RUSTED_SWORD;default->Weapon.RUSTED_DAGGER;};
   h.weapon(item,true);
  }
 }
 public static void armor(Host h){
  if(h.nextFloat()<.15f){
   int material=h.nextInt(2);float stop=h.hard()?.1f:.25f;
   for(int i=0;i<3;i++)if(h.nextFloat()<.095f)material++;
   for(int slot=4;slot>=1;slot--){
    if(slot<4&&h.nextFloat()<stop)break;
    if(!h.armorEmpty(slot))continue;
    h.armor(slot,material,true);
   }
  }
 }
 public static void equipment(Host h){weapon(h);armor(h);}
 private SourceZombieEquipment(){}
}
