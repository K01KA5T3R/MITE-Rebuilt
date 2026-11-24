package dev.koikaster.mite;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/** EntityAIMoveToRepairItem source gates, pending real owner/pickup state wiring. */
public final class OriginalMoveToRepairGoal extends OriginalMoveToItemGoal {
 public interface Owner {int pickupCooldown();boolean isRepairItem(ItemStack stack);}
 private final Owner owner;
 public OriginalMoveToRepairGoal(PathfinderMob mob,Owner owner,float speed,boolean swim){super(mob,speed,swim);this.owner=owner;}
 protected boolean interestedIn(ItemStack stack){return owner.isRepairItem(stack);}
 protected boolean willPickUp(ItemStack stack){return owner.isRepairItem(stack);}
 public boolean canUse(){if(owner.pickupCooldown()>0)return false;if(mob.getRandom().nextInt(40)>0)return false;if(mob.getHealth()/mob.getMaxHealth()>=1)return false;return super.canUse();}
 public boolean canContinueToUse(){return owner.pickupCooldown()<=0&&super.canContinueToUse();}
}
