package dev.koikaster.mite;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;

/** EntityAIMoveToFoodItem source gates, concrete owner/pickup state is connected in0.26. */
public final class OriginalMoveToFoodGoal extends OriginalMoveToItemGoal {
 public interface Owner {int pickupCooldown();boolean canEat();boolean isFood(ItemStack stack);boolean willEat(ItemStack stack);}
 private final Owner owner;
 public OriginalMoveToFoodGoal(PathfinderMob mob,Owner owner,float speed,boolean swim){super(mob,speed,swim);this.owner=owner;}
 protected boolean interestedIn(ItemStack stack){return owner.isFood(stack);}
 protected boolean willPickUp(ItemStack stack){return owner.willEat(stack);}
 public boolean canUse(){if(owner.pickupCooldown()>0)return false;if(mob.getRandom().nextInt(40)>0)return false;if(!owner.canEat())return false;if(mob instanceof TamableAnimal tame&&tame.isTame()&&(tame.isOrderedToSit()||mob.getHealth()/mob.getMaxHealth()>.5f))return false;return super.canUse();}
 public boolean canContinueToUse(){return owner.pickupCooldown()<=0&&super.canContinueToUse();}
}
