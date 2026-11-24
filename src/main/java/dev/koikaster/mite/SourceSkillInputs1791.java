package dev.koikaster.mite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
/** Manual source-container admission only; automation and running processes stay independent. */
public final class SourceSkillInputs1791 {
 public static boolean furnace(Player player,ItemStack stack,int mode){return mode!=ModernCooking.CLASSIC||SourceSkills1685.permits(player,SourceSkillFacts1710.smelting(stack).skillsets());}
 public static boolean brewing(Player player){return !SourceSkills1685.enabled(player.level())||SourceSkills1685.has(player,SourceSkillRules1685.BREWING);}
 private SourceSkillInputs1791(){}
}
