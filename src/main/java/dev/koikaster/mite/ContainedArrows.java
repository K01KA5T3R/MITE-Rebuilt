package dev.koikaster.mite;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** of.tryAddArrowToContainedItems: current held enchant, then material recovery, drop on death. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ContainedArrows {
 private static final String KEY="MiteContainedArrows";
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void struck(LivingHurtEvent event){
  if(event.isCanceled()||event.getEntity().level().isClientSide||!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)||arrow.pickup!=AbstractArrow.Pickup.ALLOWED||!(arrow.getOwner() instanceof Player owner))return;
  var victim=event.getEntity();if(victim instanceof SourceEnderman)return;var random=victim.getRandom();var original=ClassicArchery.pickup(new ItemStack(Items.ARROW),arrow);var stack=(ItemStack)original;if(!ClassicToolCatalogue.sourceItem(stack))return;
  if(victim instanceof SourceGelatinousCube cube&&cube.profile().acidic()&&SourceChemicalMaterials.item(stack,true)){cube.chemicalArrowFx(arrow);return;}
  // V065 retains native piercing: the projectile passes through bodies and is
  // still recoverable from the world. Lodging a copy per hit would create new
  // mineral stock (N018). Native onHitEntity also skips embedded-arrow count.
  if(arrow.getPierceLevel()>0)return;
  boolean retained=random.nextInt(5)<EnchantmentEffects.level("arrow_recovery",owner.getMainHandItem());if(!retained){String material=stack.is(Items.ARROW)?"flint":ClassicToolCatalogue.material(stack);retained=random.nextFloat()<ClassicToolCatalogue.arrowRecovery(material);}if(retained){var data=victim.getPersistentData();var list=data.getList(KEY,Tag.TAG_COMPOUND);stack.setCount(1);list.add(stack.save(new CompoundTag()));data.put(KEY,list);}
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void drops(LivingDropsEvent event){var actor=event.getEntity();var data=actor.getPersistentData();var list=data.getList(KEY,Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var stack=ItemStack.of(list.getCompound(i));if(!stack.isEmpty()){if(actor instanceof net.minecraft.world.entity.Mob)MineralSupply.stock(stack);event.getDrops().add(new ItemEntity(actor.level(),actor.getX(),actor.getY()+actor.getBbHeight()/2,actor.getZ(),stack));}}data.remove(KEY);}
 private ContainedArrows(){}
}
