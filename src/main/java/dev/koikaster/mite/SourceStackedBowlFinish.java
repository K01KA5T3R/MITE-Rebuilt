package dev.koikaster.mite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Native one-serving bowl code must preserve the remaining servings when source caps allow four. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceStackedBowlFinish {
 @SubscribeEvent public static void finish(LivingEntityUseItemEvent.Finish event){
  var used=event.getItem();if(event.getEntity().level().isClientSide||!(used.getItem() instanceof BowlFoodItem)||used.getCount()<=1||!event.getResultStack().is(Items.BOWL))return;
  if(event.getEntity() instanceof Player player){if(player.getAbilities().instabuild)return;var empty=new ItemStack(Items.BOWL);if(!player.getInventory().add(empty))player.drop(empty,false);}
  else event.getEntity().spawnAtLocation(Items.BOWL);
  event.setResultStack(used.copyWithCount(used.getCount()-1));
 }
 private SourceStackedBowlFinish(){}
}
