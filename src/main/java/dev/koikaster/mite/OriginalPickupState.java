package dev.koikaster.mite;

import net.minecraft.sounds.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;

/** og.c source consumption order/state. Concrete food owners connect this in0.26.
 * Policy.allowed must implement ss.canBePickedUpBy (delay/fire/carriable/physical ray),
 * retainRemainder must set the native item entity's signed age to-18000 via SourceItemAge.
 * Valuable containment stores a single item identity, as source addContainedItem(Item).
 * The cooldown is transient; source does not put it in Mob NBT.
 */
public final class OriginalPickupState {
 public interface Policy {
  boolean allowed(ItemEntity item);boolean willEat(ItemStack stack);void ate(ItemStack stack);
  boolean willRepair(ItemStack stack);void repaired(ItemStack stack);
  boolean willTakeAsValuable(ItemStack stack);void contain(Item item);void retainRemainder(ItemEntity item);
 }
 private final Mob mob;private final Policy policy;public int cooldown;
 public OriginalPickupState(Mob mob,Policy policy){this.mob=mob;this.policy=policy;}
 public void tick(){
  if(mob.level().isClientSide||mob.isRemoved()||mob.getHealth()<=0)return;
  boolean refrainFromFood=mob instanceof TamableAnimal tame&&tame.isTame()&&mob.getHealth()/mob.getMaxHealth()>.5f;
  if(cooldown>0){--cooldown;return;}
  for(var item:mob.level().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(1,.25,1))){
   if(item.isRemoved()||!policy.allowed(item))continue;var stack=item.getItem();boolean valuable=false;
   if(!refrainFromFood&&policy.willEat(stack))policy.ate(stack);
   else if(policy.willRepair(stack))policy.repaired(stack);
   else if(policy.willTakeAsValuable(stack)){policy.contain(stack.getItem());valuable=true;}
   else continue;
   cooldown=valuable?40:400;var remaining=stack.copy();remaining.shrink(1);
   mob.playSound(SoundEvents.ITEM_PICKUP,.2f,((mob.getRandom().nextFloat()-mob.getRandom().nextFloat())*.7f+1)*2);
   if(remaining.isEmpty())item.discard();else{item.setItem(remaining);policy.retainRemainder(item);}
   OriginalDespawn.refresh(mob,-2400);break;
  }
 }
}
