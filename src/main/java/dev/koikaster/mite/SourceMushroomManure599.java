package dev.koikaster.mite;
import net.minecraft.world.InteractionResult;import net.minecraftforge.event.entity.player.PlayerInteractEvent;import net.minecraftforge.eventbus.api.*;import net.minecraftforge.fml.common.Mod;
/** ItemManure's client acceptance and server mutation share geometry, not random consumption. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceMushroomManure599 {
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void use(PlayerInteractEvent.RightClickBlock event){
  if(!event.getItemStack().is(Content.MANURE.get())||event.getUseItem()==Event.Result.DENY||event.getEntity().isSpectator())return;
  var level=event.getLevel();var pos=event.getPos();var host=new SourceMushroomWorld599(level,event.getEntity(),level.isClientSide?null:level.random);boolean top=event.getFace()==net.minecraft.core.Direction.UP;
  boolean accepted=level.isClientSide?SourceManureEligibility947.accept(host,pos.getX(),pos.getY(),pos.getZ(),top):SourceSmallMushroom587.manure(host,pos.getX(),pos.getY(),pos.getZ(),top,host.player(),host.rng());
  if(!accepted)return;event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
  if(!level.isClientSide&&!event.getEntity().isCreative())event.getItemStack().shrink(1);
 }
 private SourceMushroomManure599(){}
}
