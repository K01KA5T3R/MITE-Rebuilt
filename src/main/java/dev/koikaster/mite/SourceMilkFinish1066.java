package dev.koikaster.mite;
import net.minecraft.world.entity.LivingEntity;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;
/** Native milk effects use the same server-only ordering as yi. */
public final class SourceMilkFinish1066 {
 public static boolean nativeEffects(Object entity,Object item){
  var actor=(LivingEntity)entity;
  return actor instanceof Player?ownEffects(actor):actor.curePotionEffects((ItemStack)item);
 }
 public static boolean ownEffects(LivingEntity actor){
  boolean[] removed={false};SourceUseOrder1066.milk(new SourceUseOrder1066.Milk(){public boolean server(){return !actor.level().isClientSide;}public void clearEffects(){removed[0]=actor.removeAllEffects();}public void addFood(){SurvivalFood.drinkMilk(actor);}});return removed[0];
 }
 private SourceMilkFinish1066(){}
}
