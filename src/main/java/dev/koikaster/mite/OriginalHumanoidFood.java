package dev.koikaster.mite;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
/** Ghoul/Rev ItemMeat owner and actual ss pickup policy. Other repair/valuable species remain separate. */
public final class OriginalHumanoidFood implements OriginalMoveToFoodGoal.Owner,OriginalPickupState.Policy {
 private final Mobs.Undead mob;public final OriginalPickupState state;
 public OriginalHumanoidFood(Mobs.Undead mob){this.mob=mob;state=new OriginalPickupState(mob,this);}
 public int pickupCooldown(){return state.cooldown;}public boolean canEat(){return !mob.isBaby()&&!mob.isRemoved()&&mob.getHealth()>0;}
 public boolean isFood(ItemStack stack){return OriginalMeatItems.includes(stack);}public boolean willEat(ItemStack stack){return isFood(stack)&&canEat();}
 public boolean allowed(ItemEntity item){if(!isFood(item.getItem())||item.hasPickUpDelay()||item.isOnFire())return false;var origin=item.getBoundingBox().getCenter();for(float fraction:new float[]{.25f,.5f,.75f})if(OriginalRaycast.pickupLine(item,origin,mob.position().add(0,mob.getBbHeight()*fraction,0)))return true;return false;}
 public void ate(ItemStack stack){if(mob.getMobType()!=MobType.UNDEAD)mob.heal(mob.getMaxHealth()*.5f);}
 public boolean willRepair(ItemStack stack){return false;}public void repaired(ItemStack stack){}
 public boolean willTakeAsValuable(ItemStack stack){return false;}public void contain(Item item){throw new IllegalStateException("Food-only owner cannot contain valuables");}
 public void retainRemainder(ItemEntity item){((SourceItemAge)item).mite$itemAge(-18000);}
 public void tick(){state.tick();}
}
