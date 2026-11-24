package dev.koikaster.mite;
import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;
/** va gates actual repair/combine only, not renaming or book/disenchant operations. */
public final class SourceSkillRepair1710 {
 public static ClassicRepair.Result result(Player owner,ItemStack first,ItemStack second,String anvil,String name){
  var level=owner.level();
  if(!WorldContentEvents.itemActionAllowed(level,first)||!WorldContentEvents.itemActionAllowed(level,second))return ClassicRepair.Result.empty();
  var result=ClassicRepair.result(first,second,anvil,name,WorldContentMode.r196(level));if(!SourceSkills1685.enabled(owner.level())||!(result.operation().equals("repair")||result.operation().equals("combine")))return result;var decision=SourceSkillFacts1710.repair(first);return decision.known()&&!SourceSkillRules1685.alternatives(SourceSkills1685.bits(owner),decision.skillsets())?ClassicRepair.Result.empty():result;}
 private SourceSkillRepair1710(){}
}
