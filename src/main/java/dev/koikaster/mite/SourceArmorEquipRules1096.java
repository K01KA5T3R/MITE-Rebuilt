package dev.koikaster.mite;

/** wh.onItemRightClick; the existing native curse prefix owns mayWear in the adapter. */
public final class SourceArmorEquipRules1096 {
 public interface Host<S> {
  boolean mayWear();S held();int equipmentPosition(S stack);S equipped(int index);
  boolean server();S copy(S stack);void equip(int index,S stack);void convertHeld();void suppressStat();
 }
 public static <S> boolean use(Host<S> player){
  if(!player.mayWear())return false;
  S itemStack=player.held();int index=player.equipmentPosition(itemStack)-1;S worn=player.equipped(index);
  if(worn==null){
   if(player.server()){
    player.equip(index,player.copy(itemStack));
    player.convertHeld();
    player.suppressStat();
   }
   return true;
  }
  return false;
 }
 private SourceArmorEquipRules1096(){}
}
